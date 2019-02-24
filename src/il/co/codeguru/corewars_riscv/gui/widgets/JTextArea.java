package il.co.codeguru.corewars_riscv.gui.widgets;

//import com.google.gwt.dom.client.Element;

import elemental2.dom.HTMLElement;

public class JTextArea extends JComponent<HTMLElement>
{
    public JTextArea(String id, int w, int h) {
        super(id);

    }
    public JTextArea() {
    }

    public void append(String s) {
        m_element.innerHTML = m_element.innerHTML + s;
    }

    public void scrollToBottom() {
        m_element.scrollTop = m_element.scrollHeight - m_element.clientHeight;
    }

    public String getDocument() {
        return "";
    }
    
    public void setLineWrap(boolean v) {
    }
    public void setWrapStyleWord(boolean v) {
    }

}