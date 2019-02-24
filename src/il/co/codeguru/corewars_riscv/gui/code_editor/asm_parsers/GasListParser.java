package il.co.codeguru.corewars_riscv.gui.code_editor.asm_parsers;


import elemental2.dom.DocumentFragment;
import elemental2.dom.Element;
import il.co.codeguru.corewars_riscv.gui.code_editor.CodeEditor;
import il.co.codeguru.corewars_riscv.gui.widgets.Console;
import il.co.codeguru.corewars_riscv.war.WarriorRepository;

import java.util.ArrayList;

public class GasListParser implements IListParser {
    @Override
    public boolean parseLst(String lsttext, StringBuilder opcodesText, ArrayList<CodeEditor.LstLine> m_currentListing)
    {
        String[] lines = lsttext.split("\\n");

        int lineIndex = 1; // does not increment in warning lines that appear in the listing file
        CodeEditor.LstLine prevLine = null;
        int totalOpcodeCount = 0;
        for(int i = 0; i < lines.length; ++i)
        {
            String line = lines[i];
            Field state = Field.START_SPACE;
            CodeEditor.LstLine l = new CodeEditor.LstLine();

            int indexStart = 0, addressStart = 0, opcodeStart = 0;
            int charsBeforeCode = 0; // number of characters after the space after address and before the code. used fo not missing indentation
            for(int j = 0; j < line.length(); ++j)
            {
                char c = line.charAt(j);
                switch(state) {
                    case START_SPACE:
                        if (TextUtils.isDigit(c)) {
                            indexStart = j;
                            state = Field.INDEX;
                        }
                        else if (line.equals("NO DEFINED SYMBOLS") || line.equals("DEFINED SYMBOLS")) {
                            return true; // signifies the end of the actual opcode listing
                        }
                        else if (c != ' ')
                            state = Field.PARSE_ERR;
                        break;
                    case INDEX:
                        if (c == ' ') {
                            state = Field.SINGLE_SPACE_AFTER_INDEX;
                            l.lineNum = Integer.parseInt(line.substring(indexStart,j));
                            // check the line number only at the end in order to sip warnings
                        }
                        else if (!TextUtils.isDigit(c))
                            state = Field.PARSE_ERR;
                        break;
                    case SINGLE_SPACE_AFTER_INDEX:
                        if (c == ' ') {
                            state = Field.SPACE_BEFORE_CODE;
                            charsBeforeCode = -5; // account for not having an address
                        }
                        else if (TextUtils.isHexDigit(c)) {
                            addressStart = j;
                            state = Field.ADDRESS;
                        }
                        else
                            state = Field.PARSE_ERR;
                        break;
                    case ADDRESS:
                        if (c == ' ') {
                            state = Field.SPACE_AFTER_ADDRESS;
                            l.addressStr = line.substring(addressStart, j);
                            l.address = Integer.parseInt(l.addressStr, 16);
                        }
                        else if (!TextUtils.isHexDigit(c))
                            state = Field.PARSE_ERR;
                        break;
                    case SPACE_AFTER_ADDRESS:

                        state = Field.OPCODE;
                        opcodeStart = j;

                        break;
                    case OPCODE:
                        boolean islast = (j == line.length() - 1);
                        if (c == '*') {
                            state = Field.WARNING;
                        }
                        else if (!islast && charsBeforeCode < 8)
                            ++charsBeforeCode; // take anything as long as its in the field size of the opcode. need this sinc resb adds spaces
                        else if (c == '\t' || islast) { // continueation lines of a string definition end in the middle of the opcode field.

                            l.fullOpcode = line.substring(opcodeStart, j);
                            l.opcode = TextUtils.spacedHex(l.fullOpcode);
                            l.opcodesCount = TextUtils.countDigits(l.fullOpcode) / 2;
                            totalOpcodeCount += l.opcodesCount;
                            if (totalOpcodeCount > WarriorRepository.MAX_WARRIOR_SIZE)
                                return true; // is going to fail later in setText we check here just for not getting stuch in a long loop
                            state = Field.SPACE_BEFORE_CODE;
                            ++charsBeforeCode;
                        }
                        else
                            ++charsBeforeCode;
                        break;
                    case SPACE_BEFORE_CODE:
                        if (c == '*') {
                            state = Field.WARNING;
                        }
                        else if (c != ' ' || charsBeforeCode == 9) {
                            state = Field.CODE;
                            l.code = line.substring(j);
                        }
                        else
                            ++charsBeforeCode;
                        break;
                    case CODE:
                        break; // don't care about the code part, we already have that from the input
                    case PARSE_ERR:
                        Console.log("ERROR: parsing list file! " + Integer.toString(i) + ":" + Integer.toString(j) + "\n" + lsttext);
                        return false;
                } // switch
                if (state == Field.WARNING)
                    break; // stop parsing line
            } // for j in line chars
            if (state == Field.WARNING)
                continue; // skip this line
            if (l.lineNum > lineIndex)
            {  // this can happen if there is a \ at the end of a line, extending it to the next line
                // so the next line doesn't exist in the line count, we need to just skip it in the output
                // this can happe for multiple consecutive lines
                while (l.lineNum != lineIndex) {
                    opcodesText.append("\n");
                    ++lineIndex;
                }
            }
            else if (prevLine != null && l.lineNum == prevLine.lineNum) {
                // it's a continuation line of the previous line. we need to concatenate to get the full opcode in order to know its size
                // happens with string definition db "abcdefgh"
                prevLine.fullOpcode += l.fullOpcode;
                prevLine.opcodesCount = TextUtils.countDigits(prevLine.fullOpcode) / 2;
                // no need to update the display opcode because its already too long
                continue;
            }
            else if (l.lineNum != lineIndex) {
                Console.log("wrong line number " + l.lineNum + " at " + lineIndex);
                return false;
            }

            ++lineIndex;

            m_currentListing.add(l);
            opcodesText.append(l.opcode);
            opcodesText.append("\n");

            prevLine = l;
        }

        return true;        
        

    }

