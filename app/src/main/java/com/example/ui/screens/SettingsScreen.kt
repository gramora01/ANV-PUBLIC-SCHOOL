package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.SchoolSettings
import com.example.ui.ErpModule
import com.example.ui.SchoolErpData
import com.example.ui.components.ErpPanelCard
import com.example.ui.components.ErpSectionHeader
import com.example.ui.theme.Crimson600
import com.example.ui.theme.RoyalBlue600
import com.example.util.ImageHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    data: SchoolErpData,
    activeThemeMode: String,
    onSaveSettings: (SchoolSettings) -> Unit,
    onUpdateTheme: (String) -> Unit,
    onSeedClasses: () -> Unit,
    onNavigate: (ErpModule) -> Unit,
    onExportBackup: ((String) -> Unit) -> Unit,
    onRestoreBackup: (String) -> Unit,
    onSignOut: () -> Unit,
    onNotify: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val s = data.settings

    var schoolName by remember(s.schoolName) { mutableStateOf(s.schoolName) }
    var schoolAddress by remember(s.schoolAddress) { mutableStateOf(s.schoolAddress) }
    var academicSession by remember(s.academicSession) { mutableStateOf(s.academicSession) }
    var phone by remember(s.phone) { mutableStateOf(s.phone) }
    var email by remember(s.email) { mutableStateOf(s.email) }
    var affiliationNote by remember(s.affiliationNote) { mutableStateOf(s.affiliationNote) }
    var receiptPrefix by remember(s.receiptPrefix) { mutableStateOf(s.receiptPrefix) }
    var authorizedSignatory by remember(s.authorizedSignatory) { mutableStateOf(s.authorizedSignatory) }
    var logoBase64 by remember(s.logoBase64) { mutableStateOf(s.logoBase64) }

    var pendingBackupJson by remember { mutableStateOf<String?>(null) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val b64 = withContext(Dispatchers.IO) { ImageHelper.uriToCompressedBase64(context, uri, 220) }
                if (b64 != null) {
                    logoBase64 = b64
                    onNotify("Logo ready — tap Save School Settings to apply.", false)
                } else {
                    onNotify("Unable to read selected image.", true)
                }
            }
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingBackupJson
        if (uri != null && json != null) {
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(json.toByteArray(Charsets.UTF_8))
                            out.flush()
                        } ?: error("Cannot write backup file.")
                    }
                }
                res.onSuccess { onNotify("School ERP JSON backup exported.", false) }
                    .onFailure { onNotify("Failed to write backup file.", true) }
            }
        }
        pendingBackupJson = null
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }.getOrNull()
                }
                if (!json.isNullOrBlank()) {
                    pendingRestoreJson = json
                } else {
                    onNotify("Could not read selected JSON backup file.", true)
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(6.dp))
            ErpSectionHeader(
                title = "School Settings",
                subtitle = "Institution profile, logo, theme, academic session & cloud backups"
            )
        }

        // School Branding & Logo
        item {
            ErpPanelCard(
                title = "Institution Profile & Logo",
                subtitle = "Appears on all dashboards, fee receipts, marksheets & A4 admit cards"
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    val customLogo = remember(logoBase64) { ImageHelper.base64ToImageBitmap(logoBase64) }
                    if (customLogo != null) {
                        Image(
                            bitmap = customLogo,
                            contentDescription = "School Logo",
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .border(2.dp, RoyalBlue600, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_school_crest),
                            contentDescription = "Default School Crest",
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .border(2.dp, RoyalBlue600, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                logoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        ) {
                            Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Upload School Logo")
                        }
                        if (!logoBase64.isNullOrBlank()) {
                            TextButton(onClick = { logoBase64 = null }) {
                                Icon(Icons.Default.DeleteOutline, null, tint = Crimson600, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Reset to Default Crest", color = Crimson600)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = schoolName, onValueChange = { schoolName = it }, label = { Text("School Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = schoolAddress, onValueChange = { schoolAddress = it }, label = { Text("School Address") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = academicSession, onValueChange = { academicSession = it }, label = { Text("Academic Session") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = receiptPrefix, onValueChange = { receiptPrefix = it }, label = { Text("Receipt Prefix") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("School Phone") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("School Email") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = affiliationNote, onValueChange = { affiliationNote = it }, label = { Text("Affiliation / Tagline") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = authorizedSignatory, onValueChange = { authorizedSignatory = it }, label = { Text("Authorized Signatory Label") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        onSaveSettings(
                            s.copy(
                                schoolName = schoolName.trim().ifBlank { "ANV PUBLIC SCHOOL" },
                                schoolAddress = schoolAddress.trim().ifBlank { "Nagaliya Mev, Gajraula, Uttar Pradesh, India" },
                                academicSession = academicSession.trim().ifBlank { "2026–2027" },
                                phone = phone.trim(),
                                email = email.trim(),
                                affiliationNote = affiliationNote.trim(),
                                receiptPrefix = receiptPrefix.trim().ifBlank { "ANV-2627-" },
                                authorizedSignatory = authorizedSignatory.trim().ifBlank { "Principal / Authorized Signatory" },
                                themeMode = activeThemeMode,
                                logoBase64 = logoBase64
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_settings_button")
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Save School Settings")
                }
            }
        }

        // Theme Settings
        item {
            ErpPanelCard(title = "Appearance & Theme", subtitle = "Switch between System, Light & Dark ERP themes") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("SYSTEM" to "System Default", "LIGHT" to "Light Mode", "DARK" to "Dark Mode").forEach { (mode, label) ->
                        FilterChip(
                            selected = activeThemeMode.uppercase() == mode,
                            onClick = { onUpdateTheme(mode) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        // Academic Setup Shortcuts
        item {
            ErpPanelCard(title = "Academic Structure", subtitle = "Manage Nursery–Class 8, custom classes & dynamic subjects") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onSeedClasses, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Class, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Load Nursery–8")
                    }
                    Button(onClick = { onNavigate(ErpModule.CLASSES) }, modifier = Modifier.weight(1f)) {
                        Text("Manage Classes")
                    }
                }
            }
        }

        // Backup & Restore
        item {
            ErpPanelCard(
                title = "Data Backup & Restore",
                subtitle = "Export a complete JSON snapshot or restore school records"
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            onExportBackup { json ->
                                pendingBackupJson = json
                                exportBackupLauncher.launch("ANV_Public_School_Backup.json")
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_backup_button")
                    ) {
                        Icon(Icons.Default.Backup, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export Backup")
                    }
                    OutlinedButton(
                        onClick = { restoreBackupLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("restore_backup_button")
                    ) {
                        Icon(Icons.Default.Restore, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Restore JSON")
                    }
                }
            }
        }

        // Account & Sign Out
        item {
            val user = Firebase.auth.currentUser
            ErpPanelCard(
                title = "Administrator Account",
                subtitle = user?.email ?: user?.displayName ?: "Authenticated Google Administrator"
            ) {
                Text(
                    "All records are isolated under your private school cloud workspace with strict owner-only access rules.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onSignOut,
                    colors = ButtonDefaults.buttonColors(containerColor = Crimson600),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    pendingRestoreJson?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingRestoreJson = null },
            title = { Text("Restore School Backup?") },
            text = { Text("This will merge and overwrite matching records from the selected JSON backup into your cloud school workspace.") },
            confirmButton = {
                Button(onClick = {
                    onRestoreBackup(json)
                    pendingRestoreJson = null
                }) { Text("Restore Now") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreJson = null }) { Text("Cancel") }
            }
        )
    }
}
