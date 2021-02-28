package es.upv.mist.cauder

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import es.upv.mist.cauder.erlang.Cauder
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.dsl.module
import tornadofx.*
import java.io.File

private class CauderCli : CliktCommand(), KoinComponent {
    val cauderPath: File by option("--dbg", help = "Location of CauDEr binaries", envvar = "CAUDER")
        .file(mustExist = true)
        .required()

    override fun run() {
        val configModule = module {
            single { Cauder(cauderPath) }
        }

        startKoin {
            modules(configModule)
        }

        launch<CauderApp>()
    }
}

fun main(args: Array<String>) = CauderCli().main(args)
