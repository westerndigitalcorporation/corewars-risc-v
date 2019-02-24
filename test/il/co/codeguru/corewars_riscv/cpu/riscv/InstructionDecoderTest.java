package il.co.codeguru.corewars_riscv.cpu.riscv;

import il.co.codeguru.corewars_riscv.cpu.exceptions.CpuException;
import il.co.codeguru.corewars_riscv.cpu.exceptions.InvalidOpcodeException;
import il.co.codeguru.corewars_riscv.cpu.riscv.instruction_formats.InstructionFormatBase;
import il.co.codeguru.corewars_riscv.memory.MemoryException;
import il.co.codeguru.corewars_riscv.utils.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static junit.framework.TestCase.fail;

public class InstructionDecoderTest {

    private InstructionDecoder decoder;

    @Before
    public void setUp()
    {
        decoder = new InstructionDecoder();
        Logger.setTestingMode();
    }

    @Test
    public void DecoderFailOnIllegalInstructionTest() throws MemoryException, CpuException
    {
        InstructionFormatBase base = new InstructionFormatBase(0);

        try {
            decoder.decode(base);
        } catch (InvalidOpcodeException e) {
            return;
        }
        fail("Instruction Decoder threw exception on valid instruction");
    }
}
