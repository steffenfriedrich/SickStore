package de.unihamburg.sickstore;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import de.unihamburg.sickstore.backend.Store;
import de.unihamburg.sickstore.backend.anomaly.AnomalyGenerator;
import de.unihamburg.sickstore.backend.anomaly.BasicAnomalyGenerator;
import de.unihamburg.sickstore.backend.anomaly.MongoDbAnomalies;
import de.unihamburg.sickstore.backend.timer.SystemTimeHandler;
import de.unihamburg.sickstore.backend.timer.TimeHandler;
import de.unihamburg.sickstore.database.Node;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unihamburg.sickstore.backend.QueryHandler;
import de.unihamburg.sickstore.backend.anomaly.staleness.ConstantStaleness;
import de.unihamburg.sickstore.database.SickServer;

public class ServerStartup {
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
        CommandLineParser parser = new DefaultParser();
        Options options = new Options();

        // Create options
        Option portOpt = new Option(
                "p",
                "port",
                true,
                "the port, SickStore will listen to (default 54000)");
        options.addOption(portOpt);

        Option nodesOpt = new Option(
                "n",
                "nodes",
                true,
                "the number of nodes, that will be created");
        options.addOption(nodesOpt);

        Option foreignReadsOpt = new Option(
                "f",
                "foreignReadStaleness",
                true,
                "the delay in milliseconds by which other servers' writes become visible to a server");
        options.addOption(foreignReadsOpt);
        options.addOption(portOpt);

        Option ownReadsOpt = new Option("o", "ownReadStaleness", true,
                "the delay in milliseconds by which a server can observe its own writes");
        options.addOption(ownReadsOpt);

        Option helpOpt = new Option("h", "help", false,
                "prints help and instructions");
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

        int port = Integer.parseInt(checkOption(portOpt, "54000", line));
        int nodes = Integer.parseInt(checkOption(portOpt, "1", line));
        Long foreignReads = checkOption(foreignReadsOpt, new Long(500), line);
        Long ownReads = checkOption(ownReadsOpt, new Long(0), line);

        startServers(port, nodes, foreignReads, ownReads);
    }

    private static void startServers(int port, int numberOfNodes, long foreignReads,
            long ownReads) throws IOException {

        Set<Node> nodes = new HashSet<>();
        for (int i = 1; i < numberOfNodes; i++) {
            // use the id as name
            nodes.add(new Node(i + ""));
        }

        AnomalyGenerator anomalyGenerator = new BasicAnomalyGenerator(
            new ConstantStaleness(foreignReads, ownReads),
            new MongoDbAnomalies()
        );

        TimeHandler timeHandler = new SystemTimeHandler();
        QueryHandler queryHandler = new QueryHandler(
            new Store(timeHandler),
            anomalyGenerator,
            nodes,
            timeHandler,
                0,
                false
        );

        log.info("Starting Sick server on port " + port);
        SickServer server = new SickServer(port, queryHandler);
        server.start();

        // Some variables that give you a handle on the store and the server
        // nodes during debugging
        // Store store = Store.getInstance();
        // System.out.println(store);
        // QueryHandler handler = QueryHandler.getInstance();
        // System.out.println(handler);
    }
}
