package es.upv.mist.cauder.erlang

import com.ericsson.otp.erlang.*
import es.upv.mist.cauder.model.CauderSystem
import java.io.File
import kotlin.concurrent.thread

class Cauder(path: File) : AutoCloseable {

    @Suppress("JoinDeclarationAndAssignment")
    private val proc: Process

    private val self: OtpSelf
    private val peer: OtpPeer
    val connection: CauderConnection

    init {
        proc = ProcessBuilder()
            .directory(path)
            .command("erl", "-noinput", "-noshell", "-sname", PEER_NAME, "-setcookie", COOKIE)
            .start()

        Thread.sleep(500) // FIXME What for node to start

        thread(isDaemon = true) { proc.inputStream.transferTo(System.out) }
        thread(isDaemon = true) { proc.errorStream.transferTo(System.err) }

        self = OtpSelf(SELF_NAME, COOKIE)
        peer = OtpPeer(PEER_NAME)

        connection = CauderConnection(self, peer) //self.connect(peer)

        appLoad()
    }

    private fun appLoad() {
        val result = call("application", "load", list(atom(MODULE)))
        // ok | {error, Reason}
        when {
            result is OtpErlangAtom && result.atomValue() == "ok" -> return
            result is OtpErlangTuple && result.arity() == 2 -> {
                val (error, reason) = result.elements()
                if (error is OtpErlangAtom && error.atomValue() == "error") {
                    throw OtpErlangException(reason.toString())
                }
            }
        }
        error("Unexpected return value: $result")
    }

    fun start(): OtpErlangPid {
        val result = call("start", list())
        // {ok, Pid} | {error, Error}
        if (result is OtpErlangTuple && result.arity() == 2) {
            val (atom, term) = result.elements()
            if (atom is OtpErlangAtom) when {
                atom.atomValue() == "ok" && term is OtpErlangPid -> return term
                atom.atomValue() == "error" -> throw OtpErlangException(term.toString())
            }
        }
        error("Unexpected return value: $result")
    }

    fun stop() {
        val result = call("stop", list())
        check(result is OtpErlangAtom && result.atomValue() == "ok") {
            "Unexpected return value: $result"
        }
    }

    fun subscribe() {
        val result = call("subscribe", list(self.pid()))
        check(result is OtpErlangAtom && result.atomValue() == "ok") {
            "Unexpected return value: $result"
        }
    }

    fun unsubscribe() {
        val result = call("unsubscribe", list(self.pid()))
        check(result is OtpErlangAtom && result.atomValue() == "ok") {
            "Unexpected return value: $result"
        }
    }

    fun loadFile(file: File): OtpErlangObject =
        call("load_file", list(string(file.absolutePath)))

    fun initSystem(module: String, function: String, arguments: OtpErlangList): OtpErlangObject =
        call("init_system", list(atom(module), atom(function), arguments))

    fun stopSystem(): OtpErlangObject =
        call("stop_system", list())

    fun evalOpts(system: CauderSystem): List<Option> {
        val result = call("eval_opts", list(system.erlangRecord))
        check(result is OtpErlangList) {
            "Unexpected return value: $result"
        }
        return result.elements().map { it.toOption() }
    }


    fun step(semantics: Semantics, pid: Int, steps: Int, scheduler: MessageScheduler): OtpErlangObject =
        call("step", list(atom(semantics.atom), int(pid), int(steps), atom(scheduler.key)))


    fun getEntryPoints(module: String): List<MFA> {
        val result = call("get_entry_points", list(atom(module)))
        return when (result) {
            is OtpErlangList -> result.elements().map { it.toMFA() }
            else -> error("Unexpected return value: $result")
        }
    }

    override fun close() {
        connection.sendRPC("erlang", "halt", OtpErlangList())
        connection.close()
        proc.waitFor()
    }

    companion object {
        private const val SELF_NAME = "cauder-ui"
        private const val PEER_NAME = "cauder@localhost"
        private const val COOKIE = "secret"
        private const val MODULE = "cauder"

        private fun Cauder.call(function: String, arguments: OtpErlangList): OtpErlangObject =
            call(MODULE, function, arguments)

        fun Cauder.call(module: String, function: String, arguments: OtpErlangList): OtpErlangObject {
            connection.sendRPC(module, function, arguments)
            return checkNotNull(connection.receiveRPC())
        }
    }
}
