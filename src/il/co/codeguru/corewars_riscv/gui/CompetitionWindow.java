package il.co.codeguru.corewars_riscv.gui;

import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLElement;
import il.co.codeguru.corewars_riscv.features.FeatureSet;
import il.co.codeguru.corewars_riscv.gui.code_editor.CodeEditor;
import il.co.codeguru.corewars_riscv.utils.Logger;
import il.co.codeguru.corewars_riscv.war.*;
import com.google.gwt.animation.client.AnimationScheduler;

import il.co.codeguru.corewars_riscv.gui.widgets.*;
//import java.awt.*;
//import java.awt.event.*;
import java.util.Arrays;

/**
 * @author BS
 */
public class CompetitionWindow extends JFrame implements ScoreEventListener, CompetitionEventListener {
	private static final long serialVersionUID = 1L;
	
	private Competition competition;
    private ColumnGraph columnGraph;

    // widgets
    private JLabel warCounterDisplay;
    private boolean m_isBattleShown = false;
    private JTextField battlesPerGroupField;
    public WarFrame battleFrame;

    private int warCounter;
    private int totalWars;
	private boolean competitionRunning;

    private static final String SEED_PREFIX = "SEED#";
	private JTextField seed;

    private boolean isInDebug = false;

    CodeEditor m_codeEditor;
    public PlayersPanel m_playersPanel;
    private HTMLElement stepnum;


    public boolean isBattleShown() {
        return m_isBattleShown;
    }

    public CompetitionWindow()
    {
        super("CodeGuru Extreme - Competition Viewer");
        getContentPane().setLayout(new BorderLayout());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        competition = new Competition();
        Logger.log("Creating new Competition");
        competition.addCompetitionEventListener(this);
        WarriorRepository warriorRepository = competition.getWarriorRepository();
        warriorRepository.addScoreEventListener(this);

        columnGraph = new ColumnGraph(warriorRepository.getGroupNames());
        getContentPane().add(columnGraph, BorderLayout.CENTER);
        // -------------
        JPanel controlArea = new JPanel();
        controlArea.setLayout(new BoxLayout(controlArea, BoxLayout.Y_AXIS));
        // -------------- Button Panel
        JPanel buttonPanel = new JPanel();
        warCounterDisplay = new JLabel("");
        buttonPanel.add(warCounterDisplay);
        buttonPanel.add(Box.createHorizontalStrut(30));

        controlArea.add(buttonPanel);
        // -------------
        controlArea.add(new JSeparator(JSeparator.HORIZONTAL));
        // ------------ Control panel
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout());
        controlPanel.add(new JLabel("Survivor groups per session:"));
        controlPanel.add(new JLabel("Sessions per groups combination:"));
		battlesPerGroupField = new JTextField("battlesPerGroupField", "100", 4);
		seed = new JTextField("seed", null, 4);
		seed.setText("guru");
		controlPanel.add(new JLabel("seed:"));
		controlPanel.add(seed);
		
		controlArea.add(controlPanel);
        // ------------
        getContentPane().add(controlArea, BorderLayout.SOUTH);

        showBattleRoom();

        m_codeEditor = new CodeEditor(competition);
        m_playersPanel = new PlayersPanel(this);
        m_codeEditor.m_playersPanel = m_playersPanel;

        setPlatform("riscv");

        stepnum = (HTMLElement) DomGlobal.document.getElementById("stepnum");
        
        exportMethods();

        competition.competitionEventCaster.doneAdding();
        competition.memoryEventCaster.doneAdding();

