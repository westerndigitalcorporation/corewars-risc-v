package il.co.codeguru.corewars_riscv;

import il.co.codeguru.corewars_riscv.gui.CompetitionWindow;
import il.co.codeguru.corewars_riscv.gui.widgets.JButton;

import java.io.IOException;

public class CoreWarsEngine
{
	public static void main (String args[]) throws IOException
	{
        CompetitionWindow c = new CompetitionWindow();
        c.setVisible(true);
        c.pack();
    }
}