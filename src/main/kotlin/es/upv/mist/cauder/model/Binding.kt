package es.upv.mist.cauder.model

import com.ericsson.otp.erlang.OtpErlangObject

data class Binding(
    val key: String,
    val value: OtpErlangObject,
)