        call_gwtStart();
    }

    public void setPlatform(String plat) {
        m_codeEditor.setPlatform(plat);
    }

    public static native void call_gwtStart() /*-{
        $wnd.gwtStart();
    }-*/;


    public native void exportMethods() /*-{
        var that = this
        $wnd.j_startDebug = $entry(function() { return that.@il.co.codeguru.corewars_riscv.gui.CompetitionWindow::j_startDebug()() });
        $wnd.j_stopDebug = $entry(function() { return that.@il.co.codeguru.corewars_riscv.gui.CompetitionWindow::j_stopDebug()() });
        $wnd.j_debugUiInited = $entry(function() { return that.@il.co.codeguru.corewars_riscv.gui.CompetitionWindow::j_debugUiInited()() });
        $wnd.j_triggerZeroSpeed = $entry(function() { return that.@il.co.codeguru.corewars_riscv.gui.CompetitionWindow::j_triggerZeroSpeed()() });
        $wnd.j_startCompete = $entry(function() { return that.@il.co.codeguru.corewars_riscv.gui.CompetitionWindow::j_startCompete()() });
        $wnd.j_stopCompete = $entry(function() { return that.@il.co.codeguru.corewars_riscv.gui.CompetitionWindow::j_stopCompete()() });

    }-*/;

    public boolean j_startDebug()
    {
        Logger.log("j_startDebug");
        if (!m_playersPanel.checkPlayersReady())
            return false;
        return gui_runWar( true);
    }

    public void j_stopDebug()
    {
        competition.setAbort();
        setDebugMode(false);
    }

    public boolean j_startCompete()
    {
        if (!m_playersPanel.checkPlayersReady())
            return false;
        return gui_runWar( false);
    }
    public void j_stopCompete()
    {
        competition.setAbort();
    }

    public void j_triggerZeroSpeed() {
        competition.setSpeed(0);
        battleFrame.speedSlider.setValue(0);
    }


    private void outRoundNum() {
        stepnum.innerHTML = (competition.compState == null) ? "[null]":Integer.toString(competition.compState.round);
    }

    private AnimationScheduler.AnimationCallback animCallback = timestamp -> {
        try {
            startRunAnimation();
        } catch (Exception e) {
            Console.log("continueRun EXCEPTION " + e.toString());
            e.printStackTrace();
        }
    };

    private void startRunAnimation() throws Exception {
        boolean needMore = competition.continueRun();
        outRoundNum();
        if (needMore)
            requestFrame();

    }

    public void requestFrame() {
        AnimationScheduler.get().requestAnimationFrame(animCallback);
    }
    
    /**
     * Starts a new war.
     * @return whether or not a new war was started.
     * isInDebug needed for knowing if we want fixed addresses to be used
     */
    public boolean runWar(boolean isInDebug)
    {
        long seedValue;
        if (seed.getText().startsWith(SEED_PREFIX)){
            seedValue = Long.parseLong(seed.getText().substring(SEED_PREFIX.length()));
        }
        else {
            seedValue = seed.getText().hashCode();
        }
        competition.setSeed(seedValue);

        int battlesPerGroup = Integer.parseInt(battlesPerGroupField.getText().trim());

        // having numItems and groupSize allows having 4 players and running competitions of just any 3 of them
        // this is hardly useful in reality so I just set it to the same size

        if (battlesPerGroup <= 0) {
            Console.error("battles per session needs to be more than 0");
            return false;
        }

        WarriorRepository repo = competition.getWarriorRepository();
        PlayersPanel.Code[] playerFiles = Arrays.stream(m_playersPanel.getFiles())
                .filter(code -> code.player.isEnabled)
                .toArray(PlayersPanel.Code[]::new);
        if (!repo.loadWarriors(playerFiles , m_playersPanel.getZombies(), isInDebug))
            return false;
        clear(repo.getGroupNames());

        int numOfGroups = repo.getNumberOfGroups();
        if (numOfGroups == 0) {
            Console.error("can't run without any warriors"); // TBD-ERRMSG
            return false;
        }

        try {
            competition.runCompetition(battlesPerGroup, numOfGroups, isInDebug, SettingsPanel.getEnabledOptions().getEnabledFeatures());
            startRunAnimation(); // when runWar() returns we want the War object to be already constructured and ready
            if (this.isInDebug) { // add breakpointchecked only if we're in debugger
                War war = competition.getCurrentWar();
                war.setBreakpointCheck(m_codeEditor);
                Warrior inEditorWarrior = war.getWarriorByLabel(m_playersPanel.getCodeInEditor().getLabel());
                war.setUiWarrior(inEditorWarrior);
            }
            return true;
        }
        catch (Exception e) {
            Console.log("runWar EXCEPTION " + e.toString());
            e.printStackTrace(Console.stream());
        }


        return false;
    }

    public native void clear(String[] names) /*-{
        $wnd.competitionGraph.clear(names);
    }-*/;

    public void scoreChanged(String name, float addedValue, int groupIndex, int subIndex) {
        //columnGraph.addToValue(groupIndex, subIndex, addedValue);
        addToValue(name, subIndex, addedValue);

    }

    public native void addToValue(String name, int subIndex, float addedValue) /*-{
        $wnd.competitionGraph.addToValue(name, subIndex, addedValue);
    }-*/;

    private void setDebugMode(boolean v) {
        m_codeEditor.setDebugMode(v);
        m_playersPanel.setDebugMode(v);

        battleFrame.cpuframe.setVisible(v);
        if (!v)
            battleFrame.warCanvas.revokeWar();
    }

    public boolean gui_runWar(boolean inDebug) {
        m_isBattleShown = inDebug;
        isInDebug = inDebug;
        if (runWar(inDebug)) {
            competitionRunning = true;
            if (inDebug)
                setDebugMode(true);

            stepnum.innerHTML = "0";
            battleFrame.speedSlider.setValue(0);
            return true;
        }
        return false;
    }

    public void j_debugUiInited() {
        // needs to be after gui was set up and after the editor was scrolled to its starting place and lines rendered
        // this is because if the first line has a comment, dfXXXX is only going to exist once the line is rendered
        competition.competitionEventListener.onEndRound(); // it's like round -1, show the state at the start of the game

    }

    @Override
    public void onWarPreStartClear() {}


    public void onWarStart() {

        battleFrame.cpuframe.setVisible(true); // do this here since the messages need to scroll to the bottom and it needs to be visible
    }

    private void showBattleRoom() {
        battleFrame = new WarFrame(competition, this);

        competition.addMemoryEventLister(battleFrame);
        competition.addMemoryEventLister(battleFrame.cpuframe);
        competition.addCompetitionEventListener(battleFrame);

        competition.addMemoryEventLister(battleFrame.cpuframe.stackView);
        competition.addMemoryEventLister(battleFrame.cpuframe.sharedMemView);

    }

    public void onWarEnd(int reason, String winners, boolean inDebug) {
        warCounter++;
        // this is needed so that we'll be able to recreate the war we just ran in the debugger
        // and for the seed counter to advance beteen competitions
        seed.setText(SEED_PREFIX + competition.getSeed());
        warCounterDisplay.setText("Sessions so far:" + warCounter + " (out of " + totalWars + ")");
        setBattlesRan(Integer.toString(warCounter) + "/" + Integer.toString(totalWars));
    }

    public static native void setBattlesRan(String round) /*-{
        $wnd.battlesRan.innerHTML = round
    }-*/;


    public void onRound(int round) {
    }
    @Override
    public void onPaused() {}
    @Override
    public void onNoneAlive() {}
    public void onWarriorBirth(Warrior w) {}
    public void onWarriorDeath(Warrior warrior, String reason) {}

    public void onCompetitionStart() {
        warCounter = 0;
        totalWars = competition.getTotalNumberOfWars();
    }

    public static native void jsCompeteFinish() /*-{
        $wnd.competeFinished();
    }-*/;


    public void onCompetitionEnd() {
        warCounterDisplay.setText("The competition is over. " + warCounter + " sessions were run.");
		competitionRunning = false;
        jsCompeteFinish();
    }
    
	@Override
	public void onEndRound() {
	}

	// when an edit in the registers has parse error
	public void errorPreventsStep(boolean v) {

        battleFrame.btnPause.setEnabled(!v);
        battleFrame.btnSingleRound.setEnabled(!v);
    }

    // war needs to know which player's breakpoint to check
    public void srcSelectionChanged(String label) {
        War war = competition.getCurrentWar();
        if (war == null)
            return; // not in debug mode
        Warrior warrior = war.getWarriorByLabel(label);
        war.setUiWarrior(warrior);
    }
	
}