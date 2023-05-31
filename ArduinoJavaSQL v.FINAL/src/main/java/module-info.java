module com.example.guidesign {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fazecast.jSerialComm;
    requires javafx.graphics;
    requires java.sql;


    opens com.example.guidesign to javafx.fxml;
    exports com.example.guidesign;
}