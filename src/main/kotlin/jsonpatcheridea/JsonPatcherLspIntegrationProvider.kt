package dev.mattidragon.jsonpatcheridea

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.BaseProcessHandler
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspIntegrationProvider
import com.intellij.platform.lsp.api.ProjectWideLspClientDescriptor
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
}

private val tempFileLocation = Files.createTempFile("jsonpatch-lsp-server", ".jar").toAbsolutePath().toString()
private var tempFileCreated = false

private class JsonPatcherLspClientDescriptor(project: Project) : ProjectWideLspClientDescriptor(project, "JsonPatcher") {
    override fun isSupportedFile(file: VirtualFile): Boolean {
        return file.extension == "jsonpatch"
    }

    override fun startServerProcess(): BaseProcessHandler<*> {
        if (!tempFileCreated) {
            javaClass.getResourceAsStream("/server.jar")?.use { input ->
                Files.newOutputStream(Paths.get(tempFileLocation)).use { output ->
                    input.copyTo(output)
                }
            }
        }

        return super.startServerProcess()
    }

    override fun createCommandLine(): GeneralCommandLine {
        val javaHome = System.getProperty("java.home")
        val javaBin = Paths.get(javaHome, "bin", if (SystemInfo.isWindows) "java.exe" else "java")
        check(Files.exists(javaBin)) { "Cannot find JBR executable for server" }

        return GeneralCommandLine(javaBin.toString(), "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5007", "-jar", tempFileLocation, "--stdio")
    }


}