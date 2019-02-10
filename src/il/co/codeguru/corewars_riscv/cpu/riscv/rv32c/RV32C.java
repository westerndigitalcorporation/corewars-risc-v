package il.co.codeguru.corewars_riscv.cpu.riscv.rv32c;

import il.co.codeguru.corewars_riscv.cpu.riscv.Instruction;
import il.co.codeguru.corewars_riscv.cpu.riscv.rv32c.instruction_formats.*;
import il.co.codeguru.corewars_riscv.utils.Logger;

public final class RV32C {
    private RV32C() {}

    public static CInstructionFormatCR cInstructionFormatCR(Instruction.InstructionInfo info, int rs1, int rs2)
    {
        return new CInstructionFormatCR((byte)info.getOpcode(), (byte)rs1, (byte)rs2, (byte)info.getFunct3());
    }

    public static CInstructionFormatCI cInstructionFormatCI(Instruction.InstructionInfo info, int rs1, int imm) {
        return new CInstructionFormatCI((byte)info.getOpcode(),(byte)info.getFunct3(), (byte)rs1, (byte)imm);
    }

    public static CInstructionFormatCSS cInstructionFormatCSS(Instruction.InstructionInfo info, int rs2, int imm) {
        return new CInstructionFormatCSS((byte)info.getOpcode(), (byte)info.getFunct3(), (byte)rs2, (byte) imm);
    }

    public static CInstructionFormatCSS cInstructionFormatCSSwithWord(Instruction.InstructionInfo info, int rs2, int imm) {
        return CInstructionFormatCSS.createWithWord((byte)info.getOpcode(), (byte)info.getFunct3(), (byte)rs2, (byte) imm);
    }

    public static CInstructionFormatCIW cInstructionFormatCIW(Instruction.InstructionInfo info, int rd, int imm) {
        assert rd >= 8 && rd < 16;
        return new CInstructionFormatCIW((byte)info.getOpcode(), (byte)info.getFunct3(), (byte)(rd-8), (byte)imm);
    }

    public static CInstructionFormatCL cInstructionFormatCL(Instruction.InstructionInfo info, int rd, int rs1, int imm) {
        assert rd >= 8 && rd < 16;
        assert rs1 >= 8 && rs1 < 16;
        return new CInstructionFormatCL((byte)info.getOpcode(), (byte)info.getFunct3(), (byte)(rd-8), (byte)(rs1-8), (byte)imm);
    }

    public static CInstructionFormatCS cInstructionFormatCS(Instruction.InstructionInfo info, int rs1, int rs2, int imm) {
        return new CInstructionFormatCS((byte)info.getOpcode(), (byte)info.getFunct3(), (byte)rs1, (byte)rs2, (byte)imm);
    }

    public static CInstructionFormatCS cInstructionFormatCS(Instruction.InstructionInfo info, int rs1, int rs2) {
        return CInstructionFormatCS.fromFunct6((byte)info.getOpcode(), (byte)info.getFunct3(), (byte)info.getFunct7(), (byte)rs1, (byte)rs2);
    }

    public static CInstructionFormatCB cInstructionFormatCB(Instruction.InstructionInfo info, int rs1, int imm) {
        return new CInstructionFormatCB((byte)info.getOpcode(), (byte)info.getFunct3(), (byte)info.getFunct7(), (byte)rs1, (byte)imm);
    }

    public static CInstructionFormatCB cInstructionFormatCBBranch(Instruction.InstructionInfo info, int rs1, int imm) {
        return CInstructionFormatCB.forBranch((byte)info.getOpcode(), (byte)info.getFunct3(), (byte)rs1, (byte)imm);
    }

    public static CInstructionFormatCJ cInstructionFormatCJ(Instruction.InstructionInfo info, int imm) {
        return new CInstructionFormatCJ((byte)info.getOpcode(), (byte)info.getFunct3(), (short)imm);
    }

