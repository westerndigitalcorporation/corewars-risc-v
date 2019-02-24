package il.co.codeguru.corewars_riscv.gui.code_editor.asm_parsers;

import elemental2.dom.DocumentFragment;
import il.co.codeguru.corewars_riscv.gui.code_editor.CodeEditor;

import java.util.ArrayList;

public interface IListParser {
    enum Field {
        START_SPACE,
        INDEX,
        SINGLE_SPACE_AFTER_INDEX,
        SPACE_BEFORE_CODE,
        ADDRESS,
        SPACE_AFTER_ADDRESS,
        OPCODE,
        WARNING,
        CODE,
        PARSE_ERR
    }

    boolean parseLst(String lsttext, StringBuilder opcodesText, ArrayList<CodeEditor.LstLine> m_currentListing);
    char[] parseStdout(String stdoutText, DocumentFragment asmElem, StringBuilder stdoutShorten, ArrayList<Integer> m_lineOffsets);
}