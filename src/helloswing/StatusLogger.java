// StatusLogger.java
package helloswing;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

public class StatusLogger {
    private final JTextPane statusPane;

    public StatusLogger(JTextPane statusPane) {
        if (statusPane == null) {
            throw new IllegalArgumentException("Status pane cannot be null");
        }
        this.statusPane = statusPane;
    }

    public void log(String message) {
        try {
            StyledDocument doc = statusPane.getStyledDocument();
            doc.insertString(doc.getLength(), message + "\n", null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void logError(String message) {
        log("ERROR: " + message);
    }

    public void logSuccess(String message) {
        log("SUCCESS: " + message);
    }
}