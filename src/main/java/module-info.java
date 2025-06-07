module com.example.androedtools {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires ddmlib;
    requires java.desktop;
    requires java.xml.crypto;

    opens com.example.androedtools to javafx.fxml;
    exports com.example.androedtools;
}