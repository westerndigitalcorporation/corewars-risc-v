package il.co.codeguru.corewars_riscv.war;

import il.co.codeguru.corewars_riscv.cpu.exceptions.CpuException;
import il.co.codeguru.corewars_riscv.memory.RawMemory;
import il.co.codeguru.corewars_riscv.gui.IBreakpointCheck;
import il.co.codeguru.corewars_riscv.memory.MemoryEventListener;
import il.co.codeguru.corewars_riscv.memory.MemoryException;
import il.co.codeguru.corewars_riscv.utils.Logger;
import il.co.codeguru.corewars_riscv.utils.Unsigned;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static il.co.codeguru.corewars_riscv.memory.RawMemory.*;


/**
 * Implements the main "war-logic".
 * 
 * @author DL
 */
public class War {
    /** Arena's code segment */
    public final static short ARENA_SEGMENT = 0;

    /** Arena's size in bytes (= size of a single segment) */
    public final static int ARENA_SIZE =
        PARAGRAPHS_IN_SEGMENT * PARAGRAPH_SIZE;
    /** Warrior's private stack size */
    public final static short STACK_SIZE = 2*1024;
    /** Group-shared private memory size */
    public final static short GROUP_SHARED_MEMORY_SIZE = 1024;
    /** Arena is filled with this byte */
    private final static byte ARENA_BYTE = (byte)0x00;
    /** Maximum number of warriors in a fight */
    private final static int MAX_WARRIORS = 20;
    /** Maximum attempts to load a warrior to the Arena */
    private final static int MAX_LOADING_TRIES = 100;
    /** Minimum initial space (in bytes) between loaded warriors */
    private final static int MIN_GAP = 1024;

    /** Warriors in the fight */
    private Warrior[] m_warriors;
    /** Number of loaded warriors */
    private int m_numWarriors;
    /** Number of warriors still alive */
    private int m_numWarriorsAlive;
    /**
     * Addresses equal or larger than this are still unused.
     * An address can be 'used' either by the Arena, or by the private stacks.
     */
    private int m_nextFreeAddress;
    /** The 'physical' memory core */
    private RawMemory m_core;

    /** The number of the current warrior */
    private int m_currentWarrior;

    /** The listener for war events */
    private CompetitionEventListener m_warListener;

    private IBreakpointCheck m_breakpointCheck = null;
    private int m_uiWarriorIndex = -1; // break in breakpoints only of this warrior (he's the one selected in the PlayersPanel)
    private boolean m_hasEnded = false; // this war has ended but the object remains alive for post-mortem examination
    private boolean useNewMemory;

    public void setUiWarrior(Warrior warrior) {
        if (warrior != null)
            m_uiWarriorIndex = warrior.m_myIndex;
        else
            m_uiWarriorIndex = -1; // can happen if we're not debugging right now
    }
    public void setBreakpointCheck(IBreakpointCheck brc) {
        m_breakpointCheck = brc;
    }
    public boolean hasEnded() {
        return m_hasEnded;
    }
    public void setEnded() {
        m_hasEnded = true;
    }

    /**
     * Constructor.
     * Fills the Arena with its initial data. 
     */
    public War(MemoryEventListener memoryListener, CompetitionEventListener warListener, boolean startPaused, boolean useNewMemory) {
    	isPaused = startPaused; //startPaused; // startPause just causes control to  return after startWar, we don't want to pause the first round
        m_warListener = warListener;
        m_warriors = new Warrior[MAX_WARRIORS];
        m_numWarriors = 0;
        m_numWarriorsAlive = 0;
        m_core = new RawMemory(MEMORY_SIZE);
        m_nextFreeAddress = ARENA_SIZE;
        this.useNewMemory = useNewMemory;

        // initialize arena
        for (int offset = 0; offset < ARENA_SIZE; ++offset) {
            m_core.storeByte(offset, ARENA_BYTE);
        }

        isSingleRound = false;
        
        // set the memory listener (we only do this now, to skip initialization)
        m_core.setListener(memoryListener);
    }
	
    /**
     * Runs a single round of the war (every living warrior does his turn).
     * @param round The current round number.
     * @return true when a breakpoint was hit
     */
    public boolean nextRound(int round) {
        boolean atBreakpoint = false;
        for (int i = 0; i < m_numWarriors; ++i)
        {
            Warrior warrior = m_warriors[i];
            m_currentWarrior = i;
            if (warrior.isAlive()) {
                int savedIp = warrior.getCpuState().getPc();
                try {

                    // run first InstructionInfo
                    warrior.nextOpcode();
                    atBreakpoint |= (m_breakpointCheck != null && m_currentWarrior == m_uiWarriorIndex && m_breakpointCheck.shouldBreak(warrior.getCpuState()));

                    // run one extra InstructionInfo, if warrior deserves it :)
                    updateWarriorEnergy(warrior, round);
                    if (shouldRunExtraOpcode(warrior)) {
                        warrior.nextOpcode();
                        atBreakpoint |= (m_breakpointCheck != null && m_currentWarrior == m_uiWarriorIndex && m_breakpointCheck.shouldBreak(warrior.getCpuState()));
                    }
                }
                catch (CpuException e) {
                    if(m_warListener != null)
                        m_warListener.onWarriorDeath(warrior, "CPU exception");
                    warrior.kill();
                    warrior.getCpuState().setPc(savedIp); // don't advance IP, show where the exception occured
                    --m_numWarriorsAlive;
                }
                catch (MemoryException e) {
                    if(m_warListener != null)
                        m_warListener.onWarriorDeath(warrior, "Memory exception: " + e.getMessage());
                    warrior.kill();
                    warrior.getCpuState().setPc(savedIp);
                    --m_numWarriorsAlive;
                }
            }
        }
        m_currentWarrior = -1;
        return atBreakpoint;
    }

