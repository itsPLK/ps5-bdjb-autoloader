package org.ps5jb.client.payloads.autoloader;

import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.ps5jb.sdk.io.File;
import org.ps5jb.sdk.lib.LibKernel;
import org.ps5jb.loader.Status;

final class AutoloadConfigLocator {
    static final String AUTOLOAD_DIR = "ps5_autoloader";
    static final String AUTOLOAD_FILE = "autoload.txt";

    private AutoloadConfigLocator() {
    }

    static File findFirstConfig(LibKernel libKernel) {
        List<String> candidatePaths = candidates();
        Status.println("Checking " + candidatePaths.size() + " config candidates...");
        
        for (String candidatePath : candidatePaths) {
            try {
                File candidate = new File(candidatePath);
                boolean exists = candidate.exists();
                if (exists && candidate.isFile()) {
                    Status.println("Found config: " + candidatePath);
                    return candidate;
                } else if (exists) {
                    Status.println("Candidate exists but is not a file: " + candidatePath);
                }
            } catch (Throwable t) {
                Status.println("Error checking " + candidatePath + ": " + t.getMessage());
            }
        }
        return null;
    }

    static void listDebugDir(LibKernel libKernel, String path) {
        try {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                String[] children = dir.list();
                if (children != null && children.length > 0) {
                    Status.println("Contents of " + path + ":");
                    for (int i = 0; i < Math.min(children.length, 10); i++) {
                        String childName = children[i];
                        Status.println("  - " + childName);
                        libKernel.sceKernelSendNotificationRequest(path + " contains: " + childName);
                    }
                } else {
                    Status.println(path + " is empty");
                    libKernel.sceKernelSendNotificationRequest(path + " is empty");
                }
            } else {
                Status.println(path + " does not exist or is not a directory");
                libKernel.sceKernelSendNotificationRequest(path + " inaccessible");
            }
        } catch (Throwable t) {
            Status.println("Failed to list " + path + ": " + t.getMessage());
        }
    }

    static List<String> candidates() {
        Set<String> ordered = new LinkedHashSet<String>();

        ordered.add("/app0/" + AUTOLOAD_DIR + "/" + AUTOLOAD_FILE);
        ordered.add("/app0/disc/" + AUTOLOAD_DIR + "/" + AUTOLOAD_FILE);
        for (int usb = 0; usb <= 7; usb++) {
            ordered.add("/mnt/usb" + usb + "/" + AUTOLOAD_DIR + "/" + AUTOLOAD_FILE);
        }
        ordered.add("/data/" + AUTOLOAD_DIR + "/" + AUTOLOAD_FILE);
        
        // Sandbox internal paths.
        ordered.add("/data/self/interactive/" + AUTOLOAD_DIR + "/" + AUTOLOAD_FILE);

        addSandboxCandidates(ordered);
        
        ordered.add(AUTOLOAD_DIR + "/" + AUTOLOAD_FILE);

        List<String> basePaths = new ArrayList<String>();
        
        // Add current working directory
        try {
            String userDir = System.getProperty("user.dir", ".");
            if (userDir != null) {
                basePaths.add(userDir);
            }
        } catch (Throwable ignored) {}

        // Add code source location parents
        try {
            CodeSource codeSource = AutoloadConfigLocator.class.getProtectionDomain().getCodeSource();
            if (codeSource != null) {
                URL location = codeSource.getLocation();
                if (location != null && "file".equalsIgnoreCase(location.getProtocol())) {
                    String rawPath = location.getPath();
                    if (rawPath == null || rawPath.length() == 0) {
                        rawPath = location.getFile();
                    }
                    if (rawPath != null && rawPath.length() > 0) {
                        String locStr = StringCompat.replaceLiteral(rawPath, "%20", " ");
                        // If it's a file (the JAR), go up to parent.
                        if (locStr.toLowerCase().endsWith(".jar")) {
                            locStr = getParentPath(locStr);
                        }
                        if (locStr != null) {
                            basePaths.add(locStr);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        for (String base : basePaths) {
            String cursor = base;
            for (int depth = 0; depth < 5 && cursor != null; depth++) {
                String fullPath = cursor;
                if (!fullPath.endsWith("/") && !fullPath.endsWith("\\")) {
                    fullPath += "/";
                }
                ordered.add(fullPath + AUTOLOAD_DIR + "/" + AUTOLOAD_FILE);
                cursor = getParentPath(cursor);
            }
        }

        return new ArrayList<String>(ordered);
    }

    private static String getParentPath(String path) {
        if (path == null || path.length() <= 1) {
            return null;
        }
        // Remove trailing slash if exists to find the real parent.
        if (path.endsWith("/") || path.endsWith("\\")) {
            path = path.substring(0, path.length() - 1);
        }
        
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0) {
            lastSlash = path.lastIndexOf('\\');
        }
        
        if (lastSlash > 0) {
            return path.substring(0, lastSlash);
        } else if (lastSlash == 0) {
             return "/";
        }
        return null;
    }

    private static void addSandboxCandidates(Set<String> ordered) {
        try {
            File sandboxRoot = new File("/mnt/sandbox");
            if (!sandboxRoot.exists() || !sandboxRoot.isDirectory()) {
                return;
            }

            String[] sandboxDirs = sandboxRoot.list();
            if (sandboxDirs == null) {
                return;
            }

            for (int i = 0; i < sandboxDirs.length; i++) {
                String sandboxDir = sandboxDirs[i];
                if (sandboxDir == null || !sandboxDir.endsWith("_000")) {
                    continue;
                }

                String base = "/mnt/sandbox/" + sandboxDir + "/app0/";
                ordered.add(base + AUTOLOAD_DIR + "/" + AUTOLOAD_FILE);
                ordered.add(base + "disc/" + AUTOLOAD_DIR + "/" + AUTOLOAD_FILE);
            }
        } catch (Throwable ignored) {
            // Keep candidate generation best-effort only.
        }
    }
}
