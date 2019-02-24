package il.co.codeguru.corewars_riscv.gui.widgets;

public class MouseEvent {
    int m_x, m_y;
    
    public MouseEvent(int x, int y) {
        m_x = x;
        m_y = y;
    }

    public int getX() {
        return m_x;
    }
    public int getY() {
        return m_y;
    }
    
}