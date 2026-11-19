package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.SchoolRepository
import com.example.data.firebase.ThemePreferences
import com.example.data.model.AcademicCalculations
import com.example.data.model.AttendanceRecord
import com.example.data.model.ClassSubject
import com.example.data.model.ExamScheduleEntry
import com.example.data.model.Examination
import com.example.data.model.FeeRecord
import com.example.data.model.HomeworkItem
import com.example.data.model.NoticeItem
import com.example.data.model.SchoolClass
import com.example.data.model.SchoolSettings
import com.example.data.model.Student
import com.example.data.model.StudentMark
import com.example.data.model.Teacher
import com.example.data.model.TimetableEntry
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ErpModule(val title: String, val shortLabel: String) {
    DASHBOARD("Dashboard", "Home"),
    STUDENTS("Students", "Students"),
    CLASSES("Classes & Subjects", "Classes"),
    FEES("Fees & Receipts", "Fees"),
    ATTENDANCE("Attendance", "Attend."),
    EXAMS("Exams & Results", "Exams"),
    ADMIT_CARDS("Admit Cards", "Admit"),
    TIMETABLE("Timetable", "Schedule"),
    HOMEWORK_NOTICES("Homework & Notices", "Notices"),
    TEACHERS("Teachers", "Faculty"),
    REPORTS("Reports & Analytics", "Reports"),
    SETTINGS("School Settings", "Settings")
}

data class NotificationBanner(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val isError: Boolean = false
)

data class MonthlyChartPoint(
    val label: String,
    val value: Float
)

data class SchoolErpData(
    val settings: SchoolSettings = SchoolSettings(),
    val classes: List<SchoolClass> = emptyList(),
    val subjects: List<ClassSubject> = emptyList(),
    val teachers: List<Teacher> = emptyList(),
    val students: List<Student> = emptyList(),
    val feeRecords: List<FeeRecord> = emptyList(),
    val attendance: List<AttendanceRecord> = emptyList(),
    val exams: List<Examination> = emptyList(),
    val examSchedules: List<ExamScheduleEntry> = emptyList(),
    val studentMarks: List<StudentMark> = emptyList(),
    val timetables: List<TimetableEntry> = emptyList(),
    val homeworks: List<HomeworkItem> = emptyList(),
    val notices: List<NoticeItem> = emptyList()
) {
    val todayDate: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val todayAttendanceList: List<AttendanceRecord>
        get() = attendance.filter { it.date == todayDate }

    val presentTodayCount: Int
        get() = todayAttendanceList.count { it.status == "PRESENT" }

    val absentTodayCount: Int
        get() = todayAttendanceList.count { it.status == "ABSENT" }

    val leaveTodayCount: Int
        get() = todayAttendanceList.count { it.status == "LEAVE" }

    val todayAttendancePercent: Float
        get() = if (todayAttendanceList.isEmpty()) 0f
        else (presentTodayCount.toFloat() / todayAttendanceList.size.toFloat()) * 100f

    val totalFeeCollection: Double
        get() = feeRecords.sumOf { it.paidAmount }

    val totalPendingFees: Double
        get() = feeRecords.sumOf { it.remainingBalance }

    val recentAdmissions: List<Student>
        get() = students.sortedByDescending { it.admissionDate }.take(5)

    val recentFeePayments: List<FeeRecord>
        get() = feeRecords.filter { it.paidAmount > 0 }.sortedByDescending { it.paymentDate }.take(5)

    val upcomingExams: List<Examination>
        get() = exams.filter { it.status != "COMPLETED" }.sortedBy { it.startDate }.take(5)

    val importantNotices: List<NoticeItem>
        get() = notices.sortedByDescending { it.publicationDate }.take(5)

    val monthlyFeeCollectionChart: List<MonthlyChartPoint>
        get() {
            val map = linkedMapOf<String, Float>()
            AcademicCalculations.academicMonths2026_27.take(8).forEach { m ->
                map[m.take(3)] = 0f
            }
            feeRecords.forEach { rec ->
                if (rec.feeMonths.isNotEmpty()) {
                    val perMonth = (rec.paidAmount / rec.feeMonths.size).toFloat()
                    rec.feeMonths.forEach { m ->
                        val key = m.take(3)
                        if (map.containsKey(key)) {
                            map[key] = (map[key] ?: 0f) + perMonth
                        }
                    }
                }
            }
            return map.entries.map { MonthlyChartPoint(it.key, it.value) }
        }

    val classDistributionChart: List<MonthlyChartPoint>
        get() = classes.map { cls ->
            val count = students.count { it.classId == cls.id }.toFloat()
            MonthlyChartPoint(cls.name.replace("Class ", "C"), count)
        }

    val monthlyAdmissionChart: List<MonthlyChartPoint>
        get() {
            val monthNames = listOf("Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov")
            val monthKeys = listOf("-04-", "-05-", "-06-", "-07-", "-08-", "-09-", "-10-", "-11-")
            return monthNames.mapIndexed { idx, label ->
                val count = students.count { it.admissionDate.contains(monthKeys[idx]) }.toFloat()
                MonthlyChartPoint(label, count)
            }
        }
}

