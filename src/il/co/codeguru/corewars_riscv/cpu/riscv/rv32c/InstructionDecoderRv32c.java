package il.co.codeguru.corewars_riscv.cpu.riscv.rv32c;

import il.co.codeguru.corewars_riscv.cpu.riscv.Instruction;
import il.co.codeguru.corewars_riscv.cpu.riscv.InstructionRunner;
import il.co.codeguru.corewars_riscv.cpu.riscv.RV32I;
import il.co.codeguru.corewars_riscv.cpu.riscv.instruction_formats.*;
import il.co.codeguru.corewars_riscv.cpu.riscv.rv32c.instruction_formats.*;

public class InstructionDecoderRv32c {

    public Instruction decode(CInstructionFormatBase i) {
        CInstructionFormatCI ci = new CInstructionFormatCI(i);
        CInstructionFormatCS cs = new CInstructionFormatCS(i);
        CInstructionFormatCB cb = new CInstructionFormatCB(i);
        switch (i.getOpcode()) {
            case RV32C.OpcodeTypes.C0:
                CInstructionFormatCIW ciw = new CInstructionFormatCIW(i);
                switch (i.getFunct3()) {
                    case 0:
                        /*
                         * C.ADDI4SPN is a CIW-format instruction that adds a zero-extended
                         * non-zero immediate, scaled by 4, to the stack pointer, x2, and
                         * writes the result to rd.  This instruction is used
                         * to generate pointers to stack-allocated variables, and expands to
                         * "addi rd, x2, nzuimm[9:2]".
                         */
                        if (ciw.getImmediate() == 0) {
                            return null;
                        }
                        int bit3 = ciw.getImmediate() & 1;
                        int bit2 = (ciw.getImmediate() >> 1) & 1;
                        int bit96 = (ciw.getImmediate() >> 2) & 15;
                        int bit54 = (ciw.getImmediate() >> 6) & 3;
                        int nzuimm = (bit2 | (bit3 << 1) | (bit54 << 2) | (bit96 << 4)) << 2;
                        return new Instruction(RV32C.Opcodes.CADDI4SPN, RV32I.instructionI(RV32I.Opcodes.Addi, ciw.getRd(), 2, nzuimm),
                                (InstructionFormatBase format, InstructionRunner runner) -> runner.addi(new InstructionFormatI(format)));
                    case 2:
                        /*
                         * C.LW loads a 32-bit value from memory into register rd.  It computes
                         * an effective address by adding the zero-extended offset, scaled by 4, to
                         * the base address in register rs1.
                         * It expands to "lw rd, offset[6:2](rs1)".
                         */
                        CInstructionFormatCL cl = new CInstructionFormatCL(i);
                        return new Instruction(RV32C.Opcodes.CLW, RV32I.instructionI(RV32I.Opcodes.Lw, cl.getRd(), cl.getRs1(), cl.getImmediate()),
                                (InstructionFormatBase format, InstructionRunner runner) -> runner.lw(new InstructionFormatI(format)));
                    case 6:
                        /*
                         * C.SW stores a 32-bit value in register rs2 to memory.  It computes an
                         * effective address by adding the zero-extended offset, scaled by 4, to
                         * the base address in register rs1.
                         * It expands to sw rs2, offset[6:2](rs1).
                         */
                        return new Instruction(RV32C.Opcodes.CSW, RV32I.instructionS(RV32I.Opcodes.Sw, cs.getRs1(), cs.getRs2(), cs.getImmediate()),
                                (InstructionFormatBase format, InstructionRunner runner) -> runner.sw(new InstructionFormatS(format)));
                }

            case RV32C.OpcodeTypes.C1:
                CInstructionFormatCJ cj = new CInstructionFormatCJ(i);
                switch (i.getFunct3()) {
                    case 0:
                        if (ci.getRs1() != 0) {
                            /*
                             * C.ADDI adds the non-zero sign-extended 6-bit immediate to the value in
                             * register rd then writes the result to rd.  C.ADDI expands
                             * into "addi rd, rd, nzimm[5:0]".
                             * C.ADDI is only valid when rd != x0.
                             */
                            return new Instruction(RV32C.Opcodes.CADDI, RV32I.instructionI(RV32I.Opcodes.Addi, ci.getRs1(), ci.getRs1(), ci.getImmediate()),
                                    (InstructionFormatBase format, InstructionRunner runner) -> runner.addi(new InstructionFormatI(format)));
                        }

                    case 1:
                        /*
                         * C.JAL is an RV32C-only instruction that performs the same operation as C.J,
                         * but additionally writes the address of the instruction following the jump
                         * (pc+2) to the link register, x1.  C.JAL expands to "jal x1, offset[11:1]".
                         */
                        return new Instruction(RV32C.Opcodes.CJAL, RV32I.instructionUJ(RV32I.Opcodes.Jal, 1, cj.getImmediate()),
                                (InstructionFormatBase format, InstructionRunner runner) -> runner.jal(new InstructionFormatUJ(format), 2));
                    case 2:
                        /*
                         * C.LI loads the sign-extended 6-bit immediate, imm, into
                         * register rd.  C.LI is only valid when rd != x0.
                         * C.LI expands into "addi rd, x0, imm[5:0]".
                         */
                        return new Instruction(RV32C.Opcodes.CLI, RV32I.instructionI(RV32I.Opcodes.Addi, ci.getRs1(), 0, ci.getImmediate()),
                                (InstructionFormatBase format, InstructionRunner runner) -> runner.addi(new InstructionFormatI(format)));
                    case 3:
                        if (ci.getRs1() == 2) {
                            /*
                             * C.ADDI16SP shares the opcode with C.LUI, but has a destination field
                             * of x2. C.ADDI16SP adds the non-zero sign-extended 6-bit immediate to
                             * the value in the stack pointer sp(x2), where the
                             * immediate is scaled to represent multiples of 16 in the range
                             * (-512,496). C.ADDI16SP is used to adjust the stack pointer in procedure
                             * prologues and epilogues.  It expands into "addi x2, x2, nzimm[9:4]".
                             */
                            byte imm = ci.getImmediate();
                            int bit5 = imm & 1;
                            int bits78 = (imm >> 1) & 3;
                            int bit6 = (imm >> 3) & 1;
                            int bit4 = (imm >> 4) & 1;
                            int bit9 = (imm >> 5) & 1;
                            int nzimm = (bit4 | (bit5 << 1) | (bit6 << 2) | (bits78 << 3) | (bit9 << 5)) << 4;

                            return new Instruction(RV32C.Opcodes.CADDI16SP, RV32I.instructionI(RV32I.Opcodes.Addi, 2, 2, nzimm),
                                    (InstructionFormatBase format, InstructionRunner runner) -> runner.addi(new InstructionFormatI(format)));
                        } else if (ci.getRs1() != 0) {
                            /*
                             * C.LUI loads the non-zero 6-bit immediate field into bits 17--12 of the
                             * destination register, clears the bottom 12 bits, and sign-extends bit
                             * 17 into all higher bits of the destination.  C.LUI is only valid when
                             * rd != (x0/x2),
                             * and when the immediate is not equal to zero.
                             * C.LUI expands into "lui rd, nzimm[17:12]".
                             */
                            return new Instruction(RV32C.Opcodes.CLUI, RV32I.instructionU(RV32I.Opcodes.Lui, ci.getRs1(), ci.getImmediate()),
                                    (InstructionFormatBase format, InstructionRunner runner) -> runner.lui(new InstructionFormatU(format)));
                        }

                    case 4:
                        if (cb.getFunct2() != 3) {
                            switch (cb.getFunct2()) {
                                case 0:
                                    /*
                                     * C.SRLI is a CB-format instruction that performs a logical right shift
                                     * of the value in register rd then writes the result to rd.
                                     * The shift amount is encoded in the shamt field, where
                                     * shamt[5] must be zero for RV32C.  For RV32C and RV64C, the shift
                                     * amount must be non-zero.  For RV128C, a shift amount of zero is used
                                     * to encode a shift of 64.  Furthermore, the shift amount is sign-extended
                                     * for RV128C, and so the legal shift amounts are 1--31, 64, and 96--127.
                                     * C.SRLI expands into "srli rd, rd, shamt[5:0]",
                                     * except for RV128C with shamt=0, which expands to
                                     * "srli rd, rd, 64".
                                     */
                                    return new Instruction(RV32C.Opcodes.CSRLI, RV32I.instructionI(RV32I.Opcodes.Srli, cb.getRs1(), cb.getRs1(), cb.getImmediate()),
                                            (InstructionFormatBase format, InstructionRunner runner) -> runner.srli(new InstructionFormatI(format)));
                                case 1:
                                    /*
                                     * C.SRAI is defined analogously to C.SRLI, but instead performs an arithmetic
                                     * right shift.
                                     * C.SRAI expands to "srai rd, rd, shamt[5:0]".
                                     */
                                    return new Instruction(RV32C.Opcodes.CSRAI, RV32I.instructionI(RV32I.Opcodes.Srai, cb.getRs1(), cb.getRs1(), cb.getImmediate()),
                                            (InstructionFormatBase format, InstructionRunner runner) -> runner.srai(new InstructionFormatI(format)));
                                case 2:
                                    /*
                                     * C.ANDI is a CB-format instruction that computes the bitwise AND of
                                     * of the value in register rd and the sign-extended 6-bit immediate,
                                     * then writes the result to rd.
                                     * C.ANDI expands to "andi rd, rd, imm[5:0]".
                                     */
                                    return new Instruction(RV32C.Opcodes.CANDI, RV32I.instructionI(RV32I.Opcodes.Andi, cb.getRs1(), cb.getRs1(), cb.getImmediate()),
                                            (InstructionFormatBase format, InstructionRunner runner) -> runner.andi(new InstructionFormatI(format)));
                            }
                        } else if (((cs.getFunct6() >> 2) & 1) == 0) {
                            switch (cs.getFunct2()) {
                                case 0:
                                    /*
                                     * C.SUB subtracts the value in register rs2 from the value in
                                     * register rd, then writes the result to register rd.
                                     * C.SUB expands into "sub rd, rd, rs2".
                                     */
                                    return new Instruction(RV32C.Opcodes.CSUB, RV32I.instructionR(RV32I.Opcodes.Sub, cs.getRs1(), cs.getRs1(), cs.getRs2()),
                                            (InstructionFormatBase format, InstructionRunner runner) -> runner.sub(new InstructionFormatR(format)));
                                case 1:
                                    /*
                                     * C.XOR computes the bitwise XOR of the values in registers rd
                                     * and rs2, then writes the result to register rd.
                                     * C.XOR expands into "xor rd, rd, rs2".
                                     */
                                    return new Instruction(RV32C.Opcodes.CXOR, RV32I.instructionR(RV32I.Opcodes.Xor, cs.getRs1(), cs.getRs1(), cs.getRs2()),
                                            (InstructionFormatBase format, InstructionRunner runner) -> runner.xor(new InstructionFormatR(format)));
                                case 2:
                                    /*
                                     * C.OR computes the bitwise OR of the values in registers rd
                                     * and rs2, then writes the result to register rd.
                                     * C.OR expands into "or rd, rd, rs2".
                                     */
                                    return new Instruction(RV32C.Opcodes.COR, RV32I.instructionR(RV32I.Opcodes.Or, cs.getRs1(), cs.getRs1(), cs.getRs2()),
                                            (InstructionFormatBase format, InstructionRunner runner) -> runner.or(new InstructionFormatR(format)));
                                case 3:
                                    /*
                                     * C.AND computes the bitwise AND of the values in registers rd
                                     * and rs2, then writes the result to register rd.
                                     * C.AND expands into "and rd, rd, rs2".
                                     */
                                    return new Instruction(RV32C.Opcodes.CAND, RV32I.instructionR(RV32I.Opcodes.And, cs.getRs1(), cs.getRs1(), cs.getRs2()),
                                            (InstructionFormatBase format, InstructionRunner runner) -> runner.and(new InstructionFormatR(format)));
                            }

                        }
                    case 5:
                        /*
                         * C.J performs an unconditional control transfer.  The offset is sign-extended and
                         * added to the pc to form the jump target address.  C.J can therefore target
                         * a +-2 KiB range.  C.J expands to "jal x0, offset[11:1]".
                         */
                        return new Instruction(RV32C.Opcodes.CJ, RV32I.instructionUJ(RV32I.Opcodes.Jal, 0, cj.getImmediate()),
                                (InstructionFormatBase format, InstructionRunner runner) -> runner.jal(new InstructionFormatUJ(format), 2));
                    case 6:
                        /*
                         * C.BEQZ performs conditional control transfers.  The offset is sign-extended
                         * and added to the pc to form the branch target address.  It can
                         * therefore target a +-256B range.  C.BEQZ takes the branch if the
                         * value in register rs1 is zero.  It expands to "beq rs1, x0,offset[8:1]".
                         */
                        return new Instruction(RV32C.Opcodes.CBEQZ, RV32I.instructionSB(RV32I.Opcodes.Beq, cb.getRs1(), 0, cb.getBranchImmediate()),
                                (InstructionFormatBase format, InstructionRunner runner) -> runner.beq(new InstructionFormatSB(format), 2));
                    case 7:
                        /*
                         * C.BNEZ is defined analogously to C.BEQZ, but it takes the branch if rs1 contains
                         * a nonzero value.  It expands to "bne rs1, x0, offset[8:1]".
                         */
                        return new Instruction(RV32C.Opcodes.CBNEZ, RV32I.instructionSB(RV32I.Opcodes.Bne, cb.getRs1(), 0, cb.getBranchImmediate()),
                                (InstructionFormatBase format, InstructionRunner runner) -> runner.bne(new InstructionFormatSB(format), 2));
                }

            case RV32C.OpcodeTypes.C2:
                switch (i.getFunct3()) {
                    case 0:
                        /*
                         * C.SLLI is a CI-format instruction that performs a logical left shift
                         * of the value in register rd then writes the result to rd.
                         * The shift amount is encoded in the shamt field, where
                         * shamt[5] must be zero for RV32C.  For RV32C and RV64C, the shift
                         * amount must be non-zero.  For RV128C, a shift amount of zero is used
                         * to encode a shift of 64.  C.SLLI expands into "slli rd, rd,
                         *   shamt[5:0]", except for RV128C with shamt=0, which expands to
                         * "slli rd, rd, 64".
                         */
                        return new Instruction(RV32C.Opcodes.CSLLI, RV32I.instructionI(RV32I.Opcodes.Slli, ci.getRs1(), ci.getRs1(), ci.getImmediate()),
                                (InstructionFormatBase format, InstructionRunner runner) -> runner.slli(new InstructionFormatI(format)));
                    case 2:
                        /*
                         * C.LWSP loads a 32-bit value from memory into register rd.  It computes
                         * an effective address by adding the zero-extended offset, scaled by 4, to
                         * the stack pointer, x2.  It expands to "lw rd, offset[7:2](x2)".
                         * C.LWSP is only valid when rd != x0.
                         */
                        int bit76 = ci.getUnsignedImmediate() & 3;
                        int bit42 = (ci.getUnsignedImmediate() >> 2) & 7;
                        int bit5 = (ci.getUnsignedImmediate() >> 5) & 1;
                        int uimm = (bit42 | (bit5 << 3) | (bit76 << 4)) << 2;
                        return new Instruction(RV32C.Opcodes.CLWSP, RV32I.instructionI(RV32I.Opcodes.Lw, ci.getRs1(), 2, uimm),
                                (InstructionFormatBase format, InstructionRunner runner) -> runner.lw(new InstructionFormatI(format)));
                    case 4:
                        CInstructionFormatCR cr = new CInstructionFormatCR(i);
                        if ((cr.getFunct4() & 1) == 1) {
                            if (cr.getRs1() != 0 && cr.getRs2() != 0) //c.add
                            {
                                /*
                                 * C.ADD adds the values in registers rd and rs2 and writes the
                                 * result to register rd. C.ADD expands into "add rd, rd, rs2".
                                 */
                                return new Instruction(RV32C.Opcodes.CADD, RV32I.instructionR(RV32I.Opcodes.Add, cr.getRs1(), cr.getRs1(), cr.getRs2()),
                                        (InstructionFormatBase format, InstructionRunner runner) -> runner.add(new InstructionFormatR(format)));
                            } else if (cr.getRs1() != 0 && cr.getRs2() == 0) {
                                /*
                                 * C.JALR (jump and link register) performs the same operation as C.JR,
                                 * but additionally writes the address of the instruction following the
                                 * jump (pc+2) to the link register, x1.  C.JALR expands to
                                 * "jalr x1, 0(rs1)".
                                 */
                                return new Instruction(RV32C.Opcodes.CJALR, RV32I.instructionI(RV32I.Opcodes.Jalr, 1, cr.getRs1(), 0),
                                        (InstructionFormatBase format, InstructionRunner runner) -> runner.jalr(new InstructionFormatI(format), 2));
                            }
                        } else {
                            if (cr.getRs1() != 0 && cr.getRs2() != 0) {
                                /*
                                 * C.MV copies the value in register rs2 into register rd. C.MV
                                 * expands into "add rd, x0, rs2".
                                 */
                                return new Instruction(RV32C.Opcodes.CMV, RV32I.instructionR(RV32I.Opcodes.Add, cr.getRs1(), 0, cr.getRs2()),
                                        (InstructionFormatBase format, InstructionRunner runner) -> runner.add(new InstructionFormatR(format)));
                            } else if (cr.getRs1() != 0 && cr.getRs2() == 0) {
                                /*
                                 * C.JR (jump register) performs an unconditional control transfer to
                                 * the address in register rs1.  C.JR expands to "jalr x0, 0(rs1)".
                                 */
                                return new Instruction(RV32C.Opcodes.CJR, RV32I.instructionI(RV32I.Opcodes.Jalr, 0, cr.getRs1(), 0),
                                        (InstructionFormatBase format, InstructionRunner runner) -> runner.jalr(new InstructionFormatI(format), 2));
                            }
                        }

                    case 6:
                        /*
                         * C.SWSP stores a 32-bit value in register rs2 to memory.  It computes
                         * an effective address by adding the zero-extended offset, scaled by 4, to
                         * the stack pointer, x2.
                         * It expands to "sw rs2, offset[7:2](x2)".
                         */
                        CInstructionFormatCSS css = new CInstructionFormatCSS(i);

                        return new Instruction(RV32C.Opcodes.CSWSP, RV32I.instructionS(RV32I.Opcodes.Sw, 2, css.getRs2(), css.getWord()),
                                (InstructionFormatBase format, InstructionRunner runner) -> runner.sw(new InstructionFormatS(format)));
                }
        }

        return null;
    }

}
