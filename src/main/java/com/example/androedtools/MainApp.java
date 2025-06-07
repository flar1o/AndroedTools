package com.example.androedtools;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    private DeviceMonitor deviceMonitor;
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main_view.fxml"));
        Scene scene = new Scene(loader.load());

        MainController controller = loader.getController();

        deviceMonitor = new DeviceMonitor(controller);
        deviceMonitor.start();

        stage.setTitle("Androed Tools");
        stage.setScene(scene);
        // Установка начального размера окна
        stage.setWidth(1200);
        stage.setHeight(800);



        // минимальный размер
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        stage.show();

        stage.setOnCloseRequest(event -> {
            if (deviceMonitor != null) {
                deviceMonitor.stop(); // Принудительное завершение ADB
            }
            Platform.exit(); // Завершение JavaFX
            System.exit(0); // Принудительное завершение JVM
        });

        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void stop() throws Exception {
        if (deviceMonitor != null) {
            deviceMonitor.stop(); // Теперь метод доступен
        }
        super.stop(); // Всегда вызывайте родительский метод
        System.exit(0); // Принудительное завершение
    }
}
