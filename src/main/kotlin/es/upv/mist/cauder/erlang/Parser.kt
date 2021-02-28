package es.upv.mist.cauder.erlang

import com.ericsson.otp.erlang.*
import es.upv.mist.cauder.model.Binding
import es.upv.mist.cauder.model.CauderMessage
import es.upv.mist.cauder.model.CauderProcess
import es.upv.mist.cauder.model.CauderSystem

fun OtpErlangObject.toInt(): Int = (this as OtpErlangLong).intValue()

fun OtpErlangObject.toKString(): String {
    return when (this) {
        is OtpErlangAtom -> atomValue()
        is OtpErlangList -> stringValue()
        else -> error("Unexpected term: $this")
    }
}


fun OtpErlangObject.toSystem(): CauderSystem {
    check(this is OtpErlangTuple && arity() == 6 && elementAt(0).toKString() == "sys")

    val mail = elementAt(1).toMailbox()
    val procs = (elementAt(2) as OtpErlangMap).values().map(OtpErlangObject::toProcess)

    return CauderSystem(mail, procs, this)
}

fun OtpErlangObject.toProcess(): CauderProcess {
    check(this is OtpErlangTuple && arity() == 7 && elementAt(0).toKString() == "proc")

    val pid = elementAt(1).toInt()
    val env = elementAt(4).toEnvironment()
    val expr = elementAt(5).toExpressions()
    val entryPoint = elementAt(6).toMFA()

    return CauderProcess(pid, env, expr, entryPoint)
}


fun OtpErlangObject.toMailbox(): List<CauderMessage> {
    check(this is OtpErlangTuple && arity() == 3 && elementAt(0).toKString() == "mailbox")

    return (elementAt(2) as OtpErlangMap).values().flatMap {
        (it as OtpErlangList).elements()
            .map { tuple -> (tuple as OtpErlangTuple).elementAt(1) }
            .flatMap { queue ->
                check(queue is OtpErlangTuple)

                val rear = (queue.elementAt(0) as OtpErlangList).elements().map(OtpErlangObject::toMessage)
                val front = (queue.elementAt(1) as OtpErlangList).elements().map(OtpErlangObject::toMessage)

                front + rear.asReversed()
            }
    }
}

fun OtpErlangObject.toMessage(): CauderMessage {
    check(this is OtpErlangTuple && arity() == 5 && elementAt(0).toKString() == "message")

    val uid = elementAt(1).toInt()
    val value = elementAt(2)
    val src = elementAt(3).toInt()
    val dest = elementAt(4).toInt()

    return CauderMessage(uid, value, src, dest)
}

fun OtpErlangObject.toEnvironment(): List<Binding> {
    return (this as OtpErlangMap)
        .entrySet()
        .map { (k, v) -> Binding((k as OtpErlangAtom).atomValue(), v) }
}

fun OtpErlangObject.toExpressions(): List<OtpErlangObject> {
    return (this as OtpErlangList)
        .elements()
        .asList()
}


fun OtpErlangObject.toMFA(): MFA {
    check(this is OtpErlangTuple && arity() == 3)

    val module = elementAt(0).toKString()
    val function = elementAt(1).toKString()
    val arity = elementAt(2).toInt()

    return MFA(module, function, arity)
}

fun OtpErlangObject.toOption(): Option {
    check(this is OtpErlangTuple && arity() == 4 && elementAt(0).toKString() == "opt")

    val pid = elementAt(1).toInt()
    val sem = Semantics.values().first { elementAt(2).toKString() == it.atom }
    val rule = Rule.values().first { elementAt(3).toKString() == it.atom }

    return Option(pid, sem, rule)
}
