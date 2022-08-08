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

    public static boolean bootsEnabled = false;
    public static boolean smolGhosts = true;

    public static void init() {
        configFile = FabricLoader.getInstance().getConfigDir().resolve("ghostrunner.properties").toFile();
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                saveDefaultProperties();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            loadProperties();
            saveProperties();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadProperties() throws IOException {
        Properties properties = new Properties();
        properties.load(new FileInputStream(configFile));
        bootsEnabled = Boolean.parseBoolean(properties.getProperty("bootsEnabled", "false"));
        smolGhosts = Boolean.parseBoolean(properties.getProperty("smolGhosts", "true"));
    }

    private static void saveDefaultProperties() throws IOException {
        Writer f = new BufferedWriter(new FileWriter(configFile));
        Properties properties = new Properties();
        properties.put("bootsEnabled", "false");
        properties.put("smolGhosts", "true");
        properties.store(f, "");
        f.close();
    }

    private static void saveProperties() throws IOException {
        Writer f = new BufferedWriter(new FileWriter(configFile));
        Properties properties = new Properties();
        properties.put("bootsEnabled", Boolean.toString(bootsEnabled));
        properties.put("smolGhosts", Boolean.toString(smolGhosts));
        properties.store(f, "");
        f.close();
    }
}
