package de.unihamburg.sickstore.backend.timer;

public class SystemTimeHandler implements TimeHandler {

    /**
     * @return current timestamp in milli seconds
     */
    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    /**
     * Simulate some sleep, but actually do not sleep.
     *
     * @param duration
     */
    public void sleep(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
