package es.upv.mist.cauder.erlang

enum class Rule(val atom: String) {
    SEQ("seq"),
    SELF("self"),
    SPAWN("spawn"),
    SEND("send"),
    RECEIVE("receive"),
}
