package ariel

import com.fasterxml.jackson.module.kotlin.readValue
import io.javalin.Javalin
import io.javalin.http.NotFoundResponse
import jzeus.core.json.jacksonObjectMapper
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

data class ConfigValue(val value: String)
class Ariel(private val configFile: File = File("ariel_config.json")) {
    private val configStore = mutableMapOf(
        "dev" to mutableMapOf(),
        "test" to mutableMapOf(),
        "prod" to mutableMapOf<String, String>()
    )

    private val configLock = ReentrantLock()
    private val mapper = jacksonObjectMapper()
    private val logger = LoggerFactory.getLogger(Ariel::class.java)

    /**
     * 读取配置
     * @param env 环境
     * @param key 配置名
     * @return 配置值
     */
    fun getConfig(env: String, key: String): String? {
        return configLock.withLock {
            configStore[env]?.get(key)
        }
    }

    fun getAllConfig(env: String): Map<String, String>? {
        return configLock.withLock {
            configStore[env]?.toMap()
        }
    }

    private fun saveConfigToFile() {
        configLock.withLock {
            configFile.writeText(mapper.writeValueAsString(configStore))
        }
    }

    fun setConfig(env: String, key: String, value: String) {
        configLock.withLock {
            configStore.getOrPut(env) { mutableMapOf() }[key] = value
        }
        saveConfigToFile()
    }

    fun getAllEnvs(): List<String> {
        return configStore.keys.toList()
    }

    fun loadConfigFromFile() {
        try {
            if (configFile.exists()) {
                configLock.withLock {
                    configStore.clear()
                    configStore.putAll(mapper.readValue(configFile.readText()))
                }
            }
        } catch (e: Exception) {
            // 如果文件不存在或读取失败，使用默认的空配置
            logger.error("Failed to load config: ${e.message}")
        }
    }
}

fun main() {
    val ariel = Ariel()
    ariel.loadConfigFromFile()

    val app = Javalin.create().start(7000)

    app.get("/ariel/{env}/{key}") { ctx ->
        val env = ctx.pathParam("env")
        val key = ctx.pathParam("key")
        val value = ariel.getConfig(env, key)
        if (value != null) {
            ctx.json(mapOf(key to value))
        } else {
            throw NotFoundResponse("Config not found")
        }
    }


    app.put("/ariel/{env}/{key}") { ctx ->
        val env = ctx.pathParam("env")
        val key = ctx.pathParam("key")
        val configValue = ctx.bodyAsClass(ConfigValue::class.java)
        ariel.setConfig(env, key, configValue.value)
        ctx.json(mapOf("success" to true))
    }

    app.get("/ariel/{env}") { ctx ->
        val env = ctx.pathParam("env")
        val config = ariel.getAllConfig(env)
        if (config != null) {
            ctx.json(config)
        } else {
            throw NotFoundResponse("Environment not found")
        }
    }

    app.get("/ariel") { ctx ->
        ctx.json(ariel.getAllEnvs())
    }
}
