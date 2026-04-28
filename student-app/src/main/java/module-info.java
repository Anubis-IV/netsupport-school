module anubis.netsupportschool.studentapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.net.http;
    requires java.logging;

    opens anubis.netsupportschool.studentapp to javafx.fxml;
    opens anubis.netsupportschool.studentapp.ui to javafx.fxml;
    exports anubis.netsupportschool.studentapp;
}
