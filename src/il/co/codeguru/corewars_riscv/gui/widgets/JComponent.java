package il.co.codeguru.corewars_riscv.gui.widgets;

//import com.google.gwt.dom.client.Document;
//import com.google.gwt.dom.client.Element;
//import com.google.gwt.user.client.Event;
//import com.google.gwt.user.client.EventListener;

import elemental2.dom.DomGlobal;
import elemental2.dom.Element;
import elemental2.dom.Event;
import elemental2.dom.EventListener;

public class JComponent<T>
{
    public T m_element;
    public boolean m_enabled = true;

    public JComponent(String id) {
        m_element = (T)DomGlobal.document.getElementById(id);

        if (m_element == null) {
            Console.log("did not find element " + id);
            return;
        }
        else {
            Console.log("found element " + id);
        }
    }

    public JComponent() {
    }

    public void setOnEvent(String evname) {
        ((Element)m_element).addEventListener(evname, new EventListener() {
            @Override
            public void handleEvent(Event event) { // this listener listens to all of the event
                if (m_listener == null)
                    return;
                m_listener.actionPerformed(new ActionEvent(JComponent.this));
    
            }
        });
    }


    protected ActionListener m_listener = null;
    public void addActionListener(ActionListener listener) {
        m_listener = listener;
    }
    
    public void setEnabled(boolean v) {
        if (m_element == null)
            return;
        m_enabled = v;
        ((Element)m_element).setAttribute("disabled", v?"false":"true");
    }
    public void setVisible(boolean v) {
    }
    public boolean isVisible() {
        return true;
    }
    
    public void setText(String v) {
        if (m_element == null)
            return;
        ((Element)m_element).innerHTML = v;
    }
    public String getText() {
        if (m_element == null)
            return "";
        return ((Element)m_element).innerHTML;
    }

    public void setBorder(Object obj) {
    }

    public void setFont(Object f) {

    }

    public void setOpaque(boolean v) {

    }

    public void repaint() {
        paint();
    }

    public void paint() {
    }

    public void setPreferredSize(Object obj) {
    }

    public Dimension getMinimumSize() {
        return new Dimension(50,50);
    }
  
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    public void setMinimumSize(Dimension d) {
    }

    public void dispose() {
    }
    public void pack() {
    }

    public void addMouseMotionListener(MouseInputListener m) {

    }
    public void addMouseListener(MouseInputListener m) {

    }

    public Dimension getSize() {
        return new Dimension(0,0);
    }
    public void setSize(Dimension d) {

    }
    public void setSize(int w, int h) {

    }

    public void setAlwaysOnTop(boolean v) {
    }
    
}