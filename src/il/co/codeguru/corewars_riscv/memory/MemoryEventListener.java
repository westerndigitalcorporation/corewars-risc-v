package il.co.codeguru.corewars_riscv.memory;



/**
 * Defines an interface for memory listeners
 * 
 * @author BS
 */
public interface MemoryEventListener {
    /**
     * Called when a byte is written to memory
     * @param address
     */
    void onMemoryWrite(int address, byte value);

    enum EWriteState {
        INIT,
        ADD_WARRIORS,
        RUN
    }
    void onWriteState(EWriteState state);


}
