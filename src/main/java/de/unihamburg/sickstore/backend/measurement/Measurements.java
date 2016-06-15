package de.unihamburg.sickstore.backend.measurement;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramLogWriter;
import org.HdrHistogram.Recorder;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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
        exportMeasurements( formatTimestamp(timestamp) + "-sickstore", formatTimestamp(timestamp));
        _singleton = new Measurements();
    }

    public void finishMeasurement(String output) throws IOException {
        exportMeasurements(output, output);
        _singleton = new Measurements();
    }

    public void finishMeasurement(String outputFolder, String ouputName) throws IOException {
        exportMeasurements(outputFolder, ouputName);
        _singleton = new Measurements();
    }


    public void exportMeasurements(String outputFolder, String ouputName) throws IOException
    {

        HashMap<String, HashMap<String, String>> summaries = new HashMap<String, HashMap<String, String>>();

        for (String key :_measurements.keySet()) {
            File file = new File("results/sickstore/" + outputFolder + "/percentiles-" + key.toLowerCase() + "-" + ouputName + ".dat");
            FileUtils.forceMkdir(file.getParentFile());

            Recorder measurement = _measurements.get(key);

            Histogram histogram = measurement.getIntervalHistogram();

            // Summary
            HashMap<String, String> summary = new HashMap<String, String>();
            summary.put("Count", "" + histogram.getTotalCount());
            summary.put("MaxValue(ms)", "" + histogram.getMaxValue());
            summary.put("MinValue(ms)", "" + histogram.getMinValue());
            summary.put("Mean(ms)", "" + histogram.getMean());
            summary.put("StdDeviation(ms)", "" + histogram.getStdDeviation());
            summary.put("90Percentile(ms)", "" + histogram.getValueAtPercentile(90));
            summary.put("99Percentile(ms)", "" + histogram.getValueAtPercentile(99) );
            summary.put("99.9Percentile(ms)", "" + histogram.getValueAtPercentile(99.9));
            summary.put("99.99Percentile(ms)", "" + histogram.getValueAtPercentile(99.99));
            summaries.put(key, summary);

            // percentile distribution
            histogram.outputPercentileDistribution(new PrintStream(file), 2, 1.0, true);

            FileOutputStream compressedHistogram = new FileOutputStream("results/sickstore/" + outputFolder + "/hdrhistogram-" + key.toLowerCase() + "-" + ouputName + ".hdr");
            PrintStream log = new PrintStream(compressedHistogram, false, "UTF-8");
            HistogramLogWriter histogramLogWriter = new HistogramLogWriter(log);
            histogramLogWriter.outputIntervalHistogram(histogram);
            log.close();
        }
        // export summary
        File summaryFile = new File("results/sickstore/" + outputFolder + "/summary.json");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(summaryFile, summaries);
    }


    public String formatTimestamp(long timestamp){
        Date date = new Date(timestamp);
        DateFormat formatter = new SimpleDateFormat("YMd_HHmmss");
        String dateFormatted = formatter.format(date);
        return dateFormatted;
    }
}
