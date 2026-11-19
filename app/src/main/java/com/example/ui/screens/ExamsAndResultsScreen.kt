package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AcademicCalculations
import com.example.data.model.ExamScheduleEntry
import com.example.data.model.Examination
import com.example.data.model.SchoolClass
import com.example.data.model.Student
import com.example.data.model.StudentExamResult
import com.example.data.model.StudentMark
import com.example.ui.SchoolErpData
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErpPanelCard
import com.example.ui.components.ErpSearchField
import com.example.ui.components.ErpSectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.Crimson600
import com.example.ui.theme.Emerald600
import com.example.ui.theme.GoldAmber500
import com.example.ui.theme.RoyalBlue600
import com.example.util.PdfAndPrintManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExamsAndResultsScreen(
    data: SchoolErpData,
    onSaveExam: (Examination) -> Unit,
    onDeleteExam: (String) -> Unit,
    onSaveSchedule: (ExamScheduleEntry) -> Unit,
    onDeleteSchedule: (String) -> Unit,
    onSaveMark: (StudentMark) -> Unit,
    onDeleteMark: (String) -> Unit,
    onNotify: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var selectedExam by remember { mutableStateOf<Examination?>(null) }
    var showExamForm by remember { mutableStateOf(false) }
    var editingExam by remember { mutableStateOf<Examination?>(null) }
    var deleteExamTarget by remember { mutableStateOf<Examination?>(null) }
    var previewReportCard by remember { mutableStateOf<StudentExamResult?>(null) }
    var pendingPdfBytes by remember { mutableStateOf<ByteArray?>(null) }

    val savePdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val bytes = pendingPdfBytes
        if (uri != null && bytes != null) {
            PdfAndPrintManager.writePdfToUri(context, uri, bytes)
                .onSuccess { onNotify("Marksheet PDF saved.", false) }
                .onFailure { onNotify("Failed to save PDF.", true) }
        }
        pendingPdfBytes = null
    }

    if (previewReportCard != null) {
        val result = previewReportCard!!
        BackHandler { previewReportCard = null }
        ReportCardPreview(
            data = data,
            result = result,
            onBack = { previewReportCard = null },
            onDownloadPdf = {
                pendingPdfBytes = PdfAndPrintManager.generateMarksheetPdf(context, data.settings, result)
                savePdfLauncher.launch("Marksheet_${result.student.name.replace(" ", "_")}.pdf")
            },
            onPrint = {
                val bytes = PdfAndPrintManager.generateMarksheetPdf(context, data.settings, result)
                PdfAndPrintManager.printPdfBytes(context, "Marksheet_${result.student.name}", bytes)
            }
        )
    } else if (selectedExam != null) {
        val exam = data.exams.find { it.id == selectedExam!!.id } ?: selectedExam!!
        BackHandler { selectedExam = null }
        ExamDetailWorkspace(
            exam = exam,
            data = data,
            onBack = { selectedExam = null },
            onSaveSchedule = onSaveSchedule,
            onDeleteSchedule = onDeleteSchedule,
            onSaveMark = onSaveMark,
            onDeleteMark = onDeleteMark,
            onPreviewReportCard = { previewReportCard = it }
        )
    } else {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        if (data.classes.isEmpty()) {
                            onNotify("Create classes first before creating an examination.", true)
                        } else {
                            editingExam = null
                            showExamForm = true
                        }
                    },
                    containerColor = RoyalBlue600
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Exam", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        ) { inner ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Spacer(Modifier.height(6.dp))
                    ErpSectionHeader(
                        title = "Examinations & Results",
                        subtitle = "Dynamic subjects, custom max marks, auto-ranking & PDF marksheets"
                    )
                }

                if (data.exams.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.Quiz,
                            title = "No Examinations Created",
                            message = "Create an exam, add custom subjects & max marks, enter student scores, and generate ranked report cards.",
                            actionLabel = if (data.classes.isNotEmpty()) "Create Examination" else null,
                            onAction = { editingExam = null; showExamForm = true }
                        )
                    }
                } else {
                    items(data.exams, key = { it.id }) { exam ->
                        val subjectCount = data.examSchedules.count { it.examId == exam.id }
                        Card(
                            onClick = { selectedExam = exam },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(exam.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text("${exam.className} • $subjectCount subject(s) • Starts ${exam.startDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(4.dp))
                                    StatusPill(exam.status, RoyalBlue600)
                                }
                                IconButton(onClick = { editingExam = exam; showExamForm = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Exam", tint = RoyalBlue600)
                                }
                                IconButton(onClick = { deleteExamTarget = exam }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Exam", tint = Crimson600)
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(76.dp)) }
            }
        }
    }

    if (showExamForm) {
        ExamFormDialog(
            initial = editingExam,
            classes = data.classes,
            session = data.settings.academicSession,
            onDismiss = { showExamForm = false },
            onSave = {
                onSaveExam(it)
                showExamForm = false
            }
        )
    }

    deleteExamTarget?.let { exam ->
        ConfirmDeleteDialog(
            title = "Delete Examination ${exam.name}?",
            message = "Delete this examination definition?",
            onConfirm = {
                onDeleteExam(exam.id)
                deleteExamTarget = null
            },
            onDismiss = { deleteExamTarget = null }
        )
    }
}

