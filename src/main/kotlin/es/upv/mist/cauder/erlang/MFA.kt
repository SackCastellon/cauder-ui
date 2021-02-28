package es.upv.mist.cauder.erlang

data class MFA(
    val module: String,
    val function: String,
    val arity: Int,
)
