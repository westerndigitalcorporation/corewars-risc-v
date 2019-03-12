package il.co.codeguru.corewars_riscv.features;

import org.junit.Assert;
import org.junit.Test;

public class FeatureSetTest {

    @Test
    public void registerSingleTest() {
        FeatureSet set = new FeatureSet();
        Feature f = new Feature();

        set.register("new-feature", f);

        Assert.assertArrayEquals(new Feature[]{f}, set.getRegisterdFeatures());
    }

    @Test
    public void enableSingleTest() {
        FeatureSet set = new FeatureSet();
        Feature f = new Feature();
        set.register("new-feature", f);

        set.enableFeature("new-feature");

        Assert.assertArrayEquals(set.getEnabledFeatures(), new Feature[]{f});
    }
}
