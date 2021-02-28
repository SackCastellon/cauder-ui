package es.upv.mist.cauder

import es.upv.mist.cauder.erlang.Cauder
import es.upv.mist.cauder.view.Debugger
import javafx.stage.Stage
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tornadofx.*

class CauderApp : App(Debugger::class, CaudeStyles::class), KoinComponent {
    private val cauder: Cauder by inject<Cauder>()

    override fun start(stage: Stage) {
        super.start(stage)

        cauder.start()
        cauder.subscribe()
    }

    override fun stop() {
        super.stop()

        cauder.unsubscribe()
        cauder.stopSystem()
        cauder.stop()

        cauder.close()
    }
}
