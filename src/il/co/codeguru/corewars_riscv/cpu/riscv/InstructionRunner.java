package il.co.codeguru.corewars_riscv.cpu.riscv;

import il.co.codeguru.corewars_riscv.cpu.riscv.instruction_formats.*;
import il.co.codeguru.corewars_riscv.memory.Memory;
import il.co.codeguru.corewars_riscv.memory.MemoryException;

public class InstructionRunner {

    private CpuStateRiscV state;
    private Memory memory;

    public InstructionRunner(CpuRiscV cpu) {
        this.state = cpu.getState();
        this.memory = cpu.getMemory();
    }

    /**
        ADDI (ADD immediate - I Type) adds the sign-extended 12-bit immediate to register rs1.
        Arithmetic overflow is ignored and the result is simply the low XLEN bits of the result.
        ADDI rd, rs1, 0 is used to implement the MV rd, rs1 assembler pseudo-instruction
     */
    public void addi(InstructionFormatI i) {
        state.setReg(i.getRd(), state.getReg(i.getRs1()) + i.getImmediate());
    }

    /**
        ADD (ADD - R Type) adds the registers rs1 and rs2 and stores the result in rd.
        Arithmetic overflow is ignored and the result is simply the low XLEN bits of the result.
     */
    public void add(InstructionFormatR i) {
        state.setReg(i.getRd(), state.getReg(i.getRs1()) + state.getReg(i.getRs2()));
    }

    /**
         SUB (SUB - R Type) subs the register rs2 from rs1 and stores the result in rd.
         Arithmetic overflow is ignored and the result is simply the low XLEN bits of the result.
     */
    public void sub(InstructionFormatR i) {
        state.setReg(i.getRd(), state.getReg(i.getRs1()) - state.getReg(i.getRs2()));
    }

    /**
     * AUIPC (add upper immediate to pc) is used to build pc-relative addresses and uses the U-type format.
     * AUIPC forms a 32-bit offset from the 20-bit U-immediate, filling in the lowest 12 bits with zeros, adds this offset to the pc, then places the result in register rd
     */
    public void auipc(InstructionFormatU i) {
        state.setReg(i.getRd(), state.getPc() + (i.getImmediate() << 12)); // TODO:Set this in the U type instruction
    }

    /**
     * LUI (load upper immediate) is used to build 32-bit constants and uses the U-type format.
     * LUI places the U-immediate value in the top 20 bits of the destination register rd, filling in the lowest 12 bits with zeros
     */
    public void lui(InstructionFormatU i) {
        int mask = (1 << 12) - 1;
        state.setReg(i.getRd(), (state.getReg(i.getRd()) & mask) | (i.getImmediate() << 12));
    }

    /**
     * The SW (Store word) instruction stores 32-bit value from register rs2 to memory
     * The effective byte address is obtained by adding register rs1 to the sign-extended 12-bit offset
     */
    public void sw(InstructionFormatS i) throws MemoryException {
        memory.storeWord(state.getReg(i.getRs1()) + i.getImm(), state.getReg(i.getRs2()));
    }

    /**
     * The SH (Store Halfword) instruction stores 16-bit value from the low bits of register rs2 to memory
     * The effective byte address is obtained by adding register rs1 to the sign-extended 12-bit offset
     */
    public void sh(InstructionFormatS i) throws MemoryException {
        memory.storeHalfWord(state.getReg(i.getRs1()) + i.getImm(), (short) state.getReg(i.getRs2()));
    }

    /**
     * The SB (Store Byte) instruction stores 8-bit value from the low bits of register rs2 to memory
     * The effective byte address is obtained by adding register rs1 to the sign-extended 12-bit offset
     */
    public void sb(InstructionFormatS i) throws MemoryException {
        memory.storeByte(state.getReg(i.getRs1()) + i.getImm(), (byte) state.getReg(i.getRs2()));
    }

    /**
     * ANDI is a logical operation that performs bitwise AND on register rs1 and the sign-extended 12-bit immediate and place the result in rd
     */
    public void andi(InstructionFormatI i) {
        state.setReg(i.getRd(), state.getReg(i.getRs1()) & i.getImmediate());
    }
    /**
     * AND is a logical operation that performs bitwise AND on registers rs1 and rs2 and place the result in rd
     */
    public void and(InstructionFormatR i) {
        state.setReg(i.getRd(), state.getReg(i.getRs1()) & state.getReg(i.getRs2()));
    }

    /**
     * ORI is a logical operation that performs bitwise OR on register rs1 and the sign-extended 12-bit immediate and place the result in rd
     */
    public void ori(InstructionFormatI i) {
        state.setReg(i.getRd(), state.getReg(i.getRs1()) | i.getImmediate());
    }

    /**
     * OR is a logical operation that performs bitwise OR on registers rs1 and rs2 and place the result in rd
     */
    public void or(InstructionFormatR i) {
        state.setReg(i.getRd(), state.getReg(i.getRs1()) | state.getReg(i.getRs2()));
    }

