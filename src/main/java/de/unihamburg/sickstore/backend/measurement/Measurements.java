package de.unihamburg.sickstore.backend.measurement;


import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramLogWriter;
import org.HdrHistogram.Recorder;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Steffen Friedrich on 20.10.2015.
 */
public class Measurements {

    static Measurements _singleton = null;
    private ConcurrentHashMap<String,Recorder> _measurements;

    private long timestamp = 0;


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
        timestamp = System.currentTimeMillis();
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
        exportMeasurements(formatTimestamp(timestamp));
        _singleton = new Measurements();
    }

    public void finishMeasurement(String output) throws IOException {
        exportMeasurements(output);
        _singleton = new Measurements();
    }


    public void exportMeasurements(String output) throws IOException
    {
        for (String key :_measurements.keySet()) {
            File file = new File("results/" + output + "/percentiles-" + key.toLowerCase() + "-" + output + ".dat");
            FileUtils.forceMkdir(file.getParentFile());

            Recorder measurement = _measurements.get(key);

            Histogram histogram = measurement.getIntervalHistogram();
            AbstractHistogram.Percentiles percentiles = histogram.percentiles(2);

            FileUtils.writeStringToFile(file, "percentile;count;latency" + System.lineSeparator(), true);
            percentiles.forEach((i) -> {
                long valueIteratedFrom = i.getValueIteratedTo();
                double p = i.getPercentileLevelIteratedTo();
                long count = i.getCountAtValueIteratedTo();
                try {
                    FileUtils.writeStringToFile(file, p + ";" + count + ";" + valueIteratedFrom + System.lineSeparator(), true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            FileOutputStream compressedHistogram = new FileOutputStream("results/" + output + "/hdrhistogram-" + key.toLowerCase() + "-" + ".dat");
            PrintStream log = new PrintStream(compressedHistogram, false, "UTF-8");
            HistogramLogWriter histogramLogWriter = new HistogramLogWriter(log);
            histogramLogWriter.outputIntervalHistogram(histogram);
            log.close();
        }
    }


    public String formatTimestamp(long timestamp){
        Date date = new Date(timestamp);
        DateFormat formatter = new SimpleDateFormat("YMd_HHmmss");
        String dateFormatted = formatter.format(date);
        return dateFormatted;
    }
}
