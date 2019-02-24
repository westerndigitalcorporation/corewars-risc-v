package il.co.codeguru.corewars_riscv.gui.widgets;

public interface WindowListener {
    public void windowOpened(WindowEvent e);
    public void windowClosing(WindowEvent e);

    public void windowClosed(WindowEvent e);
    public void windowIconified(WindowEvent e) ;
    public void windowDeiconified(WindowEvent e);
    public void windowActivated(WindowEvent e);
    public void windowDeactivated(WindowEvent e);
}