    /**
     * @return whether or not the War is over.
     */
    public boolean isOver() {
        // when in debugger, run until everybody dies
        return (m_numWarriorsAlive < 2);
    }


	
    /**
     * Decrements the warrior's Energy value, if the current round is
     * a multiple of DECELERATION_ROUNDS.
     * 
     * @param warrior The warrior.
     * @param round   Current round number.
     */
    private void updateWarriorEnergy(Warrior warrior, int round) {
        if ((round % DECELERATION_ROUNDS) == 0) {
            int energy = Unsigned.unsignedShort(warrior.getEnergy());

            if (energy > 0 ) {
                warrior.setEnergy((short)(energy-1));
            }
        }
    }
	
    /**
     * Determines whether or not a given warrior deserves an extra InstructionInfo,
     * by calculating the warrior's current speed (using its current Energy
     * value), and comparing it against a random value.
     * 
     * We use a random-based algorithm (as opposed to a deterministic one) for
     * the following reasons:
     *  a) simple implementation - there is no need to keep record of past
     *     decisions as our algorithm is stateless.
     *  b) we want the warrior's speed to vary between x1.0 to x2.0, and this
     *     solves the issue of determining what to do if the current speed is x1.7 :)
     * 
     * @param warrior The warrior.
     * @return true if the warrior deserves an extra InstructionInfo, otherwise
     * returns false.
     */
    private boolean shouldRunExtraOpcode(Warrior warrior) {
        short enr = warrior.getEnergy();
        if (enr == 0)
            return false;
        int energy = Unsigned.unsignedShort(enr);
        int speed = calculateWarriorSpeed(energy);

        return (rand.nextInt(MAX_SPEED) < speed);
    }

    /** Maximum possible Warrior speed. */
    private final int MAX_SPEED = 16; // when Energy = 0xFFFF 
    /** Warrior's Energy is decremented every so often rounds. */ 
    private final int DECELERATION_ROUNDS = 5;
	
    /**
     * Returns the warrior's current speed, using the following formula:
     * Speed := Min(MAX_SPEED, 1+Log2(Energy))
     * 
     * This formula forces the warrior to put more and more effort in order to
     * increase its speed, i.e. non-linear effort.
     *  
     * @param energy The warrior's Energy value.
     * @return the warrior's current speed,
     */
    private int calculateWarriorSpeed(int energy) {
        if (energy == 0) {
            return 0;
        } else {
            return Math.min(MAX_SPEED, 1 + (int)(Math.log(energy) / Math.log(2)));
        }
    }

    /**
     * Loads the given warrior groups to the Arena.
     * @param warriorGroups The warrior groups to load.
     * @throws Exception
     */
    public void loadWarriorGroups(WarriorGroup[] warriorGroups) throws Exception {
        m_currentWarrior = 0;
        ArrayList<WarriorGroup> groupsLeftToLoad = new ArrayList<>(Arrays.asList(warriorGroups));

        while (groupsLeftToLoad.size() > 0)
        {
            int randomInt = rand.nextInt(groupsLeftToLoad.size());
            loadWarriorGroup(groupsLeftToLoad.get(randomInt));
            groupsLeftToLoad.remove(randomInt);
        }
        m_currentWarrior = -1;
    }

    private void loadWarriorGroup(WarriorGroup warriorGroup) throws Exception {
        List<WarriorData> warriors = warriorGroup.getWarriors();

        int groupSharedMemory = allocateCoreMemory(GROUP_SHARED_MEMORY_SIZE);

        for (WarriorData warrior : warriors) {
            String warriorName = warrior.getName();
            byte[] warriorData = warrior.getCode();

            int loadOffset;
            if (warrior.m_debugFixedLoadAddress < 0)
                loadOffset = getLoadOffset(warriorData.length);
            else
                loadOffset = (short) warrior.m_debugFixedLoadAddress;

            int stackMemory = allocateCoreMemory(STACK_SIZE);

            Warrior w = new Warrior(
                    warriorName,
                    warrior.getLabel(),
                    warriorData.length,
                    m_core,
                    loadOffset,
                    stackMemory,
                    groupSharedMemory,
                    m_numWarriors,
                    useNewMemory);
            m_warriors[m_numWarriors++] = w;

            // load warrior to arena
            if (m_core.getListener() != null)
                m_core.getListener().onWriteState(MemoryEventListener.EWriteState.ADD_WARRIORS);
            for (int offset = 0; offset < warriorData.length; ++offset) {
                m_core.storeByte((loadOffset + offset) & 0xFFFF, warriorData[offset]);
            }
            if (m_core.getListener() != null)
                m_core.getListener().onWriteState(MemoryEventListener.EWriteState.RUN);
            ++m_numWarriorsAlive;
            ++m_currentWarrior;

            // notify listener
            if (m_warListener != null)
                m_warListener.onWarriorBirth(w);
        }

    }

