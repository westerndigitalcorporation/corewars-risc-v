package  il.co.codeguru.corewars_riscv.jsadd;

import elemental2.dom.HTMLElement;

public class Format {
    public static String hex(int num) {
        return Integer.toHexString(num).toUpperCase();
    }

    public static String hex(int num, int pad) {
        StringBuilder s = new StringBuilder(Integer.toHexString(num).toUpperCase());
        for(int i = s.length(); i < pad; ++i)
            s.insert(0, "0");
        return s.toString();
    }
    public static String hex2(int num) {
        String s = Integer.toHexString(num).toUpperCase();
        if(s.length() == 1)
            return "0" + s;
        return s;
    }
    public static String hex4(int num) {
        String s = Integer.toHexString(num).toUpperCase();;
        switch(s.length()) {
            case 1: return "000" + s;
            case 2: return "00" + s;
            case 3: return "0" + s;
            case 4: return s;
        }
        return s;
    }
    public static String hex5(int num) {
        String s = Integer.toHexString(num).toUpperCase();;
        switch(s.length()) {
            case 1: return "0000" + s;
            case 2: return "000" + s;
            case 3: return "00" + s;
            case 4: return "0" + s;
            case 5: return s;
        }
        return s;
    }

    public static String padding(int repeat, char padChar)  {
        char[] buf = new char[repeat];

        for(int i = 0; i < buf.length; ++i) {
            buf[i] = padChar;
        }

        return new String(buf);
    }


    // this is here just because it's a useful utility class
    public static native String innerText(HTMLElement elem) /*-{
        return elem.innerText
    }-*/;

    public static native void setInnerText(HTMLElement elem, String text)/*-{
        elem.innerText = text
    }-*/;

}