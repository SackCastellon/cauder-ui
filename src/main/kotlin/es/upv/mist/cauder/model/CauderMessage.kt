package es.upv.mist.cauder.model

import com.ericsson.otp.erlang.OtpErlangObject

data class CauderMessage(
    val uid: Int,
    val value: OtpErlangObject,
    val src: Int,
    val dest: Int,
)
