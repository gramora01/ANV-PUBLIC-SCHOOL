package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceRecord
import com.example.data.model.Student
import com.example.ui.SchoolErpData
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErpPanelCard
import com.example.ui.components.ErpSectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.Crimson600
import com.example.ui.theme.Emerald600
import com.example.ui.theme.GoldAmber500
import com.example.util.PdfAndPrintManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    data: SchoolErpData,
    onSaveAttendance: (List<AttendanceRecord>) -> Unit,
    onNotify: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var tabIndex by remember { mutableIntStateOf(0) }
    var selectedDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var selectedMonth by remember { mutableStateOf(SimpleDateFormat("yyyy-MM", Locale.US).format(Date())) }
    var selectedClass by remember(data.classes) { mutableStateOf(data.classes.firstOrNull()) }
    var classMenu by remember { mutableStateOf(false) }
    var historyStudent by remember { mutableStateOf<Student?>(null) }
    var pendingPdfBytes by remember { mutableStateOf<ByteArray?>(null) }

    val savePdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val bytes = pendingPdfBytes
        if (uri != null && bytes != null) {
            PdfAndPrintManager.writePdfToUri(context, uri, bytes)
                .onSuccess { onNotify("Attendance report PDF saved.", false) }
                .onFailure { onNotify("Failed to save PDF.", true) }
        }
        pendingPdfBytes = null
    }

    val classStudents = remember(data.students, selectedClass) {
        if (selectedClass == null) emptyList()
        else data.students.filter { it.classId == selectedClass!!.id }
    }

    val draftStatuses = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(selectedDate, selectedClass, data.attendance, classStudents) {
        draftStatuses.clear()
        classStudents.forEach { s ->
            val existing = data.attendance.find { it.studentId == s.id && it.date == selectedDate }
            draftStatuses[s.id] = existing?.status ?: "PRESENT"
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabIndex) {
            Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Daily Attendance") })
            Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Monthly Report") })
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                ErpSectionHeader(
                    title = if (tabIndex == 0) "Mark Daily Attendance" else "Monthly Attendance Report",
                    subtitle = "Track Present, Absent & Leave • Edit any date anytime"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = classMenu,
                        onExpandedChange = { classMenu = !classMenu },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = selectedClass?.fullName ?: "No Classes",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Class") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classMenu) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = classMenu, onDismissRequest = { classMenu = false }) {
                            data.classes.forEach { cls ->
                                DropdownMenuItem(text = { Text(cls.fullName) }, onClick = { selectedClass = cls; classMenu = false })
                            }
                        }
                    }

                    if (tabIndex == 0) {
                        OutlinedTextField(
                            value = selectedDate,
                            onValueChange = { selectedDate = it },
                            label = { Text("Date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        OutlinedTextField(
                            value = selectedMonth,
                            onValueChange = { selectedMonth = it },
                            label = { Text("Month (YYYY-MM)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (classStudents.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.FactCheck,
                        title = "No Students in Selected Class",
                        message = "Select a class that has admitted students to mark or view attendance."
                    )
                }
            } else if (tabIndex == 0) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { classStudents.forEach { draftStatuses[it.id] = "PRESENT" } },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mark All Present")
                        }
                        Button(
                            onClick = {
                                val records = classStudents.map { stu ->
                                    AttendanceRecord(
                                        studentId = stu.id,
                                        studentName = stu.name,
                                        rollNumber = stu.rollNumber,
                                        classId = stu.classId,
                                        className = stu.className,
                                        date = selectedDate.trim(),
                                        monthKey = selectedDate.trim().take(7),
                                        status = draftStatuses[stu.id] ?: "PRESENT"
                                    )
                                }
                                onSaveAttendance(records)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save (${classStudents.size})")
                        }
                    }
                }

                items(classStudents, key = { it.id }) { stu ->
                    val curStatus = draftStatuses[stu.id] ?: "PRESENT"
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${stu.rollNumber}. ${stu.name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text("Adm: ${stu.admissionNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { historyStudent = stu }) {
                                    Icon(Icons.Default.History, contentDescription = "Attendance history")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("PRESENT" to "Present", "ABSENT" to "Absent", "LEAVE" to "Leave").forEach { (key, label) ->
                                    FilterChip(
                                        selected = curStatus == key,
                                        onClick = { draftStatuses[stu.id] = key },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Monthly Report Tab
                item {
                    val rows = classStudents.map { stu ->
                        val recs = data.attendance.filter { it.studentId == stu.id && it.monthKey == selectedMonth }
                        val p = recs.count { it.status == "PRESENT" }
                        val a = recs.count { it.status == "ABSENT" }
                        val l = recs.count { it.status == "LEAVE" }
                        val pct = if (recs.isEmpty()) 0 else ((p * 100f) / recs.size).toInt()
                        listOf(stu.rollNumber, stu.name, p.toString(), a.toString(), l.toString(), "$pct%")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val bytes = PdfAndPrintManager.generateTableReportPdf(
                                    context,
                                    data.settings,
                                    "Monthly Attendance Report",
                                    "${selectedClass?.fullName ?: ""} ($selectedMonth)",
                                    listOf("Roll", "Student Name", "Present", "Absent", "Leave", "Att %"),
                                    rows
                                )
                                pendingPdfBytes = bytes
                                savePdfLauncher.launch("Attendance_${selectedClass?.name}_$selectedMonth.pdf")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Download PDF")
                        }
                        OutlinedButton(
                            onClick = {
                                val bytes = PdfAndPrintManager.generateTableReportPdf(
                                    context,
                                    data.settings,
                                    "Monthly Attendance Report",
                                    "${selectedClass?.fullName ?: ""} ($selectedMonth)",
                                    listOf("Roll", "Student Name", "Present", "Absent", "Leave", "Att %"),
                                    rows
                                )
                                PdfAndPrintManager.printPdfBytes(context, "Attendance_$selectedMonth", bytes)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Print Report")
                        }
                    }
                }

                items(classStudents, key = { it.id }) { stu ->
                    val recs = data.attendance.filter { it.studentId == stu.id && it.monthKey == selectedMonth }
                    val p = recs.count { it.status == "PRESENT" }
                    val a = recs.count { it.status == "ABSENT" }
                    val l = recs.count { it.status == "LEAVE" }
                    val pct = if (recs.isEmpty()) 0 else ((p * 100f) / recs.size).toInt()
                    ErpPanelCard(
                        title = "${stu.rollNumber}. ${stu.name}",
                        subtitle = "${recs.size} recorded day(s) in $selectedMonth",
                        trailing = { StatusPill("$pct%", if (pct >= 75) Emerald600 else if (pct >= 50) GoldAmber500 else Crimson600) }
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Present: $p", color = Emerald600, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("Absent: $a", color = Crimson600, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("Leave: $l", color = GoldAmber500, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { historyStudent = stu }) { Text("History") }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    historyStudent?.let { stu ->
        val history = data.attendance.filter { it.studentId == stu.id }.sortedByDescending { it.date }
        val p = history.count { it.status == "PRESENT" }
        val pct = if (history.isEmpty()) 0 else ((p * 100f) / history.size).toInt()
        AlertDialog(
            onDismissRequest = { historyStudent = null },
            title = { Text("${stu.name} — Attendance ($pct%)") },
            text = {
                if (history.isEmpty()) {
                    Text("No attendance history recorded yet.")
                } else {
                    LazyColumn(modifier = Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(history, key = { it.id }) { rec ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(rec.date, style = MaterialTheme.typography.bodySmall)
                                StatusPill(
                                    rec.status,
                                    when (rec.status) {
                                        "PRESENT" -> Emerald600
                                        "ABSENT" -> Crimson600
                                        else -> GoldAmber500
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { historyStudent = null }) { Text("Close") } }
        )
    }
}