    public static final class OpcodeTypes {
        public static final int C0 = 0;
        public static final int C1 = 1;
        public static final int C2 = 2;
    }

    public static final class Opcodes {
        public static final Instruction.InstructionInfo CLW = new Instruction.InstructionInfo("C.lw", OpcodeTypes.C0, 2);
        public static final Instruction.InstructionInfo CSW = new Instruction.InstructionInfo("C.sw", OpcodeTypes.C0, 6);
        public static final Instruction.InstructionInfo CLWSP = new Instruction.InstructionInfo("C.lwsp", OpcodeTypes.C2, 2);
        public static final Instruction.InstructionInfo CSWSP = new Instruction.InstructionInfo("C.swsp", OpcodeTypes.C2, 6);

        public static final Instruction.InstructionInfo CJ = new Instruction.InstructionInfo("C.j", OpcodeTypes.C1, 5);
        public static final Instruction.InstructionInfo CJAL = new Instruction.InstructionInfo("C.jal", OpcodeTypes.C1, 1);
        public static final Instruction.InstructionInfo CJR = new Instruction.InstructionInfo("C.jr", OpcodeTypes.C2, 8);
        public static final Instruction.InstructionInfo CJALR = new Instruction.InstructionInfo("C.jalr", OpcodeTypes.C2, 9);
        public static final Instruction.InstructionInfo CBEQZ = new Instruction.InstructionInfo("C.beqz", OpcodeTypes.C1, 6);
        public static final Instruction.InstructionInfo CBNEZ = new Instruction.InstructionInfo("C.bnez", OpcodeTypes.C1, 7);

        public static final Instruction.InstructionInfo CLI = new Instruction.InstructionInfo("C.li", OpcodeTypes.C1, 2);
        public static final Instruction.InstructionInfo CLUI = new Instruction.InstructionInfo("C.lui", OpcodeTypes.C1, 3);
        public static final Instruction.InstructionInfo CADDI = new Instruction.InstructionInfo("C.addi", OpcodeTypes.C1, 0);
        public static final Instruction.InstructionInfo CADDI16SP = new Instruction.InstructionInfo("C.addi16sp",OpcodeTypes.C1, 3);
        public static final Instruction.InstructionInfo CADDI4SPN = new Instruction.InstructionInfo("C.addi4spn", OpcodeTypes.C0, 0);
        public static final Instruction.InstructionInfo CSLLI = new Instruction.InstructionInfo("C.slli",OpcodeTypes.C2, 0);
        public static final Instruction.InstructionInfo CSRLI = new Instruction.InstructionInfo("C.srli",OpcodeTypes.C1, 4, 0);
        public static final Instruction.InstructionInfo CSRAI = new Instruction.InstructionInfo("C.srai",OpcodeTypes.C1, 4, 1);
        public static final Instruction.InstructionInfo CANDI = new Instruction.InstructionInfo("C.andi",OpcodeTypes.C1, 4, 2);

        public static final Instruction.InstructionInfo CMV = new Instruction.InstructionInfo("C.mv",OpcodeTypes.C2, 8);
        public static final Instruction.InstructionInfo CADD = new Instruction.InstructionInfo("C.add",OpcodeTypes.C2, 9);
        public static final Instruction.InstructionInfo CAND = new Instruction.InstructionInfo("C.and",OpcodeTypes.C1, 35, 3);
        public static final Instruction.InstructionInfo COR = new Instruction.InstructionInfo("C.or", OpcodeTypes.C1, 35, 2);
        public static final Instruction.InstructionInfo CXOR = new Instruction.InstructionInfo("C.xor", OpcodeTypes.C1, 35, 1);
        public static final Instruction.InstructionInfo CSUB = new Instruction.InstructionInfo("C.sub", OpcodeTypes.C1, 35, 0);

        public static final Instruction.InstructionInfo CNOP = new Instruction.InstructionInfo("C.nop", OpcodeTypes.C1, 0);
    }
}
