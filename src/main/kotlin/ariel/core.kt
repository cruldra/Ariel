package ariel

import com.fasterxml.jackson.module.kotlin.readValue
import io.javalin.Javalin
import io.javalin.http.NotFoundResponse
import jzeus.core.json.jacksonObjectMapper
import jzeus.io.asFile
import jzeus.io.createIfNotExists
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

const val DEFAULT_NAMESPACE = "default"

class Ariel(private val configFile: File = "${DEFAULT_NAMESPACE}.json".asFile()) {
    init {
        configFile.createIfNotExists()
    }

    private val configStore = mutableMapOf<String, MutableMap<String, String>>()

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

    fun deleteConfig(env: String, key: String) {
        configLock.withLock {
            configStore[env]?.remove(key)
        }
        saveConfigToFile()
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

fun ariel(configFile: File = "${DEFAULT_NAMESPACE}.json".asFile(), block: Ariel.() -> Unit): Ariel {
    return Ariel(configFile).apply(block)
}
