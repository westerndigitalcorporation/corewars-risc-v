package il.co.codeguru.corewars_riscv.cpu.riscv.rv32i;

import il.co.codeguru.corewars_riscv.cpu.exceptions.InvalidOpcodeException;
import il.co.codeguru.corewars_riscv.cpu.riscv.Instruction;
import il.co.codeguru.corewars_riscv.cpu.riscv.InstructionFormat;
import il.co.codeguru.corewars_riscv.cpu.riscv.rv32i.instruction_formats.*;

public class InstructionDecoder32I {
    public Instruction decode(InstructionFormat format_input) throws InvalidOpcodeException
    {
        InstructionFormatBase i = (InstructionFormatBase) format_input;
        switch(i.getOpcode())
        {
            case RV32I.OpcodeTypes.LOAD:
                return loadOpcode(i);
            case RV32I.OpcodeTypes.OP_IMM:
                return immOpcode(i);
            case RV32I.OpcodeTypes.AUIPC:
                return new Instruction(RV32I.Opcodes.Auipc, new InstructionFormatU(i),
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.auipc(new InstructionFormatU(format)));
            case RV32I.OpcodeTypes.STORE:
                return storeOpcode(i);
            case RV32I.OpcodeTypes.OP:
                return registerOpcode(i);
            case RV32I.OpcodeTypes.LUI:
                return new Instruction(RV32I.Opcodes.Lui, new InstructionFormatU(i),
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.lui(new InstructionFormatU(format)));
            case RV32I.OpcodeTypes.BRANCH:
                return branchOpcode(i);
            case RV32I.OpcodeTypes.SYS:
                return sysOpcode(i);
            case RV32I.OpcodeTypes.JALR:
                return new Instruction(RV32I.Opcodes.Jalr, new InstructionFormatI(i),
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.jalr(new InstructionFormatI(format)));
            case RV32I.OpcodeTypes.JAL:
                return new Instruction(RV32I.Opcodes.Jal, new InstructionFormatUJ(i),
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.jal(new InstructionFormatUJ(format)));
            default:
                throw new InvalidOpcodeException();
        }
    }

    private Instruction sysOpcode(InstructionFormatBase i) throws  InvalidOpcodeException {
        InstructionFormatI ii = new InstructionFormatI(i);
        switch(ii.getFunct3()) {
            case 0:
                if(ii.getImmediate() == 0) {
                    return new Instruction(RV32I.Opcodes.Ecall, ii,
                            (InstructionFormatBase format, InstructionRunner32I runner) -> runner.ecall(new InstructionFormatI(format)));
                }
            default:
                throw new InvalidOpcodeException();
        }
    }

    private Instruction branchOpcode(InstructionFormatBase i) throws InvalidOpcodeException {
        InstructionFormatSB sb = new InstructionFormatSB(i);
        switch(sb.getFunct3())
        {
            case 0:
                return new Instruction(RV32I.Opcodes.Beq, sb,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.beq(new InstructionFormatSB(format)));
            case 1:
                return new Instruction(RV32I.Opcodes.Bne, sb,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.bne(new InstructionFormatSB(format)));
            case 4:
                return new Instruction(RV32I.Opcodes.Blt, sb,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.blt(new InstructionFormatSB(format)));
            case 5:
                return new Instruction(RV32I.Opcodes.Bge, sb,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.bge(new InstructionFormatSB(format)));
            case 6:
                return new Instruction(RV32I.Opcodes.Bltu, sb,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.bltu(new InstructionFormatSB(format)));
            case 7:
                return new Instruction(RV32I.Opcodes.Bgeu, sb,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.bgeu(new InstructionFormatSB(format)));
            default:
                throw new InvalidOpcodeException();
        }
    }

