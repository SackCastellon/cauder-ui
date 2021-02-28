package es.upv.mist.cauder.view.dialog

import es.upv.mist.cauder.erlang.Cauder
import es.upv.mist.cauder.erlang.list
import javafx.geometry.Pos
import javafx.scene.control.ButtonBar
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tornadofx.*

class RunDialog : Fragment("Run..."), KoinComponent {
    private val cauder by inject<Cauder>()

    val module: String by param()
    private val functionProp = stringProperty("")

    override val root = vbox {
        vbox(spacing = 7) {
            padding = insets(10)
            hbox(spacing = 5) {
                alignment = Pos.CENTER_LEFT
                label("Module:") {
                    prefWidth = 50.0
                }
                choicebox(values = listOf(module)) {
                    selectionModel.selectFirst()
                    isDisable = true
                    prefWidth = 125.0
                }
            }
            hbox(spacing = 5) {
                alignment = Pos.CENTER_LEFT
                label("Function:") {
                    prefWidth = 50.0
                }

                val values = cauder.getEntryPoints(module)
                    .filter { it.arity == 0 }
                    .map { "${it.function}/${it.arity}" }

                combobox(functionProp, values) {
                    selectionModel.selectFirst()
                    prefWidth = 125.0
                }
            }
        }

        separator()

        buttonbar {
            padding = insets(10)
            button("Start", type = ButtonBar.ButtonData.OK_DONE) {
                enableWhen { functionProp.isNotBlank() }
                action {
                    cauder.initSystem(module, functionProp.value.substringBefore('/'), list())
                    close()
                }
            }
            button("Cancel", type = ButtonBar.ButtonData.CANCEL_CLOSE) {
                action {
                    close()
                }
            }
        }
    }
}
