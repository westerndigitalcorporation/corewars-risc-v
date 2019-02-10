package il.co.codeguru.corewars_riscv.cpu.riscv.rv32c;

import il.co.codeguru.corewars_riscv.cpu.riscv.rv32c.instruction_formats.*;

import static org.junit.Assert.*;

import il.co.codeguru.corewars_riscv.utils.Logger;
import org.junit.Test;

public class InstructionFormatTest {
    @Test
    public void testCr()
    {
        CInstructionFormatCR cr = new CInstructionFormatCR((short)0);
        assertEquals(0, cr.getRs1());
        assertEquals(0, cr.getRs2());
        assertEquals(0,cr.getFunct4());

        cr = new CInstructionFormatCR((short)0x908a); //c.add x1, x2
        assertEquals(2,cr.getOpcode());
        assertEquals(1, cr.getRs1());
        assertEquals(2, cr.getRs2());
        assertEquals(4, cr.getFunct3());
        assertEquals(9, cr.getFunct4());
    }

    @Test
    public void testCrConstructor()
    {
        CInstructionFormatCR cr = new CInstructionFormatCR((byte)0,(byte)0,(byte)0,(byte)0);
        assertEquals((short)0, cr.getRaw());

        cr = new CInstructionFormatCR((byte)2, (byte)1, (byte)2, (byte)9);
        assertEquals((short)0x908a, cr.getRaw());
    }

    @Test
    public void testCi()
    {
        CInstructionFormatCI cr = new CInstructionFormatCI((short)0);
        assertEquals(0, cr.getRs1());
        assertEquals(0, cr.getImmediate());
        assertEquals(0,cr.getFunct3());

        cr = new CInstructionFormatCI((short)0x019D); //c.add x1, x2
        assertEquals(1,cr.getOpcode());
        assertEquals(3, cr.getRs1());
        assertEquals(0, cr.getFunct3());
        assertEquals(7, cr.getImmediate());
    }

    @Test
    public void testCiConstructor()
    {
        CInstructionFormatCI cr = new CInstructionFormatCI((byte)0,(byte)0,(byte)0,(byte)0);
        assertEquals((short)0, cr.getRaw());

        cr = new CInstructionFormatCI((byte)1, (byte)0, (byte)3, (byte)7);
        assertEquals((short)0x19D, cr.getRaw());
    }

    @Test
    public void testCss()
    {
        CInstructionFormatCSS css = new CInstructionFormatCSS((short)0);
        assertEquals(0, css.getRs2());
        assertEquals(0, css.getImmediate());
        assertEquals(0,css.getFunct3());

        css = new CInstructionFormatCSS((short)0xCA16); //c.swsp x5, 20(x2)
        assertEquals(2,css.getOpcode());
        assertEquals(5, css.getRs2());
        assertEquals(6, css.getFunct3());
        assertEquals(20, css.getImmediate());
    }

    @Test
    public void testCssConstructor()
    {
        CInstructionFormatCSS css = new CInstructionFormatCSS((byte)0,(byte)0,(byte)0,(byte)0);
        assertEquals((short)0, css.getRaw());

        css = new CInstructionFormatCSS((byte)2, (byte)6, (byte)5, (byte)20);
        assertEquals((short)0xCA16, css.getRaw());
    }

    @Test
    public void testCssCreateFromWord()
    {
        CInstructionFormatCSS css = CInstructionFormatCSS.createWithWord((byte)0,(byte)0,(byte)0,(byte)0);
        assertEquals((short)0, css.getRaw());

        css = CInstructionFormatCSS.createWithWord((byte)2, (byte)6, (byte)5, (byte)20);
        assertEquals((short)0xCA16, css.getRaw());
    }

    @Test
    public void testCiw()
    {
        CInstructionFormatCIW ciw = new CInstructionFormatCIW((short)0);
        assertEquals(8, ciw.getRd());
        assertEquals(0, ciw.getImmediate());
        assertEquals(0,ciw.getFunct3());

        ciw = new CInstructionFormatCIW((short)0x1004); //c.addi4spn x9, x2, 0x20
        assertEquals(0,ciw.getOpcode());
        assertEquals(9, ciw.getRd());
        assertEquals(0, ciw.getFunct3());
        assertEquals(0x80, ciw.getImmediate() & 0xFF);
    }

