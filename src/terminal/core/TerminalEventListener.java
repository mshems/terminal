package terminal.core;

public interface TerminalEventListener {
    void submitActionPerformed(SubmitEvent e);
    void queryActionPerformed(QueryEvent e);
}