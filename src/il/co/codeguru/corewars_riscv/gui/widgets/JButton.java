package il.co.codeguru.corewars_riscv.gui.widgets;

import java.util.HashMap;

//import com.google.gwt.dom.client.InputElement;
//import com.google.gwt.user.client.Event;
//import com.google.gwt.user.client.EventListener;
//import com.google.gwt.user.client.Window;

import elemental2.dom.Element;
import elemental2.dom.Event;
import elemental2.dom.EventListener;

public class JButton extends JComponent<Element>
{
    public JButton(String id, String text) {
        super(id);
        registerBut(id,this);
        if (m_element == null)
            return;
        m_element.addEventListener("click", new EventListener() {
    
            @Override
            public void handleEvent(Event event) {
                if (m_listener == null || !m_enabled)
                    return;
                m_listener.actionPerformed(new ActionEvent(JButton.this));
            }
        });
    
    }
    

    // offline actuvation hack
    public void click() {
        if (m_listener == null)
            return;
        m_listener.actionPerformed(new ActionEvent(JButton.this));    	
    }
    

    static HashMap<String, JButton> m_buts = new HashMap<String, JButton>();
    static void registerBut(String id, JButton but) {
    	System.out.println("register button " + id);
    	m_buts.put(id, but);
    }
    
    public static void press(String id) {
    	m_buts.get(id).click();
    }
    
}