package org.ps5jb.loader;

/**
 * High-level API for controlling the loading progress UI.
 * Mimics the Y2JB JavaScript interface.
 */
public class ProgressUI {
    private static final ProgressUI instance = new ProgressUI();
    private final Screen screen;

    private ProgressUI() {
        this.screen = Screen.getInstance();
    }

    public static ProgressUI getInstance() {
        return instance;
    }

    /**
     * Set the application title displayed at the top.
     */
    public void setTitle(String title) {
        screen.setTitle(title);
    }

    /**
     * Log a message with INFO type.
     */
    public void log(String message) {
        log(message, Screen.MessageType.INFO);
    }

    /**
     * Log a message with a specific type.
     */
    public void log(String message, Screen.MessageType type) {
        screen.print(message, type, true, false);
    }

    /**
     * Log a success message (Green).
     */
    public void logSuccess(String message) {
        log(message, Screen.MessageType.SUCCESS);
    }

    /**
     * Log an error message (Red).
     */
    public void logError(String message) {
        log(message, Screen.MessageType.ERROR);
    }

    /**
     * Log a warning message (Yellow).
     */
    public void logWarning(String message) {
        log(message, Screen.MessageType.WARNING);
    }

    /**
     * Update the progress bar.
     *
     * @param percent Progress percentage (0-100).
     */
    public void setProgress(int percent) {
        setProgress(percent, null);
    }

    /**
     * Update the progress bar with a label.
     *
     * @param percent Progress percentage (0-100).
     * @param label Status message displayed below the progress bar.
     */
    public void setProgress(int percent, String label) {
        screen.setProgress(percent, label);
    }

    /**
     * Show the UI.
     */
    public void show() {
        screen.setVisible(true);
    }

    /**
     * Hide the UI.
     */
    public void hide() {
        screen.setVisible(false);
    }
}
