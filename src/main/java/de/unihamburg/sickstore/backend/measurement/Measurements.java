package de.unihamburg.sickstore.backend.measurement;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramLogWriter;
import org.HdrHistogram.Recorder;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Steffen Friedrich on 20.10.2015.
 */
public class Measurements {
    private ConcurrentHashMap<String, Recorder> _measurements = new ConcurrentHashMap<String, Recorder>();
    private ConcurrentHashMap<String, Histogram> _totalHistograms = new ConcurrentHashMap<String, Histogram>();
    private long timestamp = 0;
    private boolean printstatus = false;


    public Measurements(Boolean printstatus) {
        this.printstatus = printstatus;
    }


    public void measure(String operation, long latency) {
        Recorder r = getOneMeasurement(operation);
        r.recordValue(latency);
    }

    private Recorder getOneMeasurement(String operation) {
        Recorder r = _measurements.get(operation);
        if (r == null) {
            r = new Recorder(3);
            Recorder oldR = _measurements.putIfAbsent(operation, r);
            if (oldR != null) {
                r = oldR;
            }
        }
        return r;
    }

    public void finishMeasurement() throws IOException {
        exportMeasurements(formatTimestamp(timestamp) + "-sickstore", formatTimestamp(timestamp));
        cleanup();
    }

    public void finishMeasurement(String output) throws IOException {
        exportMeasurements(output, output);
        cleanup();
    }

    public void finishMeasurement(String outputFolder, String ouputName) throws IOException {
        exportMeasurements(outputFolder, ouputName);
        cleanup();
    }


    public void exportMeasurements(String outputFolder, String ouputName) throws IOException {

        HashMap<String, HashMap<String, String>> summaries = new HashMap<String, HashMap<String, String>>();

        for (String key : _measurements.keySet()) {
            getIntervalHistogramAndAccumulate(key);
            File file = new File("results/sickstore/" + outputFolder + "/percentiles-" + key.toLowerCase() + "-" + ouputName + ".dat");
            FileUtils.forceMkdir(file.getParentFile());

            Histogram histogram = _totalHistograms.get(key);


            // Summary
            HashMap<String, String> summary = new HashMap<String, String>();
            summary.put("Count", "" + histogram.getTotalCount());
            summary.put("MaxValue(ms)", "" + histogram.getMaxValue());
            summary.put("MinValue(ms)", "" + histogram.getMinValue());
            summary.put("Mean(ms)", "" + histogram.getMean());
            summary.put("StdDeviation(ms)", "" + histogram.getStdDeviation());
            summary.put("90Percentile(ms)", "" + histogram.getValueAtPercentile(90));
            summary.put("99Percentile(ms)", "" + histogram.getValueAtPercentile(99));
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
        summaryFile.createNewFile();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(summaryFile, summaries);
    }


    public String formatTimestamp(long timestamp) {
        Date date = new Date(timestamp);
        DateFormat formatter = new SimpleDateFormat("YMd_HHmmss");
        String dateFormatted = formatter.format(date);
        return dateFormatted;
    }


    //ExecutorService executor = Executors.newWorkStealingPool(1);

    public void report(int requestCounter, double throughput) {
        if (printstatus) {
            //executor.submit(() -> {
                String msg = "";
                for (String key : _measurements.keySet()) {
                    if(key.equals("SERVICE_TIME") || key.equals("ESTIMATED_LATENCY")) {
                        Histogram intervalHistogram = getIntervalHistogramAndAccumulate(key);
                        msg += getSummary(intervalHistogram, key) + " ";
                    }
                }
                System.err.println("SickStore request rate: " + throughput + " ops/sec;" + msg);
            //});
        }
    }

    public String getSummary(Histogram intervalHistogram, String key) {
        DecimalFormat d = new DecimalFormat("#.#########");
        return "[" + key + ": Count=" + intervalHistogram.getTotalCount() + ", Max="
                + intervalHistogram.getMaxValue() + ", Min=" + intervalHistogram.getMinValue() + ", Mean="
                + d.format(intervalHistogram.getMean()) + ", 90=" + d.format(intervalHistogram.getValueAtPercentile(90))
                + ", 99=" + d.format(intervalHistogram.getValueAtPercentile(99)) + ", 99.9="
                + d.format(intervalHistogram.getValueAtPercentile(99.9)) + ", 99.99="
                + d.format(intervalHistogram.getValueAtPercentile(99.99)) + "]";
    }


    private Histogram getIntervalHistogramAndAccumulate(String key) {
        Recorder histogram = _measurements.get(key);
        Histogram intervalHistogram = histogram.getIntervalHistogram();
        if (_totalHistograms.containsKey(key)) {
            _totalHistograms.get(key).add(intervalHistogram);
        } else {
            _totalHistograms.put(key, intervalHistogram);
        }
        return intervalHistogram;
    }

    private void cleanup() {
        _measurements = new ConcurrentHashMap<String, Recorder>();
        _totalHistograms = new ConcurrentHashMap<String, Histogram>();
        timestamp = System.currentTimeMillis();
    }
}
