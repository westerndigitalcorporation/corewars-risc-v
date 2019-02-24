package il.co.codeguru.corewars_riscv.gui;


import elemental2.dom.DocumentFragment;
import elemental2.dom.DomGlobal;
import elemental2.dom.Element;
import elemental2.dom.HTMLElement;
import il.co.codeguru.corewars_riscv.memory.Memory;
import il.co.codeguru.corewars_riscv.gui.code_editor.asm_parsers.TextUtils;
import il.co.codeguru.corewars_riscv.jsadd.Format;
import il.co.codeguru.corewars_riscv.memory.MemoryEventListener;
import il.co.codeguru.corewars_riscv.memory.MemoryRegion;
import il.co.codeguru.corewars_riscv.utils.Unsigned;

public class MemRegionView implements MemoryEventListener
{
    private HTMLElement m_htmlList;
    private MemoryRegion m_currentRegion = new MemoryRegion();
    private final String m_innerPrefix;
    private int m_step; // how many bytes in each line (only 2 supported now)
    private int m_lastMovedToLine = -1;
    private HTMLElement m_lastMovedToElem = null;



    public MemRegionView(String id, String innerPrefix) {
        m_htmlList = (HTMLElement)DomGlobal.document.getElementById(id);
        m_innerPrefix = innerPrefix;
        m_step = 2;
    }


    public void initMemRegion(MemoryRegion region, Memory Memory, boolean force) {
        if (!force && m_currentRegion.equals(region))
            return; // can happen in shared mem if we move between the two codes of a single player

        DocumentFragment df = DomGlobal.document.createDocumentFragment();
        for(int addr = region.m_start; addr <= region.m_end; addr += m_step)
        {
            Element e = DomGlobal.document.createElement("div");
            e.setAttribute("id", m_innerPrefix + Integer.toString(addr));
            // 5 spaces since this is an absolute linear address

            String sb = Format.hex5(addr) +
                    "   " +
                    Format.hex2(Unsigned.unsignedByte(Memory.loadByte(addr))) +
                    TextUtils.SPACE_FOR_HEX_CHAR +
                    Format.hex2(Unsigned.unsignedByte(Memory.loadByte(addr + 1)));
            e.appendChild(DomGlobal.document.createTextNode(sb // memory size is always even so no need to check
            ));
            df.appendChild(e);
        }
        m_htmlList.innerHTML = "";
        m_htmlList.appendChild(df);


        m_currentRegion.m_start = region.m_start;
        m_currentRegion.m_end = region.m_end;

        // clear last so that the first time registers are inited, it will set the line
        m_lastMovedToLine = -1;
        m_lastMovedToElem = null;
    }

    public void onMemoryWrite(int address, byte value)
    {
        if (address < m_currentRegion.m_start || address > m_currentRegion.m_end)
            return;

        int lineaddr = (address / m_step) * m_step;
        int offsetInLine = address % m_step;
        HTMLElement elem = (HTMLElement)DomGlobal.document.getElementById(m_innerPrefix + Integer.toString(lineaddr) );
        assert elem != null : "unexpected: did not find element";

        String text = Format.innerText(elem);
        offsetInLine = 5 + 3 + offsetInLine * 3; // 5 for address, 3 spaces and each byte has 3 chars - 2+1 space
        String newtext = text.substring(0, offsetInLine) + Format.hex2(Unsigned.unsignedByte(value)) + text.substring(offsetInLine + 2);
        Format.setInnerText(elem, newtext);
    }

    public void onWriteState(MemoryEventListener.EWriteState state)
    {}


    public void moveToLine(int addr)
    {
        if (m_lastMovedToLine == addr)
            return;
        if (m_lastMovedToElem != null)
            m_lastMovedToElem.classList.remove("atStackLine");
        m_lastMovedToElem = null;
        m_lastMovedToLine = -1;
        if (addr > m_currentRegion.m_end || addr < m_currentRegion.m_start)
            return;
        int offsetInLine = addr % m_step;
        if (offsetInLine != 0)  // somehow we want a non even line, prefer not to show anything
            return;


        int lineaddr = (addr / m_step) * m_step;
        HTMLElement elem = (HTMLElement)DomGlobal.document.getElementById(m_innerPrefix + Integer.toString(lineaddr) );
        assert elem != null :  "unexpected: did not find element2";
        elem.classList.add("atStackLine");

        // scroll to view?
        if (elem.offsetTop <= m_htmlList.scrollTop + 30 || elem.offsetTop >= m_htmlList.scrollTop + m_htmlList.offsetHeight - 30) {
            m_htmlList.scrollTop = elem.offsetTop - 50;
        }


        m_lastMovedToLine = addr;
        m_lastMovedToElem = elem;
    }
}