package dev.mattidragon.jsonpatcheridea

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsSafe
import org.jetbrains.annotations.NonNls
import org.jetbrains.plugins.textmate.TextMateBackedFileType
import org.jetbrains.plugins.textmate.api.TextMateBundleProvider
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import javax.swing.Icon

object JsonPatcherLanguage : Language("JsonPatcher") {
    private fun readResolve(): Any = JsonPatcherLanguage
}

class JsonPatcherFileType : LanguageFileType(JsonPatcherLanguage), TextMateBackedFileType {
    override fun getName(): @NonNls String = "JsonPatcher"

    override fun getDescription(): @NlsContexts.Label String = JsonPatcherMessageBundle.message("filetype.jsonpatcher.description")

    override fun getDefaultExtension(): @NlsSafe String = "jsonpatch"

    override fun getIcon(): Icon = JsonPatcherIcons.FILE
}

class JsonPatcherTextMateBundleProvider : TextMateBundleProvider {
    val bundlePath = extractBundle()

    override fun getBundles(): List<TextMateBundleProvider.PluginBundle> {
        return listOf(TextMateBundleProvider.PluginBundle("JsonPatcher", bundlePath))
    }

    private fun extractBundle(): Path {
        val dir = Files.createTempDirectory("jsonpatcher-textmate-bundle.")
        val fileNames = listOf("package.json", "language-configuration.json", "syntaxes/jsonpatcher.tmLanguage.json")

        val javaClass = JsonPatcherTextMateBundleProvider::class.java

        for (fileName in fileNames) {
            val target = dir.resolve(fileName)
            Files.createDirectories(target.parent)
            javaClass.getResourceAsStream("/textmate/$fileName")?.use {
                Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING)
            }
        }
        return dir
    }
}