sealed interface ErpUiState {
    data object Loading : ErpUiState
    data class Success(val data: SchoolErpData) : ErpUiState
    data class Error(val message: String) : ErpUiState
}

class SchoolViewModel(
    private val repository: SchoolRepository,
    private val themePreferences: ThemePreferences,
    private val currentUserId: String
) : ViewModel() {

    private val _selectedModule = MutableStateFlow(ErpModule.DASHBOARD)
    val selectedModule: StateFlow<ErpModule> = _selectedModule.asStateFlow()

    private val _notification = MutableStateFlow<NotificationBanner?>(null)
    val notification: StateFlow<NotificationBanner?> = _notification.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultSettings()
        }
    }

    private fun <T> Flow<T>.unwrapFirestoreErrors(label: String): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w("SchoolVM", "Error observing $label", firestoreError ?: error)
        throw firestoreError ?: error
    }

    private val coreSchoolFlow = combine(
        repository.observeSettings(currentUserId).unwrapFirestoreErrors("settings"),
        repository.observeClasses(currentUserId).unwrapFirestoreErrors("classes"),
        repository.observeSubjects(currentUserId).unwrapFirestoreErrors("subjects"),
        repository.observeTeachers(currentUserId).unwrapFirestoreErrors("teachers"),
        repository.observeStudents(currentUserId).unwrapFirestoreErrors("students")
    ) { settings, classes, subjects, teachers, students ->
        SchoolErpData(
            settings = settings,
            classes = classes,
            subjects = subjects,
            teachers = teachers,
            students = students
        )
    }

    private val operationsFlow = combine(
        repository.observeFeeRecords(currentUserId).unwrapFirestoreErrors("fees"),
        repository.observeAttendance(currentUserId).unwrapFirestoreErrors("attendance"),
        repository.observeExams(currentUserId).unwrapFirestoreErrors("exams"),
        repository.observeExamSchedules(currentUserId).unwrapFirestoreErrors("exam_schedules"),
        repository.observeStudentMarks(currentUserId).unwrapFirestoreErrors("exam_marks")
    ) { fees, attendance, exams, schedules, marks ->
        Triple(fees, attendance, Triple(exams, schedules, marks))
    }

    private val communicationsFlow = combine(
        repository.observeTimetables(currentUserId).unwrapFirestoreErrors("timetables"),
        repository.observeHomeworks(currentUserId).unwrapFirestoreErrors("homeworks"),
        repository.observeNotices(currentUserId).unwrapFirestoreErrors("notices")
    ) { timetables, homeworks, notices ->
        Triple(timetables, homeworks, notices)
    }

    val uiState: StateFlow<ErpUiState> = combine(
        coreSchoolFlow,
        operationsFlow,
        communicationsFlow
    ) { core, ops, comms ->
        val (fees, attendance, examBundle) = ops
        val (exams, schedules, marks) = examBundle
        val (timetables, homeworks, notices) = comms
        ErpUiState.Success(
            core.copy(
                feeRecords = fees,
                attendance = attendance,
                exams = exams,
                examSchedules = schedules,
                studentMarks = marks,
                timetables = timetables,
                homeworks = homeworks,
                notices = notices
            )
        ) as ErpUiState
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        emit(ErpUiState.Error(root.localizedMessage ?: "Failed to load ANV School ERP data."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = ErpUiState.Loading
    )

    fun selectModule(module: ErpModule) {
        _selectedModule.value = module
    }

    fun dismissNotification() {
        _notification.value = null
    }

    fun notify(message: String, isError: Boolean = false) {
        _notification.value = NotificationBanner(message = message, isError = isError)
    }

    private fun runMutation(successMessage: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = block()
            _isBusy.value = false
            res.onSuccess {
                notify(successMessage, isError = false)
            }.onFailure { err ->
                notify(err.localizedMessage ?: "Operation failed", isError = true)
            }
        }
    }

    // Settings & Theme
    fun saveSettings(settings: SchoolSettings) {
        viewModelScope.launch {
            themePreferences.setThemeMode(settings.themeMode)
        }
        runMutation("School settings updated.") { repository.saveSettings(settings) }
    }

    fun updateThemeMode(mode: String, currentSettings: SchoolSettings) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
            repository.saveSettings(currentSettings.copy(themeMode = mode))
        }
    }

    // Classes & Subjects
    fun seedStandardClasses(existingClasses: List<SchoolClass>) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.seedStandardClasses(existingClasses).onSuccess { count ->
                val msg = if (count > 0) "Added $count standard classes (Nursery–Class 8)." else "All standard classes already exist."
                notify(msg)
            }.onFailure {
                notify(it.localizedMessage ?: "Failed to initialize classes.", isError = true)
            }
            _isBusy.value = false
        }
    }

    fun saveClass(schoolClass: SchoolClass) =
        runMutation(if (schoolClass.id.isBlank()) "Class created." else "Class updated.") {
            repository.saveClass(schoolClass)
        }

    fun deleteClass(classId: String) =
        runMutation("Class removed.") { repository.deleteClass(classId) }

    fun saveSubject(subject: ClassSubject) =
        runMutation(if (subject.id.isBlank()) "Subject added." else "Subject updated.") {
            repository.saveSubject(subject)
        }

    fun deleteSubject(subjectId: String) =
        runMutation("Subject deleted.") { repository.deleteSubject(subjectId) }

    // Teachers
    fun saveTeacher(teacher: Teacher) =
        runMutation(if (teacher.id.isBlank()) "Teacher added." else "Teacher updated.") {
            repository.saveTeacher(teacher)
        }

    fun deleteTeacher(teacherId: String) =
        runMutation("Teacher deleted.") { repository.deleteTeacher(teacherId) }

    // Students
    fun saveStudent(student: Student) =
        runMutation(if (student.id.isBlank()) "Student admitted successfully." else "Student profile updated.") {
            repository.saveStudent(student)
        }

    fun deleteStudent(studentId: String) =
        runMutation("Student record deleted.") { repository.deleteStudent(studentId) }

    // Fees
    fun saveFeeRecord(fee: FeeRecord) =
        runMutation(if (fee.id.isBlank()) "Fee payment recorded & receipt generated." else "Fee record updated.") {
            repository.saveFeeRecord(fee)
        }

    fun deleteFeeRecord(feeId: String) =
        runMutation("Fee record deleted.") { repository.deleteFeeRecord(feeId) }

    // Attendance
    fun saveAttendanceBatch(records: List<AttendanceRecord>) =
        runMutation("Attendance saved for ${records.size} student(s).") {
            repository.saveAttendanceBatch(records)
        }

    // Exams, Schedules, Marks
    fun saveExam(exam: Examination) =
        runMutation(if (exam.id.isBlank()) "Examination created." else "Examination updated.") {
            repository.saveExam(exam)
        }

    fun deleteExam(examId: String) =
        runMutation("Examination deleted.") { repository.deleteExam(examId) }

    fun saveExamSchedule(entry: ExamScheduleEntry) =
        runMutation(if (entry.id.isBlank()) "Exam subject schedule added." else "Exam schedule updated.") {
            repository.saveExamSchedule(entry)
        }

    fun deleteExamSchedule(scheduleId: String) =
        runMutation("Schedule entry removed.") { repository.deleteExamSchedule(scheduleId) }

    fun saveStudentMark(mark: StudentMark) =
        runMutation("Marks saved for ${mark.studentName}.") {
            repository.saveStudentMark(mark)
        }

    fun deleteStudentMark(markId: String) =
        runMutation("Marks deleted.") { repository.deleteStudentMark(markId) }

    // Timetable
    fun saveTimetableEntry(entry: TimetableEntry) =
        runMutation(if (entry.id.isBlank()) "Timetable period added." else "Timetable updated.") {
            repository.saveTimetableEntry(entry)
        }

    fun deleteTimetableEntry(entryId: String) =
        runMutation("Timetable period deleted.") { repository.deleteTimetableEntry(entryId) }

    // Homework & Notices
    fun saveHomework(hw: HomeworkItem) =
        runMutation(if (hw.id.isBlank()) "Homework assigned." else "Homework updated.") {
            repository.saveHomework(hw)
        }

    fun deleteHomework(id: String) =
        runMutation("Homework deleted.") { repository.deleteHomework(id) }

    fun saveNotice(notice: NoticeItem) =
        runMutation(if (notice.id.isBlank()) "Notice published." else "Notice updated.") {
            repository.saveNotice(notice)
        }

    fun deleteNotice(id: String) =
        runMutation("Notice deleted.") { repository.deleteNotice(id) }

    // Backup & Restore
    fun exportBackup(onReady: (String) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.exportAllDataJson().onSuccess { json ->
                onReady(json)
            }.onFailure {
                notify(it.localizedMessage ?: "Backup export failed.", isError = true)
            }
            _isBusy.value = false
        }
    }

    fun restoreBackup(json: String) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.restoreFromBackupJson(json).onSuccess { count ->
                notify("Restored $count records from backup.")
            }.onFailure {
                notify("Invalid or incompatible backup file.", isError = true)
            }
            _isBusy.value = false
        }
    }
}
