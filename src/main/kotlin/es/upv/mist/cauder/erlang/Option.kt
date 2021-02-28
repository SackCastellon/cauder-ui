package es.upv.mist.cauder.erlang

data class Option(
    val pid: Int,
    val semantics: Semantics,
    val rule: Rule,
)
