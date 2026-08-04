package org.bdj;

import java.lang.reflect.*;

import java.awt.BorderLayout;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;

import org.bdj.sandbox.Exploit;
import org.bdj.sandbox.ExploitInternal;
import java.net.InetAddress;

public class InitXlet implements Xlet {
    private HScene scene;
    private Screen screen;
    private InternalJarLoader internalJarLoader;
    private Thread internalJarLoaderThread;
    private final String jarLoaderThreadName = "JarLoader";
    
    private final String versionString = "BD-JB5 2.0 by Gezine";
    private final boolean UseInternalJar = false;
    
    public void initXlet(XletContext context) {
        Status.setScreenOutputEnabled(true);
        Status.setNetworkLoggerEnabled(false);

        screen = Screen.getInstance();
        screen.setSize(1920, 1080);
        screen.setTitle("PS5 BD-JB Autoloader");
        Status.setProgress(0, "Initializing...");

        scene = HSceneFactory.getInstance().getDefaultHScene();
        scene.add(screen, BorderLayout.CENTER);
        scene.validate();
    }
    
    public void startXlet() {
        screen.setVisible(true);
        scene.setVisible(true);
        
        Status.success("Screen initialized");
        
        try {
            Status.info("Triggering sandbox escape exploit...");
            Status.setProgress(10, "Running kernel exploit...");
            
            if (!Exploit.disableSecurityManager()) {
                ExploitInternal.disableSecurityManager();
            }

            if (System.getSecurityManager() == null) {
                Status.success("Exploit success - sandbox escape achieved");
            } else {
                Status.error("Exploit failed - sandbox still active");
            }

            // Warm up network stack to prevent dlopen failures in dynamic JARs
            try {
                Status.info("Warming up network stack...");
                InetAddress.getByName("127.0.0.1");
            } catch (Throwable ignored) {}

        } catch (Exception e) {
            Status.printStackTrace("Error when disabling sandbox: ", e);
        }
        
        // Add sanity check
        if (System.getSecurityManager() == null) {
            try {
                Status.info("Initializing autoloader...");
                Status.setProgress(20, "Starting JarLoader...");
                internalJarLoader = new InternalJarLoader();
                internalJarLoaderThread = new Thread(internalJarLoader, jarLoaderThreadName);
                internalJarLoaderThread.start();
            } catch (Throwable e) {
                Status.printStackTrace("Loader startup failed", e);
            }
        } else {
            Status.error("Sandbox is still activated");
        }
        
    }

    public void pauseXlet() {
        screen.setVisible(false);
    }

    public void destroyXlet(boolean unconditional) {
        scene.remove(screen);
        scene = null;
    }
}
