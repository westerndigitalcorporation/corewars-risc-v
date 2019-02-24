package il.co.codeguru.corewars_riscv.gui;

public class SettingsPanel {
    public static boolean useNewMemory()
    {
        return getCheckboxValue("#new-memory");
    }

    private static native boolean getCheckboxValue(String selector) /*-{
        return $wnd.$(selector).prop("checked");
    }-*/;

}