    /**
     * XORI is a logical operation that performs bitwise XOR on register rs1 and the sign-extended 12-bit immediate and place the result in rd
     * Note, "XORI rd, rs1, -1" performs a bitwise logical inversion of register rs1(assembler pseudo-instruction NOT rd, rs)
     */
    public void xori(InstructionFormatI i) {
        state.setReg(i.getRd(), state.getReg(i.getRs1()) ^ i.getImmediate());
    }

    /**
     * XOR is a logical operation that performs bitwise XOR on registers rs1 and rs2 and place the result in rd
     */
    public void xor(InstructionFormatR i) {
        state.setReg(i.getRd(), state.getReg(i.getRs1()) ^ state.getReg(i.getRs2()));
    }

    /**
     * SLLI performs logical left shift on the value in register rs1 by the shift amount held in the lower 5 bits of the immediate
     */
    public void slli(InstructionFormatI i) {
        state.setReg(i.getRd(), state.getReg(i.getRs1()) << i.getImmediate());
    }

    /**
     * SLL performs logical left shift on the value in register rs1 by the shift amount held in the lower 5 bits of register rs2
     */
    public void sll(InstructionFormatR i) {
        state.setReg(i.getRd(), state.getReg(i.getRs1()) << state.getReg(i.getRs2()));
    }

    /**
     * SRLI performs logical right shift on the value in register rs1 by the shift amount held in the lower 5 bits of the immediate
     */
    public void srli(InstructionFormatI i) {
        state.setReg(i.getRd(), state.getReg(i.getRs1()) >>> i.getImmediate());
    }

    /**
     * SRL performs logical right shift on the value in register rs1 by the shift amount held in the lower 5 bits of register rs2
     */
    public void srl(InstructionFormatR i) {
        state.setReg(i.getRd(), state.getReg(i.getRs1()) >>> state.getReg(i.getRs2()));
    }

    /**
     * SRAI performs arithmetic right shift on the value in register rs1 by the shift amount held in the lower 5 bits of the immediate
     */
    public void srai(InstructionFormatI i) {
        state.setReg(i.getRd(), state.getReg(i.getRs1()) >> i.getImmediate());
    }

    /**
     * SRA performs arithmetic right shift on the value in register rs1 by the shift amount held in the lower 5 bits of register rs2
     */
    public void sra(InstructionFormatR i) {
        state.setReg(i.getRd(), state.getReg(i.getRs1()) >> state.getReg(i.getRs2()));
    }

    /**
     * SLTI (set less than immediate) places the value 1 in register rd if register rs1 is less than the sign-extended immediate when both are treated as signed numbers, else 0 is written to rd.
     */
    public void slti(InstructionFormatI i) {
        state.setReg(i.getRd(), state.getReg(i.getRs1()) < i.getImmediate() ? 1 : 0);
    }

    /**
     * SLT (set less than) places the value 1 in register rd if register rs1 is less than register rs2 when both are treated as signed numbers, else 0 is written to rd.
     */
    public void slt(InstructionFormatR i) {
        state.setReg(i.getRd(), state.getReg(i.getRs1()) < state.getReg(i.getRs2()) ? 1 : 0);
    }
    /**
     * SLTIU (set less than immediate unsigned) places the value 1 in register rd if register rs1 is less than the immediate when both are treated as unsigned numbers, else 0 is written to rd.
     */
    public void sltiu(InstructionFormatI i) {
        state.setReg(i.getRd(), state.getReg(i.getRs1()) + 0x80000000 < i.getImmediate() + 0x80000000 ? 1 : 0);
    }
    /**
     * SLTU (set less than unsigned) places the value 1 in register rd if register rs1 is less than register rs2 when both are treated as unsigned numbers, else 0 is written to rd.
     */
    public void sltu(InstructionFormatR i) {
        state.setReg(i.getRd(), state.getReg(i.getRs1()) + 0x80000000 < state.getReg(i.getRs2()) + 0x80000000 ? 1 : 0);
    }

    /**
     * The LW instruction loads a 32-bit value from memory into rd
     * The effective byte address is obtained by adding register rs1 to the sign-extended 12-bit offset
     */
    public void lw(InstructionFormatI i) throws MemoryException {
        state.setReg(i.getRd(), memory.loadWord(state.getReg(i.getRs1()) + i.getImmediate()));
    }

    /**
     * LH loads a 16-bit value from memory,then sign-extends to 32-bits before storing in rd
     * The effective byte address is obtained by adding register rs1 to the sign-extended 12-bit offset
     */
    public void lh(InstructionFormatI i) throws MemoryException {
        state.setReg(i.getRd(), memory.loadHalfWord(state.getReg(i.getRs1()) + i.getImmediate()));
    }