    @Test
    public void testCiwConstructor()
    {
        CInstructionFormatCIW ciw = new CInstructionFormatCIW((byte)0, (byte)0, (byte)9, (byte)0x80);
        assertEquals((short)0x1004, ciw.getRaw());
    }

    @Test
    public void testCl()
    {
        CInstructionFormatCL cl = new CInstructionFormatCL((short)0x4880); //c.lw x8,0x10(x9)
        assertEquals(0,cl.getOpcode());
        assertEquals(8, cl.getRd());
        assertEquals(9, cl.getRs1());
        assertEquals(2, cl.getFunct3());
        assertEquals(0x10, cl.getImmediate() & 0xFF);
    }

    @Test
    public void testClConstructor()
    {
        CInstructionFormatCL ciw = new CInstructionFormatCL((byte)0, (byte)2, (byte)8,(byte)9, (byte)0x10);
        assertEquals((short)0x4880, ciw.getRaw());
    }

    @Test
    public void testCs()
    {
        CInstructionFormatCS cs = new CInstructionFormatCS((short)0x8c65); //c.and x8, x9
        assertEquals(1,cs.getOpcode());
        assertEquals(8, cs.getRs1());
        assertEquals(9, cs.getRs2());
        assertEquals(4, cs.getFunct3());
        assertEquals(35, cs.getFunct6());
        assertEquals(3, cs.getFunct2());

        cs = new CInstructionFormatCS((short)0xc988); //c.sw x10,0x10(x11)
        assertEquals(0, cs.getOpcode());
        assertEquals(11, cs.getRs1());
        assertEquals(10, cs.getRs2());
        assertEquals(6, cs.getFunct3());
        assertEquals(0x10, cs.getImmediate());
    }

    @Test
    public void testCsConstructor()
    {
        CInstructionFormatCS cs = CInstructionFormatCS.fromFunct6((byte)1,(byte)35,(byte)3, (byte)8, (byte)9);
        assertEquals((short)0x8c65, cs.getRaw());

        cs = new CInstructionFormatCS((byte)0,(byte)6,(byte)11,(byte)10,(byte)0x10);
        assertEquals((short)0xc988, cs.getRaw());
    }

    @Test
    public void testCb()
    {
        CInstructionFormatCB cb = new CInstructionFormatCB((short)0x8015); //c.srli x8, 5
        assertEquals(1 ,cb.getOpcode());
        assertEquals(8, cb.getRs1());
        assertEquals(4, cb.getFunct3());
        assertEquals(5, cb.getImmediate());
        assertEquals(0, cb.getFunct2());

        cb = new CInstructionFormatCB((short)0xdcfd); //c.beqz x9, -2
        assertEquals(1, cb.getOpcode());
        assertEquals(9, cb.getRs1());
        assertEquals(6, cb.getFunct3());
        assertEquals(-2, cb.getBranchImmediate());
    }

    @Test
    public void testCbConstructor()
    {
        CInstructionFormatCB cb = new CInstructionFormatCB((byte)1, (byte)4, (byte)0, (byte)8, (byte)5);
        assertEquals((short)0x8015, cb.getRaw());

        cb = CInstructionFormatCB.forBranch((byte)1, (byte)6, (byte)9, (byte)-2);
        assertEquals((short)0xdcfd, cb.getRaw());
    }

    @Test
    public void testCj()
    {
        Logger.setTestingMode();
        CInstructionFormatCJ cd = new CInstructionFormatCJ((short)0xbfed);
        assertEquals(1, cd.getOpcode());
        assertEquals(5, cd.getFunct3());
        assertEquals(-6 ,cd.getImmediate());

        cd = new CInstructionFormatCJ((short)0xa009);
        assertEquals(1, cd.getOpcode());
        assertEquals(5, cd.getFunct3());
        assertEquals(2 ,cd.getImmediate());
    }

    @Test
    public void testCjConstructor()
    {
        CInstructionFormatCJ cj = new CInstructionFormatCJ((byte)1,(byte)5,(short)-6);
        assertEquals((short)0xbfed, cj.getRaw());

        cj = new CInstructionFormatCJ((byte)1, (byte)5, (short)2);
        assertEquals((short)0xa009, cj.getRaw());
    }



}
