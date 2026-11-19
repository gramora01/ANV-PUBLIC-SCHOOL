package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AcademicCalculations
import com.example.data.model.ClassSubject
import com.example.data.model.SchoolClass
import com.example.data.model.Teacher
import com.example.ui.SchoolErpData
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErpPanelCard
import com.example.ui.components.ErpSectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.Crimson600
import com.example.ui.theme.Emerald600
import com.example.ui.theme.RoyalBlue600

@Composable
fun ClassesAndSubjectsScreen(
    data: SchoolErpData,
    onSeedStandardClasses: () -> Unit,
    onSaveClass: (SchoolClass) -> Unit,
    onDeleteClass: (String) -> Unit,
    onSaveSubject: (ClassSubject) -> Unit,
    onDeleteSubject: (String) -> Unit
) {
    var editingClass by remember { mutableStateOf<SchoolClass?>(null) }
    var showClassDialog by remember { mutableStateOf(false) }
    var deleteClassTarget by remember { mutableStateOf<SchoolClass?>(null) }

    var subjectClassTarget by remember { mutableStateOf<SchoolClass?>(null) }
    var editingSubject by remember { mutableStateOf<ClassSubject?>(null) }
    var showSubjectDialog by remember { mutableStateOf(false) }
    var deleteSubjectTarget by remember { mutableStateOf<ClassSubject?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            ErpSectionHeader(
                title = "Classes & Dynamic Subjects",
                subtitle = "Supports Nursery, LKG, UKG, Classes 1–8, and any custom classes or subjects",
                action = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onSeedStandardClasses) {
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Nursery–8")
                        }
                        Button(onClick = {
                            editingClass = null
                            showClassDialog = true
                        }) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Class")
                        }
                    }
                }
            )
        }

        if (data.classes.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Class,
                    title = "No Classes Configured",
                    message = "Load standard classes (Nursery, LKG, UKG, Class 1 to Class 8) in one tap or add custom classes.",
                    actionLabel = "Load Nursery–Class 8",
                    onAction = onSeedStandardClasses
                )
            }
        } else {
            items(data.classes, key = { it.id }) { cls ->
                val classSubjects = data.subjects.filter { it.classId == cls.id }
                val studentCount = data.students.count { it.classId == cls.id }
                ErpPanelCard(
                    title = cls.fullName,
                    subtitle = "$studentCount student(s) • Fee: ${AcademicCalculations.formatInr(cls.monthlyFee)}/mo" +
                        if (cls.classTeacherName.isNotBlank()) " • In-charge: ${cls.classTeacherName}" else "",
                    trailing = {
                        Row {
                            IconButton(onClick = {
                                subjectClassTarget = cls
                                editingSubject = null
                                showSubjectDialog = true
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Add subject", tint = Emerald600)
                            }
                            IconButton(onClick = {
                                editingClass = cls
                                showClassDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit class", tint = RoyalBlue600)
                            }
                            IconButton(onClick = { deleteClassTarget = cls }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete class", tint = Crimson600)
                            }
                        }
                    }
                ) {
                    if (classSubjects.isEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "No subjects assigned to this class yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = {
                                subjectClassTarget = cls
                                editingSubject = null
                                showSubjectDialog = true
                            }) {
                                Text("+ Add Subject")
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            classSubjects.forEach { subj ->
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Book, null, tint = RoyalBlue600, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(subj.subjectName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "Teacher: ${subj.teacherName.ifBlank { "Unassigned" }} • Max Marks: ${subj.defaultMaxMarks}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(onClick = {
                                            subjectClassTarget = cls
                                            editingSubject = subj
                                            showSubjectDialog = true
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit subject", tint = RoyalBlue600, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { deleteSubjectTarget = subj }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove subject", tint = Crimson600, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showClassDialog) {
        ClassFormDialog(
            initial = editingClass,
            teachers = data.teachers,
            nextOrder = data.classes.size.toLong(),
            onDismiss = { showClassDialog = false },
            onSave = {
                onSaveClass(it)
                showClassDialog = false
            }
        )
    }

    if (showSubjectDialog && subjectClassTarget != null) {
        SubjectFormDialog(
            schoolClass = subjectClassTarget!!,
            initial = editingSubject,
            teachers = data.teachers,
            onDismiss = { showSubjectDialog = false },
            onSave = {
                onSaveSubject(it)
                showSubjectDialog = false
            }
        )
    }

    deleteClassTarget?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete Class ${target.fullName}?",
            message = "This removes the class definition. Existing student records will keep their class name.",
            onConfirm = {
                onDeleteClass(target.id)
                deleteClassTarget = null
            },
            onDismiss = { deleteClassTarget = null }
        )
    }

    deleteSubjectTarget?.let { target ->
        ConfirmDeleteDialog(
            title = "Remove Subject ${target.subjectName}?",
            message = "Remove ${target.subjectName} from ${target.className}?",
            onConfirm = {
                onDeleteSubject(target.id)
                deleteSubjectTarget = null
            },
            onDismiss = { deleteSubjectTarget = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassFormDialog(
    initial: SchoolClass?,
    teachers: List<Teacher>,
    nextOrder: Long,
    onDismiss: () -> Unit,
    onSave: (SchoolClass) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var section by remember { mutableStateOf(initial?.section ?: "") }
    var orderText by remember { mutableStateOf((initial?.displayOrder ?: nextOrder).toString()) }
    var feeText by remember { mutableStateOf(if (initial != null) initial.monthlyFee.toInt().toString() else "1500") }
    var selectedTeacher by remember { mutableStateOf(teachers.find { it.id == initial?.classTeacherId }) }
    var teacherMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Create New Class" else "Edit Class") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Class Name * (e.g. Nursery, Class 1)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = section, onValueChange = { section = it }, label = { Text("Section") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = orderText, onValueChange = { orderText = it }, label = { Text("Order") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(
                    value = feeText,
                    onValueChange = { feeText = it },
                    label = { Text("Monthly Tuition Fee (INR) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = teacherMenu, onExpandedChange = { teacherMenu = !teacherMenu }) {
                    OutlinedTextField(
                        value = selectedTeacher?.name ?: "None (Optional)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Class Teacher") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teacherMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = teacherMenu, onDismissRequest = { teacherMenu = false }) {
                        DropdownMenuItem(text = { Text("None") }, onClick = { selectedTeacher = null; teacherMenu = false })
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
                val fee = feeText.toDoubleOrNull()
                when {
                    name.isBlank() -> error = "Class name is required."
                    fee == null || fee < 0 -> error = "Enter a valid monthly fee."
                    else -> onSave(
                        SchoolClass(
                            id = initial?.id ?: "",
                            name = name.trim(),
                            section = section.trim(),
                            displayOrder = orderText.toLongOrNull() ?: nextOrder,
                            monthlyFee = fee,
                            classTeacherId = selectedTeacher?.id ?: "",
                            classTeacherName = selectedTeacher?.name ?: ""
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectFormDialog(
    schoolClass: SchoolClass,
    initial: ClassSubject?,
    teachers: List<Teacher>,
    onDismiss: () -> Unit,
    onSave: (ClassSubject) -> Unit
) {
    var subjectName by remember { mutableStateOf(initial?.subjectName ?: "") }
    var maxMarksText by remember { mutableStateOf((initial?.defaultMaxMarks ?: 100L).toString()) }
    var selectedTeacher by remember { mutableStateOf(teachers.find { it.id == initial?.teacherId }) }
    var teacherMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Subject to ${schoolClass.fullName}" else "Edit Subject") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = subjectName, onValueChange = { subjectName = it }, label = { Text("Subject Name * (Custom)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = maxMarksText,
                    onValueChange = { maxMarksText = it },
                    label = { Text("Default Max Marks *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = teacherMenu, onExpandedChange = { teacherMenu = !teacherMenu }) {
                    OutlinedTextField(
                        value = selectedTeacher?.name ?: "Assign Teacher (Optional)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject Teacher") },
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
                val maxM = maxMarksText.toLongOrNull()
                when {
                    subjectName.isBlank() -> error = "Subject name is required."
                    maxM == null || maxM < 1 -> error = "Enter valid max marks (>= 1)."
                    else -> onSave(
                        ClassSubject(
                            id = initial?.id ?: "",
                            classId = schoolClass.id,
                            className = schoolClass.fullName,
                            subjectName = subjectName.trim(),
                            teacherId = selectedTeacher?.id ?: "",
                            teacherName = selectedTeacher?.name ?: "",
                            defaultMaxMarks = maxM
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
