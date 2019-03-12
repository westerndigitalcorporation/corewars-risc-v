package il.co.codeguru.corewars_riscv.war;

import il.co.codeguru.corewars_riscv.features.Feature;
import il.co.codeguru.corewars_riscv.features.FeatureSet;
import il.co.codeguru.corewars_riscv.gui.widgets.EventMulticasterCompetition;
import il.co.codeguru.corewars_riscv.gui.widgets.EventMulticasterMemory;
import il.co.codeguru.corewars_riscv.memory.MemoryEventListener;
import il.co.codeguru.corewars_riscv.utils.Logger;


public class Competition {

    /** Maximum number of rounds in a single war. */
    public final static int MAX_ROUND = 200000;
    private static final String SCORE_FILENAME= "scores.csv";

    private CompetitionIterator competitionIterator;

    public EventMulticasterCompetition competitionEventCaster;
    public EventMulticasterMemory memoryEventCaster;
    public CompetitionEventListener competitionEventListener;
    private MemoryEventListener memoryEventListener;

    private WarriorRepository warriorRepository;

    private War currentWar;

    private int warsPerCombination = 20;


    private int speed;  // while debugging. 0 means 1 step each frame, >0 means how many steps to make each frame, <0 means how many frames to skip between steps

    private long seed = 0;

    public boolean globalPause = false;

    // continue state between animation frames
    public static class CompState {
        int warIndex = 0;
        boolean isInDebugger; // should we return after each round or after each war for a UI update. also activates breakpoints
        public int round;
        public boolean abort = false;
        int waitedFrames = 0; // how many frames we just waited for negative speed
        long startTime = 0;
        boolean useNewMemory;
        Feature[] features;
    }
    public CompState compState;

    public Competition() {
        warriorRepository = new WarriorRepository();

        competitionEventCaster = new EventMulticasterCompetition();
        competitionEventListener = competitionEventCaster.debugProxy;
        memoryEventCaster = new EventMulticasterMemory();
        memoryEventListener = memoryEventCaster.debugProxy;
        speed = 0;
    }

    private void switchToCompete() {
        competitionEventListener = competitionEventCaster.competeProxy;
        memoryEventListener = memoryEventCaster.competeProxy;
    }
    private void switchToDebug() {
        competitionEventListener = competitionEventCaster.debugProxy;
        memoryEventListener = memoryEventCaster.debugProxy;
    }

    private void doneCompetition() {
        competitionEventListener.onCompetitionEnd();
        long elapsed = System.currentTimeMillis() - compState.startTime;
        Logger.log("Total time=" + elapsed / 1000.0);
        compState = null;
    }

    // return true if need to continue after
    public boolean continueRun() throws Exception
    {
        if (globalPause) {
            Logger.log("globalPause");
            return false;
        }
        if (compState.abort) {
            Logger.log("Abort");
            doneWar();
            doneCompetition();
            return false;
        }
        if (currentWar == null)
        {
            if (compState.warIndex < warsPerCombination) 
            {
                startWar( warriorRepository.createGroupList(competitionIterator.next()));
                return !compState.isInDebugger;
            }
            else {
                doneCompetition();
                return false;
            }
        }
        else
        {
            int needMore = 1;
            if (compState.isInDebugger) {
                int stepsCount = calculateSpeed();
                if (stepsCount > 0) {
                    switchToCompete();
                    while (needMore == 1 && stepsCount > 1) {
                        needMore = runRound();
                        --stepsCount;
                    }
                    switchToDebug();
                    if (needMore == 1)
                        needMore = runRound();
                }
            }
            else {
                do {
                    needMore = runRound();
                } while (needMore == 1);
            }

            if (compState.isInDebugger && currentWar.hasEnded() && currentWar.getNumRemainingWarriors() == 0) {
                competitionEventListener.onNoneAlive();
                return false;
            }

            if (needMore == 0) { // we return false due to being paused
                competitionEventListener.onPaused();
                return false;
            }
            else if (needMore == -1) {
                doneWar();
                // when in debugger, we want to have the ability to continue stepping in a stopped war to see how it goes
                // and it doesn't make sense any way to start a new war
                if (!compState.isInDebugger) {
                    currentWar = null;
                    return true;
                }
                return false;
            }
            return true; // what's left is that it's equal to 1
        }
    }



