package il.co.codeguru.corewars_riscv.gui;

import elemental2.dom.CanvasRenderingContext2D;
import elemental2.dom.HTMLCanvasElement;
import il.co.codeguru.corewars_riscv.gui.widgets.*;

/**
 * @author BS
 */
public class ColumnGraph extends JComponent<HTMLCanvasElement> {

    static class PlayerColumn {
        public String name;
        public float[] values;
        public String col1, col2;

        PlayerColumn(String nm, String c1, String c2) {
            name = nm;
            col1 = c1;
            col2 = c2;
            // the first element holds the sum of all the other values
            values = new float[3];
        }
    }

    private PlayerColumn[] columns;
    private float maxValue;
    private double reduceFactor;

    private static final int NAME_HEIGHT = 17;
    private static final int BOTTOM_MARGIN = 30;

    private CanvasRenderingContext2D ctx;

    public ColumnGraph(String[] names) {
        super("graphs_canvas");
        clear(names);

        ctx = (CanvasRenderingContext2D)(Object)m_element.getContext("2d");
    }


    void clear(String[] names) {
        maxValue = 0;
        reduceFactor = 5;

        ColorHolder colorHolder = ColorHolder.getInstance();
        columns = new PlayerColumn[names.length];
        for(int i = 0; i < names.length; ++i) {
            Color c1 = colorHolder.getColor(i, false);
            Color c2 = colorHolder.getColor(i, true);
            columns[i] = new PlayerColumn(names[i], c1.toString(), c2.toString());
        }
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(500,500);
    }

    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }


    
    public void addToValue(int pos, int subIndex, float value) {
        columns[pos].values[0]+= value;
        columns[pos].values[subIndex+1]+= value;

        if (columns[pos].values[0] > maxValue) {
            // reset graph factor by half to make more room
            maxValue = columns[pos].values[0];
            if (maxValue * reduceFactor > m_element.height-15-BOTTOM_MARGIN) {
                reduceFactor *= 0.5;
            }
        }
        repaint();

        StringBuilder sb = new StringBuilder();
        for (PlayerColumn column : columns) {
            sb.append(column.values[0]);
            sb.append("  ");
        }
        Console.log("Score add " + pos + " " + subIndex + " " + value +
                    "  totals= " + sb.toString());


        paintComponent();
    }

    /* (non-Javadoc)
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    protected void paintComponent() {
        int width = m_element.width, height = m_element.height;
        ctx.fillStyle = CanvasRenderingContext2D.FillStyleUnionType.of("#fdfdfd");
        ctx.fillRect(0, 0, width, height);
        ctx.font = "16px monospace";

		final int numPlayers = columns.length;
		int columnWidth = width / numPlayers;
        for (int i = 0; i < numPlayers; i++) {
            paintColumn(i, columnWidth, height - BOTTOM_MARGIN);
            ctx.fillStyle = CanvasRenderingContext2D.FillStyleUnionType.of(columns[i].col2);
            ctx.fillText(columns[i].name, i*columnWidth+5, height - BOTTOM_MARGIN + NAME_HEIGHT - 2);
        }
    }

    private void paintColumn(int col, int width, int startHeight) {
        ctx.fillStyle = CanvasRenderingContext2D.FillStyleUnionType.of(columns[col].col1);
        int height1 = (int) (reduceFactor * columns[col].values[1]);

        ctx.fillRect(col * width, startHeight - height1, width - 5, height1);
        ctx.fillStyle = CanvasRenderingContext2D.FillStyleUnionType.of(columns[col].col2);
        int height2 = (int) (reduceFactor * columns[col].values[2]);
        ctx.fillRect(col * width, startHeight - height1 - height2, width - 5, height2);
        ctx.fillText("" + columns[col].values[0], col * width + 5, startHeight - height1 - height2 - 5);

    }
}
