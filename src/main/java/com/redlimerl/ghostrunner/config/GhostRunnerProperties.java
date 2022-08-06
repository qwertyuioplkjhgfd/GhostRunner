package com.redlimerl.ghostrunner.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Properties;

public class GhostRunnerProperties {
    public static File configFile;

    public static boolean bootsEnabled;

    public static void init() {
        configFile = FabricLoader.getInstance().getConfigDir().resolve("ghostrunner.properties").toFile();
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                saveProperties();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        loadProperties();
    }

    private static void loadProperties() {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(configFile));
            bootsEnabled = Boolean.parseBoolean(properties.getProperty("bootsEnabled", "false"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveProperties() throws IOException {
        Writer f = new BufferedWriter(new FileWriter(configFile));
        Properties properties = new Properties();
        properties.put("bootsEnabled", "false");
        properties.store(f, "");
        f.close();
    }
}
