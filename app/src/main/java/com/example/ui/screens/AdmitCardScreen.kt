package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ExamScheduleEntry
import com.example.ui.SchoolErpData
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErpPanelCard
import com.example.ui.components.ErpSectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.Crimson600
import com.example.ui.theme.Navy800
import com.example.ui.theme.RoyalBlue600
import com.example.util.PdfAndPrintManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdmitCardScreen(
    data: SchoolErpData,
    onSaveSchedule: (ExamScheduleEntry) -> Unit,
    onDeleteSchedule: (String) -> Unit,
    onNotify: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var selectedClass by remember(data.classes) { mutableStateOf(data.classes.firstOrNull()) }
    var classMenu by remember { mutableStateOf(false) }

    val classExams = remember(data.exams, selectedClass) {
        if (selectedClass == null) emptyList() else data.exams.filter { it.classId == selectedClass!!.id }
    }
    var selectedExam by remember(classExams) { mutableStateOf(classExams.firstOrNull()) }
    var examMenu by remember { mutableStateOf(false) }

    var showScheduleDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<ExamScheduleEntry?>(null) }
    var deleteScheduleTarget by remember { mutableStateOf<ExamScheduleEntry?>(null) }
    var pendingPdfBytes by remember { mutableStateOf<ByteArray?>(null) }

    val savePdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val bytes = pendingPdfBytes
        if (uri != null && bytes != null) {
            PdfAndPrintManager.writePdfToUri(context, uri, bytes)
                .onSuccess { onNotify("A4 Admit Cards PDF (3 per page) saved.", false) }
                .onFailure { onNotify("Failed to save Admit Cards PDF.", true) }
        }
        pendingPdfBytes = null
    }

    val classStudents = remember(data.students, selectedClass) {
        if (selectedClass == null) emptyList()
        else data.students.filter { it.classId == selectedClass!!.id }.sortedBy { it.rollNumber.toIntOrNull() ?: 9999 }
    }
    val schedules = remember(data.examSchedules, selectedExam) {
        if (selectedExam == null) emptyList()
        else data.examSchedules.filter { it.examId == selectedExam!!.id }.sortedBy { it.examDate }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(6.dp))
            ErpSectionHeader(
                title = "Admit Card Generator",
                subtitle = "Print-ready A4 PDF • Strictly 3 admit cards per A4 page"
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = classMenu, onExpandedChange = { classMenu = !classMenu }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedClass?.fullName ?: "No Classes",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Class") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = classMenu, onDismissRequest = { classMenu = false }) {
                        data.classes.forEach { cls ->
                            DropdownMenuItem(text = { Text(cls.fullName) }, onClick = { selectedClass = cls; classMenu = false })
                        }
                    }
                }

                ExposedDropdownMenuBox(expanded = examMenu, onExpandedChange = { examMenu = !examMenu }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedExam?.name ?: "No Exam",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Exam") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = examMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = examMenu, onDismissRequest = { examMenu = false }) {
                        classExams.forEach { ex ->
                            DropdownMenuItem(text = { Text(ex.name) }, onClick = { selectedExam = ex; examMenu = false })
                        }
                    }
                }
            }
        }

        if (selectedExam == null) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Badge,
                    title = "No Examination Selected",
                    message = "Create an examination for ${selectedClass?.fullName ?: "this class"} in Exams & Results to generate admit cards."
                )
            }
        } else {
            // Exam Schedule & Custom Syllabus Editor
            item {
                ErpPanelCard(
                    title = "Exam Schedule & Syllabus (${schedules.size})",
                    subtitle = "Add dates, days, subjects & custom syllabus printed on admit cards",
                    trailing = {
                        OutlinedButton(onClick = { editingSchedule = null; showScheduleDialog = true }) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Subject")
                        }
                    }
                ) {
                    if (schedules.isEmpty()) {
                        Text("No subjects added to this exam schedule yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        schedules.forEach { s ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${s.subjectName} • ${s.examDate} (${s.examDay})", style = MaterialTheme.typography.titleSmall)
                                    Text("${s.timing} • Syllabus: ${s.syllabus.ifBlank { "Full Syllabus" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { editingSchedule = s; showScheduleDialog = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit schedule", tint = RoyalBlue600, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { deleteScheduleTarget = s }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete schedule", tint = Crimson600, modifier = Modifier.size(18.dp))
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }

            // Print / Download Actions
            item {
                val pagesCount = ((classStudents.size + 2) / 3).coerceAtLeast(1)
                ErpPanelCard(
                    title = "A4 Batch Export",
                    subtitle = "${classStudents.size} student(s) • $pagesCount A4 page(s) (3 cards/page)"
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (classStudents.isEmpty()) {
                                    onNotify("No students in ${selectedClass?.fullName} to generate admit cards.", true)
                                } else {
                                    pendingPdfBytes = PdfAndPrintManager.generateAdmitCardsPdf(
                                        context, data.settings, selectedExam!!, classStudents, schedules
                                    )
                                    savePdfLauncher.launch("AdmitCards_${selectedClass?.name}_${selectedExam?.name}.pdf")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Download A4 PDF")
                        }
                        OutlinedButton(
                            onClick = {
                                if (classStudents.isEmpty()) {
                                    onNotify("No students in ${selectedClass?.fullName} to print admit cards.", true)
                                } else {
                                    val bytes = PdfAndPrintManager.generateAdmitCardsPdf(
                                        context, data.settings, selectedExam!!, classStudents, schedules
                                    )
                                    PdfAndPrintManager.printPdfBytes(context, "AdmitCards_${selectedClass?.name}", bytes)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Print A4 Sheet")
                        }
                    }
                }
            }

            // Live Card Preview List
            if (classStudents.isNotEmpty()) {
                item {
                    Text("Admit Card Preview (${classStudents.size} Cards)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(classStudents, key = { it.id }) { stu ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, Navy800, MaterialTheme.shapes.medium),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(data.settings.schoolName, style = MaterialTheme.typography.titleSmall, color = Navy800, fontWeight = FontWeight.Bold)
                                    Text("${data.settings.schoolAddress} • ${data.settings.academicSession}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                StatusPill("ADMIT CARD", RoyalBlue600)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            Text("${stu.name}  |  Class: ${stu.className}  |  Roll #${stu.rollNumber}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("Father: ${stu.fatherName}  |  Adm No: ${stu.admissionNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (schedules.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                schedules.forEach { s ->
                                    Text("• ${s.examDate} (${s.examDay}) — ${s.subjectName}: ${s.syllabus.ifBlank { "Full Syllabus" }}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showScheduleDialog && selectedExam != null) {
        ExamScheduleFormDialog(
            exam = selectedExam!!,
            initial = editingSchedule,
            onDismiss = { showScheduleDialog = false },
            onSave = {
                onSaveSchedule(it)
                showScheduleDialog = false
            }
        )
    }

    deleteScheduleTarget?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete Schedule Entry?",
            message = "Remove ${target.subjectName} from admit card schedule?",
            onConfirm = {
                onDeleteSchedule(target.id)
                deleteScheduleTarget = null
            },
            onDismiss = { deleteScheduleTarget = null }
        )
    }
}
