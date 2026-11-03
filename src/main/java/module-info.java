module doom {
    requires javafx.controls;
    requires javafx.graphics;
    opens doom to javafx.graphics;
    exports doom;
}
