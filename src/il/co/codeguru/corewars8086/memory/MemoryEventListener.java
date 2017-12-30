package il.co.codeguru.corewars8086.memory;



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
    void onMemoryWrite(RealModeAddress address);
}