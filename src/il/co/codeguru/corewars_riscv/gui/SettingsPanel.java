package il.co.codeguru.corewars_riscv.gui;

import il.co.codeguru.corewars_riscv.features.Feature;
import il.co.codeguru.corewars_riscv.features.FeatureSet;
import il.co.codeguru.corewars_riscv.utils.Logger;

import java.util.Map;

public class SettingsPanel {
    public static boolean useNewMemory()
    {
        return false;
    }

    public static void addAllOptions() {
        FeatureSet features = FeatureSet.getAllFeatures();
        for(Map.Entry<String, Feature> feature : features.getRegisterdFeatures()) {
            Logger.log("Adding option: " + feature.getKey());
            addOption(feature.getKey());
        }
    }

    private static native boolean isEnabled(String option) /*-{
        return $wnd.settings_menu.isEnabled(option);
    }-*/;

    private static native void addOption(String option) /*-{
        $wnd.settings_menu.addOption(option);
    }-*/;

}