    /**
     * Virtually allocates core memory of a given size, by advancing the
     * next-free-memory pointer (m_nextFreeAddress).
     * 
     * @param size   Required memory size, must be a multiple of
     *               RawMemory.PARAGRAPH_SIZE
     * @return Pointer to the beginning of the allocated memory block.
     */
    private int allocateCoreMemory(short size) {
        if ((size % PARAGRAPH_SIZE) != 0) {
            throw new IllegalArgumentException();
        }
        int allocatedMemory = m_nextFreeAddress;

        m_nextFreeAddress += size;

        return allocatedMemory;
    }
	
    /**
     * Returns a suitable random address to which a warrior with a given code
     * size can be loaded.
     * 
     * A suitable address is-
     *  1. far enough from the Arena's boundaries.
     *  2. far enough from other loaded warriors.
     * 
     * @param warriorSize   Code size of the loaded warrior. 
     * @return offset within the Arena to which the warrior can be loaded.
     * @throws Exception if no suitable address could be found.
     */
    private short getLoadOffset(int warriorSize) throws Exception {
        int loadAddress = 0;
        boolean found = false;
        int numTries = 0;

        while ((!found) && (numTries < MAX_LOADING_TRIES)) {
            ++numTries;

            loadAddress = rand.nextInt(ARENA_SIZE);

            found = loadAddress >= MIN_GAP;

            if (loadAddress+warriorSize > ARENA_SIZE-MIN_GAP) {
                found = false;
            }

            // check intersections with fixed (which might still be not loaded
            if (WarriorRepository.m_Fixed_loadAddressChecker != null) {
                if (!WarriorRepository.m_Fixed_loadAddressChecker.checkOverlap(loadAddress, warriorSize)) {
                    found = false;
                    continue;
                }
            }

            // check intersections with loaded
            for (int i = 0; i < m_numWarriors; ++i) {
                int otherLoadAddress =
                    Unsigned.unsignedShort(m_warriors[i].getLoadOffset());
                int otherSize = m_warriors[i].getCodeSize();

                int otherStart = otherLoadAddress-MIN_GAP;
                int otherEnd = otherLoadAddress+otherSize+MIN_GAP;

                if ((loadAddress+warriorSize >= otherStart) && (loadAddress < otherEnd)) {
                    found = false;
                    break;
                }
            }
        }

        if (!found) {
            throw new RuntimeException("Too many players - not enough space in the memory left to spawn all warriors");
        }

        return (short)loadAddress;
    }
	
    /**
     * @return Returns the currentWarrior.
     */
    public int getCurrentWarrior() {
        return m_currentWarrior;
    }

    /** @return the warrior with the given index */
    public Warrior getWarrior(int index) {
        return m_warriors[index];
    }
    public Warrior getWarriorByLabel(String label) {
        for (int i = 0; i < m_numWarriors; ++i) {
            Warrior w = m_warriors[i];
            if (w.getLabel().equals(label)) {
                return w;
            }
        }
        return null;
    }

    /** @return the number of warriors fighting in this match. */
    public int getNumWarriors() {
        return m_numWarriors;
    }
    
    /** @return the number of warriors still alive. */
    public int getNumRemainingWarriors() {
    	return m_numWarriorsAlive;
    }
    
    /** @return a comma-separated list of all warriors still alive. */
    public String getRemainingWarriorNames() {
        StringBuilder names = new StringBuilder();
    	for (int i = 0; i < m_numWarriors; ++i) {
            Warrior warrior = m_warriors[i];
            if (warrior.isAlive()) {
                names.append(", ").append(warrior.getName());
            }
    	}
    	return names.toString();
    }    
 
    /**
     * Updates the scores in a given score-board.
     */
    public void updateScores(WarriorRepository repository) {
        float score = (float)1.0 / m_numWarriorsAlive;
    	for (int i = 0; i < m_numWarriors; ++i) {
            Warrior warrior = m_warriors[i];
            if (warrior.isAlive()) {
                repository.addScore(warrior.getName(), score);
            }
    	}
    }
    
    private Random rand = new Random();
    
    private boolean isSingleRound;
    private boolean isPaused;
    
    public void setSeed(long seed){
    	rand.setSeed(seed);
    }
    
    public void pause(){
    	isPaused = true;
    }
    
    public boolean isPaused(){
    	return isPaused;
    }
    
    public void resume(){
    	isPaused = false;
    	isSingleRound = false;
    }
    
    public void runSingleRound(){
        isPaused = false;
    	isSingleRound = true;
    }
    
    public boolean isSingleRound(){
    	return this.isSingleRound;
    }
    
    public RawMemory getMemory(){
    	return m_core;
    }
    
    
}