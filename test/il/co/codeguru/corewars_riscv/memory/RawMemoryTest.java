package il.co.codeguru.corewars_riscv.memory;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RawMemoryTest {

    private Memory memory;

    @Before
    public void setUp()
    {
        memory = new RawMemory(1024);
    }

    @Test
    public void loadByteTest() throws MemoryException
    {
        memory.storeByte(0, (byte)0x12);
        assertEquals(0x12, memory.loadByte(0));
    }

    @Test
    public void loadHalfWordTest() throws MemoryException
    {
        memory.storeHalfWord(0, (short)0x1234);
        assertEquals(0x1234, memory.loadHalfWord(0));
        assertEquals(0x12, memory.loadByte(1));
        assertEquals(0x34, memory.loadByte(0));
    }

    @Test
    public void loadWordTest() throws MemoryException
    {
        memory.storeWord(0, 0x12345678);
        assertEquals(0x12345678, memory.loadWord(0));
        assertEquals(0x1234, memory.loadHalfWord(2));
        assertEquals(0x5678, memory.loadHalfWord(0));
        assertEquals(0x3456, memory.loadHalfWord(1));
    }

    @Test(expected = MemoryException.class)
    public void readFailOutOfBounds()
    {
        memory.loadWord(1024);
    }

}
