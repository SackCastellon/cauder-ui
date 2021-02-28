package es.upv.mist.cauder.erlang

import com.ericsson.otp.erlang.*

fun int(value: Int) = OtpErlangInt(value)
fun atom(value: String) = OtpErlangAtom(value)
fun string(value: String) = OtpErlangString(value)

fun list() = OtpErlangList()
fun list(value: OtpErlangObject) = OtpErlangList(value)
fun list(values: Array<OtpErlangObject>) = OtpErlangList(values)
fun list(values: List<OtpErlangObject>) = OtpErlangList(values.toTypedArray())

@JvmName("listVararg")
fun list(vararg values: OtpErlangObject) = OtpErlangList(values)


