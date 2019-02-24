package il.co.codeguru.corewars_riscv.gui.code_editor.asm_parsers;

import elemental2.dom.DocumentFragment;
import elemental2.dom.Element;

public class TextUtils {
    public static final char SPACE_FOR_HEX_CHAR = '\u202f';
    public static final String SPACE_FOR_HEX = "&#x202f;";


    // hex field in the opcode can have all sorts of brackets and -. need to know how many just digits
    public static int countDigits(String s) {
        boolean doingDigits = s.length() > 0 && isHexDigit(s.charAt(0)); // see below 'nesb 4'
        if (!doingDigits)
            return 0; // not supported yet
        int count = 0;
        for(int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (isHexDigit(c)) {
                ++count;
            }
        }
        return count;
    }

    public static String spacedHex(String s)
    {
        // find how many spaces from the end should be trimmed
        // spaces appear at the end since we take everything in the code area of the lst
        int upto = s.length() - 1;
        for(; upto >= 0; --upto) {
            if (s.charAt(upto) != ' ')
                break;
        }
        StringBuilder bs = new StringBuilder();
        int digitCount = 0;
        boolean doingDigits = s.length() > 0 && isHexDigit(s.charAt(0)); // if it's not a hex number thing, don't do any spacing (resb 4)

        for(int i = 0; i <= upto; ++i) {
            char c = s.charAt(i);
            if (digitCount == 7*2) {
                // don't add more than 7 bytes of opcode to not overflow the field size
                bs.append(SPACE_FOR_HEX); // ellipsis
                break;
            }
            if (doingDigits && isHexDigit(c)) {
                bs.append(c);
                ++digitCount;
                if ((digitCount % 2) == 0 && digitCount > 0)
                    bs.append("&#x202f;"); // thin space
                continue;
            }
            else if (c == '<') {
                bs.append("&lt;");
                continue;
            }
            else if (c == '>') {
                bs.append("&gt;");
                continue;
            }
            else if ( (c == ')' || c == ']') && bs.length() > 8 && bs.substring(bs.length()-8) == "&#x202f;") {
                // if we see an end brace but we just added a space
                // put the end brace before the space so it would look good
                bs.insert(bs.length() - 8, c);
                continue;
            }
            bs.append(c);
        }
        return bs.toString();
    }

    public static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
    public static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }



    public static native Element DocumentFragment_getElementById(DocumentFragment df, String id) /*-{
        return df.getElementById(id)
    }-*/;

}