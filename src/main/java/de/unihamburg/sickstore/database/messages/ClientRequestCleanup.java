package de.unihamburg.sickstore.database.messages;

import de.unihamburg.sickstore.database.WriteConcern;

import java.util.Set;

/**
 * Request to export measurements and restart measurement
 */
public class ClientRequestCleanup extends ClientRequest {
    private String _exportFolder;

    @SuppressWarnings("unused")
    private ClientRequestCleanup() {
    }

    /**
     * @param exportFolder export measurements to a folder with the given name
     */
    public ClientRequestCleanup(String exportFolder) {
        super("", "");
        _exportFolder = exportFolder;
    }


    public String getExportFolder() { return _exportFolder; }

}
