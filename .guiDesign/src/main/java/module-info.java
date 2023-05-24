module com.example.guidesign {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fazecast.jSerialComm;
    requires java.sql;


    opens com.example.guidesign to javafx.fxml;
    exports com.example.guidesign;
}