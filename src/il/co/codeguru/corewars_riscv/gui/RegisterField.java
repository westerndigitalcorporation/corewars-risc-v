package il.co.codeguru.corewars_riscv.gui;

import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLInputElement;
import il.co.codeguru.corewars_riscv.gui.widgets.*;
import il.co.codeguru.corewars_riscv.jsadd.Format;

public class RegisterField /*extends JPanel*/ {

	private HTMLInputElement textField;
	private int m_base = 16;
	private CpuFrame m_frame;
	private boolean m_lastInputOk = true;
	private int m_lastValue;
	private String m_name;

	public RegisterField(String name, CpuFrame frame) {
		m_frame = frame;
		m_name = name;
		String ename = "reg_" + name;
		textField = (HTMLInputElement) DomGlobal.document.getElementById(ename);
		if (textField == null) {
			Console.error("Not found register " + name);
		}

		textField.addEventListener("input", event -> editChanged());
	}

	public void editChanged() {
		int setok = m_frame.regChanged_callback(m_name, textField.value);
		if (setok < -999999) {
			textField.classList.add("dbg_reg_err");
			switch(setok) {
				case -1000000: textField.title = "Failed to parse hex number"; break;
				case -2000000: textField.title = "Failed to parse decimal number"; break;
				case -3000000: textField.title = "Out of range"; break;
			}
			m_lastInputOk = false;
		}
		else {
			textField.classList.remove("dbg_reg_err");
			textField.removeAttribute("title");
			m_lastInputOk = true;
			m_lastValue = (short)setok;
		}
	}

	public void setBase(int base) {
		m_base = base;

		if (!m_lastInputOk) { // maybe the user pressed the base buttons to fix the error he wrote
			editChanged();
		}
		else {
			setValue(m_lastValue);
		}
	}

	public void setValue(int value) {
		m_lastValue = value;
		if (m_base == 16)
			textField.value = Format.hex(value,8);
		else
			textField.value = Integer.toString(value & 0xffff);
	}

	public short getValue() {
		return (short) Integer.parseInt(textField.value, 16);
	}

}