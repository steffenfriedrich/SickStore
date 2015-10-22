package de.unihamburg.sickstore.backend.measurement;


import org.HdrHistogram.Recorder;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Steffen Friedrich on 20.10.2015.
 */
public class Measurements {

    static Measurements _singleton = null;
    private ConcurrentHashMap<String,Recorder> _measurements;

    /**
     * Singleton
     */
    public synchronized static Measurements getMeasurements() {
        if (_singleton == null) {
            _singleton = new Measurements();
        }
        return _singleton;
    }

    public Measurements()
    {
        _measurements = new ConcurrentHashMap<String,Recorder>();
    }


    public void measure(String operation, long latency)
    {
        Recorder r = getOneMeasurement(operation);
        r.recordValue(latency);
    }

    private Recorder getOneMeasurement(String operation) {
        Recorder r = _measurements.get(operation);
        if(r == null)
        {
            r = new Recorder(3);
            Recorder oldR = _measurements.putIfAbsent(operation, r);
            if(oldR != null)
            {
                r = oldR;
            }
        }
        return r;
    }

    public void finishMeasurement() throws IOException {
        exportMeasurements();
        _singleton = new Measurements();
    }

    private void exportMeasurements() throws IOException
    {
        for (Recorder measurement : _measurements.values())
        {
            // TODO
        }
    }
}
