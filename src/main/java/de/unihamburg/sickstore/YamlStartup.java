package de.unihamburg.sickstore;

import de.unihamburg.sickstore.config.InstanceFactory;
import de.unihamburg.sickstore.database.SickServer;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;

public class YamlStartup {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            throw new RuntimeException("You have to pass the filename as first argument.");
        }

        InputStream ios = new FileInputStream(new File(args[0]));

        Yaml yaml = new Yaml();
        Map<String, Object> config = (Map<String, Object>) yaml.load(ios);

        SickServer server = (SickServer) InstanceFactory.newInstanceFromConfig(config);
        server.start();
    }
}
