package org.ps5jb.loader;

/**
 * Helper class to output messaging on screen and to the remote logging machine.
 */
public class Status {
    /** Instance of the remote logger to double all the status output over the network */
    private static volatile RemoteLogger LOGGER;

    /**
     * True if Xlet classes are detected on classpath.
     * When <code>true</code>, output is done on Xlet screen; otherwise, it goes to stdout.
     */
    private static volatile Boolean inXlet;

    /**
     * Default constructor. This class should be used statically, so the constructor is declared as private.
     */
    private Status() {
        super();
    }

    /**
     * Initialize the remote logger. Should be done only after the security manager is disabled;
     * otherwise, black screen occurs.
     */
    private static void initLogger() {
        if (LOGGER == null) {
            synchronized (Status.class) {
                if (LOGGER == null) {
                    LOGGER = new RemoteLogger(Config.getLoggerHost(), Config.getLoggerPort(), Config.getLoggerTimeout());
                }
            }
        }
    }

    /**
     * Cleanup method which should be called just before the app termination to release the resources.
     */
    public static void close() {
        synchronized (Status.class) {
            if (LOGGER != null) {
                LOGGER.close();
            }
        }
    }

    /**
     * Change the address of the server receiving remote logging output.
     *
     * @param host IP address or the hostname of the remote logging server. If null, remote logger will be deactivated.
     * @param port Port on which the server listens for logging message.
     * @param timeout Connect timeout to the remote logging server.
     */
    public static void resetLogger(String host, int port, int timeout) {
        synchronized (Status.class) {
            close();

            if (System.getSecurityManager() == null && host != null) {
                LOGGER = new RemoteLogger(host, port, timeout);
            }
        }
    }

    /**
     * Same as {@link #println(String, boolean) println(msg, false)}.
     *
     * @param msg Message to show on screen and to log remotely.
     */
    public static void println(String msg) {
        println(msg, false);
    }

    /**
     * Outputs a message. The message will be appended with the name of the current thread.
     *
     * @param msg Message to show on screen and to log remotely.
     * @param replaceLast Whether to replace the last line of the screen output
     *   (not applicable to remote log or when not running in Xlet).
     */
    public static void println(String msg, boolean replaceLast) {
        String finalMsg = "[" + Thread.currentThread().getName() + "] " + msg;

        if (inXlet()) {
            ProgressUI.getInstance().log(finalMsg);
        } else {
            System.out.println(finalMsg);
        }

        // Remote logger does not seem to work before jailbreak
        if (System.getSecurityManager() == null) {
            initLogger();
            LOGGER.info(finalMsg);
        }
    }

    /**
     * Outputs an informational message to both the log and the UI.
     *
     * @param msg Information message.
     */
    public static void info(String msg) {
        String finalMsg = "[" + Thread.currentThread().getName() + "] " + msg;
        if (inXlet()) {
            ProgressUI.getInstance().log(finalMsg);
        } else {
            System.out.println("INFO: " + finalMsg);
        }

        if (System.getSecurityManager() == null) {
            initLogger();
            LOGGER.info("INFO: " + finalMsg);
        }
    }

    /**
     * Updates the progress bar on the UI.
     *
     * @param percent Progress percentage (0-100).
     * @param label Status message displayed below the progress bar.
     */
    public static void setProgress(int percent, String label) {
        if (inXlet()) {
            ProgressUI.getInstance().setProgress(percent, label);
        }
    }

    /**
     * Outputs a message and a stack trace of the exception.
     *
     * @param msg Message to show on screen and to log remotely.
     * @param e Exception whose stack trace to output.
     */
    public static void printStackTrace(String msg, Throwable e) {
        String finalMsg = "[" + Thread.currentThread().getName() + "] " + msg;

        if (inXlet()) {
            ProgressUI.getInstance().logError(finalMsg);
            Screen.getInstance().printStackTrace(e);
        } else {
            System.out.println(finalMsg);
            e.printStackTrace();
        }

        // Remote logger does not seem to work before jailbreak
        if (System.getSecurityManager() == null) {
            initLogger();
            LOGGER.error(finalMsg, e);
        }
    }

    /**
     * Outputs a success message.
     *
     * @param msg Success message.
     */
    public static void success(String msg) {
        String finalMsg = "[" + Thread.currentThread().getName() + "] " + msg;
        if (inXlet()) {
            ProgressUI.getInstance().logSuccess(finalMsg);
        } else {
            System.out.println("SUCCESS: " + finalMsg);
        }

        if (System.getSecurityManager() == null) {
            initLogger();
            LOGGER.info("SUCCESS: " + finalMsg);
        }
    }

    /**
     * Outputs a warning message.
     *
     * @param msg Warning message.
     */
    public static void warning(String msg) {
        String finalMsg = "[" + Thread.currentThread().getName() + "] " + msg;
        if (inXlet()) {
            ProgressUI.getInstance().logWarning(finalMsg);
        } else {
            System.out.println("WARNING: " + finalMsg);
        }

        if (System.getSecurityManager() == null) {
            initLogger();
            LOGGER.info("WARNING: " + finalMsg);
        }
    }

    /**
     * Outputs an error message.
     *
     * @param msg Error message.
     */
    public static void error(String msg) {
        String finalMsg = "[" + Thread.currentThread().getName() + "] " + msg;
        if (inXlet()) {
            ProgressUI.getInstance().logError(finalMsg);
        } else {
            System.err.println("ERROR: " + finalMsg);
        }

        if (System.getSecurityManager() == null) {
            initLogger();
            LOGGER.error(finalMsg, new RuntimeException(msg));
        }
    }

    /**
     * Determine whether the status is being requested while running inside an Xlet.
     * When this is not the case, output will be sent to standard out/err.
     *
     * @return True if Xlet is detected, false otherwise.
     */
    private static boolean inXlet() {
        if (inXlet == null) {
            synchronized (Status.class) {
                if (inXlet == null) {
                    inXlet = Boolean.TRUE;
                    try {
                        Class.forName("com.sun.xlet.XletClassLoader");
                    } catch (ClassNotFoundException | Error e) {
                        inXlet = Boolean.FALSE;
                    }
                }
            }
        }
        return inXlet.booleanValue();
    }
}
