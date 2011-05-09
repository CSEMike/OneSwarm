package edu.washington.cs.oneswarm.f2f.network;

import java.util.ArrayList;
import java.util.Random;

import junit.framework.TestCase;
import edu.washington.cs.oneswarm.test_tools.PlotCdf;

public class RandomnessManagerTester extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testGetDeterministicRandomInt() {
        try {
            RandomnessManager man1 = new RandomnessManager();
            RandomnessManager man2 = new RandomnessManager();
            Random random = new Random();

            byte[] rand = new byte[20];

            int hits = 0;
            int total = 10000;
            double frac = 0.95;
            for (int i = 0; i < total; i++) {
                random.nextBytes(rand);
                int randomVal = man1.getDeterministicRandomInt(rand);
                if (randomVal < 0) {
                    randomVal = -randomVal;
                }
                if (randomVal < Integer.MAX_VALUE * frac) {
                    hits++;
                }
            }
            System.out.println("fraction: " + ((1.0 * hits) / total));

            ArrayList<Double> uniform1 = new ArrayList<Double>();
            for (int i = 0; i < 10000; i++) {
                int seed = random.nextInt();
                // System.out.println("testing: seed=" + seed);
                // test that 2 calls to same manager will generate
                // same result
                int man1rand1 = man1.getDeterministicRandomInt(seed);
                int man1rand2 = man1.getDeterministicRandomInt(seed);
                assertEquals(man1rand1, man1rand2);
                // System.out.println("man1rand1=" + man1rand1);
                // System.out.println("man1rand2=" + man1rand2);
                int man2rand1 = man2.getDeterministicRandomInt(seed);
                int man2rand2 = man2.getDeterministicRandomInt(seed);
                assertEquals(man2rand1, man2rand2);
                // System.out.println("man2rand1=" + man2rand1);
                // test that 2 calls to different managers with same seed
                // generate different results
                boolean equals = man1rand1 == man2rand1;
                assertEquals(equals, false);

                uniform1.add(new Double(1.0 * man1rand1));
            }

            new PlotCdf("getDeterministicRandomInt", "x", "y", false, uniform1).plot();

            ArrayList<Double> uniform2 = new ArrayList<Double>();
            int MAX_VAL = 900;
            int MIN_VAL = 300;
            for (int i = 0; i < 10000; i++) {
                byte[] seed = new byte[20];
                random.nextBytes(seed);
                // System.out.println("testing: seed=" + seed);
                // test that 2 calls to same manager will generate
                // same result
                int man1rand1 = man1.getDeterministicNextInt(seed, MIN_VAL, MAX_VAL);
                int man1rand2 = man1.getDeterministicNextInt(seed, MIN_VAL, MAX_VAL);
                assertEquals(man1rand1, man1rand2);

                uniform2.add(new Double(1.0 * man1rand1));
            }
            new PlotCdf("getDeterministicNextInt_" + MIN_VAL + "-" + MAX_VAL, "x", "y", false,
                    uniform2).plot();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

}
