package il.co.codeguru.corewars_riscv.gui;

import elemental2.dom.*;
import elemental2.dom.CanvasRenderingContext2D.FillStyleUnionType;
import il.co.codeguru.corewars_riscv.memory.Memory;
import il.co.codeguru.corewars_riscv.gui.widgets.Color;
import il.co.codeguru.corewars_riscv.gui.widgets.Console;
import il.co.codeguru.corewars_riscv.gui.widgets.Dimension;
import il.co.codeguru.corewars_riscv.gui.widgets.JComponent;
import il.co.codeguru.corewars_riscv.jsadd.Format;
import il.co.codeguru.corewars_riscv.memory.MemoryException;
import il.co.codeguru.corewars_riscv.utils.Logger;
import il.co.codeguru.corewars_riscv.war.War;


public class Canvas extends JComponent<HTMLCanvasElement> {

	public static final int BOARD_SIZE = 256;
	public static final int DOT_SIZE = 3;
    public static final int MARGIN_TOP = 20, MARGIN_RIGHT = 20, MARGIN_BOTTOM = 45, MARGIN_LEFT = 45;
    public static final int BOARD_SIZE_PX = BOARD_SIZE * DOT_SIZE;

    public static final int CANVAS_HEIGHT = BOARD_SIZE*DOT_SIZE + MARGIN_TOP + MARGIN_BOTTOM;
    public static final int CANVAS_WIDTH = BOARD_SIZE*DOT_SIZE + MARGIN_LEFT + MARGIN_RIGHT;

    public static final byte EMPTY = -1;

    private CanvasRenderingContext2D ctx;
	private byte[][] data; // holds colors, not values

	private byte[][] pointer;
	private byte[][] values;

	private float m_zrHscale, m_zrVscale, m_zrX, m_zrY; // zoom rect
    private boolean m_showContent = false;
    private Path2D m_memclip, m_coordXclip, m_coordYclip;

    private HTMLInputElement m_dummyInput; // used to have something that could get input focus
    private HTMLElement m_hoverCellInfo;
    private Memory m_mem = null;
    private boolean m_indebug = false;
    private War m_currentWar = null;

    class Turtle {
        float x, y;
        Path2D p;
        public Turtle(Path2D _p) { p = _p; }
        public void moveTo(float _x, float _y) { x = _x; y = _y; p.moveTo(x, y); }
        public void up(float v)   { y -= v; p.lineTo(x, y); }
        public void down(float v) { y += v; p.lineTo(x, y); }
        public void left(float v) { x -= v; p.lineTo(x, y); }
        public void right(float v){ x += v; p.lineTo(x, y); }
    };

    public static final int TEXT_EPS = 6; // how much to leave in the clipping for half the text
    public static final int TICK_WIDTH = 7;

