module doom {
    requires javafx.controls;
    requires javafx.graphics;
    requires java.desktop;
    opens doom to javafx.graphics;
    exports doom;
}
