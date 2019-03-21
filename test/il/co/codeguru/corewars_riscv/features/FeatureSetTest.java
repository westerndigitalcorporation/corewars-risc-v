package il.co.codeguru.corewars_riscv.features;

import il.co.codeguru.corewars_riscv.war.Warrior;
import org.junit.Assert;
import org.junit.Test;

public class FeatureSetTest {

    class EmptyFeature extends Feature {

        @Override
        public void initWarriorGroup(Warrior... warriors) {

        }
    }

    @Test
    public void enableSingleTest() {
        FeatureSet set = new FeatureSet();
        Feature f = new EmptyFeature();
        set.register("new-feature", f);

        set.enableFeature("new-feature");

        Assert.assertArrayEquals(set.getEnabledFeatures(), new Feature[]{f});
    }
}
