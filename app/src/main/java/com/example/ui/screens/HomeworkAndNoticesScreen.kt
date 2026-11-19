package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ClassSubject
import com.example.data.model.HomeworkItem
import com.example.data.model.NoticeItem
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
import com.example.ui.theme.GoldAmber500
import com.example.ui.theme.RoyalBlue600
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeworkAndNoticesScreen(
    data: SchoolErpData,
    onSaveHomework: (HomeworkItem) -> Unit,
    onDeleteHomework: (String) -> Unit,
    onSaveNotice: (NoticeItem) -> Unit,
    onDeleteNotice: (String) -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) }
    var classFilterId by remember { mutableStateOf("ALL") }
    var priorityFilter by remember { mutableStateOf("ALL") }

    var showHomeworkDialog by remember { mutableStateOf(false) }
    var editingHomework by remember { mutableStateOf<HomeworkItem?>(null) }
    var deleteHomeworkTarget by remember { mutableStateOf<HomeworkItem?>(null) }

    var showNoticeDialog by remember { mutableStateOf(false) }
    var editingNotice by remember { mutableStateOf<NoticeItem?>(null) }
    var deleteNoticeTarget by remember { mutableStateOf<NoticeItem?>(null) }

    val filteredHomeworks = remember(data.homeworks, classFilterId) {
        if (classFilterId == "ALL") data.homeworks
        else data.homeworks.filter { it.classId == classFilterId }
    }

    val filteredNotices = remember(data.notices, priorityFilter) {
        if (priorityFilter == "ALL") data.notices
        else data.notices.filter { it.priority == priorityFilter }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (activeTab == 0) {
                        editingHomework = null
                        showHomeworkDialog = true
                    } else {
                        editingNotice = null
                        showNoticeDialog = true
                    }
                },
                containerColor = RoyalBlue600,
                modifier = Modifier.testTag(if (activeTab == 0) "add_homework_fab" else "add_notice_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = if (activeTab == 0) "Add Homework" else "Add Notice")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                ErpSectionHeader(
                    title = "Homework & Notices",
                    subtitle = "Assign daily class homework & broadcast school announcements"
                )
            }
            TabRow(selectedTabIndex = activeTab) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Homework (${data.homeworks.size})") }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Notices (${data.notices.size})") }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }

                if (activeTab == 0) {
                    item {
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

                    if (filteredHomeworks.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.Assignment,
                                title = "No Homework Assigned",
                                message = "Tap + to assign class homework with subject, instructions and submission deadline.",
                                actionLabel = if (data.classes.isNotEmpty()) "Assign Homework" else null,
                                onAction = if (data.classes.isNotEmpty()) {
                                    { editingHomework = null; showHomeworkDialog = true }
                                } else null
                            )
                        }
                    } else {
                        items(filteredHomeworks, key = { it.id }) { hw ->
                            ErpPanelCard(
                                title = hw.title,
                                subtitle = "${hw.className} • ${hw.subjectName} • Due: ${hw.submissionDate.ifBlank { "TBA" }}",
                                trailing = {
                                    Row {
                                        IconButton(onClick = { editingHomework = hw; showHomeworkDialog = true }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit homework", tint = RoyalBlue600, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { deleteHomeworkTarget = hw }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete homework", tint = Crimson600, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            ) {
                                Text(hw.description, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        "Assigned: ${hw.assignedDate.ifBlank { "-" }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "By: ${hw.teacherName.ifBlank { "Class Teacher" }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RoyalBlue600,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("ALL", "NORMAL", "IMPORTANT", "URGENT").forEach { p ->
                                FilterChip(
                                    selected = priorityFilter == p,
                                    onClick = { priorityFilter = p },
                                    label = { Text(if (p == "ALL") "All Priorities" else p) }
                                )
                            }
                        }
                    }

                    if (filteredNotices.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.Campaign,
                                title = "No Notices Published",
                                message = "Tap + to publish circulars, holiday notices, or urgent school announcements.",
                                actionLabel = "Publish Notice",
                                onAction = { editingNotice = null; showNoticeDialog = true }
                            )
                        }
                    } else {
                        items(filteredNotices, key = { it.id }) { notice ->
                            val badgeColor = when (notice.priority) {
                                "URGENT" -> Crimson600
                                "IMPORTANT" -> GoldAmber500
                                else -> Emerald600
                            }
                            ErpPanelCard(
                                title = notice.title,
                                subtitle = "Published: ${notice.publicationDate} • For: ${notice.targetClasses.joinToString(", ")}",
                                trailing = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        StatusPill(notice.priority, badgeColor)
                                        IconButton(onClick = { editingNotice = notice; showNoticeDialog = true }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit notice", tint = RoyalBlue600, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { deleteNoticeTarget = notice }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete notice", tint = Crimson600, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            ) {
                                Text(notice.description, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showHomeworkDialog) {
        HomeworkFormDialog(
            initial = editingHomework,
            classes = data.classes,
            subjects = data.subjects,
            teachers = data.teachers,
            onDismiss = { showHomeworkDialog = false },
            onSave = {
                onSaveHomework(it)
                showHomeworkDialog = false
            }
        )
    }

    if (showNoticeDialog) {
        NoticeFormDialog(
            initial = editingNotice,
            classes = data.classes,
            onDismiss = { showNoticeDialog = false },
            onSave = {
                onSaveNotice(it)
                showNoticeDialog = false
            }
        )
    }

    deleteHomeworkTarget?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete Homework?",
            message = "Delete homework \"${target.title}\" for ${target.className}?",
            onConfirm = {
                onDeleteHomework(target.id)
                deleteHomeworkTarget = null
            },
            onDismiss = { deleteHomeworkTarget = null }
        )
    }

    deleteNoticeTarget?.let { target ->
        ConfirmDeleteDialog(
            title = "Delete Notice?",
            message = "Delete announcement \"${target.title}\"?",
            onConfirm = {
                onDeleteNotice(target.id)
                deleteNoticeTarget = null
            },
            onDismiss = { deleteNoticeTarget = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeworkFormDialog(
    initial: HomeworkItem?,
    classes: List<SchoolClass>,
    subjects: List<ClassSubject>,
    teachers: List<Teacher>,
    onDismiss: () -> Unit,
    onSave: (HomeworkItem) -> Unit
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var selectedClass by remember { mutableStateOf(classes.find { it.id == initial?.classId } ?: classes.firstOrNull()) }
    var classMenu by remember { mutableStateOf(false) }
    var subjectName by remember { mutableStateOf(initial?.subjectName ?: "") }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var assignedDate by remember { mutableStateOf(initial?.assignedDate?.ifBlank { today } ?: today) }
    var submissionDate by remember { mutableStateOf(initial?.submissionDate?.ifBlank { today } ?: today) }
    var teacherName by remember { mutableStateOf(initial?.teacherName ?: (teachers.firstOrNull()?.name ?: "")) }
    var teacherMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val classSubjects = remember(subjects, selectedClass) {
        if (selectedClass == null) emptyList()
        else subjects.filter { it.classId == selectedClass!!.id }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Assign Homework" else "Edit Homework") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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

                if (classSubjects.isNotEmpty()) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        classSubjects.forEach { sub ->
                            AssistChip(onClick = {
                                subjectName = sub.subjectName
                                if (sub.teacherName.isNotBlank()) teacherName = sub.teacherName
                            }, label = { Text(sub.subjectName) })
                        }
                    }
                }

                OutlinedTextField(value = subjectName, onValueChange = { subjectName = it }, label = { Text("Subject *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Homework Title *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Instructions / Work Details *") }, minLines = 2, maxLines = 4, modifier = Modifier.fillMaxWidth())

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = assignedDate, onValueChange = { assignedDate = it }, label = { Text("Assigned Date") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = submissionDate, onValueChange = { submissionDate = it }, label = { Text("Due Date") }, singleLine = true, modifier = Modifier.weight(1f))
                }

                ExposedDropdownMenuBox(expanded = teacherMenu, onExpandedChange = { teacherMenu = !teacherMenu }) {
                    OutlinedTextField(
                        value = teacherName,
                        onValueChange = { teacherName = it },
                        label = { Text("Teacher Name") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teacherMenu) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable)
                    )
                    ExposedDropdownMenu(expanded = teacherMenu, onDismissRequest = { teacherMenu = false }) {
                        teachers.forEach { t ->
                            DropdownMenuItem(text = { Text(t.name) }, onClick = { teacherName = t.name; teacherMenu = false })
                        }
                    }
                }

                error?.let { Text(it, color = Crimson600, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val cls = selectedClass
                when {
                    cls == null -> error = "Please create/select a class first."
                    subjectName.isBlank() -> error = "Subject is required."
                    title.isBlank() -> error = "Title is required."
                    description.isBlank() -> error = "Description is required."
                    else -> onSave(
                        HomeworkItem(
                            id = initial?.id ?: "",
                            classId = cls.id,
                            className = cls.fullName,
                            subjectName = subjectName.trim(),
                            title = title.trim(),
                            description = description.trim(),
                            assignedDate = assignedDate.trim(),
                            submissionDate = submissionDate.trim(),
                            teacherName = teacherName.trim()
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoticeFormDialog(
    initial: NoticeItem?,
    classes: List<SchoolClass>,
    onDismiss: () -> Unit,
    onSave: (NoticeItem) -> Unit
) {
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var publicationDate by remember { mutableStateOf(initial?.publicationDate?.ifBlank { today } ?: today) }
    var priority by remember { mutableStateOf(initial?.priority ?: "NORMAL") }
    var selectedTargets by remember { mutableStateOf(initial?.targetClasses?.toSet() ?: setOf("All Classes")) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Publish Notice" else "Edit Notice") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Notice Title *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Notice Details *") }, minLines = 3, maxLines = 5, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = publicationDate, onValueChange = { publicationDate = it }, label = { Text("Publication Date") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Text("Priority", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("NORMAL", "IMPORTANT", "URGENT").forEach { p ->
                        FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(p) })
                    }
                }

                Text("Target Classes", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedTargets.contains("All Classes"),
                        onClick = { selectedTargets = setOf("All Classes") },
                        label = { Text("All Classes") }
                    )
                    classes.forEach { cls ->
                        val label = cls.fullName
                        val isSel = selectedTargets.contains(label)
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                val next = (selectedTargets - "All Classes").toMutableSet()
                                if (isSel) next.remove(label) else next.add(label)
                                selectedTargets = if (next.isEmpty()) setOf("All Classes") else next
                            },
                            label = { Text(label) }
                        )
                    }
                }
                error?.let { Text(it, color = Crimson600, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    title.isBlank() -> error = "Notice title is required."
                    description.isBlank() -> error = "Notice description is required."
                    else -> onSave(
                        NoticeItem(
                            id = initial?.id ?: "",
                            title = title.trim(),
                            description = description.trim(),
                            targetClasses = selectedTargets.toList(),
                            publicationDate = publicationDate.trim(),
                            priority = priority
                        )
                    )
                }
            }) { Text("Publish") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
