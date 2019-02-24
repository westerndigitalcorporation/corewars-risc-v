package il.co.codeguru.corewars_riscv.memory;

import org.junit.Before;
import org.junit.Test;

public class RestrictedMemoryTest {
    private RestrictedMemory memory;
    private Memory raw;

    @Before
    public void setUp()
    {
        raw = new RawMemory(1024);
    }
    @Test
    public void loadNormalTest()
    {
        memory = new RestrictedMemory(raw, new MemoryRegion[]{ new MemoryRegion(0, 10), new MemoryRegion(1020, 1024)});

        memory.loadWord(0);
        memory.loadWord(1020);
        memory.loadHalfWord(1022);
        memory.loadByte(1023);
    }

    @Test
    public void storeNormalTest()
    {
        memory = new RestrictedMemory(raw, new MemoryRegion[]{ new MemoryRegion(0, 10), new MemoryRegion(1020, 1024)});
        memory.storeWord(0, 1);
        memory.storeWord(1020, 1999);
        memory.storeHalfWord(1022, (short)2012);
        memory.storeByte(1023, (byte)255);
    }

    @Test(expected = MemoryException.class)
    public void loadFail()
    {
        memory = new RestrictedMemory(raw, new MemoryRegion[]{ new MemoryRegion(0, 10), new MemoryRegion(1020, 1024)});
        memory.loadWord(50);
    }

    @Test(expected = MemoryException.class)
    public void storeFail()
    {
        memory = new RestrictedMemory(raw, new MemoryRegion[]{ new MemoryRegion(0, 10), new MemoryRegion(1020, 1024)});
        memory.storeWord(50,0);
    }




}