    public void runCompetition(int warsPerCombination, int warriorsPerGroup, boolean isInDebugger, boolean useNewMemory, Feature[] features) {
        this.warsPerCombination = warsPerCombination;
        Logger.log("Running competition");
        competitionIterator = new CompetitionIterator(warriorRepository.getNumberOfGroups(), warriorsPerGroup, seed);

        // run on every possible combination of warrior groups
        competitionEventListener.onCompetitionStart();
        Logger.log("runCompetition " + warsPerCombination + " wars");

        compState = new CompState();
        compState.warIndex = 0;
        currentWar = null;
        compState.isInDebugger = isInDebugger;
        compState.startTime = System.currentTimeMillis();
        compState.useNewMemory = useNewMemory;
        compState.features = features;

        if (isInDebugger)
            switchToDebug();
        else
            switchToCompete();

    }

    public int getTotalNumberOfWars() {
        return (int) competitionIterator.getNumberOfItems() * warsPerCombination;
    }

    // return 1 if need another round, 0 if paused, -1 if we're done
    private int runRound()
    {
        competitionEventListener.onRound(compState.round);

        boolean atBreakpoint = currentWar.nextRound(compState.round);

        ++compState.round;

        competitionEventListener.onEndRound();

        // it's possible to continue stepping in a war that has ended and was over
        // don't tell that it's over every time, just on the time it ended first.
        if (!currentWar.hasEnded() && currentWar.isOver()) {
            currentWar.pause();
            return -1;
        }


        if (currentWar.isSingleRound() || atBreakpoint) {
            currentWar.pause();
        }
        if (currentWar.isPaused()) {
            return 0;
        }
        if (compState.round >= MAX_ROUND) {
            currentWar.pause();
            return -1;
        }

        return 1;
    }

    private void startWar(WarriorGroup[] warriorGroups) throws Exception
    {
        currentWar = new War(memoryEventListener, competitionEventListener, compState.isInDebugger, compState.useNewMemory, compState.features);
        currentWar.setSeed(this.seed);
        competitionEventListener.onWarPreStartClear();
        currentWar.loadWarriorGroups(warriorGroups);
        competitionEventListener.onWarStart(); // need to be before loadWarriorGroups since this clears the canvas and that writes the warriors to the canvas
        compState.round = 0;
    }

    private void doneWar()
    {
        if (currentWar == null)
            return;
        competitionEventListener.onRound(compState.round);

        ++seed; // make sure the next war is differently randomized

        int numAlive = currentWar.getNumRemainingWarriors();
        String names = currentWar.getRemainingWarriorNames();

        if (numAlive == 1) { // we have a single winner!
            competitionEventListener.onWarEnd(CompetitionEventListener.SINGLE_WINNER, names, compState.isInDebugger);
            currentWar.updateScores(warriorRepository);
        } else if (compState.round == MAX_ROUND) { // maximum round reached
            competitionEventListener.onWarEnd(CompetitionEventListener.MAX_ROUND_REACHED, names, compState.isInDebugger);
            currentWar.updateScores(warriorRepository);
        } else { // user abort
            competitionEventListener.onWarEnd(CompetitionEventListener.ABORTED, names, compState.isInDebugger);
            // don't update scores on abort since that would create fraction score
        }
        currentWar.setEnded();
        ++compState.warIndex;


    }

    public int getCurrentWarrior() {
        if (currentWar != null) {
            return currentWar.getCurrentWarrior();
        } else {
            return -1;
        }
    }
    private int calculateSpeed() {
        int stepsCount = 1;
        if (!currentWar.isSingleRound()) { // speed doesn't do anyhthing when clicking single step
            if (speed > 1)
                stepsCount = speed;
            else if (speed < 0) {
                if (compState.waitedFrames > 0) {
                    --compState.waitedFrames;
                    stepsCount = 0;
                }
                else { // == 0
                    compState.waitedFrames = -speed;
                }
            }
        }
        return stepsCount;
    }


    public void addCompetitionEventListener(CompetitionEventListener lis) {
        competitionEventCaster.add(lis);
    }
    
    public void addMemoryEventLister(MemoryEventListener lis) {
        memoryEventCaster.add(lis);
    }
    
    public WarriorRepository getWarriorRepository() {
        return warriorRepository;
    }


    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getSpeed() {
        return speed;
    }

    public void setAbort() {
        if (compState == null)
            return;
        compState.abort = true;
    }
    
    
    public War getCurrentWar(){
    	return currentWar;
    }
    
    public void setSeed(long seed){
    	this.seed = seed;
    }

    public long getSeed(){
        return seed;
    }
}