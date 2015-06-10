package de.unihamburg.sickstore.backend.timer;

public class FakeTimeHandler implements TimeHandler {

    private long currentTime;

    /**
     * @return current timestamp in milli seconds
     */
    public long getCurrentTime() {
        return currentTime;
    }

    /**
     * Simulate some sleep, but actually do not sleep.
     *
     * @param duration
     */
    public void sleep(long duration) {
        increaseTime(duration);
    }

    public void increaseTime(long delay) {
        currentTime += delay;
    }
}