@Composable
private fun ExamDetailWorkspace(
    exam: Examination,
    data: SchoolErpData,
    onBack: () -> Unit,
    onSaveSchedule: (ExamScheduleEntry) -> Unit,
    onDeleteSchedule: (String) -> Unit,
    onSaveMark: (StudentMark) -> Unit,
    onDeleteMark: (String) -> Unit,
    onPreviewReportCard: (StudentExamResult) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<ExamScheduleEntry?>(null) }
    var deleteScheduleTarget by remember { mutableStateOf<ExamScheduleEntry?>(null) }

    var markTarget by remember { mutableStateOf<Pair<Student, ExamScheduleEntry>?>(null) }
    var search by remember { mutableStateOf("") }

    val schedules = remember(data.examSchedules, exam.id) {
        data.examSchedules.filter { it.examId == exam.id }.sortedBy { it.examDate }
    }
    val classStudents = remember(data.students, exam.classId) {
        data.students.filter { it.classId == exam.classId }
    }
    val results = remember(exam, classStudents, schedules, data.studentMarks) {
        AcademicCalculations.computeClassResults(exam, classStudents, schedules, data.studentMarks)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(exam.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(exam.className, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = { editingSchedule = null; showScheduleDialog = true }) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Subject")
            }
        }

        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Subjects (${schedules.size})") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Marks Entry") })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Results & Rank") })
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Spacer(Modifier.height(6.dp)) }

            when (tab) {
                0 -> {
                    if (schedules.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.Quiz,
                                title = "No Subjects Added Yet",
                                message = "Add any custom subjects with custom Maximum Marks, Pass Marks, Dates & Syllabus.",
                                actionLabel = "Add Subject",
                                onAction = { editingSchedule = null; showScheduleDialog = true }
                            )
                        }
                    } else {
                        items(schedules, key = { it.id }) { sch ->
                            ErpPanelCard(
                                title = sch.subjectName,
                                subtitle = "${sch.examDate} (${sch.examDay}) • ${sch.timing}",
                                trailing = {
                                    Row {
                                        IconButton(onClick = { editingSchedule = sch; showScheduleDialog = true }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit subject", tint = RoyalBlue600)
                                        }
                                        IconButton(onClick = { deleteScheduleTarget = sch }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove subject", tint = Crimson600)
                                        }
                                    }
                                }
                            ) {
                                Text("Max Marks: ${sch.maxMarks} • Pass Marks: ${sch.passMarks}", style = MaterialTheme.typography.bodySmall, color = RoyalBlue600, fontWeight = FontWeight.SemiBold)
                                if (sch.syllabus.isNotBlank()) {
                                    Text("Syllabus: ${sch.syllabus}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                1 -> {
                    item {
                        ErpSearchField(
                            query = search,
                            onQueryChange = { search = it },
                            placeholder = "Search student name or roll no..."
                        )
                    }
                    val filteredStudents = classStudents.filter {
                        val q = search.trim().lowercase()
                        q.isEmpty() || it.name.lowercase().contains(q) || it.rollNumber.lowercase().contains(q)
                    }
                    if (schedules.isEmpty() || filteredStudents.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.Edit,
                                title = "Ready for Marks Entry",
                                message = if (schedules.isEmpty()) "Add exam subjects in the Subjects tab first." else "No students found in ${exam.className}."
                            )
                        }
                    } else {
                        items(filteredStudents, key = { it.id }) { stu ->
                            ErpPanelCard(title = "${stu.rollNumber}. ${stu.name}", subtitle = "Adm: ${stu.admissionNumber}") {
                                schedules.forEach { sch ->
                                    val existingMark = data.studentMarks.find {
                                        it.examId == exam.id && it.studentId == stu.id && it.scheduleId == sch.id
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(sch.subjectName, style = MaterialTheme.typography.titleSmall)
                                            Text(
                                                if (existingMark == null) "Not entered (Max ${sch.maxMarks})"
                                                else if (existingMark.absent) "ABSENT (Max ${sch.maxMarks})"
                                                else "Score: ${existingMark.obtainedMarks} / ${sch.maxMarks}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (existingMark == null) MaterialTheme.colorScheme.onSurfaceVariant else Emerald600
                                            )
                                        }
                                        Row {
                                            TextButton(onClick = { markTarget = stu to sch }) {
                                                Text(if (existingMark == null) "Enter" else "Edit")
                                            }
                                            if (existingMark != null) {
                                                IconButton(onClick = { onDeleteMark(existingMark.id) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete mark", tint = Crimson600, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }

                2 -> {
                    if (results.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.EmojiEvents,
                                title = "No Results Calculated Yet",
                                message = "Enter student marks in the Marks Entry tab to auto-calculate totals, percentages, grades & class ranks."
                            )
                        }
                    } else {
                        items(results, key = { it.student.id }) { res ->
                            ErpPanelCard(
                                title = "#${res.rank} • ${res.student.name}",
                                subtitle = "Roll #${res.student.rollNumber} • ${"%.1f".format(res.percentage)}% (${res.grade})",
                                trailing = {
                                    StatusPill(
                                        if (res.passed) "PASS" else "FAIL",
                                        if (res.passed) Emerald600 else Crimson600
                                    )
                                }
                            ) {
                                Text("Total: ${res.totalObtained} / ${res.totalMax}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { onPreviewReportCard(res) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("View Report Card / PDF")
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showScheduleDialog) {
        ExamScheduleFormDialog(
            exam = exam,
            initial = editingSchedule,
            onDismiss = { showScheduleDialog = false },
            onSave = {
                onSaveSchedule(it)
                showScheduleDialog = false
            }
        )
    }

    markTarget?.let { (stu, sch) ->
        val existing = data.studentMarks.find { it.examId == exam.id && it.studentId == stu.id && it.scheduleId == sch.id }
        MarkEntryDialog(
            student = stu,
            schedule = sch,
            exam = exam,
            existing = existing,
            onDismiss = { markTarget = null },
            onSave = {
                onSaveMark(it)
                markTarget = null
            }
        )
    }

    deleteScheduleTarget?.let { target ->
        ConfirmDeleteDialog(
            title = "Remove ${target.subjectName}?",
            message = "Remove ${target.subjectName} from ${exam.name}?",
            onConfirm = {
                onDeleteSchedule(target.id)
                deleteScheduleTarget = null
            },
            onDismiss = { deleteScheduleTarget = null }
        )
    }
}

@Composable
private fun ReportCardPreview(
    data: SchoolErpData,
    result: StudentExamResult,
    onBack: () -> Unit,
    onDownloadPdf: () -> Unit,
    onPrint: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Student Report Card", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        }

        ErpPanelCard(
            title = data.settings.schoolName,
            subtitle = "${result.exam.name} • Class Rank #${result.rank}"
        ) {
            Text("${result.student.name} (Roll #${result.student.rollNumber})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Class: ${result.student.className} • Father: ${result.student.fatherName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            result.marks.forEach { m ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(m.subjectName, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        if (m.absent) "ABSENT / ${m.maxMarks}" else "${m.obtainedMarks} / ${m.maxMarks}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (m.absent) Crimson600 else RoyalBlue600
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total: ${result.totalObtained} / ${result.totalMax}", fontWeight = FontWeight.Bold)
                Text("${"%.2f".format(result.percentage)}% (${result.grade})", color = Emerald600, fontWeight = FontWeight.Bold)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onDownloadPdf, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Download PDF")
            }
            OutlinedButton(onClick = onPrint, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Print, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Print Marksheet")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamFormDialog(
    initial: Examination?,
    classes: List<SchoolClass>,
    session: String,
    onDismiss: () -> Unit,
    onSave: (Examination) -> Unit
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var selectedClass by remember { mutableStateOf(classes.find { it.id == initial?.classId } ?: classes.firstOrNull()) }
    var classMenu by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(initial?.startDate?.ifBlank { today } ?: today) }
    var endDate by remember { mutableStateOf(initial?.endDate ?: "") }
    var status by remember { mutableStateOf(initial?.status ?: "UPCOMING") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Create Examination" else "Edit Examination") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Exam Name * (e.g. Half Yearly)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                ExposedDropdownMenuBox(expanded = classMenu, onExpandedChange = { classMenu = !classMenu }) {
                    OutlinedTextField(
                        value = selectedClass?.fullName ?: "Select Class",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Class *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = classMenu, onDismissRequest = { classMenu = false }) {
                        classes.forEach { cls ->
                            DropdownMenuItem(text = { Text(cls.fullName) }, onClick = { selectedClass = cls; classMenu = false })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("Start Date *") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = endDate, onValueChange = { endDate = it }, label = { Text("End Date") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("UPCOMING", "ONGOING", "COMPLETED").forEach { st ->
                        FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st) })
                    }
                }
                error?.let { Text(it, color = Crimson600, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val cls = selectedClass
                when {
                    name.isBlank() -> error = "Exam name is required."
                    cls == null -> error = "Select a class."
                    else -> onSave(
                        Examination(
                            id = initial?.id ?: "",
                            name = name.trim(),
                            classId = cls.id,
                            className = cls.fullName,
                            academicSession = session,
                            startDate = startDate.trim(),
                            endDate = endDate.trim(),
                            status = status
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ExamScheduleFormDialog(
    exam: Examination,
    initial: ExamScheduleEntry?,
    onDismiss: () -> Unit,
    onSave: (ExamScheduleEntry) -> Unit
) {
    var subjectName by remember { mutableStateOf(initial?.subjectName ?: "") }
    var examDate by remember { mutableStateOf(initial?.examDate?.ifBlank { exam.startDate } ?: exam.startDate) }
    var examDay by remember { mutableStateOf(initial?.examDay ?: "Monday") }
    var timing by remember { mutableStateOf(initial?.timing ?: "09:00 AM – 12:00 PM") }
    var maxMarks by remember { mutableStateOf((initial?.maxMarks ?: 100L).toString()) }
    var passMarks by remember { mutableStateOf((initial?.passMarks ?: 33L).toString()) }
    var syllabus by remember { mutableStateOf(initial?.syllabus ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Subject to ${exam.name}" else "Edit Subject Schedule") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = subjectName, onValueChange = { subjectName = it }, label = { Text("Subject Name * (Custom)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = examDate, onValueChange = { examDate = it }, label = { Text("Date *") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = examDay, onValueChange = { examDay = it }, label = { Text("Day *") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = timing, onValueChange = { timing = it }, label = { Text("Timing") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = maxMarks,
                        onValueChange = { maxMarks = it },
                        label = { Text("Max Marks *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = passMarks,
                        onValueChange = { passMarks = it },
                        label = { Text("Pass Marks *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(value = syllabus, onValueChange = { syllabus = it }, label = { Text("Custom Syllabus (for Admit Card)") }, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = Crimson600, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val maxM = maxMarks.toLongOrNull()
                val passM = passMarks.toLongOrNull()
                when {
                    subjectName.isBlank() -> error = "Subject name is required."
                    examDate.isBlank() || examDay.isBlank() -> error = "Exam date and day are required."
                    maxM == null || maxM < 1 -> error = "Enter valid max marks."
                    passM == null || passM < 0 || passM > maxM -> error = "Pass marks must be 0..$maxM."
                    else -> onSave(
                        ExamScheduleEntry(
                            id = initial?.id ?: "",
                            examId = exam.id,
                            examName = exam.name,
                            classId = exam.classId,
                            className = exam.className,
                            subjectName = subjectName.trim(),
                            examDate = examDate.trim(),
                            examDay = examDay.trim(),
                            timing = timing.trim(),
                            maxMarks = maxM,
                            passMarks = passM,
                            syllabus = syllabus.trim()
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun MarkEntryDialog(
    student: Student,
    schedule: ExamScheduleEntry,
    exam: Examination,
    existing: StudentMark?,
    onDismiss: () -> Unit,
    onSave: (StudentMark) -> Unit
) {
    var marksText by remember { mutableStateOf(existing?.obtainedMarks?.toString() ?: "") }
    var absent by remember { mutableStateOf(existing?.absent ?: false) }
    var remarks by remember { mutableStateOf(existing?.remarks ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${student.name} — ${schedule.subjectName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Maximum Marks: ${schedule.maxMarks} • Pass Marks: ${schedule.passMarks}", style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = absent, onCheckedChange = { absent = it })
                    Text("Mark Student Absent")
                }
                if (!absent) {
                    OutlinedTextField(
                        value = marksText,
                        onValueChange = { marksText = it },
                        label = { Text("Obtained Marks (0 – ${schedule.maxMarks}) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(value = remarks, onValueChange = { remarks = it }, label = { Text("Remarks") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = Crimson600, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val score = if (absent) 0.0 else marksText.toDoubleOrNull()
                if (!absent && (score == null || score < 0.0 || score > schedule.maxMarks.toDouble())) {
                    error = "Enter marks between 0 and ${schedule.maxMarks}."
                } else {
                    onSave(
                        StudentMark(
                            id = existing?.id ?: "",
                            examId = exam.id,
                            examName = exam.name,
                            classId = exam.classId,
                            className = exam.className,
                            studentId = student.id,
                            studentName = student.name,
                            rollNumber = student.rollNumber,
                            scheduleId = schedule.id,
                            subjectName = schedule.subjectName,
                            maxMarks = schedule.maxMarks,
                            obtainedMarks = score ?: 0.0,
                            absent = absent,
                            remarks = remarks.trim()
                        )
                    )
                }
            }) { Text("Save Marks") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
