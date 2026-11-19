package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AcademicCalculations
import com.example.ui.ErpModule
import com.example.ui.SchoolErpData
import com.example.ui.components.AttendanceDonutCard
import com.example.ui.components.BarChartCard
import com.example.ui.components.ErpPanelCard
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.LineTrendChartCard
import com.example.ui.components.StatusPill
import com.example.ui.components.StudentAvatar
import com.example.ui.theme.Crimson600
import com.example.ui.theme.Cyan600
import com.example.ui.theme.Emerald600
import com.example.ui.theme.GoldAmber500
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.RoyalBlue500
import com.example.ui.theme.RoyalBlue600
import com.example.ui.theme.Violet600
import com.example.util.ImageHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    data: SchoolErpData,
    onNavigate: (ErpModule) -> Unit,
    onSeedClasses: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Hero School Header Banner
            item {
                val logoBmp = ImageHelper.base64ToImageBitmap(data.settings.logoBase64)
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = Navy900),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(listOf(Navy900, Navy800, RoyalBlue600))
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (logoBmp != null) {
                                    Image(
                                        bitmap = logoBmp,
                                        contentDescription = "School Logo",
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_school_crest),
                                        contentDescription = "School Crest",
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = data.settings.schoolName,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = data.settings.schoolAddress,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "Session ${data.settings.academicSession}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AssistChip(
                                    onClick = { onNavigate(ErpModule.STUDENTS) },
                                    label = { Text("Admit Student", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.PersonAdd, null, tint = Color.White, modifier = Modifier.size(16.dp)) },
                                    modifier = Modifier.testTag("quick_admit_student")
                                )
                                AssistChip(
                                    onClick = { onNavigate(ErpModule.FEES) },
                                    label = { Text("Collect Fee", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.ReceiptLong, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                                )
                                AssistChip(
                                    onClick = { onNavigate(ErpModule.ATTENDANCE) },
                                    label = { Text("Mark Attendance", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.FactCheck, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                                )
                                AssistChip(
                                    onClick = { onNavigate(ErpModule.ADMIT_CARDS) },
                                    label = { Text("Admit Cards", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Badge, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                                )
                                if (data.classes.isEmpty()) {
                                    AssistChip(
                                        onClick = onSeedClasses,
                                        label = { Text("Load Nursery–8 Classes", color = Color(0xFFFEF3C7)) },
                                        leadingIcon = { Icon(Icons.Default.School, null, tint = Color(0xFFFEF3C7), modifier = Modifier.size(16.dp)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 8 Live KPI Cards Grid
            item {
                val kpis = listOf(
                    KpiSpec("Total Students", data.students.size.toString(), "Enrolled", Icons.Default.Groups, RoyalBlue600) { onNavigate(ErpModule.STUDENTS) },
                    KpiSpec("Total Teachers", data.teachers.size.toString(), "Faculty", Icons.Default.School, Violet600) { onNavigate(ErpModule.TEACHERS) },
                    KpiSpec("Total Classes", data.classes.size.toString(), "${data.subjects.size} Subjects", Icons.Default.Class, Cyan600) { onNavigate(ErpModule.CLASSES) },
                    KpiSpec("Today's Attendance", "${data.todayAttendancePercent.toInt()}%", data.todayDate, Icons.Default.FactCheck, Emerald600) { onNavigate(ErpModule.ATTENDANCE) },
                    KpiSpec("Present Today", data.presentTodayCount.toString(), "On Campus", Icons.Default.CheckCircle, Emerald600) { onNavigate(ErpModule.ATTENDANCE) },
                    KpiSpec("Absent Today", data.absentTodayCount.toString(), "${data.leaveTodayCount} Leave", Icons.Default.PersonOff, Crimson600) { onNavigate(ErpModule.ATTENDANCE) },
                    KpiSpec("Fee Collection", AcademicCalculations.formatInr(data.totalFeeCollection), "${data.feeRecords.size} Receipts", Icons.Default.AccountBalanceWallet, Emerald600) { onNavigate(ErpModule.FEES) },
                    KpiSpec("Pending Fees", AcademicCalculations.formatInr(data.totalPendingFees), "Balance Due", Icons.Default.HourglassBottom, GoldAmber500) { onNavigate(ErpModule.FEES) }
                )

                val cols = if (isWide) 4 else 2
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    kpis.chunked(cols).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { k ->
                                KpiMetricCard(
                                    title = k.title,
                                    value = k.value,
                                    subtitle = k.subtitle,
                                    icon = k.icon,
                                    accentColor = k.color,
                                    modifier = Modifier.weight(1f),
                                    onClick = k.onClick
                                )
                            }
                            repeat(cols - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 4 Analytics Charts
            item {
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BarChartCard(
                            title = "Monthly Fee Collection",
                            subtitle = "INR collected across months (2026–27)",
                            points = data.monthlyFeeCollectionChart,
                            barColor = RoyalBlue500,
                            modifier = Modifier.weight(1f)
                        )
                        AttendanceDonutCard(
                            present = data.presentTodayCount,
                            absent = data.absentTodayCount,
                            leave = data.leaveTodayCount,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BarChartCard(
                            title = "Monthly Fee Collection",
                            subtitle = "INR collected across months (2026–27)",
                            points = data.monthlyFeeCollectionChart,
                            barColor = RoyalBlue500
                        )
                        AttendanceDonutCard(
                            present = data.presentTodayCount,
                            absent = data.absentTodayCount,
                            leave = data.leaveTodayCount
                        )
                    }
                }
            }

            item {
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BarChartCard(
                            title = "Class-Wise Student Distribution",
                            subtitle = "Active enrollment per class",
                            points = data.classDistributionChart,
                            barColor = Violet600,
                            modifier = Modifier.weight(1f)
                        )
                        LineTrendChartCard(
                            title = "Monthly Admission Statistics",
                            subtitle = "New admissions trend (2026)",
                            points = data.monthlyAdmissionChart,
                            lineColor = Emerald600,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BarChartCard(
                            title = "Class-Wise Student Distribution",
                            subtitle = "Active enrollment per class",
                            points = data.classDistributionChart,
                            barColor = Violet600
                        )
                        LineTrendChartCard(
                            title = "Monthly Admission Statistics",
                            subtitle = "New admissions trend (2026)",
                            points = data.monthlyAdmissionChart,
                            lineColor = Emerald600
                        )
                    }
                }
            }

            // Recent Admissions & Recent Fee Payments
            item {
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RecentAdmissionsPanel(data, onNavigate, Modifier.weight(1f))
                        RecentPaymentsPanel(data, onNavigate, Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        RecentAdmissionsPanel(data, onNavigate)
                        RecentPaymentsPanel(data, onNavigate)
                    }
                }
            }

            // Upcoming Tests & Important Announcements
            item {
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        UpcomingExamsPanel(data, onNavigate, Modifier.weight(1f))
                        AnnouncementsPanel(data, onNavigate, Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        UpcomingExamsPanel(data, onNavigate)
                        AnnouncementsPanel(data, onNavigate)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

private data class KpiSpec(
    val title: String,
    val value: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

@Composable
private fun RecentAdmissionsPanel(
    data: SchoolErpData,
    onNavigate: (ErpModule) -> Unit,
    modifier: Modifier = Modifier
) {
    ErpPanelCard(
        title = "Recent Admissions",
        subtitle = "Latest enrolled students",
        modifier = modifier,
        trailing = { TextButton(onClick = { onNavigate(ErpModule.STUDENTS) }) { Text("View All") } }
    ) {
        if (data.recentAdmissions.isEmpty()) {
            Text("No students admitted yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            data.recentAdmissions.forEachIndexed { idx, s ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StudentAvatar(name = s.name, photoBase64 = s.photoBase64, sizeDp = 38)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.name, style = MaterialTheme.typography.titleSmall)
                        Text("${s.className} • Adm: ${s.admissionNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(s.admissionDate, style = MaterialTheme.typography.labelSmall, color = RoyalBlue600)
                }
                if (idx < data.recentAdmissions.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun RecentPaymentsPanel(
    data: SchoolErpData,
    onNavigate: (ErpModule) -> Unit,
    modifier: Modifier = Modifier
) {
    ErpPanelCard(
        title = "Recent Fee Payments",
        subtitle = "Latest fee receipts",
        modifier = modifier,
        trailing = { TextButton(onClick = { onNavigate(ErpModule.FEES) }) { Text("View All") } }
    ) {
        if (data.recentFeePayments.isEmpty()) {
            Text("No fee payments recorded yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            data.recentFeePayments.forEachIndexed { idx, fee ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(fee.studentName, style = MaterialTheme.typography.titleSmall)
                        Text("${fee.className} • ${fee.receiptNumber} • ${fee.paymentDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            AcademicCalculations.formatInr(fee.paidAmount),
                            style = MaterialTheme.typography.titleSmall,
                            color = Emerald600,
                            fontWeight = FontWeight.Bold
                        )
                        if (fee.remainingBalance > 0) {
                            Text("Bal: ${AcademicCalculations.formatInr(fee.remainingBalance)}", style = MaterialTheme.typography.labelSmall, color = Crimson600)
                        }
                    }
                }
                if (idx < data.recentFeePayments.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun UpcomingExamsPanel(
    data: SchoolErpData,
    onNavigate: (ErpModule) -> Unit,
    modifier: Modifier = Modifier
) {
    ErpPanelCard(
        title = "Upcoming Tests & Exams",
        subtitle = "Scheduled assessments",
        modifier = modifier,
        trailing = { TextButton(onClick = { onNavigate(ErpModule.EXAMS) }) { Text("Manage") } }
    ) {
        if (data.upcomingExams.isEmpty()) {
            Text("No upcoming tests scheduled.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            data.upcomingExams.forEachIndexed { idx, exam ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.EventNote, null, tint = RoyalBlue600, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(exam.name, style = MaterialTheme.typography.titleSmall)
                        Text("${exam.className} • Starts ${exam.startDate}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusPill(exam.status, RoyalBlue600)
                }
                if (idx < data.upcomingExams.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun AnnouncementsPanel(
    data: SchoolErpData,
    onNavigate: (ErpModule) -> Unit,
    modifier: Modifier = Modifier
) {
    ErpPanelCard(
        title = "Important Announcements",
        subtitle = "School notice board",
        modifier = modifier,
        trailing = { TextButton(onClick = { onNavigate(ErpModule.HOMEWORK_NOTICES) }) { Text("Post Notice") } }
    ) {
        if (data.importantNotices.isEmpty()) {
            Text("No announcements posted yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            data.importantNotices.forEachIndexed { idx, notice ->
                val pColor = when (notice.priority) {
                    "URGENT" -> Crimson600
                    "IMPORTANT" -> GoldAmber500
                    else -> RoyalBlue600
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Default.Campaign, null, tint = pColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(notice.title, style = MaterialTheme.typography.titleSmall)
                        Text(notice.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        Text("${notice.publicationDate} • ${notice.targetClasses.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = pColor)
                    }
                }
                if (idx < data.importantNotices.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}
