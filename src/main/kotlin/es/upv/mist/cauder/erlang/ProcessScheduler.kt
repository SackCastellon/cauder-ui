package es.upv.mist.cauder.erlang

enum class ProcessScheduler(val key: String) {
    ROUND_ROBIN("round_robin"), FCFS("fcfs")
}
