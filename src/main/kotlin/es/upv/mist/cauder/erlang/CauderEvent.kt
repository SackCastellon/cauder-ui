package es.upv.mist.cauder.erlang

import com.ericsson.otp.erlang.*
import es.upv.mist.cauder.model.CauderSystem
import kotlin.io.path.Path

data class Event(
    val state: State,
    val task: Task,
    val system: CauderSystem?,
) {
    fun getKey(): String = "${state.atom}.$task"

    companion object {
        fun from(term: OtpErlangObject): Event {
            term as OtpErlangTuple
            check((term.elementAt(0) as OtpErlangAtom).atomValue() == "dbg")

            val data = term.elementAt(1) as OtpErlangTuple

            val stateAtom = (data.elementAt(0) as OtpErlangAtom).atomValue()
            val state = State.values().first { it.atom == stateAtom }

            val taskAtom = (data.elementAt(1) as OtpErlangAtom).atomValue()
            val taskTuple = (data.elementAt(2) as OtpErlangTuple)
            val task: Task = when (taskAtom) {
                "load" -> {
                    val file = (taskTuple.elementAt(0) as OtpErlangString).stringValue().let(::Path)
                    val module = (taskTuple.elementAt(1) as OtpErlangAtom).atomValue()
                    Task.Load(file, module)
                }
                "start" -> {
                    Task.Start
                }
                "step" -> {
                    val semAtom = (taskTuple.elementAt(0) as OtpErlangAtom).atomValue()
                    val sem = Semantics.values().first { it.atom == semAtom }
                    val stepsTuple = (taskTuple.elementAt(1) as OtpErlangTuple)
                    val stepsDone = (stepsTuple.elementAt(0) as OtpErlangLong).intValue()
                    val stepsTotal = (stepsTuple.elementAt(1) as OtpErlangLong).intValue()
                    Task.Manual.Step(sem, stepsDone, stepsTotal)
                }
                "step_multiple" -> {
                    val semAtom = (taskTuple.elementAt(0) as OtpErlangAtom).atomValue()
                    val sem = Semantics.values().first { it.atom == semAtom }
                    val stepsTuple = (taskTuple.elementAt(1) as OtpErlangTuple)
                    val stepsDone = (stepsTuple.elementAt(0) as OtpErlangLong).intValue()
                    val stepsTotal = (stepsTuple.elementAt(1) as OtpErlangLong).intValue()
                    Task.Manual.Multiple(sem, stepsDone, stepsTotal)
                }
                "replay_steps" -> {
                    val stepsTuple = (taskTuple.elementAt(0) as OtpErlangTuple)
                    val stepsDone = (stepsTuple.elementAt(0) as OtpErlangLong).intValue()
                    val stepsTotal = (stepsTuple.elementAt(1) as OtpErlangLong).intValue()
                    Task.Replay.Step(stepsDone, stepsTotal)
                }
                "replay_spawn" -> {
                    val pid = (taskTuple.elementAt(0) as OtpErlangLong).intValue()
                    Task.Replay.Spawn(pid)
                }
                "replay_send" -> {
                    val uid = (taskTuple.elementAt(0) as OtpErlangLong).intValue()
                    Task.Replay.Send(uid)
                }
                "replay_receive" -> {
                    val uid = (taskTuple.elementAt(0) as OtpErlangLong).intValue()
                    Task.Replay.Receive(uid)
                }
                "replay_full_log" -> {
                    Task.Replay.Full
                }
                "rollback_steps" -> {
                    val stepsTuple = (taskTuple.elementAt(0) as OtpErlangTuple)
                    val stepsDone = (stepsTuple.elementAt(0) as OtpErlangLong).intValue()
                    val stepsTotal = (stepsTuple.elementAt(1) as OtpErlangLong).intValue()
                    Task.Rollback.Step(stepsDone, stepsTotal)
                }
                "rollback_spawn" -> {
                    val pid = (taskTuple.elementAt(0) as OtpErlangLong).intValue()
                    Task.Rollback.Spawn(pid)
                }
                "rollback_send" -> {
                    val uid = (taskTuple.elementAt(0) as OtpErlangLong).intValue()
                    Task.Rollback.Send(uid)
                }
                "rollback_receive" -> {
                    val uid = (taskTuple.elementAt(0) as OtpErlangLong).intValue()
                    Task.Rollback.Receive(uid)
                }
                "rollback_variable" -> {
                    val name = (taskTuple.elementAt(0) as OtpErlangList).stringValue()
                    Task.Rollback.Variable(name)
                }
                else -> error("Unexpected data: $data")
            }

            val system = (data.elementAt(4) as? OtpErlangTuple)?.toSystem()

            return Event(state, task, system)
        }
    }

    enum class State(val atom: String) {
        SUSPEND("suspend"),
        RESUME("resume"),
        CANCEL("cancel"),
        SUCCESS("success"),
        FAILURE("failure"),
    }
}
