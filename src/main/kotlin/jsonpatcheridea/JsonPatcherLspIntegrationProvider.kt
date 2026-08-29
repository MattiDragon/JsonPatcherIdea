package dev.mattidragon.jsonpatcheridea

import com.google.gson.JsonObject
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.BaseProcessHandler
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.*
import com.intellij.platform.lsp.api.lsWidget.LspClientWidgetItem
import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.InitializeResult
import java.nio.file.Files
import java.nio.file.Paths

internal class JsonPatcherLspIntegrationProvider : LspIntegrationProvider {
    override fun fileOpened(
        project: Project,
        file: VirtualFile,
        clientStarter: LspIntegrationProvider.LspClientStarter
    ) {
        if (file.extension == "jsonpatch") {
            clientStarter.ensureClientStarted(
                JsonPatcherLspClientDescriptor(project)
            )
        }
    }

    override fun createWidgetItem(
        lspClient: LspClient,
        currentFile: VirtualFile?
    ): LspClientWidgetItem = LspClientWidgetItem(lspClient, currentFile, JsonPatcherIcons.FILE, JsonPatcherConfigurable::class.java)
}

private val tempFileLocation = Files.createTempFile("jsonpatch-lsp-server-", ".jar").toAbsolutePath().toString()
private var tempFileCreated = false

private class JsonPatcherLspClientDescriptor(project: Project) : ProjectWideLspClientDescriptor(project, "JsonPatcher") {
    override val lspServerListener: LspServerListener = object : LspServerListener {
        override fun serverInitialized(params: InitializeResult) {
            val client = LspClientManager.getInstance(project).getClients(JsonPatcherLspIntegrationProvider::class.java).singleOrNull()
            client?.let {
                it.sendNotification { server ->
                    val rootJson = JsonObject()
                    val jsonPatcherJson = JsonObject()
                    rootJson.add("jsonpatcher", jsonPatcherJson)
                    val serverOptionsJson = JsonObject()
                    jsonPatcherJson.add("serverOptions", serverOptionsJson)

                    val settings = JsonPatcherSettings.of(project).serverOptions
                    serverOptionsJson.addProperty("formatterEnabled", settings.formatterEnabled)
                    serverOptionsJson.addProperty("formatterColumns", settings.formatterColumns)
                    serverOptionsJson.addProperty("inlayTypesEnabled", settings.inlayTypesEnabled)

                    server.workspaceService.didChangeConfiguration(DidChangeConfigurationParams(rootJson))
                }
            }
        }
    }

    override fun isSupportedFile(file: VirtualFile): Boolean {
        return file.extension == "jsonpatch"
    }

    override fun startServerProcess(): BaseProcessHandler<*> {
        if (!tempFileCreated && !JsonPatcherSettings.of(project).useCustomServer) {
            javaClass.getResourceAsStream("/server.jar")?.use { input ->
                Files.newOutputStream(Paths.get(tempFileLocation)).use { output ->
                    input.copyTo(output)
                }
            }
        }

        return super.startServerProcess()
    }

    override fun createCommandLine(): GeneralCommandLine {
        val settings = JsonPatcherSettings.of(project)

        val javaBin: String
        if (settings.useCustomJava) {
            javaBin = settings.javaLocation
        } else {
            val javaHome = System.getProperty("java.home")
            val javaPath = Paths.get(javaHome, "bin", if (SystemInfo.isWindows) "java.exe" else "java")
            check(Files.exists(javaPath)) { "Unable find java executable for server" }
            javaBin = javaPath.toString()
        }

        val args = arrayListOf(javaBin)
        args.addAll(settings.javaOptions)
        args.add("-jar")
        args.add(if (settings.useCustomServer) settings.serverLocation else tempFileLocation)
        args.add("--stdio")

        return GeneralCommandLine(args)
    }
}