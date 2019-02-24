package il.co.codeguru.corewars_riscv.gui;

import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLElement;
import il.co.codeguru.corewars_riscv.cpu.riscv.CpuStateRiscV;
import il.co.codeguru.corewars_riscv.gui.widgets.*;
import il.co.codeguru.corewars_riscv.jsadd.Format;
import il.co.codeguru.corewars_riscv.memory.MemoryEventListener;
import il.co.codeguru.corewars_riscv.utils.Logger;
import il.co.codeguru.corewars_riscv.utils.Unsigned;
import il.co.codeguru.corewars_riscv.war.Competition;
import il.co.codeguru.corewars_riscv.war.CompetitionEventListener;
import il.co.codeguru.corewars_riscv.war.War;
import il.co.codeguru.corewars_riscv.war.Warrior;

import static il.co.codeguru.corewars_riscv.war.War.ARENA_SIZE;

/**
 * The main GUI class for core-wars.
 * The frame includes:
 * <ul>
 * <li> Canvas for showing the memory
 * <li> a list of warrior names
 * <li> messaging area
 * <li> start/stop buttons
 * <li> speed slider
 * </ul>
 *
 * @author BS
 */
public class WarFrame extends JFrame implements MemoryEventListener, CompetitionEventListener {

    private final Competition competition;
    /**
     * the canvas which show the core war memory area
     */
    Canvas warCanvas;
    CpuFrame cpuframe;
    JButton btnPause;
    JButton btnSingleRound;
    JSlider speedSlider;
    /**
     * the message area show misc. information about the current fight
     */
    private JTextArea messagesArea;
    /**
     * Model for the name list
     */
    private DefaultListModel nameListModel;
    /**
     * Holds the current round number
     */
    private int nRoundNumber;

    private CompetitionWindow mainWnd;


