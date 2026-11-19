package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.example.R
import com.example.data.model.AcademicCalculations
import com.example.data.model.AttendanceRecord
import com.example.data.model.ExamScheduleEntry
import com.example.data.model.Examination
import com.example.data.model.FeeRecord
import com.example.data.model.SchoolSettings
import com.example.data.model.Student
import com.example.data.model.StudentExamResult
import com.example.data.model.TimetableEntry
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

object PdfAndPrintManager {

    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842

    private val navyColor = Color.rgb(15, 39, 74)
    private val royalColor = Color.rgb(29, 78, 216)
    private val slateColor = Color.rgb(71, 85, 105)
    private val lightTint = Color.rgb(239, 246, 255)
    private val borderColor = Color.rgb(203, 213, 225)
    private val emeraldColor = Color.rgb(5, 150, 105)
    private val crimsonColor = Color.rgb(220, 38, 38)

    private fun getSchoolCrestBitmap(context: Context, settings: SchoolSettings): Bitmap? {
        return ImageHelper.base64ToBitmap(settings.logoBase64) ?: try {
            BitmapFactory.decodeResource(context.resources, R.drawable.img_school_crest)
        } catch (_: Exception) {
            null
        }
    }

    fun writePdfToUri(context: Context, uri: Uri, pdfBytes: ByteArray): Result<Unit> {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(pdfBytes)
                out.flush()
            } ?: error("Unable to open destination file.")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun printPdfBytes(context: Context, jobName: String, pdfBytes: ByteArray) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    return
                }
                try {
                    FileOutputStream(destination.fileDescriptor).use { out ->
                        out.write(pdfBytes)
                    }
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }
        val attrs = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        printManager.print(jobName, adapter, attrs)
    }

    // 1. OFFICIAL FEE RECEIPT PDF
    fun generateFeeReceiptPdf(
        context: Context,
        settings: SchoolSettings,
        fee: FeeRecord,
        student: Student?
    ): ByteArray {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create())
        val c = page.canvas
        val crest = getSchoolCrestBitmap(context, settings)

        val borderPaint = Paint().apply {
            color = navyColor
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        c.drawRect(28f, 28f, A4_WIDTH - 28f, A4_HEIGHT - 28f, borderPaint)

        // Header Band
        val headerPaint = Paint().apply { color = navyColor }
        c.drawRect(28f, 28f, A4_WIDTH - 28f, 128f, headerPaint)
        crest?.let {
            c.drawBitmap(it, null, RectF(44f, 42f, 114f, 112f), null)
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(219, 234, 254)
            textSize = 10.5f
        }
        c.drawText(settings.schoolName.uppercase(), 130f, 64f, titlePaint)
        c.drawText(settings.schoolAddress, 130f, 84f, subPaint)
        c.drawText("Academic Session: ${settings.academicSession}  |  OFFICIAL FEE RECEIPT", 130f, 104f, subPaint)

        // Receipt Meta Row
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = slateColor
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 11.5f
        }
        val boldValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = navyColor
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val boxPaint = Paint().apply { color = lightTint }
        c.drawRect(44f, 146f, A4_WIDTH - 44f, 188f, boxPaint)
        c.drawText("RECEIPT NO:", 56f, 165f, labelPaint)
        c.drawText(fee.receiptNumber, 134f, 165f, boldValPaint)
        c.drawText("PAYMENT DATE:", 330f, 165f, labelPaint)
        c.drawText(fee.paymentDate, 430f, 165f, boldValPaint)
        c.drawText("MODE: ${fee.paymentMode}", 56f, 181f, labelPaint)
        c.drawText("STATUS: ${fee.status}", 330f, 181f, Paint(boldValPaint).apply {
            color = if (fee.remainingBalance <= 0.0) emeraldColor else crimsonColor
        })

        // Student Info
        var y = 218f
        c.drawText("STUDENT DETAILS", 44f, y, labelPaint)
        y += 10f
        c.drawLine(44f, y, A4_WIDTH - 44f, y, Paint().apply { color = borderColor; strokeWidth = 1f })
        y += 22f
        c.drawText("Student Name:", 44f, y, labelPaint)
        c.drawText(fee.studentName, 160f, y, boldValPaint)
        c.drawText("Class:", 360f, y, labelPaint)
        c.drawText(fee.className, 430f, y, valuePaint)
        y += 22f
        c.drawText("Father's Name:", 44f, y, labelPaint)
        c.drawText(student?.fatherName ?: "-", 160f, y, valuePaint)
        c.drawText("Roll No:", 360f, y, labelPaint)
        c.drawText(fee.rollNumber.ifBlank { student?.rollNumber ?: "-" }, 430f, y, valuePaint)
        y += 22f
        c.drawText("Admission No:", 44f, y, labelPaint)
        c.drawText(fee.admissionNumber.ifBlank { student?.admissionNumber ?: "-" }, 160f, y, valuePaint)
        c.drawText("Cycle:", 360f, y, labelPaint)
        c.drawText(fee.cycleLabel.ifBlank { "Custom" }, 430f, y, valuePaint)

        // Fee Months
        y += 34f
        c.drawText("FEE MONTHS COVERED", 44f, y, labelPaint)
        y += 18f
        val monthsText = if (fee.feeMonths.isEmpty()) "General Fee" else fee.feeMonths.joinToString(", ")
        drawWrappedText(c, monthsText, 44f, y, A4_WIDTH - 88f, valuePaint, 16f)

        // Amount Summary Table
        y += 54f
        val tableHeader = Paint().apply { color = royalColor }
        c.drawRect(44f, y, A4_WIDTH - 44f, y + 28f, tableHeader)
        val thText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        c.drawText("FEE PARTICULAR", 58f, y + 18f, thText)
        c.drawText("AMOUNT (INR)", A4_WIDTH - 150f, y + 18f, thText)

        y += 28f
        val rowStroke = Paint().apply { color = borderColor; style = Paint.Style.STROKE; strokeWidth = 1f }
        fun drawRow(label: String, amt: String, highlight: Int = Color.BLACK) {
            c.drawRect(44f, y, A4_WIDTH - 44f, y + 30f, rowStroke)
            c.drawText(label, 58f, y + 20f, valuePaint)
            c.drawText(amt, A4_WIDTH - 150f, y + 20f, Paint(boldValPaint).apply { color = highlight })
            y += 30f
        }
        drawRow("Total Fee Payable (${fee.feeMonths.size.coerceAtLeast(1)} month(s))", AcademicCalculations.formatInr(fee.totalAmount))
        drawRow("Amount Paid Now", AcademicCalculations.formatInr(fee.paidAmount), emeraldColor)
        drawRow("Remaining Pending Balance", AcademicCalculations.formatInr(fee.remainingBalance), if (fee.remainingBalance > 0) crimsonColor else emeraldColor)

        if (fee.remarks.isNotBlank()) {
            y += 24f
            c.drawText("Remarks: ${fee.remarks}", 44f, y, valuePaint)
        }

        // Signature Footer
        val sigY = A4_HEIGHT - 110f
        c.drawLine(44f, sigY, 200f, sigY, Paint().apply { color = slateColor; strokeWidth = 1f })
        c.drawText("Parent / Depositor Signature", 44f, sigY + 16f, labelPaint)
        c.drawLine(A4_WIDTH - 220f, sigY, A4_WIDTH - 44f, sigY, Paint().apply { color = slateColor; strokeWidth = 1f })
        c.drawText(settings.authorizedSignatory, A4_WIDTH - 220f, sigY + 16f, labelPaint)

        doc.finishPage(page)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    // 2. REPORT CARD / MARKSHEET PDF
    fun generateMarksheetPdf(
        context: Context,
        settings: SchoolSettings,
        result: StudentExamResult
    ): ByteArray {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create())
        val c = page.canvas
        val crest = getSchoolCrestBitmap(context, settings)

        c.drawRect(24f, 24f, A4_WIDTH - 24f, A4_HEIGHT - 24f, Paint().apply {
            color = navyColor; style = Paint.Style.STROKE; strokeWidth = 2.5f
        })
        c.drawRect(24f, 24f, A4_WIDTH - 24f, 124f, Paint().apply { color = navyColor })
        crest?.let { c.drawBitmap(it, null, RectF(40f, 38f, 110f, 108f), null) }

        val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 20f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(219, 234, 254); textSize = 10.5f
        }
        c.drawText(settings.schoolName.uppercase(), 126f, 58f, titleP)
        c.drawText(settings.schoolAddress, 126f, 78f, subP)
        c.drawText("STATEMENT OF MARKS — ${result.exam.name.uppercase()} (${settings.academicSession})", 126f, 100f, subP)

        val lbl = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = slateColor; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val vP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 11f }
        val bP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = navyColor; textSize = 11.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }

        var y = 152f
        c.drawText("Student Name:", 42f, y, lbl); c.drawText(result.student.name, 140f, y, bP)
        c.drawText("Class:", 360f, y, lbl); c.drawText(result.student.className, 430f, y, bP)
        y += 20f
        c.drawText("Father's Name:", 42f, y, lbl); c.drawText(result.student.fatherName, 140f, y, vP)
        c.drawText("Roll No:", 360f, y, lbl); c.drawText(result.student.rollNumber, 430f, y, vP)
        y += 20f
        c.drawText("Mother's Name:", 42f, y, lbl); c.drawText(result.student.motherName.ifBlank { "-" }, 140f, y, vP)
        c.drawText("Adm. No:", 360f, y, lbl); c.drawText(result.student.admissionNumber, 430f, y, vP)

        y += 28f
        c.drawRect(40f, y, A4_WIDTH - 40f, y + 26f, Paint().apply { color = royalColor })
        val th = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 10.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        c.drawText("SUBJECT", 52f, y + 17f, th)
        c.drawText("MAX MARKS", 260f, y + 17f, th)
        c.drawText("OBTAINED", 355f, y + 17f, th)
        c.drawText("GRADE", 445f, y + 17f, th)
        c.drawText("STATUS", 500f, y + 17f, th)
        y += 26f

        val strokeP = Paint().apply { color = borderColor; style = Paint.Style.STROKE; strokeWidth = 1f }
        for (mark in result.marks) {
            c.drawRect(40f, y, A4_WIDTH - 40f, y + 26f, strokeP)
            val pct = if (mark.maxMarks > 0) (mark.obtainedMarks / mark.maxMarks) * 100.0 else 0.0
            val g = if (mark.absent) "AB" else AcademicCalculations.calculateGrade(pct)
            val status = if (mark.absent) "ABSENT" else if (pct >= 33.0) "PASS" else "FAIL"
            c.drawText(mark.subjectName, 52f, y + 17f, vP)
            c.drawText(mark.maxMarks.toString(), 275f, y + 17f, vP)
            c.drawText(if (mark.absent) "AB" else formatScore(mark.obtainedMarks), 370f, y + 17f, bP)
            c.drawText(g, 455f, y + 17f, vP)
            c.drawText(status, 502f, y + 17f, Paint(bP).apply { color = if (status == "PASS") emeraldColor else crimsonColor })
            y += 26f
        }

        // Grand Summary Box
        y += 24f
        c.drawRect(40f, y, A4_WIDTH - 40f, y + 76f, Paint().apply { color = lightTint })
        c.drawRect(40f, y, A4_WIDTH - 40f, y + 76f, strokeP)
        c.drawText("TOTAL MARKS: ${formatScore(result.totalObtained)} / ${result.totalMax}", 56f, y + 26f, bP)
        c.drawText("PERCENTAGE: ${"%.2f".format(result.percentage)}%", 310f, y + 26f, bP)
        c.drawText("OVERALL GRADE: ${result.grade}", 56f, y + 54f, bP)
        c.drawText("CLASS RANK: #${result.rank}", 250f, y + 54f, bP)
        c.drawText("RESULT: ${if (result.passed) "PASSED" else "NEEDS IMPROVEMENT"}", 390f, y + 54f, Paint(bP).apply {
            color = if (result.passed) emeraldColor else crimsonColor
        })

        val sigY = A4_HEIGHT - 88f
        c.drawLine(42f, sigY, 180f, sigY, Paint().apply { color = slateColor; strokeWidth = 1f })
        c.drawText("Class Teacher", 42f, sigY + 15f, lbl)
        c.drawLine(A4_WIDTH - 200f, sigY, A4_WIDTH - 42f, sigY, Paint().apply { color = slateColor; strokeWidth = 1f })
        c.drawText(settings.authorizedSignatory, A4_WIDTH - 200f, sigY + 15f, lbl)

        doc.finishPage(page)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    // 3. ADMIT CARD PDF — STRICTLY 3 ADMIT CARDS PER A4 PAGE, ZERO CUTOFF
    fun generateAdmitCardsPdf(
        context: Context,
        settings: SchoolSettings,
        exam: Examination,
        students: List<Student>,
        schedules: List<ExamScheduleEntry>
    ): ByteArray {
        val doc = PdfDocument()
        val crest = getSchoolCrestBitmap(context, settings)
        val sortedStudents = students.sortedBy { it.rollNumber.toIntOrNull() ?: 9999 }
        val sortedSchedule = schedules.sortedBy { it.examDate }

        val marginX = 20f
        val marginY = 16f
        val gapY = 10f
        val cardW = A4_WIDTH - marginX * 2f // 555 pt
        val cardH = (A4_HEIGHT - marginY * 2f - gapY * 2f) / 3f // 263.3 pt

        val chunks = sortedStudents.chunked(3)
        chunks.forEachIndexed { pageIndex, group ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageIndex + 1).create())
            val c = page.canvas

            group.forEachIndexed { slot, student ->
                val top = marginY + slot * (cardH + gapY)
                drawSingleAdmitCard(c, settings, exam, student, sortedSchedule, crest, marginX, top, cardW, cardH)
                if (slot < 2 && slot < group.size - 1) {
                    val cutY = top + cardH + gapY / 2f
                    val dashP = Paint().apply {
                        color = Color.GRAY
                        strokeWidth = 0.8f
                        pathEffect = DashPathEffect(floatArrayOf(5f, 5f), 0f)
                    }
                    c.drawLine(marginX, cutY, marginX + cardW, cutY, dashP)
                }
            }

            doc.finishPage(page)
        }

        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    private fun drawSingleAdmitCard(
        c: Canvas,
        settings: SchoolSettings,
        exam: Examination,
        student: Student,
        schedules: List<ExamScheduleEntry>,
        crest: Bitmap?,
        left: Float,
        top: Float,
        width: Float,
        height: Float
    ) {
        val right = left + width
        val bottom = top + height

        // Outer Card Frame
        c.drawRect(left, top, right, bottom, Paint().apply {
            color = navyColor; style = Paint.Style.STROKE; strokeWidth = 1.5f
        })

        // Header Strip (36pt)
        val headerH = 36f
        c.drawRect(left, top, right, top + headerH, Paint().apply { color = navyColor })
        crest?.let {
            c.drawBitmap(it, null, RectF(left + 8f, top + 4f, left + 36f, top + 32f), null)
        }

        val hTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 11.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val hSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(219, 234, 254); textSize = 7.8f
        }
        val badgeP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(254, 243, 199); textSize = 9.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        c.drawText(settings.schoolName.uppercase(), left + 44f, top + 15f, hTitle)
        c.drawText("${settings.schoolAddress} | Session: ${settings.academicSession}", left + 44f, top + 27f, hSub)
        c.drawText("ADMIT CARD", right - 10f, top + 15f, badgeP)
        c.drawText(exam.name.take(30), right - 10f, top + 27f, Paint(hSub).apply { textAlign = Paint.Align.RIGHT })

        // Student Info Strip + Photo Box (52pt)
        val infoTop = top + headerH + 4f
        val photoSize = 44f
        val photoLeft = right - photoSize - 10f
        val photoRect = RectF(photoLeft, infoTop, photoLeft + photoSize, infoTop + photoSize)
        val stuBmp = ImageHelper.base64ToBitmap(student.photoBase64)
        if (stuBmp != null) {
            c.drawBitmap(stuBmp, null, photoRect, null)
        } else {
            c.drawRect(photoRect, Paint().apply { color = lightTint })
            c.drawText("PHOTO", photoRect.centerX(), photoRect.centerY() + 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = slateColor; textSize = 7f; textAlign = Paint.Align.CENTER
            })
        }
        c.drawRect(photoRect, Paint().apply { color = borderColor; style = Paint.Style.STROKE; strokeWidth = 0.8f })

        val lblP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = slateColor; textSize = 7.8f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; textSize = 8.4f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        c.drawText("Student:", left + 10f, infoTop + 11f, lblP)
        c.drawText(student.name.take(26), left + 54f, infoTop + 11f, valP)
        c.drawText("Class:", left + 220f, infoTop + 11f, lblP)
        c.drawText(student.className, left + 252f, infoTop + 11f, valP)
        c.drawText("Roll No:", left + 355f, infoTop + 11f, lblP)
        c.drawText(student.rollNumber, left + 394f, infoTop + 11f, valP)

        c.drawText("Father:", left + 10f, infoTop + 25f, lblP)
        c.drawText(student.fatherName.take(26), left + 54f, infoTop + 25f, valP)
        c.drawText("Adm. No:", left + 220f, infoTop + 25f, lblP)
        c.drawText(student.admissionNumber, left + 262f, infoTop + 25f, valP)
        c.drawText("DOB:", left + 355f, infoTop + 25f, lblP)
        c.drawText(student.dob.ifBlank { "-" }, left + 382f, infoTop + 25f, valP)

        c.drawText("Mother:", left + 10f, infoTop + 39f, lblP)
        c.drawText(student.motherName.ifBlank { "-" }.take(26), left + 54f, infoTop + 39f, valP)
        c.drawText("Mobile:", left + 220f, infoTop + 39f, lblP)
        c.drawText(student.mobile, left + 256f, infoTop + 39f, valP)

        // Schedule Table
        val tableTop = infoTop + 48f
        val footerH = 22f
        val availTableH = (bottom - footerH) - tableTop - 4f
        val thH = 14f
        c.drawRect(left + 8f, tableTop, right - 8f, tableTop + thH, Paint().apply { color = royalColor })
        val thP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 7.4f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        c.drawText("DATE", left + 12f, tableTop + 10f, thP)
        c.drawText("DAY", left + 76f, tableTop + 10f, thP)
        c.drawText("TIMING", left + 132f, tableTop + 10f, thP)
        c.drawText("SUBJECT", left + 222f, tableTop + 10f, thP)
        c.drawText("SYLLABUS / REMARKS", left + 324f, tableTop + 10f, thP)

        val rowCount = schedules.size.coerceAtLeast(1)
        val rowH = ((availTableH - thH) / rowCount).coerceIn(11f, 17f)
        val cellTextSize = if (rowH < 13f) 6.8f else 7.5f
        val cellP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = cellTextSize }
        val cellStroke = Paint().apply { color = borderColor; style = Paint.Style.STROKE; strokeWidth = 0.6f }

        if (schedules.isEmpty()) {
            c.drawRect(left + 8f, tableTop + thH, right - 8f, tableTop + thH + 18f, cellStroke)
            c.drawText("No examination schedule subjects added yet.", left + 14f, tableTop + thH + 12f, cellP)
        } else {
            schedules.forEachIndexed { idx, s ->
                val ry = tableTop + thH + idx * rowH
                if (ry + rowH <= bottom - footerH) {
                    c.drawRect(left + 8f, ry, right - 8f, ry + rowH, cellStroke)
                    val ty = ry + rowH - 3.5f
                    c.drawText(s.examDate.take(12), left + 12f, ty, cellP)
                    c.drawText(s.examDay.take(10), left + 76f, ty, cellP)
                    c.drawText(s.timing.take(19), left + 132f, ty, cellP)
                    c.drawText(s.subjectName.take(18), left + 222f, ty, Paint(cellP).apply {
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    })
                    c.drawText(s.syllabus.ifBlank { "Full Syllabus" }.take(38), left + 324f, ty, cellP)
                }
            }
        }

        // Footer Signatures
        val sigLineY = bottom - 12f
        val sigP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = slateColor; textSize = 7.2f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        c.drawText("Student Signature", left + 14f, bottom - 4f, sigP)
        c.drawText("Class Teacher", left + width * 0.42f, bottom - 4f, sigP)
        c.drawText(settings.authorizedSignatory.take(28), right - 140f, bottom - 4f, sigP)
        c.drawLine(left + 14f, sigLineY, left + 110f, sigLineY, Paint().apply { color = borderColor; strokeWidth = 0.7f })
        c.drawLine(left + width * 0.40f, sigLineY, left + width * 0.56f, sigLineY, Paint().apply { color = borderColor; strokeWidth = 0.7f })
        c.drawLine(right - 144f, sigLineY, right - 12f, sigLineY, Paint().apply { color = borderColor; strokeWidth = 0.7f })
    }

    // 4. GENERIC REPORT / TIMETABLE / ATTENDANCE TABLE PDF
    fun generateTableReportPdf(
        context: Context,
        settings: SchoolSettings,
        reportTitle: String,
        subtitle: String,
        columns: List<String>,
        rows: List<List<String>>,
        summaryFooter: String = ""
    ): ByteArray {
        val doc = PdfDocument()
        val crest = getSchoolCrestBitmap(context, settings)
        val rowsPerPage = 24
        val pages = if (rows.isEmpty()) listOf(emptyList()) else rows.chunked(rowsPerPage)

        pages.forEachIndexed { pageIdx, pageRows ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageIdx + 1).create())
            val c = page.canvas

            // Header
            c.drawRect(24f, 24f, A4_WIDTH - 24f, 96f, Paint().apply { color = navyColor })
            crest?.let { c.drawBitmap(it, null, RectF(36f, 32f, 88f, 84f), null) }
            c.drawText(settings.schoolName.uppercase(), 100f, 50f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE; textSize = 16f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            })
            c.drawText("${settings.schoolAddress} | Session: ${settings.academicSession}", 100f, 66f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(219, 234, 254); textSize = 9.5f
            })
            c.drawText("$reportTitle — $subtitle", 100f, 84f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(254, 243, 199); textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            })

            var y = 114f
            val tableLeft = 24f
            val tableW = A4_WIDTH - 48f
            val colW = tableW / columns.size.coerceAtLeast(1)

            c.drawRect(tableLeft, y, tableLeft + tableW, y + 24f, Paint().apply { color = royalColor })
            val thP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE; textSize = 8.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            columns.forEachIndexed { i, col ->
                c.drawText(col.take(18), tableLeft + i * colW + 6f, y + 16f, thP)
            }
            y += 24f

            val rowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 8.2f }
            val strokeP = Paint().apply { color = borderColor; style = Paint.Style.STROKE; strokeWidth = 0.7f }
            if (pageRows.isEmpty()) {
                c.drawRect(tableLeft, y, tableLeft + tableW, y + 26f, strokeP)
                c.drawText("No records available for this report.", tableLeft + 10f, y + 17f, rowP)
                y += 26f
            } else {
                pageRows.forEachIndexed { rIdx, r ->
                    if (rIdx % 2 == 1) {
                        c.drawRect(tableLeft, y, tableLeft + tableW, y + 24f, Paint().apply { color = lightTint })
                    }
                    c.drawRect(tableLeft, y, tableLeft + tableW, y + 24f, strokeP)
                    r.forEachIndexed { cIdx, cell ->
                        if (cIdx < columns.size) {
                            c.drawText(cell.take(20), tableLeft + cIdx * colW + 6f, y + 16f, rowP)
                        }
                    }
                    y += 24f
                }
            }

            if (summaryFooter.isNotBlank() && pageIdx == pages.lastIndex) {
                y += 16f
                c.drawText(summaryFooter, 24f, y, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = navyColor; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                })
            }

            c.drawText("Page ${pageIdx + 1} of ${pages.size} • ${settings.schoolName}", 24f, A4_HEIGHT - 18f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = slateColor; textSize = 8.5f
            })
            doc.finishPage(page)
        }

        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
        lineHeight: Float
    ) {
        var y = startY
        var line = ""
        for (word in text.split(" ")) {
            val test = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(test) > maxWidth) {
                canvas.drawText(line, x, y, paint)
                line = word
                y += lineHeight
            } else {
                line = test
            }
        }
        if (line.isNotEmpty()) canvas.drawText(line, x, y, paint)
    }

    private fun formatScore(score: Double): String {
        return if (score % 1.0 == 0.0) score.toLong().toString() else "%.1f".format(score)
    }
}
