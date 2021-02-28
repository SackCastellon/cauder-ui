module cauder {
    requires kotlin.stdlib.jdk8;
    requires kotlin.reflect;

    requires javafx.controls;

    requires tornadofx;

    opens es.upv.mist.cauder to javafx.graphics, tornadofx;
    opens es.upv.mist.cauder.view to javafx.graphics, tornadofx;
}