    public WarFrame(final Competition competition, final CompetitionWindow mainWnd) {
        super("CodeGuru Extreme - Session Viewer");

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.competition = competition;
        this.mainWnd = mainWnd;
        getContentPane().setLayout(new BorderLayout());

        // build widgets
        JPanel mainPanel = new JPanel(new BorderLayout());

        // build war zone (canvas + title)
        JPanel warZone = new JPanel(new BorderLayout());
        warZone.setBackground(Color.BLACK);

        JPanel canvasPanel = new JPanel();
        canvasPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(169, 154, 133), 3),
                BorderFactory.createEmptyBorder(10, 10, 20, 10)));
        canvasPanel.setBackground(Color.BLACK);
        warCanvas = new Canvas("warCanvas");
        canvasPanel.add(warCanvas);
        warZone.add(canvasPanel, BorderLayout.CENTER);

        mainPanel.add(warZone, BorderLayout.CENTER);

        // build info zone (message area + buttons)
        JPanel infoZone = new JPanel(new BorderLayout());
        messagesArea = new JTextArea("messagesArea", 5, 60);
        messagesArea.setFont(new Font("Tahoma", Font.PLAIN, 12));

        infoZone.add(new JScrollPane(messagesArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();

        buttonPanel.add(new JLabel("Speed:"));

        HTMLElement speedSliderVal = (HTMLElement) DomGlobal.document.getElementById("speedSliderVal");
        speedSlider = new JSlider("speedSlider", "speedSliderVal");
        speedSlider.addActionListener(e -> {
            int s = speedSlider.getValue();
            if (s > 100)
                s = (int) Math.pow((s - 80.0), 1.5);
            speedSliderVal.innerHTML = Integer.toString(s);
            this.competition.setSpeed(s); //exponential speed slider
        });
        buttonPanel.add(speedSlider);
        nRoundNumber = 0;
        infoZone.add(buttonPanel, BorderLayout.SOUTH);
        infoZone.setBackground(Color.black);

        // Debugger
        JLabel addressFiled = new JLabel("Click on the arena to see the memory");
        cpuframe = new CpuFrame(competition, this.mainWnd);
        competition.addCompetitionEventListener(cpuframe);

        competition.addCompetitionEventListener(this);

        btnPause = new JButton("btnPause", "XXPause");
        btnPause.setEnabled(false);
        btnPause.addActionListener(arg -> {
            if (competition.getCurrentWar() == null) // pauses between wars
            {
                if (competition.globalPause) { // do resume
                    competition.globalPause = false;
                    btnPause.setText("Pause");
                    mainWnd.requestFrame();
                    btnSingleRound.setEnabled(false);
                } else { // do pause
                    competition.globalPause = true;
                    btnPause.setText("Resume");
                    if (competition.getCurrentWar() != null)
                        btnSingleRound.setEnabled(true);
                }
            } else {
                if (competition.getCurrentWar().isPaused()) {
                    competition.getCurrentWar().resume();
                    btnPause.setText("Pause");
                    mainWnd.requestFrame();
                    btnSingleRound.setEnabled(false);
                } else {
                    competition.getCurrentWar().pause();
                    btnPause.setText("Resume");
                    btnSingleRound.setEnabled(true);
                }
            }

        });

        btnSingleRound = new JButton("btnSingleRound", "Single Round");
        btnSingleRound.setEnabled(false);
        btnSingleRound.addActionListener(arg0 -> {
            if (competition.getCurrentWar() == null) {
                Console.log("no war");
                return;
            }
            competition.getCurrentWar().runSingleRound();
            mainWnd.requestFrame(); // request frame but still paused so it'll be just one frame
        });

        buttonPanel.add(btnPause);
        buttonPanel.add(btnSingleRound);
        buttonPanel.add(addressFiled);

        // build warrior zone (warrior list + title) 
        JPanel warriorZone = new JPanel(new BorderLayout());
        warriorZone.setBackground(Color.BLACK);
        nameListModel = new DefaultListModel();
        /*
         * list of warrior names
         */
        JList nameList = new JList(nameListModel);
        nameList.setPreferredSize(new Dimension(200, 0));
        nameList.setCellRenderer(new NameCellRenderer());
        nameList.setOpaque(false);
        nameList.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(169, 154, 133), 3),
                BorderFactory.createEmptyBorder(10, 10, 20, 10)));
        nameList.repaint();
        warriorZone.add(nameList, BorderLayout.CENTER);
        warriorZone.add(Box.createHorizontalStrut(20), BorderLayout.WEST);
        mainPanel.add(warriorZone, BorderLayout.EAST);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(Color.BLACK);
        getContentPane().add(mainPanel, BorderLayout.CENTER);
        getContentPane().add(infoZone, BorderLayout.SOUTH);
    }


    @Override
    public void onPaused() { // this can potentially replace all other places where we do the same thing
        btnPause.setText("Resume");
        btnSingleRound.setEnabled(true);
    }

    /**
     * Add a message to the message zone
     */
    private void addMessage(String message) {
        messagesArea.append(message + "\n");
        messagesArea.scrollToBottom();
    }

    /**
     * Add a message to the message zone (with round number)
     */
    private void addMessage(int round, String message) {
        addMessage("[" + round + "] " + message);
    }

    @Override
    public void onMemoryWrite(int address, byte value) {
        if (!mainWnd.isBattleShown())
            return; // canvas not shown, no reason to update it


        if (address >= 0 && address < ARENA_SIZE) {
            warCanvas.paintPixel(Unsigned.unsignedShort(address), (byte) competition.getCurrentWarrior(), value);
        }
    }

    @Override
    public void onWriteState(EWriteState state) {
    }

    @Override
    public void onWarPreStartClear() {
        nameListModel.clear();
        warCanvas.clear();
        warCanvas.initStartWar(competition.getCurrentWar());
    }

    /* @see CompetitionEventListener#onWarStart(int) */
    public void onWarStart() {
        addMessage("=== Session started ===");
        if (competition.getCurrentWar().isPaused()) {
            btnPause.setText("Resume");
            btnSingleRound.setEnabled(true);
        }
    }

    public void onWarEnd(int reason, String winners, boolean inDebug) {
        if (!inDebug)
            return;

        switch (reason) {
            case SINGLE_WINNER:
                addMessage(nRoundNumber, "Session over: The winner is " + winners + "!");
                break;
            case MAX_ROUND_REACHED:
                addMessage(nRoundNumber, "Maximum round reached: The winners are " + winners + "!");
                break;
            case ABORTED:
                addMessage(nRoundNumber, "Session aborted: The winners are " + winners + "!");
                break;
            default:
                throw new RuntimeException();
        }
        btnPause.setText("Resume");
        btnSingleRound.setEnabled(true);

        warCanvas.revokeWar();
    }

    @Override
    public void onNoneAlive() {
        addMessage(nRoundNumber, "No players left alive");
        btnSingleRound.setEnabled(false);
        btnPause.setEnabled(false);

    }


    /**
     * @see CompetitionEventListener#onRound(int)
     */
    public void onRound(int round) {
        if (!mainWnd.isBattleShown())
            return;

        nRoundNumber = round;
        btnPause.setEnabled(true);
    }

    /**
     * @see CompetitionEventListener#onWarriorBirth(Warrior)
     */
    public void onWarriorBirth(Warrior w) {
        addMessage(nRoundNumber, w.getName() + " enters the arena at " + Format.hex4(w.getLoadOffset() & 0xffff));
        nameListModel.addElement(new WarriorInfo(w.getName()));
    }

    /* @see CompetitionEventListener#onWarriorDeath(String) */
    public void onWarriorDeath(Warrior warrior, String reason) {
        String warriorName = warrior.getName();
        addMessage(nRoundNumber, warriorName + " died due to " + reason + ".");

    }

    public void onCompetitionStart() {
        btnPause.setEnabled(true);
    }

    public void onCompetitionEnd() {
        btnPause.setEnabled(false);
    }

    @Override
    public void onEndRound() {
        if (!mainWnd.isBattleShown())
            return; // canvas not shown, no reason to update it

        this.warCanvas.deletePointers();
        War currentWar = this.competition.getCurrentWar();
        for (int i = 0; i < currentWar.getNumWarriors(); i++)
            if (currentWar.getWarrior(i).isAlive()) {
                CpuStateRiscV state = currentWar.getWarrior(i).getCpuState();
                int ip = state.getPc();

                this.warCanvas.paintPointer((char) ip, (byte) i);
            }
    }

    /**
     * A renderer for the names on the warrior list.
     * Paints each warrior with its color and uses <S>strikeout</S> to show
     * dead warriors.
     */
    class NameCellRenderer extends JLabel {
        private static final long serialVersionUID = 1L;

        private static final int FONT_SIZE = 20;

        /**
         * Construct a name cell renderer
         * Set font size to FONT_SIZE.
         */
        public NameCellRenderer() {
            setFont(new Font("Tahoma", Font.PLAIN, FONT_SIZE));
        }

        /**
         * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
         */
        public Object getListCellRendererComponent(Object value, int index) {
            WarriorInfo info = (WarriorInfo) value;

            String text = info.name;
            if (!info.alive) {
                // strike out dead warriors
                text = "<html><S>" + text + "</S></html>";
            }
            setText(text);
            Color c = ColorHolder.getInstance().getColor(index, false);

            setForeground(c);
            return this;
        }
    }

    class WarriorInfo {
        String name;
        boolean alive;

        public WarriorInfo(String name) {
            this.name = name;
            this.alive = true;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            return (obj instanceof String) && (obj.equals(name));
        }
    }

}