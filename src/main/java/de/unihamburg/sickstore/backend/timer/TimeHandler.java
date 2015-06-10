package de.unihamburg.sickstore.backend.timer;

public interface TimeHandler {

    /**
     * @return the current timestamp in milliseconds
     */
    long getCurrentTime();

    /**
     * Let the system sleep for some seconds.
     *
     */
    void sleep(long duration);
}
