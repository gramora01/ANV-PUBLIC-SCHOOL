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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AcademicCalculations
import com.example.ui.SchoolErpData
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErpPanelCard
import com.example.ui.components.ErpSectionHeader
import com.example.ui.theme.RoyalBlue600
import com.example.util.PdfAndPrintManager

enum class ErpReportType(val label: String) {
    STUDENT_REPORT("Student Directory"),
    FEE_COLLECTION("Fee Collection"),
    PENDING_FEES("Pending Fees"),
    ATTENDANCE_REPORT("Attendance Summary"),
    EXAM_REPORT("Examination Summary"),
    CLASS_PERFORMANCE("Class Performance"),
    ADMISSION_REPORT("Admissions Report")
}

private data class GeneratedReportPayload(
    val title: String,
    val subtitle: String,
    val columns: List<String>,
    val rows: List<List<String>>,
    val footer: String
)

@Composable
fun ReportsScreen(
    data: SchoolErpData,
    onNotify: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var selectedReport by remember { mutableStateOf(ErpReportType.STUDENT_REPORT) }
    var classFilterId by remember { mutableStateOf("ALL") }
    var pendingPdfBytes by remember { mutableStateOf<ByteArray?>(null) }

    val savePdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val bytes = pendingPdfBytes
        if (uri != null && bytes != null) {
            PdfAndPrintManager.writePdfToUri(context, uri, bytes)
                .onSuccess { onNotify("${selectedReport.label} PDF saved.", false) }
                .onFailure { onNotify("Failed to save report PDF.", true) }
        }
        pendingPdfBytes = null
    }

    val payload = remember(selectedReport, classFilterId, data) {
        val scopedStudents = if (classFilterId == "ALL") data.students else data.students.filter { it.classId == classFilterId }
        val scopedFees = if (classFilterId == "ALL") data.feeRecords else data.feeRecords.filter { it.classId == classFilterId }
        val scopedAttendance = if (classFilterId == "ALL") data.attendance else data.attendance.filter { it.classId == classFilterId }
        val scopeLabel = if (classFilterId == "ALL") "All Classes" else (data.classes.find { it.id == classFilterId }?.fullName ?: "Filtered")

        when (selectedReport) {
            ErpReportType.STUDENT_REPORT -> GeneratedReportPayload(
                title = "STUDENT DIRECTORY REPORT",
                subtitle = scopeLabel,
                columns = listOf("Adm No", "Student Name", "Class", "Roll", "Father Name", "Mobile"),
                rows = scopedStudents.map {
                    listOf(it.admissionNumber, it.name, it.className, it.rollNumber, it.fatherName, it.mobile)
                },
                footer = "Total Students: ${scopedStudents.size}"
            )

            ErpReportType.FEE_COLLECTION -> GeneratedReportPayload(
                title = "FEE COLLECTION REPORT",
                subtitle = scopeLabel,
                columns = listOf("Receipt", "Date", "Student", "Class", "Paid", "Mode"),
                rows = scopedFees.filter { it.paidAmount > 0 }.map {
                    listOf(it.receiptNumber, it.paymentDate, it.studentName, it.className, AcademicCalculations.formatInr(it.paidAmount), it.paymentMode)
                },
                footer = "Total Collected: ${AcademicCalculations.formatInr(scopedFees.sumOf { it.paidAmount })}"
            )

            ErpReportType.PENDING_FEES -> {
                val pending = scopedFees.filter { it.remainingBalance > 0.0 }
                GeneratedReportPayload(
                    title = "PENDING FEE DUES REPORT",
                    subtitle = scopeLabel,
                    columns = listOf("Receipt", "Student", "Class", "Total", "Paid", "Balance"),
                    rows = pending.map {
                        listOf(
                            it.receiptNumber,
                            it.studentName,
                            it.className,
                            AcademicCalculations.formatInr(it.totalAmount),
                            AcademicCalculations.formatInr(it.paidAmount),
                            AcademicCalculations.formatInr(it.remainingBalance)
                        )
                    },
                    footer = "Total Pending Dues: ${AcademicCalculations.formatInr(pending.sumOf { it.remainingBalance })}"
                )
            }

            ErpReportType.ATTENDANCE_REPORT -> {
                val rows = scopedStudents.map { stu ->
                    val recs = scopedAttendance.filter { it.studentId == stu.id }
                    val p = recs.count { it.status == "PRESENT" }
                    val a = recs.count { it.status == "ABSENT" }
                    val l = recs.count { it.status == "LEAVE" }
                    val pct = if (recs.isEmpty()) "0%" else "${(p * 100) / recs.size}%"
                    listOf(stu.name, stu.className, p.toString(), a.toString(), l.toString(), pct)
                }
                GeneratedReportPayload(
                    title = "STUDENT ATTENDANCE REPORT",
                    subtitle = scopeLabel,
                    columns = listOf("Student", "Class", "Present", "Absent", "Leave", "Att %"),
                    rows = rows,
                    footer = "Records Analysed: ${scopedAttendance.size}"
                )
            }

            ErpReportType.EXAM_REPORT -> {
                val allResults = data.exams.filter { classFilterId == "ALL" || it.classId == classFilterId }.flatMap { exam ->
                    val clsStudents = data.students.filter { it.classId == exam.classId }
                    val schedules = data.examSchedules.filter { it.examId == exam.id }
                    AcademicCalculations.computeClassResults(exam, clsStudents, schedules, data.studentMarks)
                }
                GeneratedReportPayload(
                    title = "EXAMINATION RESULTS REPORT",
                    subtitle = scopeLabel,
                    columns = listOf("Exam", "Student", "Class", "Marks", "%", "Rank / Grade"),
                    rows = allResults.map { r ->
                        listOf(
                            r.exam.name,
                            r.student.name,
                            r.student.className,
                            "${r.totalObtained.toInt()}/${r.totalMax}",
                            "${"%.1f".format(r.percentage)}%",
                            "#${r.rank} (${r.grade})"
                        )
                    },
                    footer = "Evaluated Result Cards: ${allResults.size}"
                )
            }

            ErpReportType.CLASS_PERFORMANCE -> {
                val targetClasses = if (classFilterId == "ALL") data.classes else data.classes.filter { it.id == classFilterId }
                val rows = targetClasses.map { cls ->
                    val clsStu = data.students.filter { it.classId == cls.id }
                    val clsFees = data.feeRecords.filter { it.classId == cls.id }
                    val clsAtt = data.attendance.filter { it.classId == cls.id }
                    val attPct = if (clsAtt.isEmpty()) "-" else "${(clsAtt.count { it.status == "PRESENT" } * 100) / clsAtt.size}%"
                    val clsMarks = data.studentMarks.filter { it.classId == cls.id && !it.absent }
                    val avgScore = if (clsMarks.isEmpty()) "-" else {
                        val obt = clsMarks.sumOf { it.obtainedMarks }
                        val max = clsMarks.sumOf { it.maxMarks }.coerceAtLeast(1L)
                        "${"%.1f".format((obt / max.toDouble()) * 100.0)}%"
                    }
                    listOf(
                        cls.fullName,
                        clsStu.size.toString(),
                        attPct,
                        avgScore,
                        AcademicCalculations.formatInr(clsFees.sumOf { it.paidAmount }),
                        AcademicCalculations.formatInr(clsFees.sumOf { it.remainingBalance })
                    )
                }
                GeneratedReportPayload(
                    title = "CLASS-WISE PERFORMANCE REPORT",
                    subtitle = scopeLabel,
                    columns = listOf("Class", "Students", "Att %", "Avg Score", "Collected", "Pending"),
                    rows = rows,
                    footer = "Classes Covered: ${targetClasses.size}"
                )
            }

            ErpReportType.ADMISSION_REPORT -> {
                val sorted = scopedStudents.sortedByDescending { it.admissionDate }
                GeneratedReportPayload(
                    title = "NEW ADMISSIONS REPORT",
                    subtitle = scopeLabel,
                    columns = listOf("Adm Date", "Adm No", "Student Name", "Class", "Prev School", "Mobile"),
                    rows = sorted.map {
                        listOf(
                            it.admissionDate.ifBlank { "-" },
                            it.admissionNumber,
                            it.name,
                            it.className,
                            it.previousSchool.ifBlank { "Fresh" },
                            it.mobile
                        )
                    },
                    footer = "Total Admissions: ${sorted.size}"
                )
            }
        }
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
                title = "Reports & Analytics",
                subtitle = "7 live ERP reports with A4 PDF download & direct print"
            )
            Text("Report Category", style = MaterialTheme.typography.labelLarge, color = RoyalBlue600)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ErpReportType.entries.forEach { report ->
                    FilterChip(
                        selected = selectedReport == report,
                        onClick = { selectedReport = report },
                        label = { Text(report.label) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = classFilterId == "ALL",
                    onClick = { classFilterId = "ALL" },
                    label = { Text("All Classes") }
                )
                data.classes.forEach { cls ->
                    FilterChip(
                        selected = classFilterId == cls.id,
                        onClick = { classFilterId = cls.id },
                        label = { Text(cls.fullName) }
                    )
                }
            }
        }

        item {
            ErpPanelCard(title = payload.title, subtitle = "${payload.subtitle} • ${payload.footer}") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            pendingPdfBytes = PdfAndPrintManager.generateTableReportPdf(
                                context = context,
                                settings = data.settings,
                                reportTitle = payload.title,
                                subtitle = payload.subtitle,
                                columns = payload.columns,
                                rows = payload.rows,
                                summaryFooter = payload.footer
                            )
                            savePdfLauncher.launch("${selectedReport.name.lowercase()}_anv.pdf")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("download_report_pdf_button")
                    ) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Download PDF")
                    }
                    OutlinedButton(
                        onClick = {
                            val bytes = PdfAndPrintManager.generateTableReportPdf(
                                context = context,
                                settings = data.settings,
                                reportTitle = payload.title,
                                subtitle = payload.subtitle,
                                columns = payload.columns,
                                rows = payload.rows,
                                summaryFooter = payload.footer
                            )
                            PdfAndPrintManager.printPdfBytes(context, payload.title, bytes)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("print_report_button")
                    ) {
                        Icon(Icons.Default.Print, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Print Report")
                    }
                }
            }
        }

        if (payload.rows.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Assessment,
                    title = "No Data for ${selectedReport.label}",
                    message = "Add records in the corresponding module to populate this report automatically."
                )
            }
        } else {
            item {
                ErpPanelCard(
                    title = "Live Preview (${payload.rows.size} rows)",
                    subtitle = payload.columns.joinToString(" | ")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        payload.rows.take(40).forEachIndexed { idx, row ->
                            Column {
                                Text(
                                    text = "${idx + 1}. ${row.firstOrNull().orEmpty()} — ${row.getOrNull(1).orEmpty()}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = payload.columns.drop(2).zip(row.drop(2)) { c, v -> "$c: $v" }.joinToString("  •  "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                        if (payload.rows.size > 40) {
                            Text(
                                "+ ${payload.rows.size - 40} more rows included in PDF / Print export.",
                                style = MaterialTheme.typography.labelMedium,
                                color = RoyalBlue600
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
