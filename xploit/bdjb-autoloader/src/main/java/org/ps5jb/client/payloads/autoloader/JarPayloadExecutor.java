package org.ps5jb.client.payloads.autoloader;

import org.ps5jb.loader.ManifestUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

final class JarPayloadExecutor {
    private static final String MANIFEST_BACKGROUND_KEY = "PS5JB-Client-Background-Thread-Name";

    void execute(File jarFile) throws Exception {
        if (jarFile == null) {
            throw new IllegalArgumentException("JAR file is null");
        }

        Manifest manifest = ManifestUtils.loadJarManifest(jarFile);
        if (manifest == null || manifest.getMainAttributes() == null) {
            throw new IllegalStateException("JAR manifest could not be read: " + jarFile.getAbsolutePath());
        }

        String mainClassName = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
        if (mainClassName == null || mainClassName.trim().length() == 0) {
            throw new ClassNotFoundException("Main class not defined in the JAR manifest");
        }

        String backgroundThreadName = manifest.getMainAttributes().getValue(MANIFEST_BACKGROUND_KEY);
        if (backgroundThreadName != null) {
            backgroundThreadName = backgroundThreadName.trim();
            if (backgroundThreadName.length() == 0) {
                backgroundThreadName = null;
            }
        }

        ClassLoader parentLoader = getClass().getClassLoader();
        ClassLoader bypassRestrictionsLoader = new URLClassLoader(new URL[0], parentLoader) {
            @Override
            protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("java.nio")
                        || name.startsWith("javax.security.auth")
                        || name.startsWith("javax.net.ssl")) {
                    return findSystemClass(name);
                }
                return super.loadClass(name, resolve);
            }
        };

        URLClassLoader contextLoader = URLClassLoader.newInstance(
            new URL[] { jarFile.toURL() },
                bypassRestrictionsLoader
        );

        final Class mainClass = contextLoader.loadClass(mainClassName);
        final Method mainMethod = mainClass.getDeclaredMethod("main", new Class[] { String[].class });

        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    mainMethod.invoke(null, new Object[] { new String[0] });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        if (backgroundThreadName != null) {
            Thread thread = new Thread(task, backgroundThreadName);
            thread.start();
        } else {
            task.run();
        }
    }
}
