package org.ps5jb.loader.jar;

import org.ps5jb.loader.Config;
import org.ps5jb.loader.ProgressUI;
import org.ps5jb.loader.Status;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * JAR loader that executes a fixed sequence of JARs from the disc.
 */
public class SequentialJarLoader implements JarLoader {
    private final String[] payloads;
    private boolean terminated = false;

    public SequentialJarLoader(String[] payloads) {
        this.payloads = payloads;
    }

    @Override
    public void run() {
        File payloadDir = null;
        try {
            payloadDir = Config.getLoaderPayloadPath();
        } catch (Throwable t) {
            Status.println("Initial payload path lookup failed: " + t.getMessage());
        }

        for (int i = 0; i < payloads.length && !terminated; i++) {
            String payloadName = payloads[i];
            String label = "Loading " + payloadName + "...";
            int progress = 10 + (int) (((i + 0.5) / payloads.length) * 30);
            if (payloadName.toLowerCase().indexOf("umtx") != -1) {
                label = "Running kernel exploit...";
                progress = 20;
            } else if (payloadName.toLowerCase().indexOf("bdjb-autoloader") != -1) {
                label = "Initializing autoloader...";
                progress = 40;
            }

            ProgressUI.getInstance().setProgress(progress, label);
            Status.warning(label);

            File jarFile = resolvePayloadJar(payloads[i], payloadDir);
            if (jarFile == null) {
                Status.println("Payload not found on first attempt, waiting for path stabilization: " + payloads[i]);
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    terminated = true;
                    break;
                }
                jarFile = resolvePayloadJar(payloads[i], payloadDir);
            }

            if (jarFile != null) {
                try {
                    payloadDir = jarFile.getParentFile();
                    loadJar(jarFile, false);
                } catch (Throwable e) {
                    Status.printStackTrace("Failed to load " + payloads[i], e);
                    break;
                }
            } else {
                Status.error("Payload not found: " + payloads[i]);
            }
        }
        
