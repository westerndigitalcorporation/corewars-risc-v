package il.co.codeguru.corewars_riscv.features;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FeatureSet {

    private Map<String, Feature> featureMap;

    public FeatureSet() {
        featureMap = new HashMap<>();
    }

    public void register(String name, Feature feature) {
        featureMap.put(name, feature);
    }

    public Set<Map.Entry<String, Feature>> getRegisterdFeatures() {
        return featureMap.entrySet();
    }

    public void enableFeature(String name) {
        featureMap.get(name).enable();
    }

    public Feature[] getEnabledFeatures() {
        return featureMap.values().stream().filter((Feature::isEnabled)).toArray(Feature[]::new);
    }

    public static FeatureSet getAllFeatures() {
        FeatureSet ans = new FeatureSet();
        ans.register("new-memory", new NewMemory());
        ans.register("push-pop-all", new PushPopAll());
        return ans;
    }
}
