package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AcademicCalculations
import com.example.data.model.SchoolClass
import com.example.data.model.Student
import com.example.ui.SchoolErpData
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErpPanelCard
import com.example.ui.components.ErpSearchField
import com.example.ui.components.ErpSectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.Crimson600
import com.example.ui.theme.Emerald600
import com.example.ui.theme.RoyalBlue600
import com.example.util.ImageHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StudentsScreen(
    data: SchoolErpData,
    onSaveStudent: (Student) -> Unit,
    onDeleteStudent: (String) -> Unit,
    onNotify: (String, Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedClassFilter by remember { mutableStateOf("ALL") }
    var editingStudent by remember { mutableStateOf<Student?>(null) }
    var showFormDialog by remember { mutableStateOf(false) }
    var profileStudent by remember { mutableStateOf<Student?>(null) }
    var deleteCandidate by remember { mutableStateOf<Student?>(null) }

    if (profileStudent != null) {
        val current = data.students.find { it.id == profileStudent!!.id } ?: profileStudent!!
        BackHandler { profileStudent = null }
        StudentProfileView(
            student = current,
            data = data,
            onBack = { profileStudent = null },
            onEdit = {
                editingStudent = current
                showFormDialog = true
            },
            onDelete = { deleteCandidate = current }
        )
    } else {
        val filtered = remember(data.students, searchQuery, selectedClassFilter) {
            data.students.filter { s ->
                val matchesClass = selectedClassFilter == "ALL" || s.classId == selectedClassFilter
                val q = searchQuery.trim().lowercase()
                val matchesQuery = q.isEmpty() ||
                    s.name.lowercase().contains(q) ||
                    s.className.lowercase().contains(q) ||
                    s.admissionNumber.lowercase().contains(q) ||
                    s.fatherName.lowercase().contains(q)
                matchesClass && matchesQuery
            }
        }

        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        if (data.classes.isEmpty()) {
                            onNotify("Please create or load classes first in Classes & Subjects.", true)
                        } else {
                            editingStudent = null
                            showFormDialog = true
                        }
                    },
                    containerColor = RoyalBlue600,
                    modifier = Modifier.testTag("add_student_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Student", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    ErpSectionHeader(
                        title = "Student Directory",
                        subtitle = "${data.students.size} total students • Search by name, class, or admission no."
                    )
                    ErpSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search student name, class, or admission no..."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedClassFilter == "ALL",
                            onClick = { selectedClassFilter = "ALL" },
                            label = { Text("All (${data.students.size})") }
                        )
                        data.classes.forEach { cls ->
                            val count = data.students.count { it.classId == cls.id }
                            FilterChip(
                                selected = selectedClassFilter == cls.id,
                                onClick = { selectedClassFilter = cls.id },
                                label = { Text("${cls.name} ($count)") }
                            )
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.Person,
                            title = if (data.students.isEmpty()) "No Students Admitted Yet" else "No Matching Students",
                            message = if (data.classes.isEmpty()) {
                                "Create classes in Classes & Subjects first, then admit students."
                            } else {
                                "Tap the + button to admit a new student with full profile and photograph."
                            },
                            actionLabel = if (data.classes.isNotEmpty()) "Admit First Student" else null,
                            onAction = {
                                editingStudent = null
                                showFormDialog = true
                            }
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { student ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { profileStudent = student },
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StudentAvatar(name = student.name, photoBase64 = student.photoBase64, sizeDp = 50)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(student.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${student.className} • Roll #${student.rollNumber} • Adm: ${student.admissionNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = RoyalBlue600
                                    )
                                    Text(
                                        "S/D of ${student.fatherName} • ${student.mobile}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = {
                                    editingStudent = student
                                    showFormDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit student", tint = RoyalBlue600)
                                }
                                IconButton(onClick = { deleteCandidate = student }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete student", tint = Crimson600)
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(76.dp)) }
            }
        }
    }

    if (showFormDialog) {
        StudentFormDialog(
            initial = editingStudent,
            classes = data.classes,
            onDismiss = { showFormDialog = false },
            onSave = { student ->
                onSaveStudent(student)
                showFormDialog = false
            }
        )
    }

    deleteCandidate?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete Student Record?",
            message = "Are you sure you want to remove ${target.name} (${target.className})? This cannot be undone.",
            onConfirm = {
                onDeleteStudent(target.id)
                if (profileStudent?.id == target.id) profileStudent = null
                deleteCandidate = null
            },
            onDismiss = { deleteCandidate = null }
        )
    }
}