    @Override
    public char[] parseStdout(String stdoutText, DocumentFragment asmElem, StringBuilder stdoutShorten, ArrayList<Integer> m_lineOffsets) {
        Console.log(stdoutText);

        String[] lines = stdoutText.split("\\n");
        // warning come before errors so we can't assume the line numbers are ascending
        // so we need to save all the line nums, sort and then go over from start to end of the text

        int countAllNL = m_lineOffsets.size();

        // have a potential char for every line in the asm text. this way there's no need to sort
        // and there is only one entry per line, error trumps warning
        // used for determining the color of a line
        char[] m_errLines = new char[countAllNL]; // for every line in the asmText, 0,'e' or 'w'

        // go over stdout, find out which lines need marking
        for (String line : lines) {
            int firstColon = -1;
            int lineNum = -1; // this would be zero based
            char lineType = 0;
            // find first and second columns chars
            for (int j = 0; j < line.length(); ++j) {
                if (line.charAt(j) == ':') {
                    if (firstColon == -1)
                        firstColon = j;
                    else {
                        if (line.charAt(firstColon + 1) == ' ') { // number-less warning line
                            lineNum = -2;
                        } else {
                            lineNum = Integer.parseInt(line.substring(firstColon + 1, j));
                            lineNum -= 1; // read numbers are 1 based
                            assert lineNum < countAllNL : "unexpected lineNum";
                        }
                        if (j + 2 < line.length()) { // sanity check on the line length
                            lineType = Character.toLowerCase(line.charAt(j + 2)); // +2 for a space and then the first letter of 'Error' or 'Warning'
                            if (!(lineType == 'w' && m_errLines[lineNum] == 'e')) // not the case where an 'w' overwrites a 'e'
                                m_errLines[lineNum] = lineType;
                        }
                        break;
                    }
                }
            }
            if (lineNum == -1) {
                Console.log("Failed parsing error stdout");
                return m_errLines;
            }


            stdoutShorten.append("<div class='stdout_line_").append(lineType).append("'");
            if (lineNum != -2)
                stdoutShorten.append("ondblclick='asm_cursorToLine(").append(m_lineOffsets.get(lineNum)).append(")'");
            stdoutShorten.append(">");
            stdoutShorten.append(line.substring(firstColon + 1));
            stdoutShorten.append("</div>");

        }


        for(int lineNum = 0; lineNum < m_errLines.length; ++lineNum)
        {
            char ec = m_errLines[lineNum];
            if (ec == 0)
                continue;

            Element e = TextUtils.DocumentFragment_getElementById(asmElem, "mline_" + (lineNum + 1));
            if (e == null)
                continue; // can happen with some strange case of dz... ? could not reproduce but it happened
            if (ec == 'e')
                e.classList.add("edit_error");
            else
                e.classList.add("edit_warning");

        }

        return m_errLines;

    }
}