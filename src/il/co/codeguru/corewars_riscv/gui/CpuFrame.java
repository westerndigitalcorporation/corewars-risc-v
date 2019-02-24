package il.co.codeguru.corewars_riscv.gui;

import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLElement;
import il.co.codeguru.corewars_riscv.cpu.riscv.CpuStateRiscV;
import il.co.codeguru.corewars_riscv.memory.Memory;
import il.co.codeguru.corewars_riscv.gui.widgets.Console;
import il.co.codeguru.corewars_riscv.jsadd.Format;
import il.co.codeguru.corewars_riscv.memory.MemoryEventListener;
import il.co.codeguru.corewars_riscv.war.Competition;
import il.co.codeguru.corewars_riscv.war.CompetitionEventListener;
import il.co.codeguru.corewars_riscv.war.War;
import il.co.codeguru.corewars_riscv.war.Warrior;

import java.util.HashMap;

public class CpuFrame implements CompetitionEventListener, MemoryEventListener {
	
	//private War currentWar;
	private CompetitionWindow m_mainwnd;
	private String m_currentWarriorLabel = null;
	private int m_currentWarriorIndex = -1; // faster to use index than label during debug
	
	private Competition competition;
	private int m_base = 16;

	private RegisterField[] registers;
	private RegisterField regPc;


    private HTMLElement cpuPanel;

    MemRegionView stackView;
	MemRegionView sharedMemView;

    public void setVisible(boolean v) {
        if (v) {
			cpuPanel.style.display = "";
			showMemoryRegions(SettingsPanel.useNewMemory());
		}
        else {
			cpuPanel.style.display = "none";
		}
    }

    private native void showMemoryRegions(boolean toShow) /*-{
    	if(toShow){
    		$wnd.$('#sharedMemArea').removeClass('hide');
    		$wnd.$('#stackArea').removeClass('hide');
    	}
    	else {
    		$wnd.$('#sharedMemArea').addClass('hide');
    		$wnd.$('#stackArea').addClass('hide');
    	}
    }-*/;

	public void setSelectedPlayer(String playerLabel, boolean isDebugMode) {
		m_currentWarriorLabel = playerLabel;
		m_currentWarriorIndex = -1; // invalidate

		if (isDebugMode) {
			// need to do this first so that reading the registers would put this ss:sp in the right place
			initMemRegions(false);
			updateFields();
		}
	}

	public int regChanged_callback(String name, String value)
	{
		War currentWar = competition.getCurrentWar();
		if (currentWar == null)
			return 1;

		CpuStateRiscV state = currentWar.getWarriorByLabel(m_currentWarriorLabel).getCpuState();

		int v;

		value = value.trim();
		if (value.length() > 2 && value.charAt(0) == '0' && value.charAt(1) == 'x')
			value = value.substring(2);

		try {
			if (m_base == 10)
				v = Integer.parseInt(value, 10);
			else
				v = Integer.parseInt(value, 16);
		}
		catch(NumberFormatException e) {
			m_mainwnd.errorPreventsStep(true);
			return (m_base == 10) ? -2000000 : -1000000;
		}
		if (v < 0 || v > 0xffff) {
			m_mainwnd.errorPreventsStep(true);
			return -3000000;
		}
		m_mainwnd.errorPreventsStep(false);


		switch(name) {
			case "PC": state.setPc(v); changedCSIP(); break;
			default:
				state.setReg(Integer.valueOf(name), v);
		}

		// reeval watch - might change depending on the register that just changed
		m_stateAccess.state = state;
		for (WatchEntry entry : m_watches.values()) {
			entry.evalAndDisplay();
		}

		return v;
	}

	public void changedCSIP() {
		m_mainwnd.m_codeEditor.onEndRound(); // redraw the ip indicator

		m_mainwnd.battleFrame.onEndRound(); // make it redraw ip pointers.

	}

	public void onMemoryWrite(int address, byte value){
		for (WatchEntry entry : m_watches.values()) {
			entry.evalAndDisplay();
		}
	}

	public void onWriteState(MemoryEventListener.EWriteState state)
	{}

	public void flagChanged_callback(String name, boolean v)
	{
	}

	public CpuFrame(Competition c, CompetitionWindow mainwnd)
	{
		exportMethods();
		m_mainwnd = mainwnd;

		this.competition = c;

        cpuPanel = (HTMLElement) DomGlobal.document.getElementById("cpuPanel");

        registers = new RegisterField[32];
		for(int i=0; i<32;i++)
		{
			registers[i] = new RegisterField(Integer.toString(i), this);
		}
		regPc = new RegisterField("PC", this);

		stackView = new MemRegionView("stackList", "mk");
		sharedMemView = new MemRegionView("sharedMemList", "mh");

		m_parser.m_stateAccess = m_stateAccess;
	}

	public native void exportMethods() /*-{
        var that = this
        $wnd.j_setRegistersBase = $entry(function(b) { that.@il.co.codeguru.corewars_riscv.gui.CpuFrame::j_setRegistersBase(I)(b) });
        $wnd.j_watchTextChanged = $entry(function(s,i) { return that.@il.co.codeguru.corewars_riscv.gui.CpuFrame::j_watchTextChanged(Ljava/lang/String;I)(s,i) });
        $wnd.j_addWatch = $entry(function(i) { return that.@il.co.codeguru.corewars_riscv.gui.CpuFrame::j_addWatch(I)(i) });
        $wnd.j_delWatch = $entry(function(i) { return that.@il.co.codeguru.corewars_riscv.gui.CpuFrame::j_delWatch(I)(i) });

	}-*/;

