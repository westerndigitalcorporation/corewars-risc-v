package il.co.codeguru.corewars_riscv.cpu.riscv;

import il.co.codeguru.corewars_riscv.cpu.exceptions.CpuException;
import il.co.codeguru.corewars_riscv.cpu.riscv.instruction_formats.InstructionFormatBase;
import il.co.codeguru.corewars_riscv.memory.MemoryException;

public class Instruction {

    private InstructionInfo info;
    private InstructionFormatBase instructionFormat;
    private Action action;

    public Instruction(InstructionInfo info,
                       InstructionFormatBase format,
                       Action action)
    {
        this.info = info;
        this.instructionFormat = format;
        this.action = action;
    }

    public InstructionInfo getInfo()
    {
        return info;
    }

    public InstructionFormatBase getFormat() {
        return instructionFormat;
    }

    public void execute(InstructionRunner runner) throws CpuException, MemoryException
    {
        action.apply(instructionFormat, runner);
    }

    @FunctionalInterface
    public interface Action{
        void apply(InstructionFormatBase i, InstructionRunner runner) throws CpuException, MemoryException;
    }

    public static class InstructionInfo {
        final private String name;
        final private int opcode;
        final private int funct3; //Or funct4 in the case of RV32C
        final private int funct7;

        public InstructionInfo(String name, int type)
        {
            this(name, type, 0);
        }

        public InstructionInfo(String name, int type, int funct3) {
            this(name, type, funct3, 0);
        }

        public InstructionInfo(String name, int type, int funct3, int funct7){
            this.name = name;
            this.opcode = type;
            this.funct3 = funct3;
            this.funct7 = funct7;
        }
        public String getName() {return name;}

        public int getOpcode() {
            return opcode;
        }

        public int getFunct3() {
            return funct3;
        }

        public int getFunct7() {return funct7;}
    }
}
