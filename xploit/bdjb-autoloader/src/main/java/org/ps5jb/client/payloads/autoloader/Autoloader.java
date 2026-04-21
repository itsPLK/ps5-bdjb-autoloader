package org.ps5jb.client.payloads.autoloader;

import org.ps5jb.client.payloads.autoloader.elf.GenericElfLoader;
import org.ps5jb.loader.Status;
import org.ps5jb.sdk.lib.LibKernel;

import org.ps5jb.sdk.io.File;
import java.util.List;

public class Autoloader implements Runnable {
    private static final int ELFLDR_PORT = 9021;
    private static final int SOCKET_CONNECT_TIMEOUT_MS = 1500;
    private static final int ELFLDR_BOOT_WAIT_MS = 4000;

    private final LibKernel libKernel;
    private final JarPayloadExecutor jarPayloadExecutor;
    private final LocalElfSender elfSender;
    private final GenericElfLoader genericElfLoader;

    public Autoloader() {
        this.libKernel = new LibKernel();
        this.jarPayloadExecutor = new JarPayloadExecutor();
        this.elfSender = new LocalElfSender("127.0.0.1", ELFLDR_PORT, SOCKET_CONNECT_TIMEOUT_MS);
        this.genericElfLoader = new GenericElfLoader();
    }

    @Override
    public void run() {
        try {
            runInternal();
        } catch (Throwable t) {
            Status.printStackTrace("Autoloader fatal error", t);
            notifyPs("[Autoloader] Fatal error: " + t.getMessage());
        } finally {
            libKernel.closeLibrary();
        }
    }

    private void runInternal() throws Exception {
        Status.println("Autoloader starting...");
        
        // Sandbox escape and root access are now handled by jailbreak-1.0-SNAPSHOT.jar
        // before this payload runs. We just verify the status here.

        try {
            int uid = libKernel.getuid();
            boolean sandboxed = libKernel.is_in_sandbox();
            notifyPs("UID: " + uid + ", Sandboxed: " + sandboxed);
            Status.println("UID: " + uid + ", Sandboxed: " + sandboxed);
        } catch (Throwable ignored) {}

        File configFile = AutoloadConfigLocator.findFirstConfig(this.libKernel);
        if (configFile == null) {
            Status.println("No autoload config found. Listing debug info...");
            notifyPs("No autoload config found. Checking /data...");
            AutoloadConfigLocator.listDebugDir(this.libKernel, "/data");
            for (int i = 0; i <= 2; i++) {
                AutoloadConfigLocator.listDebugDir(this.libKernel, "/mnt/usb" + i);
            }
            throw new IllegalStateException("Autoload configuration not found");
        }
        Status.println("Found autoload config: " + configFile.getAbsolutePath());
        notifyPs("Found autoload config:\n" + configFile.getAbsolutePath());

        File configDir = configFile.getParentFile();
        if (configDir == null) {
            configDir = new File(".");
        }
        List<String> commands = AutoloadConfigParser.parseCommands(configFile);
        if (commands == null || commands.isEmpty()) {
            Status.println("Autoload config contains no actionable entries");
            return;
        }
        for (String command : commands) {
            processCommand(command, configDir);
        }
    }

    private void processCommand(String command, File configDir) throws Exception {
        if (command == null) {
            return;
        }
        command = command.trim();
        if (command.length() == 0) {
            return;
        }

        if (command.startsWith("!")) {
            handleDelay(command.substring(1).trim());
            return;
        }

        if (command.startsWith("@")) {
            String message = StringCompat.replaceLiteral(command.substring(1).trim(), "\\n", "\n");
            notifyPs(message);
            return;
        }

        String lower = command.toLowerCase();
        if (lower.endsWith(".jar")) {
            File jarFile = resolvePath(command, configDir);
            requireFileExists(jarFile, "JAR not found");
            Status.println("Executing JAR: " + jarFile.getAbsolutePath());
            notifyPs("Executing JAR:\n" + jarFile.getAbsolutePath());
            jarPayloadExecutor.execute(jarFile);
            return;
        }

        if ("elfldr.elf".equalsIgnoreCase(command) || "elfldr.bin".equalsIgnoreCase(command)) {
            File customElfLdr = resolvePath(command, configDir);
            requireFileExists(customElfLdr, "Custom elfldr not found");
            ensureElfLoaderRunning(customElfLdr);
            return;
        }

        if (lower.endsWith(".elf") || lower.endsWith(".bin")) {
            File elfFile = resolvePath(command, configDir);
            requireFileExists(elfFile, "ELF/BIN not found");

            ensureElfLoaderRunning(null);
            int sent = elfSender.sendFile(elfFile);
            Status.println("Sent " + sent + " bytes: " + elfFile.getAbsolutePath());
            notifyPs("ELF sent: " + elfFile.getName() + " (" + sent + " bytes)");
            return;
        }

        String msg = "Unsupported autoload entry: " + command;
        Status.println(msg);
        notifyPs("[ERROR] " + msg);
    }

    private void handleDelay(String delayMs) throws InterruptedException {
        long ms;
        try {
            ms = Long.parseLong(delayMs);
        } catch (NumberFormatException e) {
            String msg = "Invalid delay value: " + delayMs;
            Status.println(msg);
            notifyPs("[ERROR] " + msg);
            return;
        }

        if (ms <= 0) {
            String msg = "Delay must be positive: " + delayMs;
            Status.println(msg);
            notifyPs("[ERROR] " + msg);
            return;
        }

        Status.println("Sleeping for " + ms + " ms");
        Thread.sleep(ms);
    }

    private void ensureElfLoaderRunning(File overrideElfLdr) throws Exception {
        if (elfSender.isListenerAlive()) {
            return;
        }

        String source = (overrideElfLdr == null) ? "bundled /elfldr.elf" : overrideElfLdr.getAbsolutePath();
        Status.println("Starting elfldr from " + source);
        notifyPs("Starting elfldr...\n" + source);

        genericElfLoader.loadAndRun(overrideElfLdr == null ? null : overrideElfLdr.getAbsolutePath());

        // Poll for elfldr connectivity
        long startTime = System.currentTimeMillis();
        long timeout = 10000; // 10 seconds total timeout
        boolean success = false;
        
        while (System.currentTimeMillis() - startTime < timeout) {
            if (elfSender.isListenerAlive()) {
                success = true;
                break;
            }
            Thread.sleep(500);
        }

        if (!success) {
            throw new IllegalStateException("elfldr did not become reachable on 127.0.0.1:" + ELFLDR_PORT + " after " + (timeout / 1000) + "s");
        }
        Status.println("elfldr is active and reachable.");
    }

    private static File resolvePath(String entry, File configDir) {
        if (entry == null) {
            throw new IllegalArgumentException("Path entry is null");
        }
        File f = new File(entry);
        if (f.isAbsolute()) {
            return f;
        }
        if (configDir == null) {
            return f;
        }
        
        String path = configDir.getAbsolutePath();
        if (path != null) {
            if (!path.endsWith("/") && !path.endsWith("\\")) {
                path += "/";
            }
            return new File(path + entry);
        }
        return f;
    }

    private static void requireFileExists(File file, String errorPrefix) {
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException(errorPrefix + ": " + file.getAbsolutePath());
        }
    }


    private void notifyPs(String message) {
        try {
            if (message == null) {
                message = "(null)";
            }
            libKernel.sceKernelSendNotificationRequest(message);
        } catch (Throwable ignored) {
            // Notification failure must never stop the autoloader.
        }
    }
}
