package clood.multibind

import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.LinkedHashSet

object MultibindBindings {
    private val logger = LoggerFactory.getLogger("multibind")
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val filePath: Path = FabricLoader.getInstance().configDir.resolve("multibind.json")

    private var loaded = false
    private var bindings: MutableMap<String, MutableList<String>> = linkedMapOf()

    fun load() {
        if (loaded) {
            return
        }

        loaded = true
        bindings = linkedMapOf()

        if (!Files.exists(filePath)) {
            return
        }

        runCatching {
            Files.newBufferedReader(filePath, StandardCharsets.UTF_8).use { reader ->
                val data = gson.fromJson(reader, StoredBindings::class.java)
                if (data?.bindings != null) {
                    bindings.putAll(data.bindings)
                }
            }
        }.onFailure { error ->
            logger.warn("Failed to load multibind config", error)
            bindings = linkedMapOf()
        }
    }

    fun getAdditionalKeyNames(name: String): LinkedHashSet<String> {
        load()

        val savedKeys = bindings[name] ?: return linkedSetOf()
        return LinkedHashSet(savedKeys)
    }

    fun setAdditionalKeyNames(name: String, keyNames: Collection<String>) {
        load()

        val serializedKeys = keyNames.distinct().toMutableList()
        if (serializedKeys.isEmpty()) {
            bindings.remove(name)
        } else {
            bindings[name] = serializedKeys
        }
        save()
    }

    private fun save() {
        runCatching {
            Files.createDirectories(filePath.parent)
            Files.newBufferedWriter(filePath, StandardCharsets.UTF_8).use { writer ->
                gson.toJson(StoredBindings(bindings), writer)
            }
        }.onFailure { error ->
            logger.warn("Failed to save multibind config", error)
        }
    }

    private data class StoredBindings(val bindings: MutableMap<String, MutableList<String>> = linkedMapOf())
}