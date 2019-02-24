package il.co.codeguru.corewars_riscv.utils.disassembler;

public interface IDisassembler {
    void reset(int offset, int endOffset);
    int lastOpcodeSize();
    String nextOpcode() throws DisassemblerException;

    class DisassemblerException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    class DisassemblerLengthException extends DisassemblerException {
        private static final long serialVersionUID = 1L;
    }
}
