package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class SchoolSettings(
    @DocumentId val id: String = "school_config",
    val userId: String = "",
    val schoolName: String = "ANV PUBLIC SCHOOL",
    val schoolAddress: String = "Nagaliya Mev, Gajraula, Uttar Pradesh, India",
    val academicSession: String = "2026–2027",
    val phone: String = "",
    val email: String = "",
    val affiliationNote: String = "Recognized English Medium Public School",
    val receiptPrefix: String = "ANV-2627-",
    val authorizedSignatory: String = "Principal / Authorized Signatory",
    val themeMode: String = "SYSTEM",
    val logoBase64: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SchoolClass(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val section: String = "",
    val displayOrder: Long = 0L,
    val monthlyFee: Double = 0.0,
    val classTeacherId: String = "",
    val classTeacherName: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
) {
    val fullName: String
        get() = if (section.isBlank()) name else "$name ($section)"
}

@Keep
@IgnoreExtraProperties
data class ClassSubject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val classId: String = "",
    val className: String = "",
    val subjectName: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val defaultMaxMarks: Long = 100L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Teacher(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val qualification: String = "",
    val mobile: String = "",
    val address: String = "",
    val email: String = "",
    val joiningDate: String = "",
    val assignedClasses: List<String> = emptyList(),
    val assignedSubjects: List<String> = emptyList(),
    val salary: Double = 0.0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Student(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val fatherName: String = "",
    val motherName: String = "",
    val dob: String = "",
    val gender: String = "Male",
    val classId: String = "",
    val className: String = "",
    val rollNumber: String = "",
    val admissionNumber: String = "",
    val mobile: String = "",
    val address: String = "",
    val admissionDate: String = "",
    val previousSchool: String = "",
    val accountNumber: String = "",
    val photoBase64: String? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FeeRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val classId: String = "",
    val className: String = "",
    val rollNumber: String = "",
    val admissionNumber: String = "",
    val receiptNumber: String = "",
    val feeMonths: List<String> = emptyList(),
    val cycleLabel: String = "",
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val paymentDate: String = "",
    val paymentMode: String = "Cash",
    val status: String = "PAID",
    val remarks: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val rollNumber: String = "",
    val classId: String = "",
    val className: String = "",
    val date: String = "",
    val monthKey: String = "",
    val status: String = "PRESENT",
    val remarks: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Examination(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val classId: String = "",
    val className: String = "",
    val academicSession: String = "2026–2027",
    val startDate: String = "",
    val endDate: String = "",
    val status: String = "UPCOMING",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ExamScheduleEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val examId: String = "",
    val examName: String = "",
    val classId: String = "",
    val className: String = "",
    val subjectName: String = "",
    val examDate: String = "",
    val examDay: String = "",
    val timing: String = "09:00 AM – 12:00 PM",
    val maxMarks: Long = 100L,
    val passMarks: Long = 33L,
    val syllabus: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudentMark(
    @DocumentId val id: String = "",
    val userId: String = "",
    val examId: String = "",
    val examName: String = "",
    val classId: String = "",
    val className: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val rollNumber: String = "",
    val scheduleId: String = "",
    val subjectName: String = "",
    val maxMarks: Long = 100L,
    val obtainedMarks: Double = 0.0,
    val absent: Boolean = false,
    val remarks: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TimetableEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val classId: String = "",
    val className: String = "",
    val dayOfWeek: String = "Monday",
    val periodNumber: Long = 1L,
    val startTime: String = "08:30 AM",
    val endTime: String = "09:15 AM",
    val subjectName: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val roomNumber: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class HomeworkItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val classId: String = "",
    val className: String = "",
    val subjectName: String = "",
    val title: String = "",
    val description: String = "",
    val assignedDate: String = "",
    val submissionDate: String = "",
    val teacherName: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NoticeItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val targetClasses: List<String> = listOf("All Classes"),
    val publicationDate: String = "",
    val priority: String = "NORMAL",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

// Computed Result Summary with automatic class ranking
data class StudentExamResult(
    val student: Student,
    val exam: Examination,
    val marks: List<StudentMark>,
    val totalObtained: Double,
    val totalMax: Long,
    val percentage: Double,
    val grade: String,
    val passed: Boolean,
    val rank: Int
)

object AcademicCalculations {
    val academicMonths2026_27 = listOf(
        "April 2026", "May 2026", "June 2026",
        "July 2026", "August 2026", "September 2026",
        "October 2026", "November 2026", "December 2026",
        "January 2027", "February 2027", "March 2027"
    )

    val quarterlyCycles = listOf(
        "Q1 (Apr–Jun 2026)" to listOf("April 2026", "May 2026", "June 2026"),
        "Q2 (Jul–Sep 2026)" to listOf("July 2026", "August 2026", "September 2026"),
        "Q3 (Oct–Dec 2026)" to listOf("October 2026", "November 2026", "December 2026"),
        "Q4 (Jan–Mar 2027)" to listOf("January 2027", "February 2027", "March 2027")
    )

    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    fun calculateGrade(percentage: Double): String = when {
        percentage >= 90.0 -> "A+"
        percentage >= 80.0 -> "A"
        percentage >= 70.0 -> "B+"
        percentage >= 60.0 -> "B"
        percentage >= 50.0 -> "C"
        percentage >= 33.0 -> "D"
        else -> "F"
    }

    fun computeClassResults(
        exam: Examination,
        studentsInClass: List<Student>,
        schedules: List<ExamScheduleEntry>,
        allMarks: List<StudentMark>
    ): List<StudentExamResult> {
        val schedulePassMap = schedules.associateBy({ it.id }, { it.passMarks })
        val rawResults = studentsInClass.mapNotNull { student ->
            val stuMarks = allMarks.filter { it.studentId == student.id && it.examId == exam.id }
            if (stuMarks.isEmpty()) return@mapNotNull null
            val totalObtained = stuMarks.sumOf { if (it.absent) 0.0 else it.obtainedMarks }
            val totalMax = stuMarks.sumOf { it.maxMarks }.coerceAtLeast(1L)
            val percentage = (totalObtained / totalMax.toDouble()) * 100.0
            val passed = stuMarks.all { mark ->
                !mark.absent && mark.obtainedMarks >= (schedulePassMap[mark.scheduleId] ?: (mark.maxMarks * 0.33).toLong())
            }
            StudentExamResult(
                student = student,
                exam = exam,
                marks = stuMarks.sortedBy { it.subjectName },
                totalObtained = totalObtained,
                totalMax = totalMax,
                percentage = percentage,
                grade = calculateGrade(percentage),
                passed = passed,
                rank = 0
            )
        }
        val sorted = rawResults.sortedByDescending { it.percentage }
        var currentRank = 0
        var prevPct = -1.0
        return sorted.mapIndexed { index, item ->
            val rank = if (item.percentage == prevPct && currentRank > 0) currentRank else (index + 1).also {
                currentRank = it
                prevPct = item.percentage
            }
            item.copy(rank = rank)
        }
    }

    fun formatInr(amount: Double): String {
        val rounded = kotlin.math.round(amount).toLong()
        val s = rounded.toString()
        if (s.length <= 3) return "₹$s"
        val lastThree = s.takeLast(3)
        val rest = s.dropLast(3)
        val sb = StringBuilder()
        var count = 0
        for (i in rest.length - 1 downTo 0) {
            sb.append(rest[i])
            count++
            if (count == 2 && i != 0) {
                sb.append(',')
                count = 0
            }
        }
        return "₹${sb.reverse()},$lastThree"
    }
}

fun buildCreateTimestamps(): Map<String, Any> = mapOf(
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp()
)
