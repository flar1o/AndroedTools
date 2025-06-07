package com.example.androedtools;

public class DeviceInfo {
    private final String model;
    private final String manufacturer;
    private final String androidVersion;
    private final String serial;
    private MainController controller;

    public DeviceInfo(String model, String manufacturer, String androidVersion, String serial) {
        this.model = model;
        this.manufacturer = manufacturer;
        this.androidVersion = androidVersion;
        this.serial = serial;
    }

    public String getModel() { return model; }
    public String getManufacturer() { return manufacturer; }
    public String getAndroidVersion() { return androidVersion; }
    public String getSerial() { return serial; }
}