    private Instruction registerOpcode(InstructionFormatBase i) throws InvalidOpcodeException {
        InstructionFormatR ir = new InstructionFormatR(i);
        switch(ir.getFunct3())
        {
            case 0:
                switch (ir.getFunct7())
                {
                    case 0:
                        return new Instruction(RV32I.Opcodes.Add, ir,
                                (InstructionFormatBase format, InstructionRunner32I runner) -> runner.add(new InstructionFormatR(format)));
                    case 32:
                        return new Instruction(RV32I.Opcodes.Sub, ir,
                                (InstructionFormatBase format, InstructionRunner32I runner) -> runner.sub(new InstructionFormatR(format)));
                    default:
                        throw new InvalidOpcodeException();
                }
            case 1:
                return new Instruction(RV32I.Opcodes.Sll, ir,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.sll(new InstructionFormatR(format)));
            case 2:
                return new Instruction(RV32I.Opcodes.Slt, ir,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.slt(new InstructionFormatR(format)));
            case 3:
                return new Instruction(RV32I.Opcodes.Sltu, ir,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.sltu(new InstructionFormatR(format)));
            case 4:
                return new Instruction(RV32I.Opcodes.Xor, ir,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.xor(new InstructionFormatR(format)));
            case 5:
                switch(ir.getFunct7())
                {
                    case 0:
                        return new Instruction(RV32I.Opcodes.Srl, ir,
                                (InstructionFormatBase format, InstructionRunner32I runner) -> runner.srl(new InstructionFormatR(format)));
                    case 32:
                        return new Instruction(RV32I.Opcodes.Sra, ir,
                                (InstructionFormatBase format, InstructionRunner32I runner) -> runner.sra(new InstructionFormatR(format)));
                    default:
                        throw new InvalidOpcodeException();
                }
            case 6:
                return new Instruction(RV32I.Opcodes.Or, ir,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.or(new InstructionFormatR(format)));
            case 7:
                return new Instruction(RV32I.Opcodes.And, ir,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.and(new InstructionFormatR(format)));
            default:
                throw new InvalidOpcodeException();
        }
    }

    private Instruction storeOpcode(InstructionFormatBase i) throws InvalidOpcodeException {
        InstructionFormatS is = new InstructionFormatS(i);
        switch(is.getFunct3())
        {
            case 0:
                return new Instruction(RV32I.Opcodes.Sb, is,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.sb(new InstructionFormatS(format)));
            case 1:
                return new Instruction(RV32I.Opcodes.Sh, is,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.sh(new InstructionFormatS(format)));
            case 2:
                return new Instruction(RV32I.Opcodes.Sw, is,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.sw(new InstructionFormatS(format)));
            default:
                throw new InvalidOpcodeException();
        }
    }

    private Instruction immOpcode(InstructionFormatBase i) throws InvalidOpcodeException {
        InstructionFormatI ii = new InstructionFormatI(i);
        switch (ii.getFunct3())
        {
            case 0x0:
                return new Instruction(RV32I.Opcodes.Addi, ii,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.addi(new InstructionFormatI(format)));
            case 0x1:
                return new Instruction(RV32I.Opcodes.Slli, ii,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.slli(new InstructionFormatI(format)));
            case 0x2:
                return new Instruction(RV32I.Opcodes.Slti, ii,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.slti(new InstructionFormatI(format)));
            case 0x3:
                return new Instruction(RV32I.Opcodes.Sltiu, ii,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.sltiu(new InstructionFormatI(format)));
            case 0x4:
                return new Instruction(RV32I.Opcodes.Xori, ii,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.xori(new InstructionFormatI(format)));
            case 0x5:
                int imm = ii.getImmediate() >> 5;
                switch(imm)
                {
                    case 0:
                        return new Instruction(RV32I.Opcodes.Srli, ii,
                                (InstructionFormatBase format, InstructionRunner32I runner) -> runner.srli(new InstructionFormatI(format)));
                    case 32:
                        return new Instruction(RV32I.Opcodes.Srai, ii,
                                (InstructionFormatBase format, InstructionRunner32I runner) -> runner.srai(new InstructionFormatI(format)));
                    default:
                        throw new InvalidOpcodeException();
                }
            case 0x6:
                return new Instruction(RV32I.Opcodes.Ori, ii,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.ori(new InstructionFormatI(format)));
            case 0x7:
                return new Instruction(RV32I.Opcodes.Andi, ii,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.andi(new InstructionFormatI(format)));
            default:
                throw new InvalidOpcodeException();
        }
    }

    private Instruction loadOpcode(InstructionFormatBase i) throws InvalidOpcodeException {
        InstructionFormatI ii = new InstructionFormatI(i);
        switch(ii.getFunct3())
        {
            case 0x0:
                return new Instruction(RV32I.Opcodes.Lb, ii,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.lb(new InstructionFormatI(format)));
            case 0x1:
                return new Instruction(RV32I.Opcodes.Lh, ii,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.lh(new InstructionFormatI(format)));
            case 0x2:
                return new Instruction(RV32I.Opcodes.Lw, ii,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.lw(new InstructionFormatI(format)));
            case 0x4:
                return new Instruction(RV32I.Opcodes.Lbu, ii,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.lbu(new InstructionFormatI(format)));
            case 0x5:
                return new Instruction(RV32I.Opcodes.Lhu, ii,
                        (InstructionFormatBase format, InstructionRunner32I runner) -> runner.lhu(new InstructionFormatI(format)));
            default:
                throw new InvalidOpcodeException();
        }
    }
}
