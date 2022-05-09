module mayaseii.wildsmoddingtool {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.web;
    requires javafx.swing;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires java.datatransfer;
    requires java.desktop;
    requires org.jetbrains.annotations;

    opens mayaseii.wildsmoddingtool to javafx.fxml;
    exports mayaseii.wildsmoddingtool;
}