package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AcademicCalculations
import com.example.data.model.SchoolClass
import com.example.data.model.Teacher
import com.example.ui.SchoolErpData
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErpPanelCard
import com.example.ui.components.ErpSearchField
import com.example.ui.components.ErpSectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.Crimson600
import com.example.ui.theme.RoyalBlue600
import com.example.ui.theme.Violet600
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TeachersScreen(
    data: SchoolErpData,
    onSaveTeacher: (Teacher) -> Unit,
    onDeleteTeacher: (String) -> Unit
) {
    var search by remember { mutableStateOf("") }
    var editingTeacher by remember { mutableStateOf<Teacher?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf<Teacher?>(null) }
    var deleteCandidate by remember { mutableStateOf<Teacher?>(null) }

    if (selectedProfile != null) {
        val current = data.teachers.find { it.id == selectedProfile!!.id } ?: selectedProfile!!
        BackHandler { selectedProfile = null }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedProfile = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Teacher Profile", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = { editingTeacher = current; showForm = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = RoyalBlue600)
                }
                IconButton(onClick = { deleteCandidate = current }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Crimson600)
                }
            }
            ErpPanelCard(title = current.name, subtitle = current.qualification.ifBlank { "Faculty Member" }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StudentAvatar(name = current.name, photoBase64 = null, sizeDp = 64)
                    Spacer(Modifier.width(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Mobile: ${current.mobile}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Email: ${current.email.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall)
                        Text("Joined: ${current.joiningDate.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall)
                        if (current.salary > 0) {
                            Text("Monthly Salary: ${AcademicCalculations.formatInr(current.salary)}", style = MaterialTheme.typography.bodySmall, color = RoyalBlue600)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Address: ${current.address.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(10.dp))
                Text("Assigned Classes:", style = MaterialTheme.typography.labelLarge)
                Text(current.assignedClasses.joinToString(", ").ifBlank { "None assigned" }, style = MaterialTheme.typography.bodySmall, color = RoyalBlue600)
                Spacer(Modifier.height(8.dp))
                Text("Assigned Subjects:", style = MaterialTheme.typography.labelLarge)
                Text(current.assignedSubjects.joinToString(", ").ifBlank { "None assigned" }, style = MaterialTheme.typography.bodySmall, color = Violet600)
            }
        }
    } else {
        val filtered = remember(data.teachers, search) {
            val q = search.trim().lowercase()
            data.teachers.filter {
                q.isEmpty() || it.name.lowercase().contains(q) ||
                    it.assignedSubjects.any { s -> s.lowercase().contains(q) } ||
                    it.assignedClasses.any { c -> c.lowercase().contains(q) }
            }
        }
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { editingTeacher = null; showForm = true },
                    containerColor = RoyalBlue600
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Teacher", tint = MaterialTheme.colorScheme.onPrimary)
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
                        title = "Faculty & Teachers",
                        subtitle = "${data.teachers.size} teacher(s) • Assign classes & subjects"
                    )
                    ErpSearchField(
                        query = search,
                        onQueryChange = { search = it },
                        placeholder = "Search teacher name, class, or subject..."
                    )
                }

                if (filtered.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.School,
                            title = "No Teachers Added Yet",
                            message = "Add faculty members with their subjects, classes, mobile numbers, and addresses.",
                            actionLabel = "Add Teacher",
                            onAction = { editingTeacher = null; showForm = true }
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { teacher ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedProfile = teacher },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StudentAvatar(name = teacher.name, photoBase64 = null, sizeDp = 48)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(teacher.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${teacher.qualification.ifBlank { "Teacher" }} • ${teacher.mobile}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (teacher.assignedClasses.isNotEmpty() || teacher.assignedSubjects.isNotEmpty()) {
                                        Spacer(Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (teacher.assignedClasses.isNotEmpty()) {
                                                StatusPill(teacher.assignedClasses.take(2).joinToString(", "), RoyalBlue600)
                                            }
                                            if (teacher.assignedSubjects.isNotEmpty()) {
                                                StatusPill(teacher.assignedSubjects.take(2).joinToString(", "), Violet600)
                                            }
                                        }
                                    }
                                }
                                IconButton(onClick = { editingTeacher = teacher; showForm = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = RoyalBlue600)
                                }
                                IconButton(onClick = { deleteCandidate = teacher }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Crimson600)
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
        TeacherFormDialog(
            initial = editingTeacher,
            classes = data.classes,
            availableSubjects = data.subjects.map { it.subjectName }.distinct(),
            onDismiss = { showForm = false },
            onSave = {
                onSaveTeacher(it)
                showForm = false
            }
        )
    }

    deleteCandidate?.let { t ->
        ConfirmDeleteDialog(
            title = "Delete Teacher ${t.name}?",
            message = "Are you sure you want to remove ${t.name} from the faculty directory?",
            onConfirm = {
                onDeleteTeacher(t.id)
                if (selectedProfile?.id == t.id) selectedProfile = null
                deleteCandidate = null
            },
            onDismiss = { deleteCandidate = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TeacherFormDialog(
    initial: Teacher?,
    classes: List<SchoolClass>,
    availableSubjects: List<String>,
    onDismiss: () -> Unit,
    onSave: (Teacher) -> Unit
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var qualification by remember { mutableStateOf(initial?.qualification ?: "") }
    var mobile by remember { mutableStateOf(initial?.mobile ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var joiningDate by remember { mutableStateOf(initial?.joiningDate?.ifBlank { today } ?: today) }
    var salaryText by remember { mutableStateOf(if ((initial?.salary ?: 0.0) > 0) initial!!.salary.toInt().toString() else "") }
    var selectedClasses by remember { mutableStateOf(initial?.assignedClasses ?: emptyList()) }
    var subjectsText by remember { mutableStateOf(initial?.assignedSubjects?.joinToString(", ") ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add New Teacher" else "Edit Teacher") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Teacher Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = qualification, onValueChange = { qualification = it }, label = { Text("Qualification (e.g. M.A., B.Ed.)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = joiningDate, onValueChange = { joiningDate = it }, label = { Text("Joining Date") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = salaryText,
                        onValueChange = { salaryText = it },
                        label = { Text("Salary (INR)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (classes.isNotEmpty()) {
                    Text("Assign Classes:", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        classes.forEach { cls ->
                            val picked = selectedClasses.contains(cls.fullName)
                            FilterChip(
                                selected = picked,
                                onClick = {
                                    selectedClasses = if (picked) selectedClasses - cls.fullName else selectedClasses + cls.fullName
                                },
                                label = { Text(cls.fullName) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = subjectsText,
                    onValueChange = { subjectsText = it },
                    label = { Text("Assigned Subjects (comma-separated)") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (availableSubjects.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        availableSubjects.take(8).forEach { subj ->
                            FilterChip(
                                selected = subjectsText.split(",").map { it.trim() }.contains(subj),
                                onClick = {
                                    val current = subjectsText.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
                                    if (current.contains(subj)) current.remove(subj) else current.add(subj)
                                    subjectsText = current.joinToString(", ")
                                },
                                label = { Text(subj) }
                            )
                        }
                    }
                }
                error?.let { Text(it, color = Crimson600, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    name.isBlank() -> error = "Teacher name is required."
                    mobile.isBlank() -> error = "Mobile number is required."
                    else -> {
                        val parsedSubjects = subjectsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                        onSave(
                            Teacher(
                                id = initial?.id ?: "",
                                name = name.trim(),
                                qualification = qualification.trim(),
                                mobile = mobile.trim(),
                                address = address.trim(),
                                email = email.trim(),
                                joiningDate = joiningDate.trim(),
                                assignedClasses = selectedClasses,
                                assignedSubjects = parsedSubjects,
                                salary = salaryText.toDoubleOrNull() ?: 0.0
                            )
                        )
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
