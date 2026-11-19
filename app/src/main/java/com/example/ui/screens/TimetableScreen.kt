package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AcademicCalculations
import com.example.data.model.ClassSubject
import com.example.data.model.SchoolClass
import com.example.data.model.Teacher
import com.example.data.model.TimetableEntry
import com.example.ui.SchoolErpData
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErpPanelCard
import com.example.ui.components.ErpSectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.Crimson600
import com.example.ui.theme.RoyalBlue600
import com.example.util.PdfAndPrintManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    data: SchoolErpData,
    onSaveEntry: (TimetableEntry) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onNotify: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var selectedClass by remember(data.classes) { mutableStateOf(data.classes.firstOrNull()) }
    var classMenu by remember { mutableStateOf(false) }
    var selectedDayIdx by remember { mutableIntStateOf(0) }
    var showWeeklyAll by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<TimetableEntry?>(null) }
    var deleteTarget by remember { mutableStateOf<TimetableEntry?>(null) }
    var pendingPdfBytes by remember { mutableStateOf<ByteArray?>(null) }

    val savePdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val bytes = pendingPdfBytes
        if (uri != null && bytes != null) {
            PdfAndPrintManager.writePdfToUri(context, uri, bytes)
                .onSuccess { onNotify("Timetable PDF saved.", false) }
                .onFailure { onNotify("Failed to save timetable PDF.", true) }
        }
        pendingPdfBytes = null
    }

    val days = AcademicCalculations.daysOfWeek
    val activeDay = days[selectedDayIdx]

    val classTimetable = remember(data.timetables, selectedClass) {
        if (selectedClass == null) emptyList()
        else data.timetables.filter { it.classId == selectedClass!!.id }
    }

    val dayPeriods = remember(classTimetable, activeDay) {
        classTimetable.filter { it.dayOfWeek == activeDay }.sortedBy { it.periodNumber }
    }

    fun buildTimetablePdfBytes(): ByteArray {
        val cols = listOf("Day", "Period", "Timing", "Subject", "Teacher", "Room")
        val rows = classTimetable.sortedWith(
            compareBy<TimetableEntry> { days.indexOf(it.dayOfWeek).let { idx -> if (idx < 0) 99 else idx } }
                .thenBy { it.periodNumber }
        ).map { e ->
            listOf(
                e.dayOfWeek.take(3),
                "P${e.periodNumber}",
                "${e.startTime}-${e.endTime}",
                e.subjectName,
                e.teacherName.ifBlank { "-" },
                e.roomNumber.ifBlank { "-" }
            )
        }
        return PdfAndPrintManager.generateTableReportPdf(
            context = context,
            settings = data.settings,
            reportTitle = "CLASS TIMETABLE",
            subtitle = selectedClass?.fullName ?: "All Classes",
            columns = cols,
            rows = rows,
            summaryFooter = "Total Weekly Periods: ${rows.size}"
        )
    }

    Scaffold(
        floatingActionButton = {
            if (selectedClass != null) {
                FloatingActionButton(
                    onClick = { editingEntry = null; showForm = true },
                    containerColor = RoyalBlue600,
                    modifier = Modifier.testTag("add_timetable_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Period")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(6.dp))
                ErpSectionHeader(
                    title = "Class Timetable",
                    subtitle = "Weekly Mon–Sat period scheduling & print-ready exports"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        expanded = classMenu,
                        onExpandedChange = { classMenu = !classMenu },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedClass?.fullName ?: "No Classes Created",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Class") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classMenu) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = classMenu, onDismissRequest = { classMenu = false }) {
                            data.classes.forEach { cls ->
                                DropdownMenuItem(
                                    text = { Text(cls.fullName) },
                                    onClick = { selectedClass = cls; classMenu = false }
                                )
                            }
                        }
                    }

                    FilterChip(
                        selected = showWeeklyAll,
                        onClick = { showWeeklyAll = !showWeeklyAll },
                        label = { Text(if (showWeeklyAll) "Full Week" else "Day View") },
                        leadingIcon = { Icon(Icons.Default.CalendarViewWeek, null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            if (selectedClass != null) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                pendingPdfBytes = buildTimetablePdfBytes()
                                savePdfLauncher.launch("Timetable_${selectedClass?.name}.pdf")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Download PDF")
                        }
                        OutlinedButton(
                            onClick = {
                                PdfAndPrintManager.printPdfBytes(
                                    context,
                                    "Timetable_${selectedClass?.name}",
                                    buildTimetablePdfBytes()
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Print Schedule")
                        }
                    }
                }
            }

            if (selectedClass == null) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.Schedule,
                        title = "No Classes Found",
                        message = "Create classes first in Classes & Subjects to manage weekly timetables."
                    )
                }
            } else if (!showWeeklyAll) {
                item {
                    ScrollableTabRow(selectedTabIndex = selectedDayIdx, edgePadding = 0.dp) {
                        days.forEachIndexed { idx, day ->
                            Tab(
                                selected = selectedDayIdx == idx,
                                onClick = { selectedDayIdx = idx },
                                text = { Text(day.take(3)) }
                            )
                        }
                    }
                }

                if (dayPeriods.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.Schedule,
                            title = "No Periods on $activeDay",
                            message = "Tap + to schedule a period for ${selectedClass?.fullName} on $activeDay.",
                            actionLabel = "Add Period",
                            onAction = { editingEntry = null; showForm = true }
                        )
                    }
                } else {
                    items(dayPeriods, key = { it.id }) { period ->
                        TimetablePeriodCard(
                            entry = period,
                            onEdit = { editingEntry = period; showForm = true },
                            onDelete = { deleteTarget = period }
                        )
                    }
                }
            } else {
                days.forEach { day ->
                    val list = classTimetable.filter { it.dayOfWeek == day }.sortedBy { it.periodNumber }
                    item(key = "header_$day") {
                        Text(
                            text = "$day (${list.size} periods)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (list.isEmpty()) {
                        item(key = "empty_$day") {
                            Text(
                                "No periods scheduled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(list, key = { it.id }) { period ->
                            TimetablePeriodCard(
                                entry = period,
                                onEdit = { editingEntry = period; showForm = true },
                                onDelete = { deleteTarget = period }
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showForm && selectedClass != null) {
        val classSubjects = data.subjects.filter { it.classId == selectedClass!!.id }
        TimetableFormDialog(
            schoolClass = selectedClass!!,
            defaultDay = activeDay,
            initial = editingEntry,
            classSubjects = classSubjects,
            teachers = data.teachers,
            onDismiss = { showForm = false },
            onSave = {
                onSaveEntry(it)
                showForm = false
            }
        )
    }

    deleteTarget?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete Period?",
            message = "Remove Period ${target.periodNumber} (${target.subjectName}) on ${target.dayOfWeek}?",
            onConfirm = {
                onDeleteEntry(target.id)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun TimetablePeriodCard(
    entry: TimetableEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ErpPanelCard(
        title = "Period ${entry.periodNumber} • ${entry.subjectName}",
        subtitle = "${entry.startTime} – ${entry.endTime}" + if (entry.roomNumber.isNotBlank()) " • Room ${entry.roomNumber}" else "",
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusPill(entry.dayOfWeek.take(3).uppercase(), RoyalBlue600)
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit period", tint = RoyalBlue600, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete period", tint = Crimson600, modifier = Modifier.size(18.dp))
                }
            }
        }
    ) {
        Text(
            text = "Teacher: ${entry.teacherName.ifBlank { "Unassigned" }}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimetableFormDialog(
    schoolClass: SchoolClass,
    defaultDay: String,
    initial: TimetableEntry?,
    classSubjects: List<ClassSubject>,
    teachers: List<Teacher>,
    onDismiss: () -> Unit,
    onSave: (TimetableEntry) -> Unit
) {
    var dayOfWeek by remember { mutableStateOf(initial?.dayOfWeek ?: defaultDay) }
    var dayMenu by remember { mutableStateOf(false) }
    var periodNo by remember { mutableStateOf((initial?.periodNumber ?: 1L).toString()) }
    var startTime by remember { mutableStateOf(initial?.startTime ?: "08:30 AM") }
    var endTime by remember { mutableStateOf(initial?.endTime ?: "09:15 AM") }
    var subjectName by remember { mutableStateOf(initial?.subjectName ?: (classSubjects.firstOrNull()?.subjectName ?: "")) }
    var selectedTeacher by remember { mutableStateOf(teachers.find { it.id == initial?.teacherId }) }
    var teacherMenu by remember { mutableStateOf(false) }
    var roomNumber by remember { mutableStateOf(initial?.roomNumber ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Timetable Period" else "Edit Timetable Period") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Class: ${schoolClass.fullName}", style = MaterialTheme.typography.labelLarge, color = RoyalBlue600)

                ExposedDropdownMenuBox(expanded = dayMenu, onExpandedChange = { dayMenu = !dayMenu }) {
                    OutlinedTextField(
                        value = dayOfWeek,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Day of Week *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = dayMenu, onDismissRequest = { dayMenu = false }) {
                        AcademicCalculations.daysOfWeek.forEach { d ->
                            DropdownMenuItem(text = { Text(d) }, onClick = { dayOfWeek = d; dayMenu = false })
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = periodNo,
                        onValueChange = { periodNo = it },
                        label = { Text("Period # *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = roomNumber,
                        onValueChange = { roomNumber = it },
                        label = { Text("Room") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("Start Time *") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = endTime, onValueChange = { endTime = it }, label = { Text("End Time *") }, singleLine = true, modifier = Modifier.weight(1f))
                }

                if (classSubjects.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        classSubjects.forEach { sub ->
                            AssistChip(
                                onClick = {
                                    subjectName = sub.subjectName
                                    if (sub.teacherId.isNotBlank()) {
                                        selectedTeacher = teachers.find { it.id == sub.teacherId }
                                    }
                                },
                                label = { Text(sub.subjectName) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Subject Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(expanded = teacherMenu, onExpandedChange = { teacherMenu = !teacherMenu }) {
                    OutlinedTextField(
                        value = selectedTeacher?.name ?: "Unassigned",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assigned Teacher") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teacherMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = teacherMenu, onDismissRequest = { teacherMenu = false }) {
                        DropdownMenuItem(text = { Text("Unassigned") }, onClick = { selectedTeacher = null; teacherMenu = false })
                        teachers.forEach { t ->
                            DropdownMenuItem(text = { Text(t.name) }, onClick = { selectedTeacher = t; teacherMenu = false })
                        }
                    }
                }

                error?.let { Text(it, color = Crimson600, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val pNum = periodNo.toLongOrNull()
                when {
                    pNum == null || pNum !in 1..12 -> error = "Enter valid period number (1–12)."
                    subjectName.isBlank() -> error = "Subject name is required."
                    startTime.isBlank() || endTime.isBlank() -> error = "Start and end time are required."
                    else -> onSave(
                        TimetableEntry(
                            id = initial?.id ?: "",
                            classId = schoolClass.id,
                            className = schoolClass.fullName,
                            dayOfWeek = dayOfWeek,
                            periodNumber = pNum,
                            startTime = startTime.trim(),
                            endTime = endTime.trim(),
                            subjectName = subjectName.trim(),
                            teacherId = selectedTeacher?.id ?: "",
                            teacherName = selectedTeacher?.name ?: "",
                            roomNumber = roomNumber.trim()
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
