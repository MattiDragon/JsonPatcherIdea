package dev.mattidragon.jsonpatcheridea

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil


@Service(Service.Level.PROJECT)
@State(name = "JsonPatcherSettings", storages = [Storage("jsonpatcher.xml")])
class JsonPatcherSettings : PersistentStateComponent<JsonPatcherSettings> {
    var useCustomJava = false
    var javaLocation: String = ""
    var javaOptions: List<String> = listOf()

    var serverEnabled = true
    var useCustomServer = false
    var serverLocation: String = ""

    val serverOptions = ServerOptions()

    data class ServerOptions(
        var formatterEnabled: Boolean = false,
        var formatterColumns: Int = 120,
        var inlayTypesEnabled: Boolean = true
    )

    override fun getState(): JsonPatcherSettings = this

    override fun loadState(state: JsonPatcherSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun of(project: Project) = project.service<JsonPatcherSettings>()
    }
}