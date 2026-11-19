package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.FeeRecord
import com.example.data.model.SchoolClass
import com.example.data.model.SchoolSettings
import com.example.data.model.Student
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
import com.example.ui.theme.Navy800
import com.example.ui.theme.RoyalBlue600
import com.example.util.PdfAndPrintManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FeesScreen(
    data: SchoolErpData,
    onSaveFee: (FeeRecord) -> Unit,
    onDeleteFee: (String) -> Unit,
    onNotify: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }
    var showForm by remember { mutableStateOf(false) }
    var editingFee by remember { mutableStateOf<FeeRecord?>(null) }
    var deleteCandidate by remember { mutableStateOf<FeeRecord?>(null) }
    var previewReceipt by remember { mutableStateOf<FeeRecord?>(null) }
    var pendingPdfBytes by remember { mutableStateOf<ByteArray?>(null) }

    val savePdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val bytes = pendingPdfBytes
        if (uri != null && bytes != null) {
            PdfAndPrintManager.writePdfToUri(context, uri, bytes)
                .onSuccess { onNotify("Fee receipt PDF saved successfully.", false) }
                .onFailure { onNotify("Failed to save PDF.", true) }
        }
        pendingPdfBytes = null
    }

    if (previewReceipt != null) {
        val fee = previewReceipt!!
        val stu = data.students.find { it.id == fee.studentId }
        BackHandler { previewReceipt = null }
        ReceiptPreviewView(
            settings = data.settings,
            fee = fee,
            student = stu,
            onBack = { previewReceipt = null },
            onDownloadPdf = {
                pendingPdfBytes = PdfAndPrintManager.generateFeeReceiptPdf(context, data.settings, fee, stu)
                savePdfLauncher.launch("${fee.receiptNumber}.pdf")
            },
            onPrint = {
                val bytes = PdfAndPrintManager.generateFeeReceiptPdf(context, data.settings, fee, stu)
                PdfAndPrintManager.printPdfBytes(context, "FeeReceipt_${fee.receiptNumber}", bytes)
            }
        )
    } else {
        val filtered = remember(data.feeRecords, search, statusFilter) {
            val q = search.trim().lowercase()
            data.feeRecords.filter { rec ->
                val matchesStatus = statusFilter == "ALL" || rec.status == statusFilter
                val matchesQ = q.isEmpty() ||
                    rec.studentName.lowercase().contains(q) ||
                    rec.className.lowercase().contains(q) ||
                    rec.receiptNumber.lowercase().contains(q)
                matchesStatus && matchesQ
            }
        }

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        if (data.students.isEmpty()) {
                            onNotify("Please admit students first before collecting fees.", true)
                        } else {
                            editingFee = null
                            showForm = true
                        }
                    },
                    containerColor = RoyalBlue600
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Collect Fee", tint = MaterialTheme.colorScheme.onPrimary)
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
                        title = "Fees & Receipts",
                        subtitle = "Collected: ${AcademicCalculations.formatInr(data.totalFeeCollection)} • Pending: ${AcademicCalculations.formatInr(data.totalPendingFees)}"
                    )
                    ErpSearchField(
                        query = search,
                        onQueryChange = { search = it },
                        placeholder = "Search student name, class, or receipt no..."
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ALL" to "All", "PAID" to "Paid", "PARTIAL" to "Partial", "PENDING" to "Pending").forEach { (key, label) ->
                            FilterChip(
                                selected = statusFilter == key,
                                onClick = { statusFilter = key },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.ReceiptLong,
                            title = "No Fee Records Found",
                            message = "Collect 3-month quarterly cycles or custom monthly fees with auto balance & PDF receipts.",
                            actionLabel = if (data.students.isNotEmpty()) "Record Fee Payment" else null,
                            onAction = { editingFee = null; showForm = true }
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { fee ->
                        val statusColor = when (fee.status) {
                            "PAID" -> Emerald600
                            "PARTIAL" -> GoldAmber500
                            else -> Crimson600
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(fee.studentName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${fee.className} • ${fee.receiptNumber} • ${fee.paymentDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StatusPill(fee.status, statusColor)
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Months: ${fee.feeMonths.joinToString(", ").ifBlank { fee.cycleLabel }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RoyalBlue600
                                )
                                Spacer(Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total: ${AcademicCalculations.formatInr(fee.totalAmount)}", style = MaterialTheme.typography.bodySmall)
                                    Text("Paid: ${AcademicCalculations.formatInr(fee.paidAmount)}", style = MaterialTheme.typography.bodySmall, color = Emerald600, fontWeight = FontWeight.Bold)
                                    Text("Balance: ${AcademicCalculations.formatInr(fee.remainingBalance)}", style = MaterialTheme.typography.bodySmall, color = if (fee.remainingBalance > 0) Crimson600 else Emerald600, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { previewReceipt = fee }, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.ReceiptLong, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Receipt / PDF")
                                    }
                                    IconButton(onClick = { editingFee = fee; showForm = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit fee", tint = RoyalBlue600)
                                    }
                                    IconButton(onClick = { deleteCandidate = fee }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete fee", tint = Crimson600)
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(76.dp)) }
            }
        }
    }

    if (showForm) {
        FeeFormDialog(
            initial = editingFee,
            students = data.students,
            classes = data.classes,
            settings = data.settings,
            nextSeq = data.feeRecords.size + 1,
            onDismiss = { showForm = false },
            onSave = {
                onSaveFee(it)
                showForm = false
            }
        )
    }

    deleteCandidate?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete Fee Receipt ${target.receiptNumber}?",
            message = "Remove fee record for ${target.studentName}?",
            onConfirm = {
                onDeleteFee(target.id)
                deleteCandidate = null
            },
            onDismiss = { deleteCandidate = null }
        )
    }
}

@Composable
private fun ReceiptPreviewView(
    settings: SchoolSettings,
    fee: FeeRecord,
    student: Student?,
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
            Text("Official Fee Receipt", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        }

        ErpPanelCard(
            title = settings.schoolName,
            subtitle = "${settings.schoolAddress} • Session ${settings.academicSession}"
        ) {
            Surface(color = Navy800.copy(alpha = 0.08f), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Receipt No", style = MaterialTheme.typography.labelSmall)
                        Text(fee.receiptNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Payment Date", style = MaterialTheme.typography.labelSmall)
                        Text(fee.paymentDate, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            ReceiptLine("Student Name", fee.studentName)
            ReceiptLine("Father's Name", student?.fatherName ?: "-")
            ReceiptLine("Class", fee.className)
            ReceiptLine("Fee Months", fee.feeMonths.joinToString(", ").ifBlank { fee.cycleLabel })
            ReceiptLine("Payment Mode", fee.paymentMode)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ReceiptLine("Total Amount", AcademicCalculations.formatInr(fee.totalAmount))
            ReceiptLine("Paid Amount", AcademicCalculations.formatInr(fee.paidAmount))
            ReceiptLine("Remaining Balance", AcademicCalculations.formatInr(fee.remainingBalance))
            Spacer(Modifier.height(12.dp))
            Text("Authorized Signature: ${settings.authorizedSignatory}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text("Print Receipt")
            }
        }
    }
}

@Composable
private fun ReceiptLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FeeFormDialog(
    initial: FeeRecord?,
    students: List<Student>,
    classes: List<SchoolClass>,
    settings: SchoolSettings,
    nextSeq: Int,
    onDismiss: () -> Unit,
    onSave: (FeeRecord) -> Unit
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var studentQuery by remember { mutableStateOf("") }
    var selectedStudent by remember {
        mutableStateOf(students.find { it.id == initial?.studentId } ?: students.firstOrNull())
    }
    var studentMenu by remember { mutableStateOf(false) }
    var selectedMonths by remember { mutableStateOf(initial?.feeMonths ?: AcademicCalculations.quarterlyCycles.first().second) }
    var cycleLabel by remember { mutableStateOf(initial?.cycleLabel?.ifBlank { "Q1 (Apr–Jun 2026)" } ?: "Q1 (Apr–Jun 2026)") }

    val defaultMonthlyFee = classes.find { it.id == selectedStudent?.classId }?.monthlyFee ?: 1500.0
    var totalText by remember {
        mutableStateOf(
            initial?.totalAmount?.toInt()?.toString() ?: (defaultMonthlyFee * selectedMonths.size).toInt().toString()
        )
    }
    var paidText by remember { mutableStateOf(initial?.paidAmount?.toInt()?.toString() ?: totalText) }
    var paymentDate by remember { mutableStateOf(initial?.paymentDate?.ifBlank { today } ?: today) }
    var paymentMode by remember { mutableStateOf(initial?.paymentMode ?: "Cash") }
    var receiptNumber by remember {
        mutableStateOf(initial?.receiptNumber ?: "${settings.receiptPrefix}${"%04d".format(nextSeq)}")
    }
    var remarks by remember { mutableStateOf(initial?.remarks ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    val filteredStudents = remember(students, studentQuery) {
        val q = studentQuery.trim().lowercase()
        if (q.isEmpty()) students else students.filter {
            it.name.lowercase().contains(q) || it.className.lowercase().contains(q) || it.admissionNumber.lowercase().contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Collect Student Fee" else "Update Fee Record") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = studentQuery,
                    onValueChange = { studentQuery = it },
                    label = { Text("Filter Student by Name or Class") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = studentMenu, onExpandedChange = { studentMenu = !studentMenu }) {
                    OutlinedTextField(
                        value = selectedStudent?.let { "${it.name} (${it.className})" } ?: "Select Student",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Student *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = studentMenu, onDismissRequest = { studentMenu = false }) {
                        filteredStudents.forEach { stu ->
                            DropdownMenuItem(
                                text = { Text("${stu.name} • ${stu.className} (Roll ${stu.rollNumber})") },
                                onClick = {
                                    selectedStudent = stu
                                    val mFee = classes.find { it.id == stu.classId }?.monthlyFee ?: 1500.0
                                    if (initial == null) {
                                        val autoTotal = (mFee * selectedMonths.size).toInt().toString()
                                        totalText = autoTotal
                                        paidText = autoTotal
                                    }
                                    studentMenu = false
                                }
                            )
                        }
                    }
                }

                Text("3-Month Fee Cycles (Quick Select):", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AcademicCalculations.quarterlyCycles.forEach { (label, months) ->
                        FilterChip(
                            selected = selectedMonths == months,
                            onClick = {
                                selectedMonths = months
                                cycleLabel = label
                                val mFee = classes.find { it.id == selectedStudent?.classId }?.monthlyFee ?: 1500.0
                                val calc = (mFee * months.size).toInt().toString()
                                totalText = calc
                                paidText = calc
                            },
                            label = { Text(label.substringBefore(" (")) }
                        )
                    }
                }

                Text("Or Select Custom Months:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    AcademicCalculations.academicMonths2026_27.forEach { m ->
                        val picked = selectedMonths.contains(m)
                        FilterChip(
                            selected = picked,
                            onClick = {
                                val next = if (picked) selectedMonths - m else selectedMonths + m
                                selectedMonths = next
                                cycleLabel = "Custom (${next.size} mo)"
                                val mFee = classes.find { it.id == selectedStudent?.classId }?.monthlyFee ?: 1500.0
                                val calc = (mFee * next.size).toInt().toString()
                                totalText = calc
                            },
                            label = { Text(m.take(3)) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalText,
                        onValueChange = { totalText = it },
                        label = { Text("Total (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = paidText,
                        onValueChange = { paidText = it },
                        label = { Text("Paid (₹) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                val totVal = totalText.toDoubleOrNull() ?: 0.0
                val paidVal = (paidText.toDoubleOrNull() ?: 0.0).coerceAtMost(totVal)
                val balVal = (totVal - paidVal).coerceAtLeast(0.0)
                Text(
                    "Auto Balance: ${AcademicCalculations.formatInr(balVal)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (balVal > 0) Crimson600 else Emerald600
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = receiptNumber, onValueChange = { receiptNumber = it }, label = { Text("Receipt No *") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = paymentDate, onValueChange = { paymentDate = it }, label = { Text("Date *") }, singleLine = true, modifier = Modifier.weight(1f))
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Cash", "UPI", "Bank Transfer", "Cheque").forEach { mode ->
                        FilterChip(selected = paymentMode == mode, onClick = { paymentMode = mode }, label = { Text(mode) })
                    }
                }

                OutlinedTextField(value = remarks, onValueChange = { remarks = it }, label = { Text("Remarks") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = Crimson600, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val stu = selectedStudent
                val tot = totalText.toDoubleOrNull()
                val paid = paidText.toDoubleOrNull()
                when {
                    stu == null -> error = "Select a student."
                    selectedMonths.isEmpty() -> error = "Select at least 1 fee month."
                    tot == null || tot < 0 -> error = "Enter valid total fee."
                    paid == null || paid < 0 || paid > tot -> error = "Paid amount must be between 0 and Total."
                    receiptNumber.isBlank() -> error = "Receipt number is required."
                    else -> {
                        val bal = (tot - paid).coerceAtLeast(0.0)
                        onSave(
                            FeeRecord(
                                id = initial?.id ?: "",
                                studentId = stu.id,
                                studentName = stu.name,
                                classId = stu.classId,
                                className = stu.className,
                                rollNumber = stu.rollNumber,
                                admissionNumber = stu.admissionNumber,
                                receiptNumber = receiptNumber.trim(),
                                feeMonths = selectedMonths,
                                cycleLabel = cycleLabel,
                                totalAmount = tot,
                                paidAmount = paid,
                                remainingBalance = bal,
                                paymentDate = paymentDate.trim(),
                                paymentMode = paymentMode,
                                remarks = remarks.trim()
                            )
                        )
                    }
                }
            }) { Text("Save Receipt") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
