module com.example.guidesign {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.guidesign to javafx.fxml;
    exports com.example.guidesign;
}