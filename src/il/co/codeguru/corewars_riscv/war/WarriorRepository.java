package il.co.codeguru.corewars_riscv.war;

import java.util.*;

import il.co.codeguru.corewars_riscv.gui.FixedLoadAddressChecker;
import il.co.codeguru.corewars_riscv.gui.PlayersPanel;
import il.co.codeguru.corewars_riscv.gui.widgets.*;
import il.co.codeguru.corewars_riscv.utils.Logger;


public class WarriorRepository {

    /** Maximum initial code size of a single warrior */	
    public final static int MAX_WARRIOR_SIZE = 512;

    public List<WarriorGroup> getWarriorGroups() {
        return warriorGroups;
    }

    private List<WarriorGroup> warriorGroups;
    private WarriorGroup zombieGroup;
    private Map<String,Integer> warriorNameToGroup;  // map name to group index in warriorGroups

    private EventMulticasterScore scoreEventsCaster;
    private ScoreEventListener scoreListener;

    public WarriorRepository() {
        warriorNameToGroup = new HashMap<>();
        warriorGroups = new ArrayList<>();

        scoreEventsCaster = new EventMulticasterScore();
        scoreListener = scoreEventsCaster.proxy;
    }

    public void addScoreEventListener(ScoreEventListener lis) {
        scoreEventsCaster.add(lis);
        scoreEventsCaster.doneAdding();
    }

    public void addScore(String name, float value) {
        Integer groupIndex = warriorNameToGroup.get(name);
        if (groupIndex == null) {// zombies
            return;
        }
        WarriorGroup group = warriorGroups.get(groupIndex);
        int subIndex = group.addScoreToWarrior(name, value);
        if(scoreEventsCaster.isEnabled())
            scoreListener.scoreChanged(name, value, groupIndex, subIndex);
    }

    public int getNumberOfGroups() {
        return warriorGroups.size();
    }

    public String[] getGroupNames() {
        List<String> names = new ArrayList<>();
        for (WarriorGroup group : warriorGroups) {
            names.add(group.getName());
        }
        return names.toArray(new String[0]);
    }

    private byte[] truncToSize(byte[] arr) {
        if (arr.length > MAX_WARRIOR_SIZE)
            return Arrays.copyOf(arr, MAX_WARRIOR_SIZE);
        return arr;
    }

    // it's an ugly singleton but that's the only reasonable way War and WarriorRepository can communicate about fixed addresses
    //TODO: Make this class not singleton
    static FixedLoadAddressChecker m_Fixed_loadAddressChecker;

    private boolean loadZombies(PlayersPanel.Code[] zombieFiles, FixedLoadAddressChecker fixedLoadAddressChecker) {
        zombieGroup = null;
        if (zombieFiles == null || zombieFiles.length == 0)
            return true;

        zombieGroup = new WarriorGroup("ZoMbIeS");
        for (PlayersPanel.Code c : zombieFiles) {
            if (!validateWarrior(c, "zombie")) return false;

            int startAddress = -1;
            if (!c.startAddrRandom) {
                startAddress = fixedLoadAddressChecker.addCheck(c.startAddress, c.bin.length, c.getName());
                if (startAddress == -2)
                    return false;
            }

            WarriorData data = new WarriorData(c.getName(), truncToSize(c.getBin()), c.getLabel(), startAddress);
            zombieGroup.addWarrior(data);
        }
        return true;
    }

    public boolean loadWarriors(PlayersPanel.Code[] files, PlayersPanel.Code[] zombies, boolean isInDebug)
    {
        warriorNameToGroup.clear();
        warriorGroups.clear();

        Logger.log("Found " + files.length + " survivors, " + zombies.length + " zombies");
        m_Fixed_loadAddressChecker = null; // reset it before potentially being used again

        Arrays.sort(files, (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));

        WarriorLoader loader = new WarriorLoader();

        for(PlayersPanel.Code c: files)
        {
            if (!validateWarrior(c, "player")) return false;

            int startAddress = getStartAddress(files, zombies, isInDebug, c);
            if (startAddress == -2)
                return false;

            loader.loadWarrior(c, startAddress);
        }

        if (warriorGroups.isEmpty()) {
            Logger.error("no players to start a competition with");
            return false;
        }

        if (!loadZombies(zombies, m_Fixed_loadAddressChecker))
            return false;

        return true;
    }

    private int getStartAddress(PlayersPanel.Code[] files, PlayersPanel.Code[] zombies, boolean isInDebug, PlayersPanel.Code c) {
        int startAddr = -1;
        if (!c.startAddrRandom && isInDebug) {
            if (m_Fixed_loadAddressChecker == null) {
                // having it in the competition window is an ugly hack but is the only way for it to get to the random
                // loading where it will be needed later
                m_Fixed_loadAddressChecker = new FixedLoadAddressChecker(files.length + zombies.length);
            }
            startAddr = m_Fixed_loadAddressChecker.addCheck(c.startAddress, c.getBin().length, c.getName());

        }
        return startAddr;
    }

    private boolean validateWarrior(PlayersPanel.Code c, String type) {
        if (c.getName().isEmpty()) {
            Logger.error("All " + type + "s must have a name for starting a competition");
            return false;
        }
        if (c.getBin() == null || c.getBin().length == 0) {
            Logger.error("" + type + " " + c.getName() + "does not have any code, can't start competition");
            return false;
        }
        return true;
    }




    /**
     * @return the warrior groups corresponding to a given list of indices, and
     * the zombies group.
     * 
     * @param groupIndices  Required warrior groups indices.
     */
    public WarriorGroup[] createGroupList(int[] groupIndices) {
        ArrayList<WarriorGroup> groupsList = new ArrayList<>();

        // add requested warrior groups
        for (int groupIndex : groupIndices) {
            groupsList.add(warriorGroups.get(groupIndex));
        }

        // add zombies (if exist)
        if (zombieGroup != null) {
            groupsList.add(zombieGroup);
        }

        WarriorGroup[] groups = new WarriorGroup[groupsList.size()];
        groupsList.toArray(groups);
        return groups;
    }

    private class WarriorLoader {
        private WarriorGroup currentGroup;

        WarriorLoader() {
            this.currentGroup = null;
        }

        void loadWarrior(PlayersPanel.Code c, int startAddr) {
            WarriorData data = new WarriorData(c.getName(), truncToSize(c.getBin()), c.getLabel(), startAddr);
            if (c.player.wtype != PlayersPanel.EWarriorType.SINGLE)
            {
                if (data.getLabel().endsWith("0")) {
                    // start a new group!
                    currentGroup = new WarriorGroup(data.getName().substring(0, data.getName().length()-1));
                    currentGroup.addWarrior(data);
                    warriorNameToGroup.put(data.getName(), warriorGroups.size());
                }
                else if (data.getLabel().endsWith("1")) {
                    currentGroup.addWarrior(data);
                    warriorNameToGroup.put(data.getName(), warriorGroups.size());
                    warriorGroups.add(currentGroup);
                    currentGroup = null;
                }
                else {
                    Logger.error("Unexpected suffix for warrior name. expected 1 or 2: " + data.getName());
                }
            } else {
                currentGroup = new WarriorGroup(data.getName());
                currentGroup.addWarrior(data);
                warriorNameToGroup.put(data.getName(), warriorGroups.size());
                warriorGroups.add(currentGroup);
                currentGroup = null;
            }
        }
    }
}