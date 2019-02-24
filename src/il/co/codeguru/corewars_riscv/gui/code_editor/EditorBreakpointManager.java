package il.co.codeguru.corewars_riscv.gui.code_editor;

import il.co.codeguru.corewars_riscv.gui.PlayersPanel;

import java.util.List;
import java.util.ListIterator;

public class EditorBreakpointManager {
    private final CodeEditor codeEditor;

    private List<PlayersPanel.Breakpoint> m_breakpoints = null;

    public EditorBreakpointManager(CodeEditor codeEditor) {
        this.codeEditor = codeEditor;
    }

    public void setBreakpoints(List<PlayersPanel.Breakpoint> newBreakpoints) {
        m_breakpoints = newBreakpoints;
    }

    public void changePlayer(List<PlayersPanel.Breakpoint> newBreakpoints)
    {
        m_breakpoints = newBreakpoints;
        for(PlayersPanel.Breakpoint b: m_breakpoints)
            codeEditor.setLineNumBreakpoint(b.lineNum, true);
    }

    public void toggleBreakpointEdit(int atline) {
        PlayersPanel.Breakpoint found = null;
        for(PlayersPanel.Breakpoint breakpoint: m_breakpoints)
            if (breakpoint.lineNum == atline) {
                found = breakpoint;
                break;
            }

        codeEditor.setLineNumBreakpoint(atline, found == null);
        if (found != null)
            m_breakpoints.remove(found);
        else
            m_breakpoints.add(new PlayersPanel.Breakpoint(atline));
    }

    public void removeCurrentBreakpoints() {
        if (m_breakpoints != null) // will be null on the first time
            for(PlayersPanel.Breakpoint b: m_breakpoints)
                codeEditor.setLineNumBreakpoint(b.lineNum, false);
    }

    public void updateBreakpoints(List<Integer> prevLineOffsets, int prevLineCount, String prevInText, int selStart, int selEnd, String keydown) {
        if (m_breakpoints == null || m_breakpoints.size() == 0)
            return;

        // for every breakpoint decide if it should be moved or deleted
        ListIterator<PlayersPanel.Breakpoint> it = m_breakpoints.listIterator();
        while(it.hasNext())
        {
            PlayersPanel.Breakpoint br = it.next();
            int lineStartIndex = 0; // the case for lineNum==1
            assert br.lineNum > 0: "unexpected breakpoint line number";
            if (br.lineNum != 1) // -1 to make it 0 based, -1 because we want where the previous line ends, +1 to move past the NL
                lineStartIndex = prevLineOffsets.get(br.lineNum - 1 - 1) + 1;

            int lineNLIndex = prevLineOffsets.get(br.lineNum - 1); // -1 make it 0 based

            if (selStart != selEnd && (selStart < lineStartIndex && selEnd > lineStartIndex   // removed the before the start of the line, including the start
                    ||  selStart == lineStartIndex && selEnd > lineNLIndex ))  // removed from the start of the line, passed the end of it
            { // removing the whole line
                // no need to remove markers because we just made a new line numbers column
                it.remove();
                continue;
            }

            // if it's not the above case, we don't care about anything that doesn't change the line count
            if  (prevLineCount != codeEditor.getLineCount())
            {
                // if we backspaced on a breakpoint,  need to remove it
                // need to happen in normal caret, no selection and the backspace on the line of the breakpoint
                if (selStart == selEnd && selStart == lineStartIndex && (keydown == "Backspace" || keydown == "Delete")) {
                    boolean isLineWithText = false;
                    if (prevInText != null) {
                        for(int i = lineStartIndex; i < lineNLIndex && !isLineWithText; ++i) {
                            char c = prevInText.charAt(i);
                            isLineWithText = (c != ' ' && c != '\n');
                        }
                    }
                    if (!isLineWithText) {
                        it.remove();
                        continue;
                    }
                }
                // if we removed lines before this breakpoint line, move it up
                if (selStart <= lineStartIndex && selEnd <= lineStartIndex) {
                    br.lineNum += codeEditor.getLineCount() - prevLineCount;
                    codeEditor.setLineNumBreakpoint(br.lineNum, true);
                }
            }

            codeEditor.setLineNumBreakpoint(br.lineNum, true);
        }
    }
}
