package de.unihamburg.sickstore.main;

import java.io.IOException;

import de.unihamburg.sickstore.backend.Store;
import de.unihamburg.sickstore.backend.anomaly.replicationdelay.MongoDbReplicationDelay;
import de.unihamburg.sickstore.backend.timer.SystemTimeHandler;
import de.unihamburg.sickstore.backend.timer.TimeHandler;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unihamburg.sickstore.backend.QueryHandler;
import de.unihamburg.sickstore.backend.staleness.ConstantStaleness;
import de.unihamburg.sickstore.database.SickServer;

public class Server {
    private static final Logger log = LoggerFactory.getLogger("sickstore");
    
    @SuppressWarnings("unchecked")
    private static <ReturnType extends Object> ReturnType checkOption(
            Option option, ReturnType defaultValue, CommandLine line) {
        Object value = defaultValue;
        String string = null;
        if (line.hasOption(option.getOpt())) {
            try {
                string = line.getOptionValue(option.getOpt());
                if (defaultValue instanceof Integer) {
                    value = Integer.parseInt(string);
                } else if (defaultValue instanceof Long) {
                    value = Long.parseLong(string);
                } else if (defaultValue instanceof String) {
                    value = string;
                } else if (defaultValue instanceof String[]) {
                    value = Integer.parseInt(string);
                }
                return (ReturnType) value;
            } catch (Exception e) {
                System.out.println("Unexpected exception:" + e.getMessage());
                System.out.println("\tUsing default value:\t" + defaultValue);
                return defaultValue;
            }
        }
        System.out.println("WARNING: option \"" + option.getOpt()
                + "\" was not set! Using default value:\t" + defaultValue);
        return (ReturnType) value;
    }

    // TODO wrap different staleness generators into the CLI

    /**
     * can be started with the following command
     * 
     * <pre>
     * java -jar sickstore.jar -p=55000,55001 -f=500 -o=0
     * </pre>
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // args = " -p=55000,55001 -f=500 -o=,0 -h".split(" ");
        // args = "   ".split(" ");
        parseArguments(args);
    }

    private static void parseArguments(String[] args)
            throws IndexOutOfBoundsException, ParseException, IOException {
        CommandLineParser parser = new GnuParser();
        Options options = new Options();

        // Create options
        Option portOpt = new Option(
                "p",
                "ports",
                true,
                "a comma-separated list of all ports that Sick servers listen (there will be one simulated server node for each port)");
        options.addOption(portOpt);

        Option foreignReadsOpt = new Option(
                "f",
                "foreignReadStaleness",
                true,
                "the delay in milliseconds by which other servers' writes become visible to a server");
        foreignReadsOpt.setType(0);
        options.addOption(foreignReadsOpt);
        options.addOption(portOpt);

        Option ownReadsOpt = new Option("o", "ownReadStaleness", true,
                "the delay in milliseconds by which a server can observe its own writes");
        ownReadsOpt.setType(0);
        options.addOption(ownReadsOpt);

        Option helpOpt = new Option("h", "help", false,
                "prints help and instructions");
        helpOpt.setType(0);
        options.addOption(helpOpt);

        CommandLine line = null;
        try {
            line = parser.parse(options, args);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // print unrecognised arguments
        boolean unrecognisedArguments = false;
        for (Object argument : line.getArgList()) {
            if (argument != null && !argument.equals("")) {
                unrecognisedArguments = true;
                break;
            }
        }
        if (unrecognisedArguments) {
            System.out.println("Unrecognised arguments:");
            for (Object argument : line.getArgList()) {
                System.out.println("\t" + argument.toString());
            }
            System.out.println("Proceeding...");
        }
        // print help if necessary
        boolean showHelp = line.getOptions().length == 0
                || line.hasOption(helpOpt.getOpt());
        if (showHelp) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("SickStore Server", options);
            System.out.println();
        }

        String[] ports = ((String) checkOption(portOpt, "54000", line))
                .split(",");

        Long foreignReads = checkOption(foreignReadsOpt, new Long(500), line);

        Long ownReads = checkOption(ownReadsOpt, new Long(0), line);

        startServers(ports, foreignReads, ownReads);
    }

    private static void startServers(String[] ports, long foreignReads,
            long ownReads) throws IOException {

        log.info("Starting Sick server...");

        TimeHandler timeHandler = new SystemTimeHandler();
        QueryHandler queryHandler = new QueryHandler(
                new Store(timeHandler),
                new ConstantStaleness(foreignReads, ownReads),
                new MongoDbReplicationDelay(100),
                timeHandler
        );

        int p = -1;
        for (String port : ports) {
            p = Integer.parseInt(port);
            SickServer server = new SickServer(p, queryHandler, timeHandler);
            server.start();

            log.info("... on port " + port);
        }

        // Some variables that give you a handle on the store and the server
        // nodes during debugging
        // Store store = Store.getInstance();
        // System.out.println(store);
        // QueryHandler handler = QueryHandler.getInstance();
        // System.out.println(handler);
    }
}
