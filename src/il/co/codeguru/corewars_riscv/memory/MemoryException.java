package il.co.codeguru.corewars_riscv.memory;

/**
 * Base class for all Exceptions thrown by the RealModeMemory classes.
 * 
 * @author DL
 */
public class MemoryException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public MemoryException() {}
	public MemoryException(String msg) { super(msg); }
}
