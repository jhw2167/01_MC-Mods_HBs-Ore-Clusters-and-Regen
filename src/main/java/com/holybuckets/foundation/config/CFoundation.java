package com.holybuckets.foundation.config;


public class CFoundation extends ConfigBase {

    public static final String FOUNDATIONS_DATASTORE_FILE_PATH = "hbs-datastore.json";
    public final ConfigString regenerateOreClusterUpgradeItems = s(FOUNDATIONS_DATASTORE_FILE_PATH, "foundationsDatastoreFilePath", Comments.FOUNDATIONS_DATASTORE_FILE_PATH);

    public static class Comments {
        public static final String FOUNDATIONS_DATASTORE_FILE_PATH = "Filepath to json datastore that all HBs Foundation supported mods will use to store data at the save file level. The file is stored by default in the root directory of the server, and there is not much reason to change it.";
    }

    @Override
    public String getName() {
        return "HBs Foundation Library Server Config";
    }
}
