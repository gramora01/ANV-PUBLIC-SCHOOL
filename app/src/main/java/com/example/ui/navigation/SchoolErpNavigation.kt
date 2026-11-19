package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.firebase.SchoolRepository
import com.example.data.firebase.ThemePreferences
import com.example.data.model.SchoolSettings
import com.example.ui.ErpModule
import com.example.ui.ErpUiState
import com.example.ui.SchoolErpData
import com.example.ui.SchoolViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutAdmin
import com.example.ui.components.NotificationToastOverlay
import com.example.ui.screens.AdmitCardScreen
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.ClassesAndSubjectsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExamsAndResultsScreen
import com.example.ui.screens.FeesScreen
import com.example.ui.screens.HomeworkAndNoticesScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudentsScreen
import com.example.ui.screens.TeachersScreen
import com.example.ui.screens.TimetableScreen
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.RoyalBlue600
import com.example.util.ImageHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val auth = Firebase.auth
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    auth.addAuthStateListener(listener)
    trySend(auth.currentUser)
    awaitClose { auth.removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(
    themePreferences: ThemePreferences,
    activeThemeMode: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val currentUser by remember { authStateFlow() }.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            val viewModel: SchoolViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        SchoolViewModel(
                            repository = SchoolRepository(context.applicationContext),
                            themePreferences = themePreferences,
                            currentUserId = user.uid
                        )
                    }
                }
            )
            SchoolErpShell(
                viewModel = viewModel,
                activeThemeMode = activeThemeMode,
                onSignOut = {
                    signOutAdmin(
                        credentialManager = credentialManager,
                        scope = scope
                    )
                }
            )
        }
    }
}

private fun moduleIcon(module: ErpModule): ImageVector = when (module) {
    ErpModule.DASHBOARD -> Icons.Default.Dashboard
    ErpModule.STUDENTS -> Icons.Default.Groups
    ErpModule.CLASSES -> Icons.Default.Class
    ErpModule.FEES -> Icons.Default.AccountBalanceWallet
    ErpModule.ATTENDANCE -> Icons.Default.FactCheck
    ErpModule.EXAMS -> Icons.Default.EventNote
    ErpModule.ADMIT_CARDS -> Icons.Default.Badge
    ErpModule.TIMETABLE -> Icons.Default.Schedule
    ErpModule.HOMEWORK_NOTICES -> Icons.Default.Campaign
    ErpModule.TEACHERS -> Icons.Default.School
    ErpModule.REPORTS -> Icons.Default.Assessment
    ErpModule.SETTINGS -> Icons.Default.Settings
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchoolErpShell(
    viewModel: SchoolViewModel,
    activeThemeMode: String,
    onSignOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedModule by viewModel.selectedModule.collectAsStateWithLifecycle()
    val notification by viewModel.notification.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(notification?.id) {
        if (notification != null) {
            delay(3500)
            viewModel.dismissNotification()
        }
    }

    BackHandler(enabled = selectedModule != ErpModule.DASHBOARD) {
        viewModel.selectModule(ErpModule.DASHBOARD)
    }

    val settings = (uiState as? ErpUiState.Success)?.data?.settings ?: SchoolSettings()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 840.dp

        if (isExpanded) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(modifier = Modifier.width(268.dp)) {
                        SidebarDrawerContent(
                            settings = settings,
                            selectedModule = selectedModule,
                            onSelectModule = { viewModel.selectModule(it) }
                        )
                    }
                }
            ) {
                ErpScaffoldContent(
                    isExpanded = true,
                    selectedModule = selectedModule,
                    settings = settings,
                    activeThemeMode = activeThemeMode,
                    isBusy = isBusy,
                    notification = notification,
                    uiState = uiState,
                    onOpenDrawer = {},
                    onToggleTheme = {
                        val next = if (activeThemeMode.uppercase() == "DARK") "LIGHT" else "DARK"
                        viewModel.updateThemeMode(next, settings)
                    },
                    onDismissNotification = viewModel::dismissNotification,
                    onSelectModule = viewModel::selectModule,
                    viewModel = viewModel,
                    onSignOut = onSignOut
                )
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(modifier = Modifier.width(284.dp)) {
                        SidebarDrawerContent(
                            settings = settings,
                            selectedModule = selectedModule,
                            onSelectModule = { mod ->
                                viewModel.selectModule(mod)
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                }
            ) {
                ErpScaffoldContent(
                    isExpanded = false,
                    selectedModule = selectedModule,
                    settings = settings,
                    activeThemeMode = activeThemeMode,
                    isBusy = isBusy,
                    notification = notification,
                    uiState = uiState,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onToggleTheme = {
                        val next = if (activeThemeMode.uppercase() == "DARK") "LIGHT" else "DARK"
                        viewModel.updateThemeMode(next, settings)
                    },
                    onDismissNotification = viewModel::dismissNotification,
                    onSelectModule = viewModel::selectModule,
                    viewModel = viewModel,
                    onSignOut = onSignOut
                )
            }
        }
    }
}

