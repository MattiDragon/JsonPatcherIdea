@file:Suppress("UnstableApiUsage")

package dev.mattidragon.jsonpatcheridea

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.JBColor
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableModel

class JsonPatcherConfigurable(private val project: Project) : Configurable {
    private lateinit var panel: DialogPanel

    override fun getDisplayName(): @NlsContexts.ConfigurableName String =
        JsonPatcherMessageBundle.message("settings.jsonpatcher.display_name")

    override fun createComponent(): JComponent = panel {
        val settings = JsonPatcherSettings.of(project)

        group(JsonPatcherMessageBundle.message("settings.jsonpatcher.java.title")) {
            row {
                val checkBox = checkBox(JsonPatcherMessageBundle.message("settings.jsonpatcher.java.custom_location"))
                    .bindSelected(settings::useCustomJava)

                textFieldWithBrowseButton(FileChooserDescriptorFactory.singleFile(), project)
                    .bindText(settings::javaLocation)
                    .enabledIf(checkBox.selected)
                    .columns(COLUMNS_LARGE)
            }

            row {
                cell(JvmOptionsPanel())
                    .bind(
                        JvmOptionsPanel::commitAndGet,
                        JvmOptionsPanel::setOptions,
                        settings::javaOptions.toMutableProperty()
                    )
            }
        }

        group(JsonPatcherMessageBundle.message("settings.jsonpatcher.server.title")) {
            row {
                checkBox(JsonPatcherMessageBundle.message("settings.jsonpatcher.server.enabled"))
                    .bindSelected(settings::serverEnabled)
            }

            row {
                val checkBox = checkBox(JsonPatcherMessageBundle.message("settings.jsonpatcher.server.custom_location"))
                    .bindSelected(settings::useCustomServer)

                textFieldWithBrowseButton(FileChooserDescriptorFactory.singleFile(), project)
                    .bindText(settings::serverLocation)
                    .enabledIf(checkBox.selected)
                    .columns(COLUMNS_LARGE)
            }
        }

        group(JsonPatcherMessageBundle.message("settings.jsonpatcher.server.options.title")) {
            val serverOptions = settings.serverOptions

            row {
                checkBox(JsonPatcherMessageBundle.message("settings.jsonpatcher.server.options.formatter_enabled"))
                    .bindSelected(serverOptions::formatterEnabled)
            }

            row {
                label(JsonPatcherMessageBundle.message("settings.jsonpatcher.server.options.formatter_columns"))
                intTextField(0..10000, 10)
                    .bindIntText(serverOptions::formatterColumns)
            }

            row {
                checkBox(JsonPatcherMessageBundle.message("settings.jsonpatcher.server.options.inlay_types_enabled"))
                    .bindSelected(serverOptions::inlayTypesEnabled)
            }
        }
    }.also { panel = it }

    override fun isModified(): Boolean = panel.isModified()

    override fun apply() = panel.apply()

    private class JvmOptionsPanel : JPanel(BorderLayout()) {
        private val tableModel = DefaultTableModel(arrayOf("JVM option"), 0)
        private val table = JBTable(tableModel)

        init {
            table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
            table.setShowGrid(false)

            table.tableHeader.isVisible = false
            table.tableHeader.preferredSize = JBUI.size(0, 0)
            table.tableHeader.minimumSize = JBUI.size(0, 0)

            table.setRowHeight(JBUI.scale(22))
            table.preferredScrollableViewportSize = JBUI.size(520, 240)
            table.fillsViewportHeight = true

            val decorator = ToolbarDecorator.createDecorator(table)
                .setPanelBorder(JBUI.Borders.customLine(JBColor.border(), 1, 1, 1, 1))
                .setAddAction {
                    tableModel.addRow(arrayOf(""))
                    val row = tableModel.rowCount - 1
                    table.setRowSelectionInterval(row, row)
                    table.editCellAt(row, 0)
                    table.scrollRectToVisible(table.getCellRect(row, 0, true))
                }
                .setRemoveAction {
                    table.cellEditor?.stopCellEditing()
                    val sel = table.selectedRow
                    if (sel >= 0) {
                        tableModel.removeRow(sel)
                        val newSel = if (sel >= tableModel.rowCount) tableModel.rowCount - 1 else sel
                        if (newSel >= 0) table.setRowSelectionInterval(newSel, newSel) else table.clearSelection()
                        table.revalidate()
                        table.repaint()
                        table.requestFocusInWindow()
                    }
                }

            add(JBLabel(JsonPatcherMessageBundle.message("settings.jsonpatcher.java.jvm_options")), BorderLayout.NORTH)
            add(decorator.createPanel(), BorderLayout.CENTER)
        }

        fun setOptions(opts: List<String>) {
            tableModel.rowCount = 0
            opts.forEach { tableModel.addRow(arrayOf(it)) }
        }

        fun commitAndGet(): List<String> {
            return (0 until tableModel.rowCount).map { r -> tableModel.getValueAt(r, 0) as? String ?: "" }
        }
    }
}
