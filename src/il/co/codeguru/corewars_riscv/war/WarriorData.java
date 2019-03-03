package il.co.codeguru.corewars_riscv.war;

/**
 * Holds a single warrior's name & code.
 * 
 * @author DL
 */
public class WarriorData {

    public static final int RANDOM_LOAD_ADDRESS = -1;

    /**
     * Constructor.
     * @param name   Warrior's name.
     * @param code   Warrior's code.
     */
    public WarriorData(String name, byte[] code, String label, int debugLoadAddr) {
        m_name = name;
        m_code = code;
        m_label = label;
        m_debugFixedLoadAddress = debugLoadAddr;
    }

    /** @return the warrior's name. */
    public String getName() {
        return m_name;
    }
    public String getLabel() {
        return m_label;
    }

    /** @return the warrior's code. */
    public byte[] getCode() {
        return m_code;
    }

    /** Holds warrior's name */
    private final String m_name;
    private final String m_label; // label is unique, it looks like A1 for player A, warrior 1

    /** Holds warrior's code */
    private final byte[] m_code;
    public int m_debugFixedLoadAddress; // -1 for random or an address of the UI


    @Override
    public String toString() {
        return m_name;
    }
}