@Composable
private fun SidebarDrawerContent(
    settings: SchoolSettings,
    selectedModule: ErpModule,
    onSelectModule: (ErpModule) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Navy900, Navy800, RoyalBlue600)))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val logoBmp = remember(settings.logoBase64) { ImageHelper.base64ToImageBitmap(settings.logoBase64) }
                if (logoBmp != null) {
                    Image(
                        bitmap = logoBmp,
                        contentDescription = "School Logo",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_school_crest),
                        contentDescription = "ANV School Crest",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = settings.schoolName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Session ${settings.academicSession}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        ErpModule.entries.forEach { module ->
            NavigationDrawerItem(
                icon = { Icon(moduleIcon(module), contentDescription = module.title) },
                label = { Text(module.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                selected = selectedModule == module,
                onClick = { onSelectModule(module) },
                modifier = Modifier
                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                    .testTag("nav_drawer_${module.name.lowercase()}")
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErpScaffoldContent(
    isExpanded: Boolean,
    selectedModule: ErpModule,
    settings: SchoolSettings,
    activeThemeMode: String,
    isBusy: Boolean,
    notification: com.example.ui.NotificationBanner?,
    uiState: ErpUiState,
    onOpenDrawer: () -> Unit,
    onToggleTheme: () -> Unit,
    onDismissNotification: () -> Unit,
    onSelectModule: (ErpModule) -> Unit,
    viewModel: SchoolViewModel,
    onSignOut: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = selectedModule.title,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${settings.schoolName} • ${settings.academicSession}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    if (!isExpanded) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.testTag("open_sidebar_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Navigation Sidebar")
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("quick_theme_toggle")
                    ) {
                        Icon(
                            imageVector = if (activeThemeMode.uppercase() == "DARK") Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Light or Dark Theme"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (!isExpanded) {
                NavigationBar {
                    val primaryTabs = listOf(
                        ErpModule.DASHBOARD,
                        ErpModule.STUDENTS,
                        ErpModule.FEES,
                        ErpModule.ATTENDANCE
                    )
                    primaryTabs.forEach { mod ->
                        NavigationBarItem(
                            selected = selectedModule == mod,
                            onClick = { onSelectModule(mod) },
                            icon = { Icon(moduleIcon(mod), contentDescription = mod.title) },
                            label = { Text(mod.shortLabel, maxLines = 1) }
                        )
                    }
                    NavigationBarItem(
                        selected = selectedModule !in primaryTabs,
                        onClick = onOpenDrawer,
                        icon = { Icon(Icons.Default.Menu, contentDescription = "More Modules") },
                        label = { Text("More", maxLines = 1) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isBusy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                when (val state = uiState) {
                    is ErpUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = RoyalBlue600)
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Loading ANV Public School ERP...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    is ErpUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Unable to load school records", style = MaterialTheme.typography.titleLarge)
                                Text(state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(onClick = { viewModel.selectModule(ErpModule.DASHBOARD) }) {
                                        Text("Retry")
                                    }
                                    OutlinedButton(onClick = onSignOut) {
                                        Text("Sign Out")
                                    }
                                }
                            }
                        }
                    }

                    is ErpUiState.Success -> {
                        ErpModuleBody(
                            module = selectedModule,
                            data = state.data,
                            activeThemeMode = activeThemeMode,
                            viewModel = viewModel,
                            onSignOut = onSignOut
                        )
                    }
                }
            }

            Box(modifier = Modifier.align(Alignment.TopCenter)) {
                NotificationToastOverlay(
                    banner = notification,
                    onDismiss = onDismissNotification
                )
            }
        }
    }
}

@Composable
private fun ErpModuleBody(
    module: ErpModule,
    data: SchoolErpData,
    activeThemeMode: String,
    viewModel: SchoolViewModel,
    onSignOut: () -> Unit
) {
    when (module) {
        ErpModule.DASHBOARD -> DashboardScreen(
            data = data,
            onNavigate = viewModel::selectModule,
            onSeedClasses = { viewModel.seedStandardClasses(data.classes) }
        )

        ErpModule.STUDENTS -> StudentsScreen(
            data = data,
            onSaveStudent = viewModel::saveStudent,
            onDeleteStudent = viewModel::deleteStudent,
            onNotify = viewModel::notify
        )

        ErpModule.CLASSES -> ClassesAndSubjectsScreen(
            data = data,
            onSeedStandardClasses = { viewModel.seedStandardClasses(data.classes) },
            onSaveClass = viewModel::saveClass,
            onDeleteClass = viewModel::deleteClass,
            onSaveSubject = viewModel::saveSubject,
            onDeleteSubject = viewModel::deleteSubject
        )

        ErpModule.TEACHERS -> TeachersScreen(
            data = data,
            onSaveTeacher = viewModel::saveTeacher,
            onDeleteTeacher = viewModel::deleteTeacher
        )

        ErpModule.FEES -> FeesScreen(
            data = data,
            onSaveFee = viewModel::saveFeeRecord,
            onDeleteFee = viewModel::deleteFeeRecord,
            onNotify = viewModel::notify
        )

        ErpModule.ATTENDANCE -> AttendanceScreen(
            data = data,
            onSaveAttendance = viewModel::saveAttendanceBatch,
            onNotify = viewModel::notify
        )

        ErpModule.EXAMS -> ExamsAndResultsScreen(
            data = data,
            onSaveExam = viewModel::saveExam,
            onDeleteExam = viewModel::deleteExam,
            onSaveSchedule = viewModel::saveExamSchedule,
            onDeleteSchedule = viewModel::deleteExamSchedule,
            onSaveMark = viewModel::saveStudentMark,
            onDeleteMark = viewModel::deleteStudentMark,
            onNotify = viewModel::notify
        )

        ErpModule.ADMIT_CARDS -> AdmitCardScreen(
            data = data,
            onSaveSchedule = viewModel::saveExamSchedule,
            onDeleteSchedule = viewModel::deleteExamSchedule,
            onNotify = viewModel::notify
        )

        ErpModule.TIMETABLE -> TimetableScreen(
            data = data,
            onSaveEntry = viewModel::saveTimetableEntry,
            onDeleteEntry = viewModel::deleteTimetableEntry,
            onNotify = viewModel::notify
        )

        ErpModule.HOMEWORK_NOTICES -> HomeworkAndNoticesScreen(
            data = data,
            onSaveHomework = viewModel::saveHomework,
            onDeleteHomework = viewModel::deleteHomework,
            onSaveNotice = viewModel::saveNotice,
            onDeleteNotice = viewModel::deleteNotice
        )

        ErpModule.REPORTS -> ReportsScreen(
            data = data,
            onNotify = viewModel::notify
        )

        ErpModule.SETTINGS -> SettingsScreen(
            data = data,
            activeThemeMode = activeThemeMode,
            onSaveSettings = viewModel::saveSettings,
            onUpdateTheme = { mode -> viewModel.updateThemeMode(mode, data.settings) },
            onSeedClasses = { viewModel.seedStandardClasses(data.classes) },
            onNavigate = viewModel::selectModule,
            onExportBackup = viewModel::exportBackup,
            onRestoreBackup = viewModel::restoreBackup,
            onSignOut = onSignOut,
            onNotify = viewModel::notify
        )
    }
}