    /**
     * LHU loads a 16-bit value from memory,then zero-extends to 32-bits before storing in rd
     * The effective byte address is obtained by adding register rs1 to the sign-extended 12-bit offset
     */
    public void lhu(InstructionFormatI i) throws MemoryException {
        int val = memory.loadHalfWord(state.getReg(i.getRs1()) + i.getImmediate());
        state.setReg(i.getRd(), val & 0xFFFF);
    }

    /**
     * LB loads a 8-bit value from memory,then sign-extends to 32-bits before storing in rd
     * The effective byte address is obtained by adding register rs1 to the sign-extended 12-bit offset
     */
    public void lb(InstructionFormatI i) throws MemoryException {
        state.setReg(i.getRd(), memory.loadByte(state.getReg(i.getRs1()) + i.getImmediate()));
    }

    /**
     * LBU loads a 8-bit value from memory,then zero-extends to 32-bits before storing in rd
     * The effective byte address is obtained by adding register rs1 to the sign-extended 12-bit offset
     */
    public void lbu(InstructionFormatI i) throws MemoryException {
        int val = memory.loadByte(state.getReg(i.getRs1()) + i.getImmediate());
        state.setReg(i.getRd(), val & 0xFF);
    }

    /**
     * The  jump  and  link  (JAL)  instruction  uses  the  J-type  format,  where  the  J-immediate  encodes  a signed offset in multiples of 2 bytes.
     * The offset is sign-extended and added to the pc to form the jump target address.  Jumps can therefore target a +-1 MiB range.
     * JAL stores the address of the instruction following the jump (pc+4) into register rd.
     */
    public void jal(InstructionFormatUJ i) {
        state.setReg(i.getRd(), state.getPc() + 4);
        jump(state, i.getImmediate());
    }

    public void jal(InstructionFormatUJ i, int instructionSize) {
        state.setReg(i.getRd(), state.getPc() + instructionSize);
        jump(state, i.getImmediate(), instructionSize);
    }

    /**
     * The indirect jump instruction JALR (jump and link register) uses the I-type encoding.
     * The target address is obtained by adding the 12-bit signed I-immediate to the register rs1, then setting the least-significant bit of the result to zero.
     * The address of the instruction following the jump (pc+4)is written to register rd.
     * Register x0 can be used as the destination if the result is not required
     */
    public void jalr(InstructionFormatI i) {
       this.jalr(i, 4);
    }

    public void jalr(InstructionFormatI i, int instructionSize) {
        state.setReg(i.getRd(), state.getPc() + instructionSize);
        state.setPc(state.getReg(i.getRs1()) + i.getImmediate() - instructionSize);
    }

    /**
     * BEQ (Branch if Equal) takes the branch if registers rs1 and rs2 are equal
     */
    void beq(InstructionFormatSB i) {
        beq(i,4);
    }

    public void beq(InstructionFormatSB i, int instructionSize) {
        if (state.getReg(i.getRs1()) == state.getReg(i.getRs2())) jump(state, i.getImm(), instructionSize);
    }

    /**
     * BNE (Branch if Not Equal) takes the branch if registers rs1 and rs2 are not equal
     */
    void bne(InstructionFormatSB i) {
        bne(i, 4);
    }

    public void bne(InstructionFormatSB i, int instructionSize) {
        if (state.getReg(i.getRs1()) != state.getReg(i.getRs2())) jump(state, i.getImm(), instructionSize);
    }

    /**
     * BLT (Branch Less than) takes the branch if register rs1 is less than rs2 using signed comparision
     */
    void blt(InstructionFormatSB i) {
        if (state.getReg(i.getRs1()) < state.getReg(i.getRs2())) jump(state, i.getImm());
    }

    /**
     * BLTU (Branch Less than Unsigned) takes the branch if register rs1 is less than rs2 using unsigned comparision
     */
    void bltu(InstructionFormatSB i) {
        if (state.getReg(i.getRs1()) + 0x80000000 < state.getReg(i.getRs2()) + 0x80000000) jump(state, i.getImm());
    }

    /**
     * BGE (Branch Greater or Equal) takes the branch if register rs1 is greater than rs2 or equal using signed comparision
     */
    void bge(InstructionFormatSB i) {
        if (state.getReg(i.getRs1()) >= state.getReg(i.getRs2())) jump(state, i.getImm());
    }

    /**
     * BGE (Branch Greater or Equal Unsigned) takes the branch if register rs1 is greater than rs2 or equal using unsigned comparision
     */
    void bgeu(InstructionFormatSB i) {
        if (state.getReg(i.getRs1()) + 0x80000000 >= state.getReg(i.getRs2()) + 0x80000000) jump(state, i.getImm());
    }


    private void jump(CpuStateRiscV state, int immediate, int instructionSize) {
        state.setPc(state.getPc() + immediate - instructionSize);
    }

    private void jump(CpuStateRiscV state, int immediate) {
        jump(state, immediate, 4);
    }
}
