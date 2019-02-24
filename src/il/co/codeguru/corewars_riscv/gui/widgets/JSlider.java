package il.co.codeguru.corewars_riscv.gui.widgets;


import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLElement;
import elemental2.dom.HTMLInputElement;

public class JSlider extends JComponent<HTMLInputElement>
{
    HTMLElement m_valueElem = null;
    public JSlider(String id, String valueId) {
        super(id);
        setOnEvent("input");
        if (valueId != null)
            m_valueElem = (HTMLElement)DomGlobal.document.getElementById(valueId);
    }

    public void setValue(int v) {
        String sv = Integer.toString(v);
        m_element.value = sv;
        if (m_valueElem != null)
            m_valueElem.innerHTML = sv;
    }

    public int getValue() {
        return Integer.parseInt(m_element.value);
    }
    
}