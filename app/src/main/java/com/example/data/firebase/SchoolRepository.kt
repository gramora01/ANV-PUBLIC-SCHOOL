package com.example.data.firebase

import android.content.Context
import com.example.R
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
import com.example.data.model.buildCreateTimestamps
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class SchoolRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Administrator must sign in with Google before accessing school records.")
    }

    private fun userRoot(uid: String) = db.collection("users").document(uid)

    private inline fun <reified T> observeCollection(
        userId: String,
        collectionName: String
    ): Flow<List<T>> = flow {
        val activeUid = auth.currentUser?.uid
        if (activeUid.isNullOrBlank() || activeUid != userId) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$userId/$collectionName"
        emitAll(
            userRoot(userId).collection(collectionName)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(T::class.java, ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeSettings(userId: String): Flow<SchoolSettings> =
        observeCollection<SchoolSettings>(userId, "settings").map { list ->
            list.firstOrNull { it.id == "school_config" } ?: SchoolSettings(userId = userId)
        }

    fun observeClasses(userId: String): Flow<List<SchoolClass>> =
        observeCollection<SchoolClass>(userId, "classes").map { list ->
            list.sortedWith(compareBy<SchoolClass> { it.displayOrder }.thenBy { it.name })
        }

    fun observeSubjects(userId: String): Flow<List<ClassSubject>> =
        observeCollection<ClassSubject>(userId, "class_subjects").map { list ->
            list.sortedBy { it.subjectName }
        }

    fun observeTeachers(userId: String): Flow<List<Teacher>> =
        observeCollection<Teacher>(userId, "teachers").map { list ->
            list.sortedBy { it.name }
        }

    fun observeStudents(userId: String): Flow<List<Student>> =
        observeCollection<Student>(userId, "students").map { list ->
            list.sortedWith(compareBy<Student> { it.className }.thenBy { it.rollNumber.toIntOrNull() ?: 9999 }.thenBy { it.name })
        }

    fun observeFeeRecords(userId: String): Flow<List<FeeRecord>> =
        observeCollection<FeeRecord>(userId, "fee_records").map { list ->
            list.sortedByDescending { it.paymentDate }
        }

    fun observeAttendance(userId: String): Flow<List<AttendanceRecord>> =
        observeCollection<AttendanceRecord>(userId, "attendance").map { list ->
            list.sortedByDescending { it.date }
        }

    fun observeExams(userId: String): Flow<List<Examination>> =
        observeCollection<Examination>(userId, "exams").map { list ->
            list.sortedByDescending { it.startDate }
        }

    fun observeExamSchedules(userId: String): Flow<List<ExamScheduleEntry>> =
        observeCollection<ExamScheduleEntry>(userId, "exam_schedules").map { list ->
            list.sortedBy { it.examDate }
        }

    fun observeStudentMarks(userId: String): Flow<List<StudentMark>> =
        observeCollection(userId, "exam_marks")

    fun observeTimetables(userId: String): Flow<List<TimetableEntry>> =
        observeCollection<TimetableEntry>(userId, "timetables").map { list ->
            list.sortedBy { it.periodNumber }
        }

    fun observeHomeworks(userId: String): Flow<List<HomeworkItem>> =
        observeCollection<HomeworkItem>(userId, "homeworks").map { list ->
            list.sortedByDescending { it.submissionDate }
        }

    fun observeNotices(userId: String): Flow<List<NoticeItem>> =
        observeCollection<NoticeItem>(userId, "notices").map { list ->
            list.sortedByDescending { it.publicationDate }
        }

    // --- SETTINGS ---
    suspend fun ensureDefaultSettings(): Result<Unit> = runCatchingFirestore(OperationType.CREATE, "settings/school_config") {
        val uid = requireUserId()
        val ref = userRoot(uid).collection("settings").document("school_config")
        val snap = ref.get().await()
        if (!snap.exists()) {
            val payload = settingsToMap(SchoolSettings(userId = uid), isCreate = true, uid = uid)
            ref.set(payload).await()
        }
    }

    suspend fun saveSettings(settings: SchoolSettings): Result<Unit> =
        runCatchingFirestore(OperationType.WRITE, "settings/school_config") {
            val uid = requireUserId()
            val ref = userRoot(uid).collection("settings").document("school_config")
            val exists = ref.get().await().exists()
            if (exists) {
                ref.update(settingsToMap(settings, isCreate = false, uid = uid)).await()
            } else {
                ref.set(settingsToMap(settings, isCreate = true, uid = uid)).await()
            }
        }

    private fun settingsToMap(s: SchoolSettings, isCreate: Boolean, uid: String): Map<String, Any> {
        val base = mutableMapOf<String, Any?>(
            "schoolName" to s.schoolName.trim().ifBlank { "ANV PUBLIC SCHOOL" }.take(150),
            "schoolAddress" to s.schoolAddress.trim().ifBlank { "Nagaliya Mev, Gajraula, Uttar Pradesh, India" }.take(250),
            "academicSession" to s.academicSession.trim().ifBlank { "2026–2027" }.take(40),
            "phone" to s.phone.trim().take(40),
            "email" to s.email.trim().take(120),
            "affiliationNote" to s.affiliationNote.trim().take(200),
            "receiptPrefix" to s.receiptPrefix.trim().take(30),
            "authorizedSignatory" to s.authorizedSignatory.trim().take(100),
            "themeMode" to s.themeMode.takeIf { it in listOf("SYSTEM", "LIGHT", "DARK") }.orEmpty().ifBlank { "SYSTEM" },
            "logoBase64" to s.logoBase64?.take(80000),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isCreate) {
            base["userId"] = uid
            base["createdAt"] = FieldValue.serverTimestamp()
        }
        @Suppress("UNCHECKED_CAST")
        return base.filterValues { it != null } as Map<String, Any>
    }

    // --- CLASSES ---
    suspend fun seedStandardClasses(existingClasses: List<SchoolClass>): Result<Int> =
        runCatchingFirestore(OperationType.CREATE, "classes") {
            val uid = requireUserId()
            val defaults = listOf(
                Triple("Nursery", 0L, 1200.0),
                Triple("LKG", 1L, 1300.0),
                Triple("UKG", 2L, 1400.0),
                Triple("Class 1", 3L, 1500.0),
                Triple("Class 2", 4L, 1600.0),
                Triple("Class 3", 5L, 1700.0),
                Triple("Class 4", 6L, 1800.0),
                Triple("Class 5", 7L, 1900.0),
                Triple("Class 6", 8L, 2100.0),
                Triple("Class 7", 9L, 2300.0),
                Triple("Class 8", 10L, 2500.0)
            )
            val existingNames = existingClasses.map { it.name.trim().lowercase() }.toSet()
            var added = 0
            val batch = db.batch()
            defaults.forEach { (name, order, fee) ->
                if (!existingNames.contains(name.lowercase())) {
                    val docId = "std_${name.lowercase().replace(" ", "_")}"
                    val ref = userRoot(uid).collection("classes").document(docId)
                    batch.set(
                        ref,
                        mapOf(
                            "userId" to uid,
                            "name" to name,
                            "section" to "",
                            "displayOrder" to order,
                            "monthlyFee" to fee,
                            "classTeacherId" to "",
                            "classTeacherName" to "",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    added++
                }
            }
            if (added > 0) batch.commit().await()
            added
        }

    suspend fun saveClass(schoolClass: SchoolClass): Result<Unit> =
        runCatchingFirestore(OperationType.WRITE, "classes") {
            val uid = requireUserId()
            require(schoolClass.name.isNotBlank()) { "Class name is required" }
            val col = userRoot(uid).collection("classes")
            val isUpdate = schoolClass.id.isNotBlank()
            val ref = if (isUpdate) col.document(schoolClass.id) else col.document()
            val map = mutableMapOf<String, Any>(
                "name" to schoolClass.name.trim().take(60),
                "section" to schoolClass.section.trim().take(20),
                "displayOrder" to schoolClass.displayOrder.coerceIn(0L, 1000L),
                "monthlyFee" to schoolClass.monthlyFee.coerceIn(0.0, 1000000.0),
                "classTeacherId" to schoolClass.classTeacherId.trim().take(128),
                "classTeacherName" to schoolClass.classTeacherName.trim().take(100),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isUpdate) {
                ref.update(map).await()
            } else {
                map["userId"] = uid
                map["createdAt"] = FieldValue.serverTimestamp()
                ref.set(map).await()
            }
        }

    suspend fun deleteClass(classId: String): Result<Unit> =
        deleteDocument("classes", classId)

    // --- CLASS SUBJECTS ---
    suspend fun saveSubject(subject: ClassSubject): Result<Unit> =
        runCatchingFirestore(OperationType.WRITE, "class_subjects") {
            val uid = requireUserId()
            require(subject.subjectName.isNotBlank()) { "Subject name is required" }
            require(subject.classId.isNotBlank()) { "Class selection is required" }
            val col = userRoot(uid).collection("class_subjects")
            val isUpdate = subject.id.isNotBlank()
            val ref = if (isUpdate) col.document(subject.id) else col.document()
            val map = mutableMapOf<String, Any>(
                "classId" to subject.classId,
                "className" to subject.className.trim().take(80),
                "subjectName" to subject.subjectName.trim().take(80),
                "teacherId" to subject.teacherId.trim().take(128),
                "teacherName" to subject.teacherName.trim().take(100),
                "defaultMaxMarks" to subject.defaultMaxMarks.coerceIn(1L, 1000L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isUpdate) {
                ref.update(map).await()
            } else {
                map["userId"] = uid
                map["createdAt"] = FieldValue.serverTimestamp()
                ref.set(map).await()
            }
        }

    suspend fun deleteSubject(subjectId: String): Result<Unit> =
        deleteDocument("class_subjects", subjectId)

    // --- TEACHERS ---
    suspend fun saveTeacher(teacher: Teacher): Result<Unit> =
        runCatchingFirestore(OperationType.WRITE, "teachers") {
            val uid = requireUserId()
            require(teacher.name.isNotBlank()) { "Teacher name is required" }
            require(teacher.mobile.isNotBlank()) { "Teacher mobile number is required" }
            val col = userRoot(uid).collection("teachers")
            val isUpdate = teacher.id.isNotBlank()
            val ref = if (isUpdate) col.document(teacher.id) else col.document()
            val map = mutableMapOf<String, Any>(
                "name" to teacher.name.trim().take(120),
                "qualification" to teacher.qualification.trim().take(120),
                "mobile" to teacher.mobile.trim().take(30),
                "address" to teacher.address.trim().take(250),
                "email" to teacher.email.trim().take(120),
                "joiningDate" to teacher.joiningDate.trim().take(30),
                "assignedClasses" to teacher.assignedClasses.take(25),
                "assignedSubjects" to teacher.assignedSubjects.take(25),
                "salary" to teacher.salary.coerceIn(0.0, 10000000.0),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isUpdate) {
                ref.update(map).await()
            } else {
                map["userId"] = uid
                map["createdAt"] = FieldValue.serverTimestamp()
                ref.set(map).await()
            }
        }

    suspend fun deleteTeacher(teacherId: String): Result<Unit> =
        deleteDocument("teachers", teacherId)

    // --- STUDENTS ---
    suspend fun saveStudent(student: Student): Result<Unit> =
        runCatchingFirestore(OperationType.WRITE, "students") {
            val uid = requireUserId()
            require(student.name.isNotBlank()) { "Student name is required" }
            require(student.fatherName.isNotBlank()) { "Father's name is required" }
            require(student.classId.isNotBlank()) { "Class is required" }
            require(student.rollNumber.isNotBlank()) { "Roll number is required" }
            require(student.admissionNumber.isNotBlank()) { "Admission number is required" }
            require(student.mobile.isNotBlank()) { "Mobile number is required" }
            val col = userRoot(uid).collection("students")
            val isUpdate = student.id.isNotBlank()
            val ref = if (isUpdate) col.document(student.id) else col.document()
            val rawMap = mutableMapOf<String, Any?>(
                "name" to student.name.trim().take(120),
                "fatherName" to student.fatherName.trim().take(120),
                "motherName" to student.motherName.trim().take(120),
                "dob" to student.dob.trim().take(30),
                "gender" to student.gender.takeIf { it in listOf("Male", "Female", "Other") }.orEmpty().ifBlank { "Male" },
                "classId" to student.classId,
                "className" to student.className.trim().take(80),
                "rollNumber" to student.rollNumber.trim().take(40),
                "admissionNumber" to student.admissionNumber.trim().take(50),
                "mobile" to student.mobile.trim().take(30),
                "address" to student.address.trim().take(300),
                "admissionDate" to student.admissionDate.trim().take(30),
                "previousSchool" to student.previousSchool.trim().take(150),
                "accountNumber" to student.accountNumber.trim().take(60),
                "photoBase64" to student.photoBase64?.take(80000),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isUpdate) {
                @Suppress("UNCHECKED_CAST")
                ref.update(rawMap.filterValues { it != null } as Map<String, Any>).await()
            } else {
                rawMap["userId"] = uid
                rawMap["createdAt"] = FieldValue.serverTimestamp()
                @Suppress("UNCHECKED_CAST")
                ref.set(rawMap.filterValues { it != null } as Map<String, Any>).await()
            }
        }

    suspend fun deleteStudent(studentId: String): Result<Unit> =
        deleteDocument("students", studentId)

    // --- FEES ---
    suspend fun saveFeeRecord(fee: FeeRecord): Result<Unit> =
        runCatchingFirestore(OperationType.WRITE, "fee_records") {
            val uid = requireUserId()
            require(fee.studentId.isNotBlank()) { "Student selection is required" }
            require(fee.receiptNumber.isNotBlank()) { "Receipt number is required" }
            require(fee.paymentDate.isNotBlank()) { "Payment date is required" }
            val total = fee.totalAmount.coerceIn(0.0, 10000000.0)
            val paid = fee.paidAmount.coerceIn(0.0, total)
            val balance = (total - paid).coerceAtLeast(0.0)
            val computedStatus = when {
                paid <= 0.0 && total > 0.0 -> "PENDING"
                balance > 0.0 -> "PARTIAL"
                else -> "PAID"
            }
            val col = userRoot(uid).collection("fee_records")
            val isUpdate = fee.id.isNotBlank()
            val ref = if (isUpdate) col.document(fee.id) else col.document()
            val map = mutableMapOf<String, Any>(
                "studentId" to fee.studentId,
                "studentName" to fee.studentName.trim().take(120),
                "classId" to fee.classId,
                "className" to fee.className.trim().take(80),
                "rollNumber" to fee.rollNumber.trim().take(40),
                "admissionNumber" to fee.admissionNumber.trim().take(50),
                "receiptNumber" to fee.receiptNumber.trim().take(60),
                "feeMonths" to fee.feeMonths.take(12),
                "cycleLabel" to fee.cycleLabel.trim().take(80),
                "totalAmount" to total,
                "paidAmount" to paid,
                "remainingBalance" to balance,
                "paymentDate" to fee.paymentDate.trim().take(30),
                "paymentMode" to fee.paymentMode.trim().take(40).ifBlank { "Cash" },
                "status" to computedStatus,
                "remarks" to fee.remarks.trim().take(250),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isUpdate) {
                ref.update(map).await()
            } else {
                map["userId"] = uid
                map["createdAt"] = FieldValue.serverTimestamp()
                ref.set(map).await()
            }
        }

    suspend fun deleteFeeRecord(feeId: String): Result<Unit> =
        deleteDocument("fee_records", feeId)

    // --- ATTENDANCE ---
    suspend fun saveAttendanceBatch(records: List<AttendanceRecord>): Result<Unit> =
        runCatchingFirestore(OperationType.WRITE, "attendance") {
            val uid = requireUserId()
            if (records.isEmpty()) return@runCatchingFirestore
            val col = userRoot(uid).collection("attendance")
            for (record in records) {
                val safeDate = record.date.replace(Regex("[^a-zA-Z0-9_\\-]"), "-")
                val safeStudent = record.studentId.replace(Regex("[^a-zA-Z0-9_\\-]"), "")
                val docId = "${safeDate}_${safeStudent}".take(128)
                val ref = col.document(docId)
                val existing = ref.get().await().exists()
                val monthKey = if (record.monthKey.isNotBlank()) record.monthKey else record.date.take(7)
                val status = record.status.takeIf { it in listOf("PRESENT", "ABSENT", "LEAVE") } ?: "PRESENT"
                val map = mutableMapOf<String, Any>(
                    "studentName" to record.studentName.trim().take(120),
                    "rollNumber" to record.rollNumber.trim().take(40),
                    "classId" to record.classId,
                    "className" to record.className.trim().take(80),
                    "date" to record.date.trim().take(30),
                    "monthKey" to monthKey.take(20),
                    "status" to status,
                    "remarks" to record.remarks.trim().take(200),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (existing) {
                    ref.update(map).await()
                } else {
                    map["userId"] = uid
                    map["studentId"] = record.studentId
                    map["createdAt"] = FieldValue.serverTimestamp()
                    ref.set(map).await()
                }
            }
        }

    suspend fun deleteAttendance(attendanceId: String): Result<Unit> =
        deleteDocument("attendance", attendanceId)

    // --- EXAMS, SCHEDULES & MARKS ---
    suspend fun saveExam(exam: Examination): Result<Unit> =
        runCatchingFirestore(OperationType.WRITE, "exams") {
            val uid = requireUserId()
            require(exam.name.isNotBlank()) { "Examination name is required" }
            require(exam.classId.isNotBlank()) { "Class selection is required" }
            val col = userRoot(uid).collection("exams")
            val isUpdate = exam.id.isNotBlank()
            val ref = if (isUpdate) col.document(exam.id) else col.document()
            val map = mutableMapOf<String, Any>(
                "name" to exam.name.trim().take(120),
                "classId" to exam.classId,
                "className" to exam.className.trim().take(80),
                "academicSession" to exam.academicSession.trim().take(40).ifBlank { "2026–2027" },
                "startDate" to exam.startDate.trim().take(30).ifBlank { "2026-11-20" },
                "endDate" to exam.endDate.trim().take(30),
                "status" to exam.status.takeIf { it in listOf("UPCOMING", "ONGOING", "COMPLETED") }.orEmpty().ifBlank { "UPCOMING" },
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isUpdate) {
                ref.update(map).await()
            } else {
                map["userId"] = uid
                map["createdAt"] = FieldValue.serverTimestamp()
                ref.set(map).await()
            }
        }

    suspend fun deleteExam(examId: String): Result<Unit> =
        deleteDocument("exams", examId)

    suspend fun saveExamSchedule(entry: ExamScheduleEntry): Result<Unit> =
        runCatchingFirestore(OperationType.WRITE, "exam_schedules") {
            val uid = requireUserId()
            require(entry.examId.isNotBlank()) { "Examination is required" }
            require(entry.subjectName.isNotBlank()) { "Subject name is required" }
            require(entry.examDate.isNotBlank()) { "Exam date is required" }
            require(entry.examDay.isNotBlank()) { "Exam day is required" }
            val maxMarks = entry.maxMarks.coerceIn(1L, 1000L)
            val passMarks = entry.passMarks.coerceIn(0L, maxMarks)
            val col = userRoot(uid).collection("exam_schedules")
            val isUpdate = entry.id.isNotBlank()
            val ref = if (isUpdate) col.document(entry.id) else col.document()
            val map = mutableMapOf<String, Any>(
                "examName" to entry.examName.trim().take(120),
                "classId" to entry.classId,
                "className" to entry.className.trim().take(80),
                "subjectName" to entry.subjectName.trim().take(80),
                "examDate" to entry.examDate.trim().take(30),
                "examDay" to entry.examDay.trim().take(30),
                "timing" to entry.timing.trim().take(60),
                "maxMarks" to maxMarks,
                "passMarks" to passMarks,
                "syllabus" to entry.syllabus.trim().take(1000),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isUpdate) {
                ref.update(map).await()
            } else {
                map["userId"] = uid
                map["examId"] = entry.examId
                map["createdAt"] = FieldValue.serverTimestamp()
                ref.set(map).await()
            }
        }

    suspend fun deleteExamSchedule(scheduleId: String): Result<Unit> =
        deleteDocument("exam_schedules", scheduleId)

    suspend fun saveStudentMark(mark: StudentMark): Result<Unit> =
        runCatchingFirestore(OperationType.WRITE, "exam_marks") {
            val uid = requireUserId()
            require(mark.examId.isNotBlank() && mark.studentId.isNotBlank() && mark.scheduleId.isNotBlank())
            val maxM = mark.maxMarks.coerceIn(1L, 1000L)
            val obtained = if (mark.absent) 0.0 else mark.obtainedMarks.coerceIn(0.0, maxM.toDouble())
            val safeId = "${mark.examId}_${mark.studentId}_${mark.scheduleId}".replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(128)
            val ref = userRoot(uid).collection("exam_marks").document(if (mark.id.isNotBlank()) mark.id else safeId)
            val exists = ref.get().await().exists()
            val map = mutableMapOf<String, Any>(
                "examName" to mark.examName.trim().take(120),
                "classId" to mark.classId,
                "className" to mark.className.trim().take(80),
                "studentName" to mark.studentName.trim().take(120),
                "rollNumber" to mark.rollNumber.trim().take(40),
                "subjectName" to mark.subjectName.trim().take(80),
                "maxMarks" to maxM,
                "obtainedMarks" to obtained,
                "absent" to mark.absent,
                "remarks" to mark.remarks.trim().take(200),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (exists) {
                ref.update(map).await()
            } else {
                map["userId"] = uid
                map["examId"] = mark.examId
                map["studentId"] = mark.studentId
                map["scheduleId"] = mark.scheduleId
                map["createdAt"] = FieldValue.serverTimestamp()
                ref.set(map).await()
            }
        }

    suspend fun deleteStudentMark(markId: String): Result<Unit> =
        deleteDocument("exam_marks", markId)

    // --- TIMETABLE ---
    suspend fun saveTimetableEntry(entry: TimetableEntry): Result<Unit> =
        runCatchingFirestore(OperationType.WRITE, "timetables") {
            val uid = requireUserId()
            require(entry.classId.isNotBlank()) { "Class is required" }
            require(entry.subjectName.isNotBlank()) { "Subject is required" }
            val col = userRoot(uid).collection("timetables")
            val isUpdate = entry.id.isNotBlank()
            val ref = if (isUpdate) col.document(entry.id) else col.document()
            val map = mutableMapOf<String, Any>(
                "classId" to entry.classId,
                "className" to entry.className.trim().take(80),
                "dayOfWeek" to entry.dayOfWeek.takeIf { it in listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday") }.orEmpty().ifBlank { "Monday" },
                "periodNumber" to entry.periodNumber.coerceIn(1L, 12L),
                "startTime" to entry.startTime.trim().take(30),
                "endTime" to entry.endTime.trim().take(30),
                "subjectName" to entry.subjectName.trim().take(80),
                "teacherId" to entry.teacherId.trim().take(128),
                "teacherName" to entry.teacherName.trim().take(100),
                "roomNumber" to entry.roomNumber.trim().take(40),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isUpdate) {
                ref.update(map).await()
            } else {
                map["userId"] = uid
                map["createdAt"] = FieldValue.serverTimestamp()
                ref.set(map).await()
            }
        }

    suspend fun deleteTimetableEntry(entryId: String): Result<Unit> =
        deleteDocument("timetables", entryId)

    // --- HOMEWORK ---
    suspend fun saveHomework(hw: HomeworkItem): Result<Unit> =
        runCatchingFirestore(OperationType.WRITE, "homeworks") {
            val uid = requireUserId()
            require(hw.classId.isNotBlank()) { "Class is required" }
            require(hw.subjectName.isNotBlank()) { "Subject is required" }
            require(hw.title.isNotBlank()) { "Title is required" }
            require(hw.description.isNotBlank()) { "Description is required" }
            val col = userRoot(uid).collection("homeworks")
            val isUpdate = hw.id.isNotBlank()
            val ref = if (isUpdate) col.document(hw.id) else col.document()
            val map = mutableMapOf<String, Any>(
                "classId" to hw.classId,
                "className" to hw.className.trim().take(80),
                "subjectName" to hw.subjectName.trim().take(80),
                "title" to hw.title.trim().take(150),
                "description" to hw.description.trim().take(2000),
                "assignedDate" to hw.assignedDate.trim().take(30),
                "submissionDate" to hw.submissionDate.trim().take(30),
                "teacherName" to hw.teacherName.trim().take(100),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isUpdate) {
                ref.update(map).await()
            } else {
                map["userId"] = uid
                map["createdAt"] = FieldValue.serverTimestamp()
                ref.set(map).await()
            }
        }

    suspend fun deleteHomework(homeworkId: String): Result<Unit> =
        deleteDocument("homeworks", homeworkId)

    // --- NOTICES ---
    suspend fun saveNotice(notice: NoticeItem): Result<Unit> =
        runCatchingFirestore(OperationType.WRITE, "notices") {
            val uid = requireUserId()
            require(notice.title.isNotBlank()) { "Notice title is required" }
            require(notice.description.isNotBlank()) { "Notice description is required" }
            val col = userRoot(uid).collection("notices")
            val isUpdate = notice.id.isNotBlank()
            val ref = if (isUpdate) col.document(notice.id) else col.document()
            val map = mutableMapOf<String, Any>(
                "title" to notice.title.trim().take(150),
                "description" to notice.description.trim().take(3000),
                "targetClasses" to notice.targetClasses.ifEmpty { listOf("All Classes") }.take(30),
                "publicationDate" to notice.publicationDate.trim().take(30),
                "priority" to notice.priority.takeIf { it in listOf("NORMAL", "IMPORTANT", "URGENT") }.orEmpty().ifBlank { "NORMAL" },
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isUpdate) {
                ref.update(map).await()
            } else {
                map["userId"] = uid
                map["createdAt"] = FieldValue.serverTimestamp()
                ref.set(map).await()
            }
        }

    suspend fun deleteNotice(noticeId: String): Result<Unit> =
        deleteDocument("notices", noticeId)

    private suspend fun deleteDocument(collection: String, docId: String): Result<Unit> =
        runCatchingFirestore(OperationType.DELETE, "$collection/$docId") {
            val uid = requireUserId()
            require(docId.isNotBlank())
            userRoot(uid).collection(collection).document(docId).delete().await()
        }

    // --- BACKUP & RESTORE ---
    suspend fun exportAllDataJson(): Result<String> =
        runCatchingFirestore(OperationType.LIST, "backup_export") {
            val uid = requireUserId()
            val collections = listOf(
                "settings", "classes", "class_subjects", "teachers", "students",
                "fee_records", "attendance", "exams", "exam_schedules",
                "exam_marks", "timetables", "homeworks", "notices"
            )
            val rootJson = JSONObject()
            rootJson.put("app", "ANV_PUBLIC_SCHOOL_ERP")
            rootJson.put("version", 1)
            val dataObj = JSONObject()
            for (col in collections) {
                val snap = userRoot(uid).collection(col).get().await()
                val arr = JSONArray()
                for (doc in snap.documents) {
                    val obj = JSONObject()
                    obj.put("_docId", doc.id)
                    for ((k, v) in doc.data.orEmpty()) {
                        if (k == "createdAt" || k == "updatedAt" || k == "userId") continue
                        when (v) {
                            is List<*> -> obj.put(k, JSONArray(v))
                            else -> obj.put(k, v)
                        }
                    }
                    arr.put(obj)
                }
                dataObj.put(col, arr)
            }
            rootJson.put("collections", dataObj)
            rootJson.toString(2)
        }

    suspend fun restoreFromBackupJson(jsonString: String): Result<Int> =
        runCatchingFirestore(OperationType.WRITE, "backup_restore") {
            val uid = requireUserId()
            val root = JSONObject(jsonString)
            val collectionsObj = root.getJSONObject("collections")
            val allowed = listOf(
                "settings", "classes", "class_subjects", "teachers", "students",
                "fee_records", "attendance", "exams", "exam_schedules",
                "exam_marks", "timetables", "homeworks", "notices"
            )
            var restoredCount = 0
            for (col in allowed) {
                if (!collectionsObj.has(col)) continue
                val arr = collectionsObj.getJSONArray(col)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val docId = obj.optString("_docId").takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]+$")) }
                    val ref = if (docId != null) userRoot(uid).collection(col).document(docId)
                    else userRoot(uid).collection(col).document()
                    val map = mutableMapOf<String, Any>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        if (k == "_docId" || k == "createdAt" || k == "updatedAt" || k == "userId") continue
                        val value = obj.get(k)
                        if (value == JSONObject.NULL) continue
                        when (value) {
                            is JSONArray -> {
                                val list = mutableListOf<String>()
                                for (j in 0 until value.length()) list.add(value.optString(j))
                                map[k] = list
                            }
                            is Int -> map[k] = value.toLong()
                            else -> map[k] = value
                        }
                    }
                    val exists = ref.get().await().exists()
                    map["updatedAt"] = FieldValue.serverTimestamp()
                    if (exists) {
                        ref.update(map).await()
                    } else {
                        map["userId"] = uid
                        map["createdAt"] = FieldValue.serverTimestamp()
                        ref.set(map).await()
                    }
                    restoredCount++
                }
            }
            restoredCount
        }

    private suspend inline fun <T> runCatchingFirestore(
        op: OperationType,
        path: String,
        crossinline block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val uid = auth.currentUser?.uid ?: "unauthenticated"
            handleFirestoreError(e, op, "users/$uid/$path")
            Result.failure(e)
        }
    }
}
