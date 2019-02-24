package il.co.codeguru.corewars_riscv.gui;

//import java.awt.BorderLayout;
//import java.awt.GridLayout;

//import javax.swing.JCheckBox;
//import javax.swing.JLabel;
//import javax.swing.JPanel;
import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLInputElement;
import il.co.codeguru.corewars_riscv.gui.widgets.*;

public class FlagFields extends JPanel {
	
	private HTMLInputElement checkBox;
	private CpuFrame m_frame;
	
	public FlagFields(final String name, CpuFrame frame) {
		m_frame = frame;
		//super.setLayout(new GridLayout(1,2));
		//super.setSize(20,20);
		//super.add(new JLabel(name + ":"),BorderLayout.LINE_START);
		String ename = name + "_fcheck";
		checkBox = (HTMLInputElement) DomGlobal.document.getElementById( ename);
		if (checkBox == null) {
			Console.error("Did not find flag " + name);
		}
		
		//super.add(checkBox,BorderLayout.LINE_START);

		checkBox.addEventListener("change", (event) -> {
			m_frame.flagChanged_callback(name, checkBox.checked);

		});
	}
	
	public void setValue(boolean value){
		checkBox.checked = value;
	}
	
	public boolean getValue(){
		return checkBox.checked;
	}
	
}