	public void j_setRegistersBase(int base) {
		m_base = base;
		for(RegisterField reg : registers)
		{
			reg.setBase(base);
		}

        for (WatchEntry entry : m_watches.values()) {
            entry.base = base;
            entry.evalAndDisplay();
        }
	}


	
	public void updateFields(){
		War currentWar = competition.getCurrentWar();
		if (currentWar == null)
			return;
		if (m_currentWarriorIndex == -1) {
			m_currentWarriorIndex = currentWar.getWarriorByLabel(m_currentWarriorLabel).m_myIndex;
		}

		//CpuState state = currentWar.getWarrior(dropMenu.getSelectedIndex()).getCpuState();
		CpuStateRiscV state = currentWar.getWarrior(m_currentWarriorIndex).getCpuState();

		for(int i=0;i<32;i++)
		{
			registers[i].setValue(state.getReg(i));
		}
		regPc.setValue(state.getPc());

		//TODO: do this when re-implementing the stack
		//stackView.moveToLine(RealModeAddress.linearAddress(state.getSS(), state.getSP()));

		// update watches;
		m_stateAccess.state = state;
		for (WatchEntry entry : m_watches.values()) {
            entry.evalAndDisplay();
		}

	}

	private static class StateAccess implements ExpressionParser.IStateAccess {
		public CpuStateRiscV state;
		public Memory Memory;

		@Override
		public short getRegisterValue(String name) throws Exception{
		    if (state == null) {
		        throw new Exception("invalid state");
            }
            try {
				return (short) state.getReg(Integer.valueOf(name));
			}
			catch(IndexOutOfBoundsException e) {
				throw new RuntimeException("Unknown register name: " + name); // should not happen since we check before
			}
		}

		@Override
		public int getIdentifierValue(String name) throws Exception {
		    throw new Exception("unknown identifier " + name);
		}

		@Override
		public int getMemory(int addr, int seg, int size) {
			if (size == 1)
				return Memory.loadByte(addr) & 0xff;
			else
				return Memory.loadHalfWord(addr) & 0xffff;
		}

	}

	class WatchEntry {
		public ExpressionParser.INode root;
		public HTMLElement resultElem;
		boolean isValid = false;
        int base = 16;

        public void setResult(int v) {
            String sv;
            if (base == 16)
                sv = Format.hex4(v);
            else
                sv = Integer.toString(v);
            setResult(sv);
        }

		public void setResult(String v) {
            Format.setInnerText(resultElem, v);
            resultElem.title = v; // tooltip also shows the same text in case it is obscured
        }

        public void evalAndDisplay() {
            if (!isValid)
                return;
            try {
                int res = root.eval();
                setResult(res);
            } catch (Exception e) {
                Console.log("watch error: " + e.getMessage());
                setResult("Error: " + e.getMessage());
            }
        }
	}

	private HashMap<Integer, WatchEntry> m_watches = new HashMap<>();
	private StateAccess m_stateAccess = new StateAccess();
	private ExpressionParser m_parser = new ExpressionParser();

	void j_addWatch(int watchId) {
        WatchEntry entry = new WatchEntry();
        m_watches.put(watchId, entry);
        entry.resultElem = (HTMLElement)DomGlobal.document.getElementById("watch" + Integer.toString(watchId) + "_val" );
        assert entry.resultElem != null : "did not find watch result element";
    }

    void j_delWatch(int watchId) {
        m_watches.remove(watchId);
    }

    private boolean onlySpaces(String s) {
		//return s.chars().anyMatch(c -> c != ' ');
	    for(int i = 0; i < s.length(); ++i) {
	        char c = s.charAt(i);
	        if (c != ' ')
	            return false;
        }
	    return true;
    }

    // returns true if string is not empty or only spaces
	boolean j_watchTextChanged(String s, int watchId)
    {
        WatchEntry entry = m_watches.get(watchId);
        assert entry != null : "did not find watch";
        if (onlySpaces(s)) {
            entry.isValid = false;
            entry.setResult("");
            return false;
        }

        entry.isValid = false;
		try {
			entry.root = m_parser.eval(s);
			entry.isValid = true;
		}
		catch (Exception e) { // might be an exception from ast eval which doesn't make the watch not valid
			Console.debug("Watch parse error: " + e.getMessage());
            entry.setResult("Error: " + e.getMessage());
		}
		entry.evalAndDisplay();

		return true;
	}

	// set the mem regions with the correct address region and values
	// force if we must reread the memory in a new battle (don't keep the old one but it may have the same regions)
	private void initMemRegions(boolean force)
	{
		War currentWar = competition.getCurrentWar();
		if (currentWar == null)
			return;

		Warrior warrior = currentWar.getWarriorByLabel(m_currentWarriorLabel);

		stackView.initMemRegion(warrior.stackRegion, currentWar.getMemory(), force);
		sharedMemView.initMemRegion(warrior.sharedRegion, currentWar.getMemory(), force);

		m_stateAccess.Memory = currentWar.getMemory();
	}

	@Override
	public void onWarPreStartClear() {}


	@Override
	public void onWarStart() {
		m_currentWarriorIndex = -1; // invalidate
		initMemRegions(true);
	}

	@Override
	public void onWarEnd(int reason, String winners, boolean inDebug) {
	}

	@Override
	public void onRound(int round) {
	}

	@Override
	public void onWarriorBirth(Warrior w) {
	}
	@Override
	public void onPaused() {}
	@Override
	public void onNoneAlive() {}


	@Override
	public void onWarriorDeath(Warrior warrior, String reason) {
	}
	@Override
	public void onCompetitionStart() {
	}
	@Override
	public void onCompetitionEnd() {
	}

	@Override
	public void onEndRound() {
		this.updateFields();
	}

}
