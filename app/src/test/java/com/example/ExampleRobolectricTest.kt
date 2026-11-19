package com.example

import android.content.Context
import android.graphics.BitmapFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AcademicCalculations
import com.example.data.model.ExamScheduleEntry
import com.example.data.model.Examination
import com.example.data.model.Student
import com.example.data.model.StudentMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ANV School", appName)
    }

    @Test
    fun `bundled drawable images decode cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val banner = BitmapFactory.decodeResource(context.resources, R.drawable.img_campus_banner)
        val crest = BitmapFactory.decodeResource(context.resources, R.drawable.img_school_crest)
        val iconFg = BitmapFactory.decodeResource(context.resources, R.drawable.img_app_icon_fg)
        assertNotNull(banner)
        assertNotNull(crest)
        assertNotNull(iconFg)
        assertTrue(banner.width > 0 && banner.height > 0)
        assertTrue(crest.width > 0 && crest.height > 0)
        assertTrue(iconFg.width > 0 && iconFg.height > 0)
    }

    @Test
    fun `formatInr formats Indian numbering correctly`() {
        assertEquals("₹500", AcademicCalculations.formatInr(500.0))
        assertEquals("₹2,500", AcademicCalculations.formatInr(2500.0))
        assertEquals("₹1,25,000", AcademicCalculations.formatInr(125000.0))
    }

    @Test
    fun `calculateGrade maps percentages to grades`() {
        assertEquals("A+", AcademicCalculations.calculateGrade(95.0))
        assertEquals("A", AcademicCalculations.calculateGrade(82.0))
        assertEquals("B+", AcademicCalculations.calculateGrade(74.0))
        assertEquals("D", AcademicCalculations.calculateGrade(33.0))
        assertEquals("F", AcademicCalculations.calculateGrade(28.0))
    }

    @Test
    fun `computeClassResults ranks students accurately with custom max marks`() {
        val exam = Examination(id = "ex1", name = "Half Yearly", classId = "c1", className = "Class 5")
        val s1 = Student(id = "s1", name = "Aarav", classId = "c1", className = "Class 5", rollNumber = "1")
        val s2 = Student(id = "s2", name = "Diya", classId = "c1", className = "Class 5", rollNumber = "2")
        val s3 = Student(id = "s3", name = "Kabir", classId = "c1", className = "Class 5", rollNumber = "3")

        val sch1 = ExamScheduleEntry(id = "sch1", examId = "ex1", subjectName = "Math", maxMarks = 80, passMarks = 27)
        val sch2 = ExamScheduleEntry(id = "sch2", examId = "ex1", subjectName = "English", maxMarks = 50, passMarks = 17)

        val marks = listOf(
            StudentMark(examId = "ex1", studentId = "s1", scheduleId = "sch1", subjectName = "Math", maxMarks = 80, obtainedMarks = 60.0),
            StudentMark(examId = "ex1", studentId = "s1", scheduleId = "sch2", subjectName = "English", maxMarks = 50, obtainedMarks = 44.0),
            StudentMark(examId = "ex1", studentId = "s2", scheduleId = "sch1", subjectName = "Math", maxMarks = 80, obtainedMarks = 75.0),
            StudentMark(examId = "ex1", studentId = "s2", scheduleId = "sch2", subjectName = "English", maxMarks = 50, obtainedMarks = 45.0),
            StudentMark(examId = "ex1", studentId = "s3", scheduleId = "sch1", subjectName = "Math", maxMarks = 80, obtainedMarks = 20.0),
            StudentMark(examId = "ex1", studentId = "s3", scheduleId = "sch2", subjectName = "English", maxMarks = 50, obtainedMarks = 35.0)
        )

        val results = AcademicCalculations.computeClassResults(exam, listOf(s1, s2, s3), listOf(sch1, sch2), marks)
        assertEquals(3, results.size)
        assertEquals("s2", results[0].student.id)
        assertEquals(1, results[0].rank)
        assertTrue(results[0].passed)

        assertEquals("s1", results[1].student.id)
        assertEquals(2, results[1].rank)
        assertTrue(results[1].passed)

        assertEquals("s3", results[2].student.id)
        assertEquals(3, results[2].rank)
        assertFalse(results[2].passed)
    }
}
