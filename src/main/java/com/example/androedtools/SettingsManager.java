package com.example.androedtools;

import java.io.*;
import java.util.Properties;

public class SettingsManager {
    private static final String SETTINGS_FILE = "androedtools.properties";
    private final Properties properties;

    public SettingsManager() {
        properties = new Properties();
        try (InputStream input = new FileInputStream(SETTINGS_FILE)) {
            properties.load(input);
        } catch (IOException ignored) {
            // Файл не существует - создадим при первом сохранении
        }
    }

    public void saveLastIp(String ip) {
        properties.setProperty("last.ip", ip);
        try (OutputStream output = new FileOutputStream(SETTINGS_FILE)) {
            properties.store(output, "AndroedTools Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getLastIp() {
        return properties.getProperty("last.ip", "");
    }
}