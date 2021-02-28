package es.upv.mist.cauder.erlang

import java.nio.file.Path

sealed class Task {
    data class Load(val file: Path, val module: String) : Task()

    object Start : Task()

    sealed class Manual : Task() {
        data class Step(val semantics: Semantics, val stepsDone: Int, val stepsTotal: Int) : Manual()
        data class Multiple(val semantics: Semantics, val stepsDone: Int, val stepsTotal: Int) : Manual()
    }

    sealed class Replay : Task() {
        data class Step(val stepsDone: Int, val stepsTotal: Int) : Replay()
        data class Spawn(val pid: Int) : Replay()
        data class Send(val uid: Int) : Replay()
        data class Receive(val uid: Int) : Replay()
        object Full : Replay()
    }

    sealed class Rollback : Task() {
        data class Step(val pid: Int, val steps: Int) : Rollback()
        data class Spawn(val pid: Int) : Rollback()
        data class Send(val uid: Int) : Rollback()
        data class Receive(val uid: Int) : Rollback()
        data class Variable(val name: String) : Rollback()
    }
}
