package il.co.codeguru.corewars_riscv.cpu.riscv;

import il.co.codeguru.corewars_riscv.cpu.riscv.instruction_formats.*;

public final class RV32I {
    private RV32I(){}

    public static InstructionFormatI instructionI (Instruction.InstructionInfo op, int rd, int rs1, int imm) { return new InstructionFormatI(op.getOpcode(), rd, op.getFunct3(),rs1,imm | (op.getFunct7() << 5));}
    public static InstructionFormatS instructionS (Instruction.InstructionInfo op, int rs1, int rs2, int imm) { return new InstructionFormatS(op.getOpcode(),op.getFunct3(), rs1, rs2, imm);}
    public static InstructionFormatR instructionR (Instruction.InstructionInfo op, int rd, int rs1, int rs2) { return new InstructionFormatR(op.getOpcode(), rd, op.getFunct3(), rs1, rs2, op.getFunct7());}
    public static InstructionFormatU instructionU (Instruction.InstructionInfo op, int rd, int imm) { return new InstructionFormatU(op.getOpcode(), rd, imm);}
    public static InstructionFormatSB instructionSB(Instruction.InstructionInfo op, int rs1, int rs2, int imm) { return new InstructionFormatSB(op.getOpcode(),op.getFunct3(),rs1, rs2, imm);}
    public static InstructionFormatUJ instructionUJ(Instruction.InstructionInfo op, int rd, int imm) { return new InstructionFormatUJ(op.getOpcode(), rd, imm);}

    public final class OpcodeTypes {
        public static final int OP_IMM = 0x13;
        public static final int OP = 0x33;
        public static final int BRANCH = 0x63;
        public static final int LOAD = 0x03;
        public static final int STORE = 0x23;
        public static final int AUIPC = 0x17;
        public static final int LUI = 0x37;
        public static final int JALR = 0x67;
        public static final int JAL = 0x6f;
    }

    public static final class Opcodes{
        //BRANCH
        public static Instruction.InstructionInfo Beq = new Instruction.InstructionInfo("Beq", OpcodeTypes.BRANCH, 0);
        public static Instruction.InstructionInfo Bne = new Instruction.InstructionInfo("Bne",OpcodeTypes.BRANCH, 1);
        public static Instruction.InstructionInfo Blt = new Instruction.InstructionInfo("Blt",OpcodeTypes.BRANCH, 4);
        public static Instruction.InstructionInfo Bge = new Instruction.InstructionInfo("Bge",OpcodeTypes.BRANCH, 5);
        public static Instruction.InstructionInfo Bltu= new Instruction.InstructionInfo("Bltu",OpcodeTypes.BRANCH, 6);
        public static Instruction.InstructionInfo Bgeu= new Instruction.InstructionInfo("Bgeu",OpcodeTypes.BRANCH, 7);

        //REGISTER
        public static Instruction.InstructionInfo Add = new Instruction.InstructionInfo("Add",OpcodeTypes.OP, 0, 0);
        public static Instruction.InstructionInfo Sub = new Instruction.InstructionInfo("Sub",OpcodeTypes.OP, 0, 32);
        public static Instruction.InstructionInfo Sll = new Instruction.InstructionInfo("Sll",OpcodeTypes.OP, 1);
        public static Instruction.InstructionInfo Slt = new Instruction.InstructionInfo("Slt",OpcodeTypes.OP, 2);
        public static Instruction.InstructionInfo Sltu= new Instruction.InstructionInfo("Sltu",OpcodeTypes.OP, 3);
        public static Instruction.InstructionInfo Xor = new Instruction.InstructionInfo("Xor",OpcodeTypes.OP, 4);
        public static Instruction.InstructionInfo Srl = new Instruction.InstructionInfo("Srl",OpcodeTypes.OP, 5, 0);
        public static Instruction.InstructionInfo Sra = new Instruction.InstructionInfo("Sra",OpcodeTypes.OP, 5, 32);
        public static Instruction.InstructionInfo Or  = new Instruction.InstructionInfo("Or",OpcodeTypes.OP, 6);
        public static Instruction.InstructionInfo And = new Instruction.InstructionInfo("And",OpcodeTypes.OP, 7);

        //Store
        public static Instruction.InstructionInfo Sb = new Instruction.InstructionInfo("Sb",OpcodeTypes.STORE, 0);
        public static Instruction.InstructionInfo Sh = new Instruction.InstructionInfo("Sh",OpcodeTypes.STORE, 1);
        public static Instruction.InstructionInfo Sw = new Instruction.InstructionInfo("Sw",OpcodeTypes.STORE, 2);

        //Immediate
        public static Instruction.InstructionInfo Addi = new Instruction.InstructionInfo("Addi",OpcodeTypes.OP_IMM, 0);
        public static Instruction.InstructionInfo Slli = new Instruction.InstructionInfo("Slli",OpcodeTypes.OP_IMM, 1);
        public static Instruction.InstructionInfo Slti = new Instruction.InstructionInfo("Slti",OpcodeTypes.OP_IMM, 2);
        public static Instruction.InstructionInfo Sltiu= new Instruction.InstructionInfo("Sltui",OpcodeTypes.OP_IMM, 3);
        public static Instruction.InstructionInfo Xori = new Instruction.InstructionInfo("Xori",OpcodeTypes.OP_IMM, 4);
        public static Instruction.InstructionInfo Srli = new Instruction.InstructionInfo("Srli",OpcodeTypes.OP_IMM, 5, 0);
        public static Instruction.InstructionInfo Srai = new Instruction.InstructionInfo("Srai",OpcodeTypes.OP_IMM, 5, 32);
        public static Instruction.InstructionInfo Ori  = new Instruction.InstructionInfo("Ori",OpcodeTypes.OP_IMM, 6);
        public static Instruction.InstructionInfo Andi = new Instruction.InstructionInfo("Andi",OpcodeTypes.OP_IMM, 7);

        //Load
        public static Instruction.InstructionInfo Lb  = new Instruction.InstructionInfo("Lb",OpcodeTypes.LOAD, 0);
        public static Instruction.InstructionInfo Lh  = new Instruction.InstructionInfo("Lh",OpcodeTypes.LOAD, 1);
        public static Instruction.InstructionInfo Lw  = new Instruction.InstructionInfo("Lw",OpcodeTypes.LOAD, 2);
        public static Instruction.InstructionInfo Lbu = new Instruction.InstructionInfo("Lbu",OpcodeTypes.LOAD, 4);
        public static Instruction.InstructionInfo Lhu = new Instruction.InstructionInfo("Lhu",OpcodeTypes.LOAD, 5);

        //Other
        public static Instruction.InstructionInfo Lui  = new Instruction.InstructionInfo("Lui",OpcodeTypes.LUI);
        public static Instruction.InstructionInfo Auipc= new Instruction.InstructionInfo("Auipc",OpcodeTypes.AUIPC);
        public static Instruction.InstructionInfo Jal  = new Instruction.InstructionInfo("Jal",OpcodeTypes.JAL);
        public static Instruction.InstructionInfo Jalr = new Instruction.InstructionInfo("Jalr",OpcodeTypes.JALR);
    }

}
