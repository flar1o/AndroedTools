package com.example.androedtools;

import com.android.ddmlib.*;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class DeviceMonitor {

    private MainController controller;
    private AndroidDebugBridge bridge;
    private volatile boolean isMonitoring = true;
    private long[] prevCpuTimes = null;
    private String lastConnectedSerial = null;

    public DeviceMonitor(MainController controller) {
        this.controller = controller;
    }

    private Thread monitoringThread;

    public void start() {
        AndroidDebugBridge.init(false);

        bridge = AndroidDebugBridge.createBridge(
                "adb/platform-tools/adb.exe", false
        );

        if (bridge == null) {
            System.err.println("Не удалось запустить ADB.");
            controller.updateStatus("Ошибка: Не удалось запустить ADB");
            return;
        }


        // Новый поток для мониторинга
        monitoringThread = new Thread(() -> {
            while (isMonitoring) {
                try {
                    if (bridge.hasInitialDeviceList()) {
                        IDevice[] devices = bridge.getDevices();
                        if (devices.length > 0) {
                            IDevice device = devices[0];
                            // Проверяем, изменилось ли устройство
                            if (!device.getSerialNumber().equals(lastConnectedSerial)) {
                                lastConnectedSerial = device.getSerialNumber();
                                // Сбрасываем предыдущие значения CPU при подключении нового устройства
                                prevCpuTimes = null;
                            }
                            updateDeviceInfo(device);
                            updateSystemMetrics(device);
                        } else {
                            setDeviceDisconnected();
                            controller.updateStatus("Устройство не подключено");
                        }
                    }
                    Thread.sleep(1000); // проверять каждую секунду
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    controller.updateAdbLog("Ошибка мониторинга: " + e.getMessage());
                }
            }
        });
        monitoringThread.setDaemon(true); // Добавьте эту строку
        monitoringThread.start();
    }

    public void stop() {
        isMonitoring = false;
        if (bridge != null) {
            AndroidDebugBridge.terminate();
        }
        try {
            ProcessBuilder pb = new ProcessBuilder("taskkill", "/F", "/IM", "adb.exe");
            pb.start(); // Убивает все оставшиеся процессы adb.exe
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void setDeviceDisconnected() {
        Platform.runLater(() -> {
            prevCpuTimes = null; // Сбрасываем предыдущие значения CPU
            controller.setDeviceDisconnected();
        });
    }
    private void updateDeviceInfo(IDevice device) {
        try {
            DeviceInfo info = new DeviceInfo(
                    device.getProperty("ro.product.model"),
                    device.getProperty("ro.product.manufacturer"),
                    device.getProperty("ro.build.version.release"),
                    device.getSerialNumber()
            );
            // Определяем тип подключения
            boolean isWifiConnected = device.isOnline() && device.getSerialNumber().contains(":");


            Platform.runLater(() -> {
                controller.updateDeviceInfo(info);
                // Переинициализируем графики при новом подключении
                if (isWifiConnected) {
                    controller.setWifiButtonsState(false, true); // Отключить Connect, включить Disconnect
                } else {
                    controller.setWifiButtonsState(true, false); // Включить Connect, отключить Disconnect
                }
            });

            controller.waitForDeviceAndUpdate();
            controller.updateAdbLog("Устройство подключено: " + info.getModel());

            // Получаем информацию о батарее
            String batteryOutput = executeCommand(device, "dumpsys battery");
            if (batteryOutput.contains("level")) {
                int level = Integer.parseInt(batteryOutput.split("level:")[1].split("\n")[0].trim());
                controller.updateBatteryLevel(level / 100.0);
            }

        } catch (Exception e) {
            controller.updateAdbLog("Ошибка получения информации об устройстве: " + e.getMessage());
        }
    }

    private void updateSystemMetrics(IDevice device) {
        try {
            // RAM Info
            String meminfo = executeCommand(device, "cat /proc/meminfo");
            long totalMem = 0, freeMem = 0;
            // Если графики не инициализированы - выходим
            if (controller.cpuChart == null || controller.ramChart == null) {
                return;
            }
            for (String line : meminfo.split("\n")) {
                if (line.startsWith("MemTotal:")) {
                    totalMem = Long.parseLong(line.replaceAll("\\D+", ""));
                } else if (line.startsWith("MemAvailable:")) {
                    freeMem = Long.parseLong(line.replaceAll("\\D+", ""));
                }
            }
            long usedMem = totalMem - freeMem;
            controller.updateRamInfo(totalMem / 1024, usedMem / 1024);

            // CPU Usage
            String cpuStat = executeCommand(device, "cat /proc/stat");
            String[] cpuLines = cpuStat.split("\n");

            if (cpuLines.length > 0 && cpuLines[0].startsWith("cpu ")) {
                String[] cpuValues = cpuLines[0].trim().split("\\s+");
                long[] currentCpuTimes = new long[10];
                for (int i = 1; i <= 10; i++) {
                    currentCpuTimes[i-1] = Long.parseLong(cpuValues[i]);
                }
                if (prevCpuTimes != null) {
                    long totalDiff = 0;
                    for (int i = 0; i < 10; i++) {
                        totalDiff += (currentCpuTimes[i] - prevCpuTimes[i]);
                    }
                    long idleDiff = currentCpuTimes[3] - prevCpuTimes[3];
                    if (totalDiff > 0) {
                        double usagePercent = 100.0 * (totalDiff - idleDiff) / totalDiff;

                        controller.updateCpuInfo(usagePercent);
                    }
                }
                prevCpuTimes = currentCpuTimes;
            }



                    // Storage Info
            String storageOutput = executeCommand(device, "df /data | tail -n +2");
            if (!storageOutput.isEmpty()) {
                String[] parts = storageOutput.split("\\s+");
                long total = Long.parseLong(parts[1]) * 1024; // в байтах
                long used = Long.parseLong(parts[2]) * 1024;
                controller.updateStorageInfo(total, used);
            }

        } catch (Exception e) {
            controller.updateAdbLog("Ошибка получения метрик: " + e.getMessage());
        }
    }

    private int getCpuCores(IDevice device) throws Exception {
        String cpuInfo = executeCommand(device, "cat /proc/cpuinfo");
        int cores = 0;
        for (String line : cpuInfo.split("\n")) {
            if (line.trim().startsWith("processor")) {
                cores++;
            }
        }
        return cores > 0 ? cores : 1; // минимум 1 ядро
    }

    private String executeCommand(IDevice device, String command)
            throws TimeoutException, AdbCommandRejectedException,
            ShellCommandUnresponsiveException, IOException {
        CollectingOutputReceiver receiver = new CollectingOutputReceiver();
        device.executeShellCommand(command, receiver);
        return receiver.getOutput().trim();
    }

}