        if (!terminated) {
            ProgressUI.getInstance().setProgress(100, "Finished");
            Status.success("Finished");
        }
    }

    private File resolvePayloadJar(String payloadName, File currentPayloadDir) {
        List candidateDirs = buildPayloadDirCandidates(currentPayloadDir);
        for (int i = 0; i < candidateDirs.size(); i++) {
            File jarFile = new File((File) candidateDirs.get(i), payloadName);
            if (jarFile.isFile()) {
                if (currentPayloadDir == null || !candidateDirs.get(i).equals(currentPayloadDir)) {
                    Status.println("Resolved payload from: " + ((File) candidateDirs.get(i)).getAbsolutePath());
                }
                return jarFile;
            }
        }

        File prefixMatch = findJarByArtifactPrefix(payloadName, candidateDirs);
        if (prefixMatch != null) {
            Status.println("Resolved payload by artifact prefix: " + prefixMatch.getAbsolutePath());
            return prefixMatch;
        }

        File deepMatch = deepSearchPayloadJar(payloadName);
        if (deepMatch != null) {
            Status.println("Resolved payload by deep search: " + deepMatch.getAbsolutePath());
            return deepMatch;
        }

        Status.println("Checked " + candidateDirs.size() + " payload directories for " + payloadName);
        for (int i = 0; i < candidateDirs.size() && i < 6; i++) {
            Status.println("  candidate: " + ((File) candidateDirs.get(i)).getAbsolutePath());
        }
        return null;
    }

    private List buildPayloadDirCandidates(File currentPayloadDir) {
        Set ordered = new LinkedHashSet();
        String payloadRoot = Config.getLoaderPayloadRoot();

        if (currentPayloadDir != null) {
            ordered.add(currentPayloadDir.getAbsolutePath());
        }

        try {
            ordered.add(Config.getLoaderPayloadPath().getAbsolutePath());
        } catch (Throwable t) {
            Status.println("Dynamic payload path lookup failed: " + t.getMessage());
        }

        ordered.add("/app0/" + payloadRoot);
        ordered.add("/app0/disc/" + payloadRoot);
        ordered.add("/disc/" + payloadRoot);
        ordered.add("/mnt/disc/" + payloadRoot);
        ordered.add("/mnt/disc0/" + payloadRoot);
        ordered.add("/data/self/interactive/" + payloadRoot);

        addSandboxCandidates(ordered, payloadRoot);

        List result = new ArrayList(ordered.size());
        Iterator orderedIter = ordered.iterator();
        while (orderedIter.hasNext()) {
            result.add(new File((String) orderedIter.next()));
        }
        return result;
    }

    private void addSandboxCandidates(Set ordered, String payloadRoot) {
        File sandboxRoot = new File("/mnt/sandbox");
        File[] sandboxDirs = sandboxRoot.listFiles();
        if (sandboxDirs == null) {
            return;
        }

        for (int i = 0; i < sandboxDirs.length; i++) {
            File sandboxDir = sandboxDirs[i];
            if (!sandboxDir.isDirectory()) {
                continue;
            }

            String sandboxName = sandboxDir.getName();
            if (!sandboxName.endsWith("_000")) {
                continue;
            }

            String base = sandboxDir.getAbsolutePath() + "/app0";
            ordered.add(base + "/" + payloadRoot);
            ordered.add(base + "/disc/" + payloadRoot);

            String sandboxBase = sandboxDir.getAbsolutePath();
            ordered.add(sandboxBase + "/" + payloadRoot);
            ordered.add(sandboxBase + "/disc/" + payloadRoot);
        }
    }

    private File findJarByArtifactPrefix(String payloadName, List candidateDirs) {
        if (payloadName == null) {
            return null;
        }

        String lowerPayloadName = payloadName.toLowerCase();
        if (!lowerPayloadName.endsWith(".jar")) {
            return null;
        }

        String payloadNoExt = payloadName.substring(0, payloadName.length() - 4);
        String artifactPrefix = payloadNoExt;
        int versionDash = findVersionDash(payloadNoExt);
        if (versionDash > 0) {
            artifactPrefix = payloadNoExt.substring(0, versionDash);
        }

        String lowerPrefix = artifactPrefix.toLowerCase();
        File bestMatch = null;
        String bestName = null;

        for (int i = 0; i < candidateDirs.size(); i++) {
            File dir = (File) candidateDirs.get(i);
            String[] names = dir.list();
            if (names == null) {
                continue;
            }

            for (int n = 0; n < names.length; n++) {
                String name = names[n];
                String lowerName = name.toLowerCase();
                if (!lowerName.endsWith(".jar")) {
                    continue;
                }

                if (!lowerName.equals(lowerPrefix + ".jar") && !lowerName.startsWith(lowerPrefix + "-")) {
                    continue;
                }

                if (bestName == null || lowerName.compareTo(bestName) > 0) {
                    bestName = lowerName;
                    bestMatch = new File(dir, name);
                }
            }
        }

        return bestMatch;
    }

    private int findVersionDash(String payloadNoExt) {
        for (int i = 0; i < payloadNoExt.length() - 1; i++) {
            if (payloadNoExt.charAt(i) == '-' && Character.isDigit(payloadNoExt.charAt(i + 1))) {
                return i;
            }
        }
        return -1;
    }

    private File deepSearchPayloadJar(String payloadName) {
        if (payloadName == null || !payloadName.toLowerCase().endsWith(".jar")) {
            return null;
        }

        String payloadNoExt = payloadName.substring(0, payloadName.length() - 4);
        int versionDash = findVersionDash(payloadNoExt);
        String artifactPrefix = versionDash > 0 ? payloadNoExt.substring(0, versionDash) : payloadNoExt;

        String[] rootPaths = new String[] {
            "/app0",
            "/disc",
            "/mnt/disc",
            "/mnt/disc0",
            "/mnt/sandbox",
            "/data/self/interactive"
        };

        for (int i = 0; i < rootPaths.length; i++) {
            File root = new File(rootPaths[i]);
            File match = deepSearchDirectory(root, payloadName, artifactPrefix, 4);
            if (match != null) {
                return match;
            }
        }

        return null;
    }

    private File deepSearchDirectory(File dir, String payloadName, String artifactPrefix, int depthLeft) {
        if (dir == null || depthLeft < 0 || !dir.exists() || !dir.isDirectory()) {
            return null;
        }

        String[] names = dir.list();
        if (names == null) {
            return null;
        }

        String lowerPayloadName = payloadName.toLowerCase();
        String lowerPrefix = artifactPrefix.toLowerCase();
        File prefixMatch = null;

        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            String lowerName = name.toLowerCase();
            if (!lowerName.endsWith(".jar")) {
                continue;
            }

            if (lowerName.equals(lowerPayloadName)) {
                return new File(dir, name);
            }

            if (prefixMatch == null
                    && (lowerName.equals(lowerPrefix + ".jar") || lowerName.startsWith(lowerPrefix + "-"))) {
                prefixMatch = new File(dir, name);
            }
        }

        if (prefixMatch != null) {
            return prefixMatch;
        }

        if (depthLeft == 0) {
            return null;
        }

        for (int i = 0; i < names.length; i++) {
            File child = new File(dir, names[i]);
            if (!child.isDirectory()) {
                continue;
            }

            File nested = deepSearchDirectory(child, payloadName, artifactPrefix, depthLeft - 1);
            if (nested != null) {
                return nested;
            }
        }

        return null;
    }

    @Override
    public void terminate() throws IOException {
        this.terminated = true;
    }
}
