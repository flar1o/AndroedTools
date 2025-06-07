package com.example.androedtools;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.util.Pair;

import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainController {
    public final String adbPath = "adb/platform-tools/adb.exe"; // Путь к adb
    public final String fastbootPath = "adb/platform-tools/fastboot.exe"; // Путь к fastboot

    // Device info
    @FXML private ImageView deviceIcon;
    @FXML private Label deviceModel;
    @FXML private Label androidVersion;
    @FXML private Label serialNumber;
    @FXML private Button batteryButton;
    @FXML private Label connectionStatus;
    @FXML private VBox overviewContent;
    @FXML private VBox appsContent;
    @FXML private VBox toolsContent;
    @FXML private VBox consoleContent;
    @FXML private VBox cpuExtraBox;
    private boolean isFastbootMode = false;
    @FXML private Button adbModeBtn;
    @FXML private Button fastbootModeBtn;
    @FXML
    private GridPane toolsGrid;
    @FXML
    LineChart<Number, Number> ramChart;
    // Серии данных для графиков
    private final XYChart.Series<Number, Number> cpuUsageSeries = new XYChart.Series<>();
    private final ObservableList<CpuDataPoint> cpuData = FXCollections.observableArrayList();
    // Максимальное количество точек на графике
    private static final int MAX_DATA_POINTS = 60;
    @FXML
    LineChart<Number, Number> cpuChart;
    @FXML private NumberAxis ramXAxis;
    @FXML private TextArea deviceInfoText;
    @FXML private TableView<AppInfo> appsTable;
    @FXML private TableColumn<AppInfo, String> pkgColumn;
    @FXML private Button refreshAppsButton;
    @FXML private Button uninstallButton;
    @FXML private Button installApkButton;

    private ObservableList<AppInfo> appList = FXCollections.observableArrayList();
    private FilteredList<AppInfo> filteredList;

    // Для отображения средней нагрузки
    @FXML
    private ProgressBar cpuProgressBar;
    @FXML private NumberAxis cpuXAxis;
    private volatile boolean isDeviceConnected = false;
    private TextArea consoleOutput;
    private TextField consoleInput;
    private final List<String> logHistory = new ArrayList<>();

    @FXML
    private void showLogDialog() {
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.setStyle("-fx-font-family: monospace;");
        logArea.setText(String.join("\n", logHistory));

        logArea.setPrefSize(800, 500);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Журнал событий");
        dialog.getDialogPane().setContent(logArea);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void setupCpuChart() {
        if (cpuChart == null) return;
        // Настройка внешнего вида
        cpuChart.setAnimated(false);
        cpuChart.setCreateSymbols(false);
        cpuChart.setLegendVisible(false);

        // Настройка осей
        NumberAxis xAxis = (NumberAxis) cpuChart.getXAxis();
        cpuXAxis = (NumberAxis) cpuChart.getXAxis();
        cpuXAxis.setAutoRanging(false);
        cpuXAxis.setLowerBound(0);
        cpuXAxis.setUpperBound(60);
        xAxis.setTickUnit(10);

        NumberAxis yAxis = (NumberAxis) cpuChart.getYAxis();
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(100);
        yAxis.setTickUnit(10);

        // Добавляем серию данных
        cpuUsageSeries.setName("Загрузка CPU");
        cpuChart.getData().clear(); // на всякий случай
        cpuChart.getData().add(cpuUsageSeries);
    }

    private void setupRamChart() {
        if (ramChart == null) return;
        ramChart.setAnimated(false);
        ramChart.setCreateSymbols(false);
        ramUsageSeries.setName("Использование RAM");
        ramChart.getData().clear(); // на всякий случай
        ramChart.getData().add(ramUsageSeries);

        ramXAxis = (NumberAxis) ramChart.getXAxis();
        ramXAxis.setAutoRanging(false);
        ramXAxis.setLowerBound(0);
        ramXAxis.setUpperBound(60);
        ramXAxis.setTickUnit(10);

        NumberAxis yAxis = (NumberAxis) ramChart.getYAxis();
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);

        ramChart.setVerticalGridLinesVisible(false);
        ramChart.setHorizontalGridLinesVisible(false);
    }

    // Метод для обновления данных CPU
    public void updateCpuInfo(double usagePercent) {
        if (!isDeviceConnected) return;
        Platform.runLater(() -> {
            // Обновляем прогресс-бар
            cpuProgressBar.setProgress(usagePercent / 100.0);

            // Добавляем новую точку данных
            long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
            cpuData.add(new CpuDataPoint(elapsedTime, usagePercent));

            // Ограничиваем количество точек
            if (cpuData.size() > MAX_DATA_POINTS) {
                cpuData.remove(0);
            }

            cpuXAxis.setLowerBound(elapsedTime - MAX_DATA_POINTS);
            cpuXAxis.setUpperBound(elapsedTime);

            // Обновляем график
            cpuUsageSeries.getData().clear();
            for (CpuDataPoint point : cpuData) {
                cpuUsageSeries.getData().add(new XYChart.Data<>(point.getTimestamp(), point.getUsage()));
            }
        });
    }

    // Metrics
    @FXML private ProgressBar storageBar;
    @FXML private Label storageLabel;
    @FXML private Button wifiConnectBtn;
    @FXML private Button wifiDisconnectBtn;
    private String deviceIpAddress = null;
    private final SettingsManager settingsManager = new SettingsManager();
    private final ObservableList<RamDataPoint> ramData = FXCollections.observableArrayList();
    private final XYChart.Series<Number, Number> ramUsageSeries = new XYChart.Series<>();
    private long startTime = System.currentTimeMillis();
    private String lastStatus = "";
    private String lastAdbLog = "";
    Alert alert = new Alert(Alert.AlertType.INFORMATION);


    // Status bar
    @FXML private ProgressBar operationProgress;
    @FXML private Label statusMessage;
    @FXML private Label adbLog;
    @FXML private Label appVersion;
    @FXML private NumberAxis cpuYAxis;
    @FXML
    private void connectViaWifi() {
        if (!isDeviceConnected()) {
            showConnectionChoiceDialog();
        } else {
            new Thread(() -> {
                try {
                    Platform.runLater(() -> {
                        wifiConnectBtn.setDisable(true);
                        statusMessage.setText("Получение IP-адреса устройства...");
                    });

                    // 1. Получаем IP-адрес через Wi-Fi интерфейс
                    String ipCommand = adbPath + " shell ip addr show wlan0";
                    Process process = Runtime.getRuntime().exec(ipCommand);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Ищем строку с IPv4 адресом
                        if (line.trim().startsWith("inet ")) {
                            // Пример строки: "inet 192.168.1.63/24 brd 192.168.1.255 scope global wlan0"
                            String[] parts = line.trim().split("\\s+");
                            if (parts.length >= 2) {
                                // Берем часть до "/" (192.168.1.63/24 -> 192.168.1.63)
                                deviceIpAddress = parts[1].split("/")[0];
                                break;
                            }
                        }
                    }

                    if (deviceIpAddress == null) {
                        Platform.runLater(() -> {
                            statusMessage.setText("Не удалось получить IP-адрес");
                            wifiConnectBtn.setDisable(false);
                        });
                        return;
                    }

                    // 2. Переключаем устройство в TCP-режим
                    String tcpipCommand = adbPath + " tcpip 5555";
                    Runtime.getRuntime().exec(tcpipCommand);
                    Thread.sleep(2000); // Даем время на выполнение

                    Platform.runLater(() -> {
                        statusMessage.setText("Можно отключить USB. Подключаемся по Wi-Fi...");

                        alert.setTitle("Wi-Fi подключение");
                        alert.setHeaderText("Теперь можно отключить USB-кабель");
                        alert.setContentText("Подключаемся к " + deviceIpAddress);
                        alert.showAndWait();
                    });

                    // 3. Подключаемся по Wi-Fi
                    String connectCommand = adbPath + " connect " + deviceIpAddress + ":5555";
                    Process connectProcess = Runtime.getRuntime().exec(connectCommand);
                    int exitCode = connectProcess.waitFor();

                    Platform.runLater(() -> {
                        if (exitCode == 0) {
                            statusMessage.setText("Успешно подключено по Wi-Fi");
                            wifiConnectBtn.setDisable(true);
                            wifiDisconnectBtn.setDisable(false);
                        } else {
                            statusMessage.setText("Ошибка подключения по Wi-Fi");
                            wifiConnectBtn.setDisable(false);
                        }
                    });

                } catch (Exception e) {
                    Platform.runLater(() -> {
                        statusMessage.setText("Ошибка: " + e.getMessage());
                        wifiConnectBtn.setDisable(false);
                    });
                    e.printStackTrace();
                }
                settingsManager.saveLastIp(deviceIpAddress);
            }).start();
        }
    }
    private void showConnectionChoiceDialog() {
        Alert choiceDialog = new Alert(Alert.AlertType.CONFIRMATION);
        choiceDialog.setTitle("Способ подключения");
        choiceDialog.setHeaderText("Устройство не подключено по USB");
        choiceDialog.setContentText("Выберите способ подключения:");

        ButtonType usbButton = new ButtonType("Подключить USB", ButtonBar.ButtonData.YES);
        ButtonType manualButton = new ButtonType("Ручной ввод", ButtonBar.ButtonData.NO);
        ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);

        choiceDialog.getButtonTypes().setAll(usbButton, manualButton, cancelButton);

        Optional<ButtonType> result = choiceDialog.showAndWait();
        result.ifPresent(buttonType -> {
            if (buttonType == usbButton) {
                alert.setTitle("Инструкция");
                alert.setHeaderText("Подключите устройство");
                alert.setContentText("Пожалуйста, подключите устройство по USB и нажмите кнопку снова");
                alert.showAndWait();
            } else if (buttonType == manualButton) {
                showManualConnectionDialog();
            }
        });
    }
    private void showManualConnectionDialog() {
        // Создаем кастомный диалог с полями IP и порта
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Ручное подключение");
        dialog.setHeaderText("Введите параметры подключения");

        // Устанавливаем кнопки
        ButtonType connectButtonType = new ButtonType("Подключить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(connectButtonType, ButtonType.CANCEL);

        // Создаем поля ввода
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField ipField = new TextField();
        ipField.setPromptText("IP адрес");
        ipField.setText(settingsManager.getLastIp()); // Автозаполнение последнего IP

        TextField portField = new TextField();
        portField.setPromptText("Порт");
        portField.setText("5555"); // Порт по умолчанию

        grid.add(new Label("IP адрес:"), 0, 0);
        grid.add(ipField, 1, 0);
        grid.add(new Label("Порт:"), 0, 1);
        grid.add(portField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Включаем кнопку подключения только при заполненном IP
        Node connectButton = dialog.getDialogPane().lookupButton(connectButtonType);

        ipField.textProperty().addListener((observable, oldValue, newValue) -> {
            connectButton.setDisable(newValue.trim().isEmpty());
        });

        // Преобразуем результат в пару IP:PORT
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == connectButtonType) {
                return new Pair<>(ipField.getText().trim(), portField.getText().trim());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(ipPort -> {
            String ip = ipPort.getKey();
            String port = ipPort.getValue();

            if (ip.isEmpty() || port.isEmpty()) {
                alert.setTitle("Ошибка");
                alert.setHeaderText("Неверные данные");
                alert.setContentText("Пожалуйста, укажите IP и порт");
                alert.showAndWait();
                return;
            }

            // Сохраняем IP для будущих подключений
            settingsManager.saveLastIp(ip);

            // Выполняем подключение
            manualConnectToDevice(ip, port);
        });
    }
    private void manualConnectToDevice(String ip, String port) {
        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    statusMessage.setText("Подключение к " + ip + ":" + port + "...");
                    wifiConnectBtn.setDisable(true);
                });

                // Команда подключения
                String connectCommand = adbPath + " connect " + ip + ":" + port;
                Runtime.getRuntime().exec(adbPath + " start-server");
                Process process = Runtime.getRuntime().exec(connectCommand);

                // Читаем вывод команды
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                int exitCode = process.waitFor();

                Platform.runLater(() -> {
                    if (exitCode == 0 && output.toString().contains("connected")) {
                        statusMessage.setText("Успешно подключено к " + ip + ":" + port);
                        wifiConnectBtn.setDisable(true);
                        wifiDisconnectBtn.setDisable(false);
                        deviceIpAddress = ip;
                    } else {
                        statusMessage.setText("Ошибка подключения: " + output);
                        wifiConnectBtn.setDisable(false);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    statusMessage.setText("Ошибка: " + e.getMessage());
                    wifiConnectBtn.setDisable(false);
                });
            }
        }).start();
    }
    public void setWifiButtonsState(boolean enableConnect, boolean enableDisconnect) {
        Platform.runLater(() -> {
            wifiConnectBtn.setDisable(!enableConnect);
            wifiDisconnectBtn.setDisable(!enableDisconnect);
        });
    }

    private boolean isDeviceConnected() {
        try {
            Process process = Runtime.getRuntime().exec(adbPath + " devices");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.endsWith("device")) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    @FXML
    private void disconnectWifi() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Отключение устройства");
        confirmAlert.setHeaderText("Вы уверены, что хотите отключиться?");
        confirmAlert.setContentText("Устройство: " + settingsManager.getLastIp());

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    Platform.runLater(() -> {
                        wifiDisconnectBtn.setDisable(true);
                        statusMessage.setText("Остановка ADB сервера...");
                    });

                    // Выполняем adb kill-server
                    String killCommand = adbPath + " kill-server";
                    Process process = Runtime.getRuntime().exec(killCommand);
                    int exitCode = process.waitFor();

                    Platform.runLater(() -> {
                        if (exitCode == 0) {
                            statusMessage.setText("ADB сервер остановлен");
                            // Обновляем состояние кнопок
                            wifiConnectBtn.setDisable(false);
                            wifiDisconnectBtn.setDisable(true);
                            // Обновляем статус устройства
                            setDeviceDisconnected();
                        } else {
                            statusMessage.setText("Ошибка остановки ADB сервера");
                            wifiDisconnectBtn.setDisable(false);
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        statusMessage.setText("Ошибка: " + e.getMessage());
                        wifiDisconnectBtn.setDisable(false);
                    });
                    e.printStackTrace();
                }
            }).start();
            clearDeviceInfoUI();
        }
    }
    private void clearDeviceInfoUI() {
        if (deviceInfoText != null) deviceInfoText.clear();
        if (storageLabel != null) storageLabel.setText("0 / 0 GB");
        if (storageBar != null) storageBar.setProgress(0);
        if (batteryButton != null) {
            batteryButton.setText("Заряд: 0%");
            batteryButton.getStyleClass().removeAll("battery-low", "battery-medium", "battery-high");
        }
        if (cpuChart != null) cpuChart.getData().clear();
        if (ramChart != null) ramChart.getData().clear();
    }

    public void updateDeviceInfo(DeviceInfo info) {
        Platform.runLater(() -> {
            if (!isDeviceConnected) {
                // Первое подключение или повторное после отключения
                isDeviceConnected = true;
                startTime = System.currentTimeMillis(); // Сброс времени
                reinitializeCharts();

                // Полная переинициализация графиков
                cpuData.clear();
                ramData.clear();
                setupCpuChart();
                setupRamChart();
            }

            // Обновление информации об устройстве
            Image image = new Image(getClass().getResourceAsStream("/icons/phone.png"));
            deviceIcon.setImage(image);
            deviceModel.setText(info.getManufacturer() + " " + info.getModel());
            androidVersion.setText("Android " + info.getAndroidVersion());
            serialNumber.setText(info.getSerial());
            connectionStatus.setText("Подключено");
            connectionStatus.getStyleClass().add("connected");
        });
    }
    public void reinitializeCharts() {
        // Очищаем данные
        cpuData.clear();
        ramData.clear();

        // Переинициализируем графики
        if (cpuChart != null) {
            cpuChart.getData().clear();
            setupCpuChart();
        }
        if (ramChart != null) {
            ramChart.getData().clear();
            setupRamChart();
        }
        // Сбрасываем время начала мониторинга
        startTime = System.currentTimeMillis();
    }
    private String parseBatteryInfo(String rawInfo) {
        StringBuilder result = new StringBuilder("Информация о батарее:\n\n");
        String[] lines = rawInfo.split("\n");

        Map<String, String> parsed = new LinkedHashMap<>();

        boolean ac = false, usb = false, wireless = false, dock = false;
        boolean charging = false;
        String chargeCounter = null;

        for (String line : lines) {
            line = line.trim();
            if (!line.contains(":")) continue;

            String[] parts = line.split(":", 2);
            String key = parts[0].trim();
            String value = parts[1].trim();
            // Добавляем читаемые источники питания
            if (ac) parsed.put("Источник питания", "Устройство заряжается от сети");
            if (usb) parsed.put("Источник питания", "Устройство заряжается по USB");
            if (wireless) parsed.put("Источник питания", "Беспроводная зарядка");
            if (dock) parsed.put("Источник питания", "Устройство заряжается от док-станции");
            if (!ac && !usb && !wireless && !dock)
                parsed.put("Источник питания", "Устройство не заряжается");
            switch (key) {
                case "AC powered" -> ac = value.equals("true");
                case "USB powered" -> usb = value.equals("true");
                case "Wireless powered" -> wireless = value.equals("true");
                case "Dock powered" -> dock = value.equals("true");
                case "status" -> {
                    parsed.put("Статус", parseStatus(value));
                    charging = value.equals("2") || value.equals("5"); // 2: Charging, 5: Full
                }
                case "health" -> parsed.put("Состояние", parseHealth(value));
                case "level" -> parsed.put("Уровень заряда (%)", value);
                case "voltage" -> {
                    try {
                        double volts = Integer.parseInt(value) / 1000.0;
                        parsed.put("Напряжение", String.format("%.3f В", volts));
                    } catch (NumberFormatException e) {
                        parsed.put("Напряжение", value + " мВ");
                    }
                }
                case "temperature" -> {
                    String temp = value;
                    try {
                        temp += " (" + String.format("%.1f°C", Integer.parseInt(value) / 10.0) + ")";
                    } catch (NumberFormatException ignored) {}
                    parsed.put("Температура", temp);
                }
                case "technology" -> parsed.put("Технология", value);
                case "Full capacity" -> {
                    try {
                        parsed.put("Полная емкость", String.format("%d мАч", Integer.parseInt(value) / 1000));
                    } catch (NumberFormatException e) {
                        parsed.put("Полная емкость", value + " мкАч");
                    }
                }
                case "Full design capacity" -> {
                    try {
                        parsed.put("Заводская емкость", String.format("%d мАч", Integer.parseInt(value) / 1000));
                    } catch (NumberFormatException e) {
                        parsed.put("Заводская емкость", value + " мкАч");
                    }
                }
                case "cycle count" -> parsed.put("Циклы зарядки", value);
                case "state of health property" -> parsed.put("Состояние аккумулятора (%)", value);


                case "Max charging current" -> {
                    try {
                        double amps = Integer.parseInt(value) / 1_000_000.0;
                        parsed.put("Максимальный ток зарядки", String.format("%.3f А", amps));
                    } catch (NumberFormatException e) {
                        parsed.put("Макс. ток зарядки", value + " мкА");
                    }
                }
                case "Max charging voltage" -> {
                    try {
                        double volts = Integer.parseInt(value) / 1_000_000.0;
                        parsed.put("Максимальное напряжение зарядки", String.format("%.1f В", volts));
                    } catch (NumberFormatException e) {
                        parsed.put("Макс. напряжение зарядки", value + " мкВ");
                    }
                }
                case "Charge counter" -> {
                    int mAh = Integer.parseInt(value) / 1000;
                    parsed.put("Текущий заряд батареи", mAh + " мАч");
                }
                case "charge watt" -> {
                    chargeCounter = value;
                    if (charging && chargeCounter != null) {
                        parsed.put("Текущая мощность зарядки (Вт)", chargeCounter);
                    }

                }
                case "charge watt design" -> parsed.put("Заявленная мощность зарядки (Вт)", value);
            }
        }


        for (Map.Entry<String, String> entry : parsed.entrySet()) {
            result.append(String.format("%-30s: %s%n", entry.getKey(), entry.getValue()));
        }

        return result.toString();
    }

    private String parseStatus(String code) {
        return switch (code.trim()) {
            case "1" -> "Неизвестно";
            case "2" -> "Зарядка";
            case "3" -> "Разрядка";
            case "4" -> "Не заряжается";
            case "5" -> "Полностью заряжен";
            default -> "Неизвестно (" + code + ")";
        };
    }
    private String parseHealth(String code) {
        return switch (code.trim()) {
            case "1" -> "Неизвестно";
            case "2" -> "Хорошее";
            case "3" -> "Перегрев";
            case "4" -> "Мертвый аккумулятор";
            case "5" -> "Перенапряжение";
            case "6" -> "Неисправность";
            case "7" -> "Переохлаждение";
            default -> "Неизвестно (" + code + ")";
        };
    }
    @FXML
    private void onBatteryClicked() {
        if (!isDeviceConnected()) return;

        new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec(adbPath + " shell dumpsys battery");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder batteryInfo = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    batteryInfo.append(line).append("\n");
                }

                String parsed = parseBatteryInfo(batteryInfo.toString());

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Информация о батарее");
                    alert.setHeaderText("Подробности батареи");
                    alert.setContentText(parsed);
                    alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                    alert.showAndWait();
                });
            } catch (Exception e) {
                updateStatus("Ошибка получения информации о батарее: " + e.getMessage());
            }
        }).start();
    }
    public void updateBatteryLevel(double level) {
        int percent = (int) (level * 100);

        Platform.runLater(() -> {
            batteryButton.setText("Заряд: " + percent + "%");
            batteryButton.getStyleClass().removeAll("battery-low", "battery-medium", "battery-high");

            if (level < 0.2) {
                batteryButton.getStyleClass().add("battery-low");
            } else if (level < 0.5) {
                batteryButton.getStyleClass().add("battery-medium");
            } else {
                batteryButton.getStyleClass().add("battery-high");
            }
        });
    }
    @FXML


    public void updateStorageInfo(long total, long used) {
        Platform.runLater(() -> {
            double percent = (double) used / total;
            storageBar.setProgress(percent);
            storageLabel.setText(String.format("%.1f / %.1f GB",
                    used / (1024.0 * 1024 * 1024),
                    total / (1024.0 * 1024 * 1024)));
        });
    }
    private String getDeviceOverview() {
        StringBuilder result = new StringBuilder();

        result.append("=== ОСНОВНАЯ ИНФОРМАЦИЯ ===\n");
        result.append(getGeneralDeviceInfo());  // получаем с getprop

        result.append("\n=== ЭКРАН ===\n");
        result.append(getDisplayInfo());       // парсим dumpsys display

        return result.toString();
    }
    private String getGeneralDeviceInfo() {
        StringBuilder info = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec(adbPath + " shell getprop");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            Map<String, String> props = new HashMap<>();
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("[") && line.contains("]:")) {
                    String[] parts = line.split("]: \\[");
                    if (parts.length == 2) {
                        String key = parts[0].substring(1);
                        String value = parts[1].replace("]", "");
                        props.put(key, value);
                    }
                }
            }

            // Выводим нужные свойства
            info.append(String.format("%-22s: %s\n", "Модель", props.getOrDefault("ro.product.model", "N/A")));
            info.append(String.format("%-22s: %s\n", "Бренд", props.getOrDefault("ro.product.brand", "N/A")));
            info.append(String.format("%-22s: %s\n", "Устройство", props.getOrDefault("ro.product.device", "N/A")));
            info.append(String.format("%-22s: %s\n", "Версия Android", props.getOrDefault("ro.build.version.release", "N/A")));
            info.append(String.format("%-22s: %s\n", "Сборка", props.getOrDefault("ro.build.display.id", "N/A")));
            info.append(String.format("%-22s: %s\n", "Архитектура", props.getOrDefault("ro.product.cpu.abi", "N/A")));

        } catch (IOException e) {
            info.append("Ошибка получения информации: ").append(e.getMessage());
        }

        return info.toString();
    }
    private String getDisplayInfo() {
        StringBuilder displayInfo = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec(adbPath + " shell dumpsys display");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            boolean inDisplaySection = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.contains("mBaseDisplayInfo")) {
                    inDisplaySection = true;
                }

                if (inDisplaySection) {
                    if (line.contains("density")) {
                        Matcher m = Pattern.compile("density=(\\d+)").matcher(line);
                        if (m.find()) {
                            displayInfo.append(String.format("%-22s: %s dpi\n", "Плотность", m.group(1)));
                        }
                    }

                    if (line.contains("mode") && line.contains("fps")) {
                        Matcher m = Pattern.compile("mode.*fps=(\\d+(\\.\\d+)?)").matcher(line);
                        if (m.find()) {
                            displayInfo.append(String.format("%-22s: %s Гц\n", "Частота обновления", m.group(1)));
                        }
                    }

                    Matcher resolution = Pattern.compile("(\\d+) x (\\d+)").matcher(line);
                    if (resolution.find()) {
                        displayInfo.append(String.format("%-22s: %sx%s\n", "Разрешение", resolution.group(1), resolution.group(2)));
                        break; // нашли всё нужное — выходим
                    }
                }
            }

        } catch (IOException e) {
            displayInfo.append("Ошибка получения информации: ").append(e.getMessage());
        }

        return displayInfo.toString();
    }


    public void setDeviceDisconnected() {
        Platform.runLater(() -> {
            isDeviceConnected = false;
            // Очистка UI
            deviceIcon.setImage(null);
            deviceModel.setText("Не подключено");
            androidVersion.setText("");
            serialNumber.setText("");
            connectionStatus.setText("Отключено");
            connectionStatus.getStyleClass().remove("connected");

            // Очистка данных графиков
            cpuData.clear();
            ramData.clear();
            cpuUsageSeries.getData().clear();
            ramUsageSeries.getData().clear();

            // Сброс других UI элементов
            clearDeviceInfoUI();
        });
    }


    public void updateRamInfo(long totalMem, long usedMem) {
        if (!isDeviceConnected) return;
        Platform.runLater(() -> {
            // Добавляем новую точку данных
            long currentTime = (System.currentTimeMillis() - startTime) / 1000;
            ramXAxis.setLowerBound(currentTime - MAX_DATA_POINTS);
            ramXAxis.setUpperBound(currentTime);
            ramData.add(new RamDataPoint(currentTime, usedMem, totalMem));

            // Ограничиваем количество точек (например, последние 60 секунд)
            if (ramData.size() > 60) {
                ramData.remove(0);
            }

            // Обновляем график
            ramUsageSeries.getData().clear();
            for (RamDataPoint point : ramData) {
                ramUsageSeries.getData().add(new XYChart.Data<>(point.getTimestamp(), point.getUsed()));
            }

            // Автоматическое масштабирование оси Y
            NumberAxis yAxis = (NumberAxis) ramChart.getYAxis();
            yAxis.setUpperBound(totalMem * 1.1); // +10% от максимального значения
        });
    }

    public void updateStatus(String message) {
        if (message.equals(lastStatus)) return; // статус не изменился
        lastStatus = message;

        Platform.runLater(() -> statusMessage.setText(message));
        logHistory.add("[STATUS] " + message);
    }

    public void updateAdbLog(String log) {
        if (log.equals(lastAdbLog)) return; // ничего нового — игнор
        lastAdbLog = log;

        String message = "ADB: " + log;

        Platform.runLater(() -> adbLog.setText(message));
        logHistory.add("[ADB] " + log);
    }

    public void waitForDeviceAndUpdate() {
        new Thread(() -> {
            while (!isDeviceConnected()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
            }
            String info = getDeviceOverview();
            Platform.runLater(() -> deviceInfoText.setText(info));
        }).start();
    }
    private void setupConsoleUI() {
        consoleOutput = new TextArea();
        consoleOutput.setEditable(false);
        consoleOutput.setWrapText(true);
        consoleOutput.setPrefHeight(400);
        consoleOutput.setStyle("-fx-font-family: monospace;");

        consoleInput = new TextField();
        consoleInput.setPromptText("Введите ADB-команду, например: shell pm list packages");

        Button runButton = new Button("▶ Выполнить");
        runButton.setOnAction(e -> runAdbCommand());
        HBox inputBox = new HBox(10, consoleInput, runButton);
        HBox.setHgrow(consoleInput, Priority.ALWAYS);

        VBox consoleBox = new VBox(10, consoleOutput, inputBox);
        consoleBox.setPadding(new Insets(10));

        consoleContent.getChildren().add(consoleBox);
    }

    private void initConsoleModeButtons() {
        adbModeBtn.setOnAction(e -> switchToAdbMode());
        fastbootModeBtn.setOnAction(e -> switchToFastbootMode());
        switchToAdbMode(); // Режим по умолчанию
    }

    private void switchToAdbMode() {
        isFastbootMode = false;
        updateConsoleModeButtonStyles(adbModeBtn, fastbootModeBtn);
        consoleInput.setPromptText("Введите ADB-команду, например: shell pm list packages");
    }

    private void switchToFastbootMode() {
        isFastbootMode = true;
        updateConsoleModeButtonStyles(fastbootModeBtn, adbModeBtn);
        consoleInput.setPromptText("Введите Fastboot-команду, например: devices");
    }

    private void updateConsoleModeButtonStyles(Button activeBtn, Button inactiveBtn) {
        activeBtn.getStyleClass().add("header-button-active");
        inactiveBtn.getStyleClass().remove("header-button-active");
    }
    private void runAdbCommand() {
        String userCommand = consoleInput.getText().trim();
        if (userCommand.isEmpty()) return;

        consoleOutput.appendText("> " + userCommand + "\n");
        logHistory.add("> " + userCommand);

        new Thread(() -> {
            try {
                // Выбираем путь в зависимости от режима
                String executablePath = isFastbootMode ? fastbootPath : adbPath;

                // Разбиваем команду на части
                List<String> command = new ArrayList<>();
                command.add(executablePath);
                command.addAll(Arrays.asList(userCommand.split("\\s+")));

                ProcessBuilder builder = new ProcessBuilder(command);
                builder.redirectErrorStream(true);
                Process process = builder.start();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), Charset.forName("windows-1251"))
                );

                String line;
                while ((line = reader.readLine()) != null) {
                    String finalLine = line;
                    Platform.runLater(() -> {
                        consoleOutput.appendText(finalLine + "\n");
                        logHistory.add(finalLine);
                    });
                }

                int exitCode = process.waitFor();
                Platform.runLater(() ->
                        consoleOutput.appendText("[Завершено: " + exitCode + "]\n")
                );
            } catch (Exception e) {
                Platform.runLater(() ->
                        consoleOutput.appendText("Ошибка: " + e.getMessage() + "\n")
                );
            }
        }).start();

        consoleInput.clear();
    }
    // Инициализация (вызывается после загрузки FXML)
    @FXML
    public void initialize() {
        cpuYAxis.setLabel("%");
        initializeTools();
        setupConsoleUI();
        initConsoleModeButtons();
        loadInstalledApps();
        reinitializeCharts();
        setupCpuChart();
        setupRamChart();
        waitForDeviceAndUpdate();
        showOverview();
        cpuChart.setOnMouseClicked(event -> {
            boolean nowVisible = cpuExtraBox.isVisible();
            cpuExtraBox.setVisible(!nowVisible);
            cpuExtraBox.setManaged(!nowVisible);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), cpuExtraBox);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        });
    }

    // Обработчики навигации
    @FXML
    private void showOverview() {
        setActiveContent(overviewContent);
    }

    @FXML
    private void showApps() {
        setActiveContent(appsContent);
    }

    @FXML
    private void loadInstalledApps() {
        // Контейнер верхней панели: заголовок + кнопка "Обновить"
        HBox headerBox = new HBox();
        Label title = new Label("Управление приложениями");
        title.getStyleClass().add("section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshButton = new Button("Обновить");
        refreshButton.setStyle("-fx-background-color: #3f8ee6; -fx-text-fill: white; -fx-font-size: 12px;");

        headerBox.getChildren().addAll(title, spacer, refreshButton);
        headerBox.setPadding(new Insets(0, 10, 0, 10));

        TableView<AppInfo> tableView = new TableView<>();
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<AppInfo, String> pkgCol = new TableColumn<>("Пакет");
        pkgCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getPackageName()));

        tableView.getColumns().add(pkgCol);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        ObservableList<AppInfo> appList = FXCollections.observableArrayList();
        FilteredList<AppInfo> filteredList = new FilteredList<>(appList, p -> true);
        tableView.setItems(filteredList);
        TextField searchField = new TextField();
        searchField.setPromptText("Поиск по названию или пакету...");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal.toLowerCase().trim();
            filteredList.setPredicate(app -> {
                if (query.isEmpty()) return true;
                return app.getPackageName().toLowerCase().contains(query);
            });
        });
        Button uninstallButton = new Button("Удалить выбранные");
        Button installApkButton = new Button("Установить APK");
        uninstallButton.setOnAction(event -> {
            ObservableList<AppInfo> selectedApps = tableView.getSelectionModel().getSelectedItems();
            if (selectedApps.isEmpty()) {
                updateStatus("Не выбрано ни одно приложение для удаления.");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Подтверждение удаления");
            confirm.setHeaderText("Удалить выбранные приложения?");
            confirm.setContentText("Количество: " + selectedApps.size());

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                new Thread(() -> {
                    for (AppInfo app : selectedApps) {
                        try {
                            Process process = Runtime.getRuntime().exec(adbPath + " uninstall " + app.getPackageName());
                            int exitCode = process.waitFor();
                            if (exitCode == 0) {
                                Platform.runLater(() -> appList.remove(app));
                            }
                        } catch (Exception e) {
                            updateStatus("Ошибка удаления: " + e.getMessage());
                        }
                    }
                    updateStatus("Удаление завершено.");
                }).start();
            }
        });

        uninstallButton.setStyle("-fx-background-color: #c62828; -fx-text-fill: white;");
        installApkButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

        uninstallButton.setMaxWidth(Double.MAX_VALUE);
        installApkButton.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(uninstallButton, new Insets(10, 0, 0, 0));
        VBox.setMargin(installApkButton, new Insets(5, 0, 10, 0));

        VBox appsBox = new VBox(10, headerBox, searchField, tableView, uninstallButton, installApkButton);
        appsBox.setPadding(new Insets(10));

        appsContent.getChildren().add(appsBox);

        Runnable reloadData = () -> {
            appList.clear();
            new Thread(() -> {
                try {
                    Process process = Runtime.getRuntime().exec(adbPath + " shell pm list packages");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("package:")) {
                            String packageName = line.replace("package:", "").trim();
                            AppInfo app = new AppInfo(packageName);
                            Platform.runLater(() -> appList.add(app));
                        }
                    }
                } catch (Exception e) {
                    updateStatus("Ошибка получения приложений: " + e.getMessage());
                }
            }).start();
            // Обработчик кнопки установки APK
            installApkButton.setOnAction(event -> {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Выберите APK файл");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("APK файлы", "*.apk"));
                File apkFile = fileChooser.showOpenDialog(installApkButton.getScene().getWindow());
                if (apkFile != null) {
                    new Thread(() -> {
                        try {
                            updateStatus("Установка APK: " + apkFile.getName());
                            Process process = new ProcessBuilder(adbPath, "install", "-r", apkFile.getAbsolutePath()).start();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                            String line;
                            StringBuilder output = new StringBuilder();
                            while ((line = reader.readLine()) != null) {
                                output.append(line).append("\n");
                            }
                            int exitCode = process.waitFor();
                            Platform.runLater(() -> {
                                if (exitCode == 0 && output.toString().contains("Success")) {
                                    updateStatus("APK установлен: " + apkFile.getName());
                                } else {
                                    updateStatus("Ошибка установки: " + output);
                                }
                            });
                        } catch (Exception e) {
                            updateStatus("Ошибка установки APK: " + e.getMessage());
                        }
                    }).start();
                }
            });
        };

        refreshButton.setOnAction(event -> reloadData.run());
        reloadData.run(); // первоначальная загрузка
    }

    @FXML
    private void showTools() {
        setActiveContent(toolsContent);
    }
    private void initializeTools() {
        toolsGrid.getChildren().clear(); // Очистка старых кнопок

        // Список инструментов
        List<Button> tools = new ArrayList<>();
        tools.add(createToolButton("Настройка статус-бара",  this::showStatusBarDialog));
        tools.add(createToolButton("Изменить разрешение",  this::promptResolutionChange));
        tools.add(createToolButton("Сделать скриншот", this::takeScreenshot));
        tools.add(createToolButton("Перезагрузка", () -> executeAdb("reboot")));
        tools.add(createToolButton("Громкость +", () -> executeAdb("shell input keyevent 24")));
        tools.add(createToolButton("Громкость -", () -> executeAdb("shell input keyevent 25")));
        tools.add(createToolButton("Кнопка Power", () -> executeAdb("shell input keyevent 26")));
        tools.add(createToolButton("Запустить scrcpy", this::runScrcpy));

        // Расположение кнопок в GridPane (2 колонки)
        int row = 0;
        for (int i = 0; i < tools.size(); i++) {
            int col = i % 2; // 2 колонки
            if (col == 0) row++;
            toolsGrid.add(tools.get(i), col, row - 1);
        }
    }

    // Метод для создания кнопки с иконкой
    private Button createToolButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("tool-button");
        button.setOnAction(e -> action.run());

        return button;
    }

    private void showStatusBarDialog() {
        Platform.runLater(() -> {
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Настройка статус-бара");

            // Чекбоксы
            CheckBox hideClock = new CheckBox("Скрыть часы");
            CheckBox hideWifi = new CheckBox("Скрыть Wi-Fi");
            CheckBox hideMobile = new CheckBox("Скрыть мобильные данные");
            CheckBox hideBattery = new CheckBox("Скрыть батарею");

            VBox content = new VBox(10, hideClock, hideWifi, hideMobile, hideBattery);
            content.setPadding(new Insets(10));

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            dialog.setResultConverter(button -> {
                if (button == ButtonType.OK) {
                    List<String> hiddenItems = new ArrayList<>();
                    if (hideClock.isSelected()) hiddenItems.add("-clock");
                    if (hideWifi.isSelected()) hiddenItems.add("-wifi");
                    if (hideMobile.isSelected()) hiddenItems.add("-mobile");
                    if (hideBattery.isSelected()) hiddenItems.add("-battery");

                    String param = "immersive.status=" + String.join(",", hiddenItems);
                    executeAdb("shell settings put global policy_control " + param);
                    updateStatus("Применена политика: " + param);
                }
                return null;
            });

            dialog.showAndWait();
        });
    }
    private void executeCommand(String... command) {
        new Thread(() -> {
            try {
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.redirectErrorStream(true);
                Process process = builder.start();

                // Чтение вывода
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), Charset.forName("windows-1251"))
                );
                String line;
                while ((line = reader.readLine()) != null) {
                    String finalLine = line;
                    Platform.runLater(() -> {
                        consoleOutput.appendText(finalLine + "\n");
                        logHistory.add("[CMD] " + finalLine);
                    });
                }

                int exitCode = process.waitFor();
                Platform.runLater(() ->
                        consoleOutput.appendText("[Завершено: " + exitCode + "]\n")
                );
            } catch (Exception e) {
                Platform.runLater(() ->
                        consoleOutput.appendText("Ошибка: " + e.getMessage() + "\n")
                );
            }
        }).start();
    }

    private void executeAdb(String command) {
        new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec(adbPath + " " + command);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("windows-1251")));
                String line;
                while ((line = reader.readLine()) != null) {
                    String finalLine = line;
                    Platform.runLater(() -> {
                        consoleOutput.appendText(finalLine + "\n");
                        logHistory.add("[ADB] " + finalLine);
                    });
                }
                int exitCode = process.waitFor();
                Platform.runLater(() -> consoleOutput.appendText("[Завершено: " + exitCode + "]\n"));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    consoleOutput.appendText("Ошибка: " + e.getMessage() + "\n");
                    logHistory.add("[ERROR] " + e.getMessage());
                });
            }
        }).start();
    }
    private Map<String, String> getPropMap() {
        Map<String, String> props = new HashMap<>();
        try {
            Process process = Runtime.getRuntime().exec(adbPath + " shell getprop");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("windows-1251")));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("[") && line.contains("]:")) {
                    String[] parts = line.split("]: \\[");
                    if (parts.length == 2) {
                        String key = parts[0].substring(1);
                        String value = parts[1].replace("]", "");
                        props.put(key, value);
                    }
                }
            }
        } catch (IOException e) {
            updateStatus("Ошибка чтения getprop: " + e.getMessage());
        }

        return props;
    }
    private void takeScreenshot() {
        // ШАГ 1: Сначала в JavaFX потоке получаем путь к файлу
        Platform.runLater(() -> {
            try {
                String fileName;
                if (deviceModel != null) {
                    Map<String, String> props = getPropMap();
                    String manufacturer = props.getOrDefault("ro.product.brand", "unknown").replaceAll("\\s+", "_");
                    String model = props.getOrDefault("ro.product.model", "unknown").replaceAll("\\s+", "_");
                    String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"));
                    fileName = manufacturer + "_" + model + "_" + timestamp + ".png";
                } else {
                    fileName = "screenshot_" + System.currentTimeMillis() + ".png";
                }

                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Сохранить скриншот");
                fileChooser.setInitialFileName(fileName);
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
                File saveFile = fileChooser.showSaveDialog(toolsContent.getScene().getWindow());

                if (saveFile != null) {
                    // ШАГ 2: теперь запускаем фоновую задачу
                    new Thread(() -> {
                        try {
                            String remotePath = "/storage/emulated/0/" + fileName;
                            Process p1 = Runtime.getRuntime().exec(adbPath + " shell screencap -p " + remotePath);
                            p1.waitFor();

                            Process p2 = Runtime.getRuntime().exec(adbPath + " pull " + remotePath + " \"" + saveFile.getAbsolutePath() + "\"");
                            p2.waitFor();

                            Process p3 = Runtime.getRuntime().exec(adbPath + " shell rm " + remotePath);
                            p3.waitFor();

                            Platform.runLater(() -> updateStatus("Скриншот сохранён: " + saveFile.getAbsolutePath()));
                        } catch (Exception e) {
                            Platform.runLater(() -> updateStatus("Ошибка скриншота: " + e.getMessage()));
                        }
                    }).start();
                }

            } catch (Exception e) {
                updateStatus("Ошибка скриншота (UI): " + e.getMessage());
            }
        });
    }



    private void promptResolutionChange() {
        Platform.runLater(() -> {
            TextInputDialog dialog = new TextInputDialog("1080x1920");
            dialog.setTitle("Изменение разрешения");
            dialog.setHeaderText("Введите новое разрешение:");
            dialog.setContentText("Формат: ширинаХвысота (например, 720x1280)");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(res -> executeAdb("shell wm size " + res));
        });
    }
    private void runScrcpy() {
        new Thread(() -> {
            try {
                if (deviceIpAddress != null) {
                    Process process = new ProcessBuilder("scrcpy\\scrcpy.exe", "--tcpip=" + deviceIpAddress).start();
                } else {
                    Process process = new ProcessBuilder("scrcpy\\scrcpy.exe").start();
                }
            } catch (IOException e) {
                updateStatus("Ошибка запуска scrcpy: " + e.getMessage());
            }
        }).start();
    }



    @FXML
    private void showConsole() {
        setActiveContent(consoleContent);
    }

    @FXML
    private void showHelpDialog() {
        // Создаем диалог
        Dialog<Void> helpDialog = new Dialog<>();
        helpDialog.setTitle("Быстрый старт");
        helpDialog.setHeaderText(null);

        // Создаем WebView для отображения HTML
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        // Загружаем HTML из ресурсов (предполагается, что файл help.html находится в src/main/resources)
        webEngine.load(MainController.class.getResource("/help.html").toExternalForm());

        // Добавляем кнопку "Я понял!"
        ButtonType confirmButtonType = new ButtonType("Я понял!", ButtonBar.ButtonData.OK_DONE);
        helpDialog.getDialogPane().getButtonTypes().add(confirmButtonType);

        // Устанавливаем размеры и контент
        helpDialog.getDialogPane().setContent(webView);
        helpDialog.setResizable(true);
        helpDialog.setWidth(600);
        helpDialog.setHeight(500);

        // Открываем диалог
        helpDialog.showAndWait();
    }

    private void setActiveContent(VBox content) {
        // Скрываем все содержимое
        overviewContent.setVisible(false);
        appsContent.setVisible(false);
        toolsContent.setVisible(false);
        consoleContent.setVisible(false);

        // Показываем выбранное содержимое
        content.setVisible(true);
    }
}