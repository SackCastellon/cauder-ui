package es.upv.mist.cauder.erlang

enum class Semantics(val atom: String) {
    FORWARD("cauder_semantics_forwards"), BACKWARD("cauder_semantics_backwards")
}
