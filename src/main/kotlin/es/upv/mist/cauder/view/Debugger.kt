package es.upv.mist.cauder.view

import com.ericsson.otp.erlang.OtpErlangString
import es.upv.mist.cauder.erlang.*
import es.upv.mist.cauder.erlang.Cauder.Companion.call
import es.upv.mist.cauder.erlang.Event.State.*
import es.upv.mist.cauder.model.*
import es.upv.mist.cauder.view.dialog.RunDialog
import javafx.beans.Observable
import javafx.beans.binding.ListBinding
import javafx.beans.value.ObservableValue
import javafx.collections.ObservableList
import javafx.event.EventTarget
import javafx.geometry.HPos
import javafx.geometry.Orientation
import javafx.geometry.Pos
import javafx.geometry.VPos
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.control.TabPane
import javafx.scene.control.TextArea
import javafx.scene.layout.GridPane
import javafx.scene.layout.Priority
import javafx.stage.FileChooser
import javafx.stage.WindowEvent
import javafx.util.StringConverter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tornadofx.*
import kotlin.concurrent.thread

class Debugger : View("CauDEr"), KoinComponent {
    private val cauder by inject<Cauder>()

    private val moduleProp = stringProperty("")

    private val systemProp = objectProperty<CauderSystem?>()

    private val processProp = objectProperty<CauderProcess?>()
    private var process by processProp

    private var codeArea: TextArea by singleAssign()

    private val isLoaded = booleanProperty(false)
    private val isTaskRunning = booleanProperty(false)

    private var statusLabel: Label by singleAssign()

    override val root: Parent = vbox {
        prefHeight = 600.0
        prefWidth = 800.0

        menuBar()

        splitpane(Orientation.VERTICAL) {
            splitpane(Orientation.HORIZONTAL) {
                vgrow = Priority.ALWAYS

                vbox {
                    codeArea = textarea {
                        isEditable = false
                        vgrow = Priority.ALWAYS
                        style {
                            fontFamily = "Monospace"
                        }
                    }

                    textfield {
                        isEditable = false
                        textProperty().bind(processProp.stringBinding {
                            if (it != null) {
                                val result = cauder.call("cauder_pp", "expression", list(it.expressions.first()))
                                (result as OtpErlangString).stringValue()
                            } else {
                                ""
                            }
                        })
                    }
                }
                vbox {
                    processSelector()
                    actions()
                }
            }
            splitpane(Orientation.HORIZONTAL) {
                environment()
                mailbox()
            }
        }

        statusBar()
    }

    init {
        thread(isDaemon = true) {
            while (true) {
                val event = cauder.connection.receiveDbg()

                println("event: $event")

                if (event.task is Task.Load && event.state == SUCCESS) {
                    isLoaded.value = true
                    moduleProp.value = event.task.module
                }

                isTaskRunning.value = when (event.state) {
                    SUSPEND, RESUME -> true
                    CANCEL, SUCCESS, FAILURE -> false
                }

                runLater { event.system?.let(systemProp::set) }
            }
        }
    }

    private fun EventTarget.menuBar() {
        menubar {
            menu("_File") {
                item("_Open...", "Ctrl+O") {
                    action {
                        val files = chooseFile(
                            title = "Open erlang source file",
                            filters = arrayOf(FileChooser.ExtensionFilter("Erlang", "*.erl")),
                            mode = FileChooserMode.Single,
                        )

                        val file = files.singleOrNull() ?: return@action

                        cauder.loadFile(file)
                        codeArea.text = file.readText()
                    }
                }
                item("E_xit") {
                    action {
                        primaryStage.fireEvent(WindowEvent(primaryStage, WindowEvent.WINDOW_CLOSE_REQUEST))
                    }
                }
            }
            menu("R_un") {
                item("R_un...") {
                    enableWhen { isLoaded }
                    action {
                        find<RunDialog>(RunDialog::module to moduleProp.value)
                            .openModal(resizable = false)
                    }
                }
            }
        }
    }

    private fun EventTarget.environment() {
        tableview<Binding> {
            items = processProp.listBinding { it?.environmentProp ?: observableListOf() }
            placeholder = label("No variables defined")
            smartResize()

            readonlyColumn("Name", Binding::key) {
                isSortable = false
                prefWidth = 125.0
            }
            readonlyColumn("Value", Binding::value) {
                isSortable = false
                remainingWidth()
            }
        }
    }

    private fun EventTarget.mailbox() {
        tableview<CauderMessage> {
            items = systemProp.listBinding { it?.mailProp ?: observableListOf() }
            placeholder = label("No messages available")
            smartResize()

            readonlyColumn("UID", CauderMessage::uid) {
                isSortable = false
                prefWidth = 75.0
            }
            readonlyColumn("Value", CauderMessage::value) {
                isSortable = false
                remainingWidth()
            }
            readonlyColumn("Source", CauderMessage::src) {
                isSortable = false
            }
            readonlyColumn("Dest.", CauderMessage::dest) {
                isSortable = false
            }
        }
    }

