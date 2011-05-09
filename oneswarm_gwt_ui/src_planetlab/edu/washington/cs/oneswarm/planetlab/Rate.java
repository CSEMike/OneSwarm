package edu.washington.cs.oneswarm.planetlab;

/** A class which accepts cumulative values and differences. */
public class Rate<T extends Number> {

    T eldestValue = null;
    long eldestValueMs = 0;

    public Rate() {
        eldestValue = null;
    }

    /**
     * Returns the rate of change per second relative to the previous call. If
     * no call has been made, returns 0 and sets an initial base value.
     */
    public double updateAndGetRate(T latestValue) {

        long now = System.currentTimeMillis();

        if (eldestValue == null) {
            eldestValue = latestValue;
            eldestValueMs = now;
            return 0;
        }

        if (eldestValueMs == now) {
            return 0;
        }

        double out = (latestValue.doubleValue() - eldestValue.doubleValue())
                / ((now - eldestValueMs) / 1000.0);

        eldestValue = latestValue;
        eldestValueMs = now;

        return out;
    }

    /** Small 2 QPS test app. */
    public static final void main(String... args) throws Exception {

        Rate<Long> r = new Rate<Long>();

        for (long i = 0; i < 10; i++) {
            System.out.println(r.updateAndGetRate(i));
            Thread.sleep(500);
        }
    }
}
