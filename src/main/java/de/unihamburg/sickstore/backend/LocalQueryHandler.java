package de.unihamburg.sickstore.backend;

import de.unihamburg.sickstore.config.InstanceFactory;
import de.unihamburg.sickstore.database.messages.ClientRequest;
import de.unihamburg.sickstore.database.messages.ServerResponse;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

/**
 * Created by Steffen Friedrich on 03.12.2015.
 */
public class LocalQueryHandler {
    static QueryHandler queryHandler = null;

    /**
     * Singleton
     */
    public synchronized static QueryHandler getQueryHandler(String configFile) {
        if (queryHandler == null) {
                FileInputStream ios = null;
            try {
                ios = new FileInputStream(new File(configFile));
            } catch (Exception e) {
                System.out.println("SickStore config file not found:" + configFile);
                e.printStackTrace();
            }
            Yaml yaml = new Yaml();
            Map<String, Object> config = (Map<String, Object>) yaml.load(ios);
            queryHandler = (QueryHandler) InstanceFactory.newInstanceFromConfig(((Map<String, Object>) config.get("queryHandler")));
        }
        return queryHandler;
    }
}
