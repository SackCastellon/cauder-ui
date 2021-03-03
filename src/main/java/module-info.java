module cauder {
    requires kotlin.stdlib.jdk8;
    requires kotlin.reflect;

    requires javafx.controls;

    requires tornadofx;
    requires clikt.jvm;
    requires koin.core;

    exports es.upv.mist.cauder to javafx.graphics, tornadofx;
    exports es.upv.mist.cauder.view to javafx.graphics, tornadofx;
    exports es.upv.mist.cauder.view.dialog to javafx.graphics, tornadofx;
}
