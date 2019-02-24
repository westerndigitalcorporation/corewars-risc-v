package il.co.codeguru.corewars_riscv.memory;

public abstract class Memory {
    public MemoryEventListener listener;

    public abstract void storeByte(int index, byte value) throws MemoryException;

    public void storeHalfWord(int index, short value) throws MemoryException
    {
        storeByte(index,(byte)(value & 0xff));
        storeByte(index + 1,(byte)((value >> 8) & 0xff));
    }

    public void storeWord(int index, int value) throws MemoryException
    {
        storeHalfWord(index, (short) (value & 0xffff));
        storeHalfWord(index + 2, (short) ((value >> 16) & 0xffff));
    }


    public abstract byte loadByte(int index) throws MemoryException;
    public short loadHalfWord(int index) throws MemoryException
    {
        return (short)(loadByte(index) & 0xFF |
                (loadByte(index + 1) << 8));
    }
    public int loadWord(int index) throws MemoryException
    {
        return  (loadHalfWord(index) & 0xFFFF |
                (loadHalfWord(index+2) << 16));
    }

    /**
     * @return Returns the listener.
     */
    public MemoryEventListener getListener() {
        return listener;
    }
    /**
     * @param listener The listener to set.
     */
    public void setListener(MemoryEventListener listener) {
        this.listener = listener;
    }

    public static final int NUM_PARAGRAPHS = 64 * 1024;
    public static final int PARAGRAPH_SIZE = 0x10;
    public static final int PARAGRAPHS_IN_SEGMENT = 0x1000;
    public static final int MEMORY_SIZE = NUM_PARAGRAPHS * PARAGRAPH_SIZE;
}
