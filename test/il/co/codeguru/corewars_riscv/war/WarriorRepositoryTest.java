package il.co.codeguru.corewars_riscv.war;

import il.co.codeguru.corewars_riscv.gui.PlayersPanel;
import il.co.codeguru.corewars_riscv.utils.Logger;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class WarriorRepositoryTest {

    private WarriorRepository repo;

    @Before
    public void setUp()
    {
        Logger.setTestingMode();
        repo = new WarriorRepository();
    }

    @Test
    public void testAddScore() {
        PlayersPanel.PlayerInfo p = new PlayersPanel.PlayerInfo("p", "pp");
        PlayersPanel.Code a = new PlayersPanel.Code(p, 0);
        a.name = "Name1";
        a.bin = new byte[]{(byte)0xCC};
        PlayersPanel.PlayerInfo p2 = new PlayersPanel.PlayerInfo("p2", "pp2");
        PlayersPanel.Code a2 = new PlayersPanel.Code(p2, 0);
        a2.name = "Name2";
        a2.bin = new byte[]{(byte)0xCC};
        repo.loadWarriors(new PlayersPanel.Code[]{ a, a2 }, new PlayersPanel.Code[]{}, false);

        repo.addScore("Name1", (float) 1.0);

        assertEquals(1.0,repo.getWarriorGroups().get(0).getGroupScore(), 0.1);
        assertEquals(0.0,repo.getWarriorGroups().get(1).getGroupScore(), 0.1);
    }

    @Test
    public void testGetGroupNames()
    {
        PlayersPanel.PlayerInfo p = new PlayersPanel.PlayerInfo("p", "pp");
        PlayersPanel.Code a = new PlayersPanel.Code(p, 0);
        a.name = "Name1";
        a.bin = new byte[]{(byte)0xCC};
        repo.loadWarriors(new PlayersPanel.Code[]{ a }, new PlayersPanel.Code[]{}, false);

        String[] ans = repo.getGroupNames();

        assertEquals(1, ans.length);
        assertEquals("Name1", ans[0]);

        PlayersPanel.PlayerInfo p2 = new PlayersPanel.PlayerInfo("p2", "pp2");
        PlayersPanel.Code a2 = new PlayersPanel.Code(p2, 0);
        a2.name = "Name2";
        a2.bin = new byte[]{(byte)0xCC};
        repo.loadWarriors(new PlayersPanel.Code[]{ a, a2 }, new PlayersPanel.Code[]{}, false);

        ans = repo.getGroupNames();

        assertEquals(2, ans.length);
        assertEquals("Name1", ans[0]);
        assertEquals("Name2", ans[1]);
    }

    @Test
    public void loadWarriorsSuccess() {
        PlayersPanel.PlayerInfo p = new PlayersPanel.PlayerInfo("p", "pp");
        PlayersPanel.Code a = new PlayersPanel.Code(p, 0);
        a.name = "Name1";
        a.bin = new byte[]{(byte)0xCC};

        boolean result = repo.loadWarriors(new PlayersPanel.Code[]{ a }, new PlayersPanel.Code[]{}, false);

        assertTrue(result);
    }

    @Test
    public void testLoadWarriorsNoName()
    {
        PlayersPanel.PlayerInfo p = new PlayersPanel.PlayerInfo("p", "pp");
        PlayersPanel.Code a = new PlayersPanel.Code(p, 0);
        a.name = "";
        a.bin = new byte[]{(byte)0xCC};

        boolean result = repo.loadWarriors(new PlayersPanel.Code[]{ a }, new PlayersPanel.Code[]{}, false);

        assertFalse(result);
    }

    @Test
    public void testLoadWarriorsNoCode()
    {
        PlayersPanel.PlayerInfo p = new PlayersPanel.PlayerInfo("p", "pp");
        PlayersPanel.Code a = new PlayersPanel.Code(p, 0);
        a.name = "Name1";
        a.bin = new byte[]{};

        boolean result = repo.loadWarriors(new PlayersPanel.Code[]{ a }, new PlayersPanel.Code[]{}, false);

        assertFalse(result);


        a.bin = null;

        result = repo.loadWarriors(new PlayersPanel.Code[]{ a }, new PlayersPanel.Code[]{}, false);

        assertFalse(result);
    }

    @Test
    public void testCreateGroupList() {
        PlayersPanel.PlayerInfo p = new PlayersPanel.PlayerInfo("p", "pp");
        PlayersPanel.Code a = new PlayersPanel.Code(p, 0);
        a.name = "Name1";
        a.bin = new byte[]{(byte)0xCC};
        repo.loadWarriors(new PlayersPanel.Code[]{ a }, new PlayersPanel.Code[]{}, false);
        WarriorGroup[] groups = repo.createGroupList(new int[]{0});

        assertEquals("Name1", groups[0].getWarriors().get(0).getName());
        assertEquals((byte)0xCC, groups[0].getWarriors().get(0).getCode()[0]);
    }
}