    private fun EventTarget.processSelector() {
        hbox(spacing = 5) {
            padding = insets(7)
            alignment = Pos.CENTER_LEFT
            label("Process:")
            vbox {
                hgrow = Priority.ALWAYS
                combobox(processProp) {
                    fitToParentWidth()
                    enableWhen { items.sizeProperty.greaterThan(0) and isLoaded }

                    converter = object : StringConverter<CauderProcess?>() {
                        override fun toString(proc: CauderProcess?): String {
                            return if (proc == null)
                                ""
                            else {
                                val (module, function, arity) = proc.entryPoint
                                return "Pid: ${proc.pid} - $module:$function/$arity"
                            }
                        }

                        override fun fromString(string: String?): CauderProcess {
                            TODO("Not yet implemented")
                        }
                    }

                    systemProp.select { it?.procsProp ?: objectProperty() }.onChange { list ->
                        val oldPid = selectionModel.selectedItem?.pid
                        selectionModel.clearSelection()
                        items.setAll(list.orEmpty())
                        if (oldPid == null) {
                            selectionModel.selectFirst()
                        } else {
                            items.indexOfFirst { it!!.pid == oldPid }
                                .coerceAtLeast(0)
                                .let(selectionModel::select)
                        }
                    }
                }
            }
        }
    }

    private fun EventTarget.actions() {
        tabpane {
            manualTab()
        }
    }

    private fun TabPane.manualTab() {
        tab(text = "Manual") {
            isClosable = false

            val stepsProp = intProperty(1)
            val schedulerProp = objectProperty(MessageScheduler.RANDOM)
            val options = systemProp.listBinding(processProp) {
                it?.let(cauder::evalOpts)
                    ?.filter { opt -> opt.pid == process?.pid }
                    .orEmpty()
                    .asObservable()
            }
            val canBwd = options.booleanBinding { it.orEmpty().any { opt -> opt.semantics == Semantics.BACKWARD } }
            val canFwd = options.booleanBinding { it.orEmpty().any { opt -> opt.semantics == Semantics.FORWARD } }

            vbox {
                enableWhen { isLoaded and processProp.isNotNull }
                gridpane {
                    vgrow = Priority.ALWAYS
                    hgap = 7.0
                    vgap = 7.0
                    alignment = Pos.CENTER
                    padding = insets(10)
                    enableWhen { canBwd or canFwd }

                    label("Steps:") {
                        GridPane.setConstraints(this, 0, 0, 1, 1, HPos.RIGHT, VPos.CENTER)
                    }

                    spinner(property = stepsProp, min = 1, max = 10_000, amountToStepBy = 1, enableScroll = true) {
                        prefWidth = 150.0
                        GridPane.setConstraints(this, 1, 0, 1, 1, HPos.LEFT, VPos.CENTER)
                    }

                    label("Msg. Sched.:") {
                        GridPane.setConstraints(this, 0, 1, 1, 1, HPos.RIGHT, VPos.CENTER)
                    }

                    combobox(property = schedulerProp, values = listOf(MessageScheduler.RANDOM)) {
                        prefWidth = 150.0
                        GridPane.setConstraints(this, 1, 1, 1, 1, HPos.LEFT, VPos.CENTER)
                    }
                }

                separator()

                hbox(spacing = 10) {
                    alignment = Pos.CENTER
                    padding = insets(7)

                    button("Backward") {
                        minWidth = 70.0
                        enableWhen { canBwd }
                        action {
                            cauder.step(Semantics.BACKWARD, process!!.pid, stepsProp.value, schedulerProp.value)
                        }
                    }
                    button("Forward") {
                        minWidth = 70.0
                        enableWhen { canFwd }
                        action {
                            cauder.step(Semantics.FORWARD, process!!.pid, stepsProp.value, schedulerProp.value)
                        }
                    }
                }
            }
        }
    }

    private fun EventTarget.statusBar() {
        hbox(spacing = 5) {
            padding = insets(3)
            statusLabel = label("Left status") {
                hgrow = Priority.ALWAYS
                style {
                    fontSize = 11.px
                    textFill = c("9F9F9F")
                }
            }
            spacer()
            label("Right status") {
                style {
                    fontSize = 11.px
                    textFill = c("9F9F9F")
                }
            }
        }
    }
}

fun <T, R> ObservableValue<T>.listBinding(
    vararg dependencies: Observable,
    op: (T) -> ObservableList<R>,
): ListBinding<R> =
    object : ListBinding<R>() {
        init {
            bind(this@listBinding, *dependencies)
        }

        override fun computeValue(): ObservableList<R> = op(this@listBinding.value)

        override fun dispose() = super.unbind(this@listBinding, *dependencies)

        override fun getDependencies(): ObservableList<*> = observableListOf(this@listBinding, *dependencies)
    }
