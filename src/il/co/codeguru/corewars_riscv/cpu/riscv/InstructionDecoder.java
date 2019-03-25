package il.co.codeguru.corewars_riscv.cpu.riscv;

public interface InstructionDecoder {
    Instruction decode(InstructionFormat format_input);
}
