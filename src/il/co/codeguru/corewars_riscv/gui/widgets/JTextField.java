package il.co.codeguru.corewars_riscv.gui.widgets;

//import com.google.gwt.dom.client.InputElement;
import elemental2.dom.HTMLInputElement;

public class JTextField extends JComponent<HTMLInputElement>
{
    public JTextField(String id, String text, int columns) {
        super(id);
        if (text != null)
            m_element.value = text;
    }


    @Override
    public void setText(String v) {
        m_element.value = v;
    }
    @Override
    public String getText() {
        return m_element.value;
    }
}