@Composable
private fun StudentProfileView(
    student: Student,
    data: SchoolErpData,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val stuFees = data.feeRecords.filter { it.studentId == student.id }
    val totalPaid = stuFees.sumOf { it.paidAmount }
    val totalPending = stuFees.sumOf { it.remainingBalance }
    val stuAttendance = data.attendance.filter { it.studentId == student.id }
    val presentDays = stuAttendance.count { it.status == "PRESENT" }
    val attPct = if (stuAttendance.isEmpty()) 0 else ((presentDays * 100f) / stuAttendance.size).toInt()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to list")
                }
                Text("Student Profile", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit")
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Crimson600)
                }
            }
        }

        item {
            ErpPanelCard(title = student.name, subtitle = "${student.className} • Roll #${student.rollNumber}") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StudentAvatar(name = student.name, photoBase64 = student.photoBase64, sizeDp = 72)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatusPill("Adm. No: ${student.admissionNumber}", RoyalBlue600)
                        Text("Gender: ${student.gender} • DOB: ${student.dob.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall)
                        Text("Mobile: ${student.mobile}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))
                ProfileFieldRow("Father's Name", student.fatherName)
                ProfileFieldRow("Mother's Name", student.motherName.ifBlank { "-" })
                ProfileFieldRow("Admission Date", student.admissionDate.ifBlank { "-" })
                ProfileFieldRow("Previous School", student.previousSchool.ifBlank { "-" })
                ProfileFieldRow("Account Number", student.accountNumber.ifBlank { "-" })
                ProfileFieldRow("Address", student.address.ifBlank { "-" })
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Attendance", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$attPct%", style = MaterialTheme.typography.headlineSmall, color = Emerald600, fontWeight = FontWeight.Bold)
                        Text("$presentDays / ${stuAttendance.size} days", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Fee Paid", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(AcademicCalculations.formatInr(totalPaid), style = MaterialTheme.typography.headlineSmall, color = RoyalBlue600, fontWeight = FontWeight.Bold)
                        Text("Due: ${AcademicCalculations.formatInr(totalPending)}", style = MaterialTheme.typography.bodySmall, color = if (totalPending > 0) Crimson600 else Emerald600)
                    }
                }
            }
        }

        item {
            ErpPanelCard(title = "Fee Payment History", subtitle = "${stuFees.size} receipt(s)") {
                if (stuFees.isEmpty()) {
                    Text("No fee records for this student yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    stuFees.forEach { f ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${f.receiptNumber} (${f.paymentDate})", style = MaterialTheme.typography.titleSmall)
                                Text(f.feeMonths.joinToString(", ").ifBlank { f.cycleLabel }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(AcademicCalculations.formatInr(f.paidAmount), style = MaterialTheme.typography.titleSmall, color = Emerald600)
                                if (f.remainingBalance > 0) {
                                    Text("Bal ${AcademicCalculations.formatInr(f.remainingBalance)}", style = MaterialTheme.typography.labelSmall, color = Crimson600)
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileFieldRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.42f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.58f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentFormDialog(
    initial: Student?,
    classes: List<SchoolClass>,
    onDismiss: () -> Unit,
    onSave: (Student) -> Unit
) {
    val context = LocalContext.current
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var fatherName by remember { mutableStateOf(initial?.fatherName ?: "") }
    var motherName by remember { mutableStateOf(initial?.motherName ?: "") }
    var dob by remember { mutableStateOf(initial?.dob ?: "") }
    var gender by remember { mutableStateOf(initial?.gender ?: "Male") }
    var selectedClass by remember {
        mutableStateOf(classes.find { it.id == initial?.classId } ?: classes.firstOrNull())
    }
    var rollNumber by remember { mutableStateOf(initial?.rollNumber ?: "") }
    var admissionNumber by remember { mutableStateOf(initial?.admissionNumber ?: "ANV-26-${(100..999).random()}") }
    var mobile by remember { mutableStateOf(initial?.mobile ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var admissionDate by remember { mutableStateOf(initial?.admissionDate?.ifBlank { today } ?: today) }
    var previousSchool by remember { mutableStateOf(initial?.previousSchool ?: "") }
    var accountNumber by remember { mutableStateOf(initial?.accountNumber ?: "") }
    var photoBase64 by remember { mutableStateOf(initial?.photoBase64) }
    var classDropdownExpanded by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            photoBase64 = ImageHelper.uriToCompressedBase64(context, uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Admit New Student" else "Edit Student Details") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StudentAvatar(name = name.ifBlank { "New" }, photoBase64 = photoBase64, sizeDp = 56)
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (photoBase64 == null) "Upload Photo" else "Change Photo")
                    }
                }

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Student Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = fatherName, onValueChange = { fatherName = it }, label = { Text("Father's Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = motherName, onValueChange = { motherName = it }, label = { Text("Mother's Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = dob, onValueChange = { dob = it }, label = { Text("DOB (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = admissionDate, onValueChange = { admissionDate = it }, label = { Text("Adm Date") }, singleLine = true, modifier = Modifier.weight(1f))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Male", "Female", "Other").forEach { g ->
                        FilterChip(selected = gender == g, onClick = { gender = g }, label = { Text(g) })
                    }
                }

                ExposedDropdownMenuBox(expanded = classDropdownExpanded, onExpandedChange = { classDropdownExpanded = !classDropdownExpanded }) {
                    OutlinedTextField(
                        value = selectedClass?.fullName ?: "Select Class",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Class *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = classDropdownExpanded, onDismissRequest = { classDropdownExpanded = false }) {
                        classes.forEach { cls ->
                            DropdownMenuItem(
                                text = { Text(cls.fullName) },
                                onClick = {
                                    selectedClass = cls
                                    classDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = rollNumber, onValueChange = { rollNumber = it }, label = { Text("Roll No *") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = admissionNumber, onValueChange = { admissionNumber = it }, label = { Text("Adm. No *") }, singleLine = true, modifier = Modifier.weight(1f))
                }

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = previousSchool, onValueChange = { previousSchool = it }, label = { Text("Previous School") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = accountNumber, onValueChange = { accountNumber = it }, label = { Text("Bank Account Number") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                if (formError != null) {
                    Text(formError!!, color = Crimson600, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val cls = selectedClass
                when {
                    name.isBlank() -> formError = "Student name is required."
                    fatherName.isBlank() -> formError = "Father's name is required."
                    cls == null -> formError = "Please select a class."
                    rollNumber.isBlank() -> formError = "Roll number is required."
                    admissionNumber.isBlank() -> formError = "Admission number is required."
                    mobile.isBlank() -> formError = "Mobile number is required."
                    else -> {
                        onSave(
                            Student(
                                id = initial?.id ?: "",
                                name = name.trim(),
                                fatherName = fatherName.trim(),
                                motherName = motherName.trim(),
                                dob = dob.trim(),
                                gender = gender,
                                classId = cls.id,
                                className = cls.fullName,
                                rollNumber = rollNumber.trim(),
                                admissionNumber = admissionNumber.trim(),
                                mobile = mobile.trim(),
                                address = address.trim(),
                                admissionDate = admissionDate.trim(),
                                previousSchool = previousSchool.trim(),
                                accountNumber = accountNumber.trim(),
                                photoBase64 = photoBase64
                            )
                        )
                    }
                }
            }) {
                Text(if (initial == null) "Admit Student" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
