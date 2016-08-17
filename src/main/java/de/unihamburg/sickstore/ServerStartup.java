package de.unihamburg.sickstore;

import de.unihamburg.sickstore.config.InstanceFactory;
import de.unihamburg.sickstore.database.SickStoreServer;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;

public class ServerStartup {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            throw new RuntimeException("You have to pass the filename as first argument.");
        }

        InputStream ios = new FileInputStream(new File(args[0]));

        Yaml yaml = new Yaml();
        Map<String, Object> config = (Map<String, Object>) yaml.load(ios);

        SickStoreServer server = (SickStoreServer) InstanceFactory.newInstanceFromConfig(config);
        server.start();
    }
}