	public Canvas(String id) {
		super(id);
		ctx = (CanvasRenderingContext2D)(Object)m_element.getContext("2d");

		Dimension d = getMinimumSize();
		m_element.width = d.width;
		m_element.height = d.height;
        ctx.save(); // save state with full page clipping

        m_memclip = new Path2D();
        m_memclip.moveTo(MARGIN_LEFT, MARGIN_TOP);
        m_memclip.lineTo(CANVAS_WIDTH - MARGIN_RIGHT, MARGIN_TOP);
        m_memclip.lineTo(CANVAS_WIDTH - MARGIN_RIGHT, CANVAS_HEIGHT - MARGIN_BOTTOM);
        m_memclip.lineTo(MARGIN_LEFT, CANVAS_HEIGHT - MARGIN_BOTTOM);

        // make a D like shape that deletes the tick at the exact line of the canvas but leaves place for the text to not be clipped
        m_coordYclip = new Path2D();
        Turtle t = new Turtle(m_coordYclip);
        t.moveTo(0, MARGIN_TOP-TEXT_EPS);
        t.right(MARGIN_LEFT-TICK_WIDTH);
        t.down(TEXT_EPS);
        t.right(TICK_WIDTH);
        t.down(BOARD_SIZE_PX);
        t.left(TICK_WIDTH);
        t.down(TEXT_EPS);
        t.left(MARGIN_LEFT-TICK_WIDTH);

        m_coordXclip = new Path2D();
        t.p = m_coordXclip;
        t.moveTo(MARGIN_LEFT-TEXT_EPS, CANVAS_HEIGHT);
        t.up(MARGIN_BOTTOM-TICK_WIDTH);
        t.right(TEXT_EPS);
        t.up(TICK_WIDTH);
        t.right(BOARD_SIZE_PX);
        t.down(TICK_WIDTH);
        t.right(TEXT_EPS);
        t.down(MARGIN_BOTTOM-TICK_WIDTH);

        ctx.clip(m_memclip);

        m_dummyInput = (HTMLInputElement)DomGlobal.document.getElementById("warCanvasDummyInput");
        m_hoverCellInfo = (HTMLElement)DomGlobal.document.getElementById("hoverCellInfo");

		exportMethods();
		initZoom(MARGIN_TOP, MARGIN_LEFT);
		clear();
		setupInputEvents();
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT);
	}

	@Override
	public Dimension getPreferredSize() {
		return getMinimumSize();
	}


	public void paintPixel(int number, byte color, byte value) {
	    paintPixel(number % BOARD_SIZE, number / BOARD_SIZE, color, value);
	}

	public void paintPixel(int x, int y, byte colorByte, byte value) {
        values[x][y] = value;

		Color color = null;
		if (colorByte != -1)
            data[x][y] = colorByte;
        else
            colorByte = data[x][y];
        if (colorByte != -1) {
            color = ColorHolder.getInstance().getColor(colorByte, false);
            ctx.fillStyle = FillStyleUnionType.of(color.toString());
        }
        else {
            // color remains null so that fill text would keep it gray
            ctx.fillStyle = FillStyleUnionType.of("#000000");
        }

        ctx.fillRect(x * DOT_SIZE, y * DOT_SIZE, DOT_SIZE, DOT_SIZE);

        if (m_showContent)
            paintTextValue(x, y, color);
	}


	public void paintPointer(int number, byte colorByte) {
	    int x = number % BOARD_SIZE;
	    int y = number / BOARD_SIZE;
	    Color color = ColorHolder.getInstance().getColor(colorByte, true);

        pointer[x][y] = colorByte;
        ctx.fillStyle = FillStyleUnionType.of(color.toString());
        ctx.fillRect(x * DOT_SIZE, y * DOT_SIZE, DOT_SIZE, DOT_SIZE);
        if (m_showContent)
            paintTextValue(x, y, color);
	}

	public void paintTextValue(int x, int y, Color backCol) {
        String textCol;
        if (backCol != null) {
            if (backCol.m_isDark)
                textCol = "#ffffff";
            else
                textCol = "#000000";
        }
        else
            textCol = "#666666";
        ctx.fillStyle = FillStyleUnionType.of(textCol);
        ctx.fillText(  Format.hex2(values[x][y] & 0xff),  x * DOT_SIZE + 0.2, y * DOT_SIZE + 2.2);
    }


	private static native void resetZoom() /*-{
		$wnd.js_resetZoom()
	}-*/;


	public void initStartWar(War war) {
        m_mem = war.getMemory();
        m_currentWar = war;
        m_indebug = true;
        int addr = 0;
        for (int y = 0; y < BOARD_SIZE; y++)
        {
            for (int x = 0; x < BOARD_SIZE; x++)
            {
                values[x][y] = m_mem.loadByte(addr);
                addr += 1;
            }
        }
    }

	public void clear() {
		if (data == null)
			data = new byte[BOARD_SIZE][BOARD_SIZE];
		if (pointer == null)
			pointer = new byte[BOARD_SIZE][BOARD_SIZE];
		if (values == null)
            values = new byte[BOARD_SIZE][BOARD_SIZE];

		for (int i = 0; i < BOARD_SIZE; i++)
			for (int j = 0; j < BOARD_SIZE; j++) {
				data[i][j] = EMPTY;
				pointer[i][j] = EMPTY;
			}

		ctx.setTransform(1,0,0,1,0,0);


        ctx.clip(m_memclip);

		resetZoom(); // already does the repaint
		//repaint();

        // default clipping is the memory area - for random access writes to not go overboard
        //Path2D
	}

    public void revokeWar() {
	    //m_mem = null;
        //m_currentWar = null;
        m_indebug = false;
    }

	private Color paintMemCellBack(int x, int y) {
        int cellPtr = pointer[x][y];
        Color col = null;
        if (cellPtr != EMPTY) {
            col = ColorHolder.getInstance().getColor(cellPtr, true);
        }
        else {
            int cellVal = data[x][y];
            if (cellVal != EMPTY) {
                col = ColorHolder.getInstance().getColor(cellVal, false);
            }
        }

        if (col != null) {
            ctx.fillStyle = FillStyleUnionType.of(col.toString());
            ctx.fillRect(x * DOT_SIZE, y * DOT_SIZE, DOT_SIZE, DOT_SIZE);
        }
	    return col;
    }

    private static final class Rect {
	    float sx, sy, ex, ey;
	    public boolean isInside(float x, float y) {
	        return x > sx && x < ex && y > sy && y < ey;
        }
    }
    private Rect m_contentVisibleRect = new Rect(); // in memory coordinates

	@Override
	public void paint() {
		ctx.fillStyle = FillStyleUnionType.of(Color.WHITE);
        ctx.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT); // TBD remove this
        ctx.fillStyle = FillStyleUnionType.of(Color.BLACK);
		ctx.fillRect(0, 0, BOARD_SIZE * DOT_SIZE, BOARD_SIZE * DOT_SIZE);

        if (m_showContent) {
		    //ctx.font = Integer.toString((int)(DOT_SIZE * m_zrHscale)) + "px monospace";
            ctx.font = "2.3px monospace";
        }


		for (int y = 0; y < BOARD_SIZE; y++)
		{
			for (int x = 0; x < BOARD_SIZE; x++)
			{
                boolean textVisible = m_showContent && m_contentVisibleRect.isInside(x, y);
                Color col = paintMemCellBack(x, y);
                if (textVisible) {
                    paintTextValue(x, y, col);
                }
			}
		}
		//Console.log("textCount=" + Integer.toString(textVisCount));
		if (m_showContent && m_intervalId != null)
		    paintCursor(true);

		//------------ paint the X and Y coordinates at the margins -------------------

		ctx.restore(); // full clipping, no transform
        ctx.save(); // re-save the full clipping state

		//ctx.setTransform(1, 0, 0,1, 0, 0);
		ctx.fillStyle = FillStyleUnionType.of("#ffffff");
		ctx.fillRect(0,0, MARGIN_LEFT, CANVAS_HEIGHT);
        //ctx.fillRect(0,0, CANVAS_WIDTH, MARGIN_SIZE);
        //ctx.fillRect(CANVAS_WIDTH-MARGIN_SIZE,0, MARGIN_SIZE, CANVAS_HEIGHT);
        ctx.fillRect(0,CANVAS_HEIGHT-MARGIN_BOTTOM, CANVAS_WIDTH, MARGIN_BOTTOM);

        ctx.font = "14px monospace";
        ctx.textBaseline = "middle";

        int step = 32;
        int rc = (int)m_zrVscale;
        if (rc >= 2 && rc < 4)
            step /= 2;
        else if (rc >= 4 && rc <= 8)
            step /= 4;
        else if (rc >= 8 && rc <= 16)
            step /= 8;
        else if (rc >= 16)
            step /= 16;
        //step = Math.max(step / roundScale, 1);
        ctx.fillStyle = FillStyleUnionType.of("#888888");
        ctx.clip(m_coordYclip);

        for(int y = 0; y <= 256; y += step) {
            int ry = Math.min(y, 255);
            float scaledDot = DOT_SIZE*m_zrVscale;
            float ycoord = MARGIN_TOP + m_zrY + ry * scaledDot;
            if (ycoord + scaledDot < MARGIN_TOP || ycoord > CANVAS_HEIGHT-MARGIN_BOTTOM)
                continue;
            ctx.fillText(Format.hex4(ry*256), 1, ycoord + scaledDot/2);
            ctx.fillRect(38, ycoord,  6, scaledDot);
        }

        ctx.restore();
        ctx.save();
        ctx.clip(m_coordXclip);

        ctx.font = "14px monospace";
        ctx.fillStyle = FillStyleUnionType.of("#888888");

        for(int x = 0; x <= 256; x += step) {
            int rx = Math.min(x, 255);

            float scaledDot = DOT_SIZE*m_zrHscale;
            float xcoord = MARGIN_LEFT + m_zrX + rx * scaledDot;

            if (xcoord + scaledDot < MARGIN_LEFT || xcoord > CANVAS_HEIGHT-MARGIN_RIGHT)
                continue;
            ctx.fillText(Format.hex2(rx), xcoord-7+scaledDot/2, CANVAS_HEIGHT - 24);
            ctx.fillRect(xcoord, CANVAS_HEIGHT-44, scaledDot, 6);
        }

        ctx.restore();
        ctx.save();

        //ctx.stroke();


        // restore the transformation and clip we had before for random access writes
        ctx.clip(m_memclip);
        ctx.setTransform(m_zrHscale, 0, 0, m_zrVscale, m_zrX+MARGIN_LEFT, m_zrY+MARGIN_TOP);
        ctx.font = "2.3px monospace";

	}




	/*public void addListener(MouseAddressRequest l) {
		eventCaster.add(l);
	}*/

	public void deletePointers() {
		for (int i = 0; i < BOARD_SIZE; i++)
			for (int j = 0; j < BOARD_SIZE; j++) {
				if (pointer[i][j] != EMPTY) {
					pointer[i][j] = EMPTY;
					paintPixel(i, j, data[i][j], values[i][j]);
				}
			}
	}

	// ------------------------------ zoom and pan ---------------------------
	public native void exportMethods() /*-{
        var that = this
        $wnd.j_warCanvas_repaint = $entry(function() { that.@il.co.codeguru.corewars_riscv.gui.Canvas::j_warCanvas_repaint()() });
        $wnd.j_warCanvas_setTransform = $entry(function(a,b,c,d) { that.@il.co.codeguru.corewars_riscv.gui.Canvas::j_warCanvas_setTransform(FFFF)(a,b,c,d) });
        $wnd.j_warCanvas_click = $entry(function(a,b) { that.@il.co.codeguru.corewars_riscv.gui.Canvas::j_warCanvas_click(FF)(a,b) });
        $wnd.j_warCanvas_showCurrent = $entry(function(a,b) { that.@il.co.codeguru.corewars_riscv.gui.Canvas::j_warCanvas_showCurrent(FF)(a,b) });
    }-*/;


	public void j_warCanvas_repaint() {
		repaint();
	}

	private static native void initZoom(int marginTop, int marginLeft) /*-{
	    $wnd.WC_MARGIN_TOP = marginTop
	    $wnd.WC_MARGIN_LEFT = marginLeft
		$wnd.js_initZoom()
	}-*/;

    public void j_warCanvas_setTransform(float hscale, float vscale, float x, float y) {
        //Console.log("scale= " + Float.toString(hscale) + "  y=" + Float.toString(y ));
        m_zrHscale = hscale;
        m_zrVscale = vscale;
        m_zrX = x;
        m_zrY = y;
        ctx.setTransform(m_zrHscale, 0, 0, m_zrVscale, m_zrX+MARGIN_LEFT, m_zrY+MARGIN_TOP);
        m_showContent = (m_zrHscale > 4.0);

        m_contentVisibleRect.sx = -m_zrX / DOT_SIZE / m_zrHscale - 1;
        m_contentVisibleRect.sy = -m_zrY / DOT_SIZE / m_zrVscale - 1;
        m_contentVisibleRect.ex = m_contentVisibleRect.sx + (256 / m_zrHscale) + 1;
        m_contentVisibleRect.ey = m_contentVisibleRect.sy + (256 / m_zrVscale) + 1;
    }


    private double m_cursorX, m_cursorY;  // in memory coordinates, x can have half cells for the first digit of a byte
    private Double m_intervalId; // id of the blinking interval. if this is null it means there is no cursor
    private boolean m_blinkOn = false;



    private void paintCursor(boolean isOn) {
        //Console.log("BLINK");
        int x = (int)m_cursorX, y = (int)m_cursorY;
        if (isOn) {
            Color col = paintMemCellBack(x, y);

            double rx = m_cursorX;
            if (rx % 1 == 0)
                rx += 0.08; // account for the space between numbers not being the same as the space between digits
            paintMemCellBack((int)m_cursorX, (int)m_cursorY);
            ctx.fillStyle =  FillStyleUnionType.of("#eeeeee");
            ctx.fillRect(rx * DOT_SIZE, m_cursorY * DOT_SIZE + 0.32, 1.25, 2.2);

            paintTextValue(x, y, col);

        }
        else {
            Color col = paintMemCellBack(x, y);
            if (col == null) {
                ctx.fillStyle = FillStyleUnionType.of("#000000");
                ctx.fillRect(x * DOT_SIZE, y * DOT_SIZE, DOT_SIZE, DOT_SIZE);
            }
            paintTextValue(x, y, col);
        }


    }

    public void j_warCanvas_click(float x, float y) {
        if (!m_showContent || x < 0 || y < 0 || x >= BOARD_SIZE*DOT_SIZE || y >= BOARD_SIZE*DOT_SIZE)
            return;
        if (!m_indebug) // not debuggin
            return;
        float mx = (int)(( (x - m_zrX)/ DOT_SIZE/m_zrHscale )*2)/2.0f ; // need half percision to know on which digit we clicked
        float my = (int)( (y - m_zrY)/ DOT_SIZE/m_zrVscale ) ;
        //Console.log("click " + Float.toString(x) + "," + Float.toString(y) + "  : " + Float.toString(mx) + "," + Float.toString(my) );

        paintCursor(false);
        m_cursorX = mx;
        m_cursorY = my;
        m_dummyInput.focus();

        paintCursor(true);
        m_blinkOn = false;

        if (m_intervalId != null)
            DomGlobal.clearInterval(m_intervalId);
        m_intervalId = DomGlobal.setInterval((event) -> {
            if (!m_showContent)
                return; // we zoomed out when cursor is active
            paintCursor(m_blinkOn);
            m_blinkOn = !m_blinkOn;
        }, 600);
    }

    public void j_warCanvas_showCurrent(float x, float y) {
        if (m_mem == null || x < 0 || y < 0 || x >= BOARD_SIZE*DOT_SIZE || y >= BOARD_SIZE*DOT_SIZE) {
            m_hoverCellInfo.style.display = "none";
            return;
        }
        int mx = (int)( (x - m_zrX)/ DOT_SIZE/m_zrHscale );
        int my = (int)( (y - m_zrY)/ DOT_SIZE/m_zrVscale );

        int addr = (mx+my*256) & 0xffff;
        int v = 0;
        v = m_mem.loadByte(addr) & 0xff;

        StringBuilder sb = new StringBuilder();
        sb.append("0x");
        sb.append(Format.hex4(addr));
        sb.append(": 0x");
        sb.append(Format.hex2(v));
        sb.append(" (");
        int bef = sb.length();
        sb.append(v);  // signed/unsigned?
        sb.append(")");
        for(int i = sb.length() - bef; i < 5; ++i)
            sb.append('\u00A0');
        byte player = data[mx][my];
        if (player != -1) {
            sb.append("  Player: ");
            sb.append(m_currentWar.getWarrior(player).getName().substring(0,20));
        }

        m_hoverCellInfo.style.display = "";
        Format.setInnerText(m_hoverCellInfo, sb.toString());
    }

    private static native void js_updateFromKeyScroll(double nx, double ny)/*-{
        $wnd.js_updateFromKeyScroll(nx, ny)
    }-*/;

    private void moveCursor(double dx, int dy) {
        paintCursor(false);
        m_cursorX = Math.max(Math.min(m_cursorX + dx, 255.5), 0.);
        m_cursorY = Math.max(Math.min(m_cursorY + dy, 255.), 0.);

        // need while since it might be completely off screen and need several steps to get to
        while (m_cursorY < m_contentVisibleRect.sy + 1 && m_cursorY > 0) {
            m_zrY = m_zrY+(5.1f*m_zrHscale*DOT_SIZE);
            js_updateFromKeyScroll(m_zrX, m_zrY);
        }
        while (m_cursorY > m_contentVisibleRect.ey - 1 && m_cursorY <= 255) {
            m_zrY = m_zrY-(5.1f*m_zrHscale*DOT_SIZE);
            js_updateFromKeyScroll(m_zrX, m_zrY);
        }
        while (m_cursorX < m_contentVisibleRect.sx + 1 && m_cursorX > 0) {
            m_zrX = m_zrX+(5.1f*m_zrVscale*DOT_SIZE);
            js_updateFromKeyScroll(m_zrX, m_zrY);
        }
        while (m_cursorX > m_contentVisibleRect.ex - 0.5 && m_cursorX <= 255.5) {
            m_zrX = m_zrX-(5.1f*m_zrVscale*DOT_SIZE);
            js_updateFromKeyScroll(m_zrX, m_zrY);
        }

        // this will do bounds checking, update the js variable and call back to java to update the java state
        j_warCanvas_repaint();

    }

    private void setupInputEvents()
    {

        m_dummyInput.addEventListener("blur", (event)-> {
            Console.log("BLUR");
            if (m_intervalId != null)
                DomGlobal.clearInterval(m_intervalId);
            if (m_showContent)
                paintCursor(false);
            m_intervalId = null;
        });

        m_dummyInput.addEventListener("keydown", (event) -> {
            if (!m_showContent)
                return;
            String key = ((KeyboardEvent)event).key;
            event.preventDefault();
            if (key == "ArrowUp")
                moveCursor(0,-1);
            else if (key == "ArrowDown")
                moveCursor(0,1);
            else if (key == "ArrowRight")
                moveCursor(0.5, 0); // halfs because move half a byte
            else if (key == "ArrowLeft" || key == "Backspace")
                moveCursor(-0.5, 0);
            else if (m_mem != null && m_indebug) { // not debugging
                char c = key.charAt(0);
                int v = -1;
                if (c >= '0' && c <= '9')
                    v = c - '0';
                else if (c >= 'a' && c <= 'f')
                    v = c - 'a' + 10;
                else if (c >= 'A' && c <= 'A')
                    v = c - 'A' + 10;
                if (v != -1) {
                    int ix = (int)m_cursorX, iy = (int)m_cursorY;
                    int ev = values[ix][iy];
                    if (m_cursorX % 1 == 0)
                        ev = ev & 0xf | (v << 4);
                    else
                        ev = ev & 0xf0 | v;
                    values[ix][iy] = (byte)ev;
                    m_mem.storeByte((short)(ix+iy*256), (byte)ev);

                    moveCursor(0.5, 0);

                }

            }
        });

    }


}
