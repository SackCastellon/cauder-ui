package es.upv.mist.cauder.erlang

import com.ericsson.otp.erlang.*
import java.util.concurrent.LinkedBlockingQueue

class CauderConnection(
    private val self: OtpSelf,
    peer: OtpPeer?,
) : AbstractConnection(self, peer) {
    private var msgQueue = LinkedBlockingQueue<OtpMsg>()
    private var rpcQueue = LinkedBlockingQueue<OtpMsg>()
    private var dbgQueue = LinkedBlockingQueue<Event>()

    init {
        start()
    }

    override fun deliver(exception: Exception) {
        throw exception
    }

    override fun deliver(msg: OtpMsg) {
        val term = msg.msg
        println("deliver: $term")
        if (term is OtpErlangTuple && term.arity() == 2) {
            when ((term.elementAt(0) as OtpErlangAtom).atomValue()) {
                "rex" -> return rpcQueue.put(msg)
                "dbg" -> return dbgQueue.put(Event.from(msg.msg))
            }
        }

        msgQueue.put(msg)
    }

    fun send(dest: String?, msg: OtpErlangObject?) {
        // encode and send the message
        super.sendBuf(self.pid(), dest, OtpOutputStream(msg))
    }

    fun receive(): OtpErlangObject = msgQueue.take().msg

    fun sendRPC(module: String, function: String, args: OtpErlangList) {
        val rpc = arrayOfNulls<OtpErlangObject>(2)
        val call = arrayOfNulls<OtpErlangObject>(5)

        /* {self, { call, Mod, Fun, Args, user}} */
        call[0] = OtpErlangAtom("call")
        call[1] = OtpErlangAtom(module)
        call[2] = OtpErlangAtom(function)
        call[3] = args
        call[4] = OtpErlangAtom("user")
        rpc[0] = self.pid()
        rpc[1] = OtpErlangTuple(call)
        send("rex", OtpErlangTuple(rpc))
    }

    fun receiveRPC(): OtpErlangObject? {
        val term = rpcQueue.take().msg as OtpErlangTuple
        return term.elementAt(1)
    }

    fun receiveDbg(): Event = dbgQueue.take()
}
