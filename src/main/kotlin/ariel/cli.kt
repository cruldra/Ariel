package ariel

import io.javalin.Javalin
import io.javalin.http.NotFoundResponse
import jzeus.io.asFile
import picocli.CommandLine
import kotlin.system.exitProcess


@CommandLine.Command(
    name = "ariel",
    description = ["Ariel CLI for managing environment variables"],
    subcommands = [
        ArielCli.Set::class,
        ArielCli.Get::class,
        ArielCli.Delete::class,
        ArielCli.ListEnvs::class,
        ArielCli.StartServer::class
    ]
)
class ArielCli {

    @CommandLine.Command(name = "set", description = ["Set a value for a key in a specific environment"])
    class Set : Runnable {
        @CommandLine.Option(names = ["-e", "--env"], description = ["Environment (optional)"])
        var env: String = DEFAULT_NAMESPACE

        @CommandLine.Option(names = ["-k", "--key"], required = true, description = ["Key"])
        lateinit var key: String

        @CommandLine.Option(names = ["-v", "--value"], required = true, description = ["Value"])
        lateinit var value: String

        override fun run() {
            ariel("${env}.json".asFile()) {
                setConfig(env, key, value)
            }
        }
    }

    @CommandLine.Command(name = "get", description = ["Get a value for a key from a specific environment"])
    class Get : Runnable {
        @CommandLine.Option(names = ["-e", "--env"], description = ["Environment (optional)"])
        var env: String = DEFAULT_NAMESPACE

        @CommandLine.Option(names = ["-k", "--key"], required = true, description = ["Key"])
        lateinit var key: String

        override fun run() {
            ariel("${env}.json".asFile()) {
                loadConfigFromFile()
                val value = getConfig(env, key)
                if (value != null) {
                    println(value)
                } else {
                    println("Key not found")
                }
            }
        }
    }

    @CommandLine.Command(name = "del", description = ["Delete a key"])
    class Delete : Runnable {
        @CommandLine.Option(names = ["-e", "--env"], description = ["Environment (optional)"])
        var env: String = DEFAULT_NAMESPACE

        @CommandLine.Option(names = ["-k", "--key"], required = true, description = ["Key to delete"])
        lateinit var key: String

        override fun run() {
            ariel("${env}.json".asFile()) {
                loadConfigFromFile()
                deleteConfig(env, key)
                print("Key deleted")
            }
        }
    }

    @CommandLine.Command(name = "list-envs", description = ["List all environments"])
    class ListEnvs : Runnable {
        override fun run() {
            ariel {
                print(getAllEnvs().joinToString("\n"))
            }
        }
    }

    @CommandLine.Command(name = "serve", description = ["Start the Ariel server"])
    class StartServer : Runnable {
        override fun run() {
            val app = Javalin.create().start(7000)
            app.get("/{env}/{key}") { ctx ->
                val env = ctx.pathParam("env")
                val key = ctx.pathParam("key")
                ariel("${env}.json".asFile()) {
                    loadConfigFromFile()
                    val value = getConfig(env, key)
                    if (value != null) {
                        ctx.result(value)  // 直接返回值作为字符串
                        ctx.contentType("text/plain")  // 设置内容类型为纯文本
                    } else {
                        throw NotFoundResponse("Config not found")
                    }
                }
            }
        }
    }
}

fun main(vararg args: String) {
    val cli = CommandLine(ArielCli())
    cli.execute(*args)
//    exitProcess(  cli.execute(*args))

}
