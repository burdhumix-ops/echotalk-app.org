package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.auth.FirebaseAuthManager
import com.example.data.local.AppRoomDatabase
import com.example.data.local.OfflineQueueManager
import com.example.data.repository.AppTheme
import com.example.data.repository.EchoTalkRepository
import com.example.data.storage.DriveManager
import com.example.data.storage.GoogleDriveVaultService
import com.example.data.storage.StorageClassificationMatrix
import com.example.data.storage.StorageTarget
import com.example.fcm.FcmNotificationManager
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
    repository: EchoTalkRepository,
    onTriggerBackdoor: () -> Unit,
    onOpenVault: () -> Unit,
    onDismiss: () -> Unit,
    onOpenEditProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    val currentUser by repository.currentUser.collectAsState()
    val activeTheme by repository.theme.collectAsState()
    val users by repository.users.collectAsState()
    val isOffline by repository.isOffline.collectAsState()
    val pendingQueue by repository.pendingSyncQueue.collectAsState()
    val sysSecQueue by repository.sysSecVaultQueue.collectAsState()
    val fcmToken by repository.fcmToken.collectAsState()

    val authManager = remember { FirebaseAuthManager.getInstance(context) }
    val isGoogleSignedIn by authManager.isSignedIn.collectAsState()
    val googleEmail by authManager.userEmail.collectAsState()
    val isAuthenticating by authManager.isLoading.collectAsState()
    val authError by authManager.authError.collectAsState()
    val promptRestoreDialog by authManager.promptRestoreDialog.collectAsState()

    val driveManager = remember { DriveManager.getInstance(context) }
    val lastBackupStatus by driveManager.lastBackupStatus.collectAsState()

    val driveService = remember { GoogleDriveVaultService.getInstance(context) }
    val driveSyncStatus by driveService.syncStatusMessage.collectAsState()
    val isDriveSyncing by driveService.isSyncing.collectAsState()

    val roomDb = remember { AppRoomDatabase.getInstance(context) }
    val roomTasks by roomDb.pendingQueueDao().getAllPendingFlow().collectAsState(initial = emptyList())

    val isOwner = currentUser.role == "owner"
    var activeTab by remember { mutableStateOf(if (isOwner) "owner" else "themes") }
    var isGhostMode by remember { mutableStateOf(!currentUser.online) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .padding(14.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Settings & Engine",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Tab Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = mutableListOf("themes" to "Themes", "backups" to "Chat Backups", "offline" to "Room Queue", "privacy" to "Privacy")
                    if (isOwner) {
                        tabs.add(0, "owner" to "Owner Center")
                    }

                    tabs.forEach { (tabId, label) ->
                        val isSelected = activeTab == tabId
                        Button(
                            onClick = { activeTab = tabId },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }

                when (activeTab) {
                    // Hidden Owner Command Center (Exclusive to role: 'owner')
                    "owner" -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.heightIn(max = 380.dp)
                        ) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0x22E8A5C8),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8A5C8))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Owner Command Center", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE8A5C8))
                                        Text("Direct control over user verification, secret quarantine vault, and network pipelines.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            // 1. User Verification Manager Table
                            item {
                                Text("User Verification Manager", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("Toggle verified blue tick badge instantly for any member:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            items(users.filter { it.id != currentUser.id }) { member ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(member.name.take(1), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            }
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(member.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    if (member.isVerified) {
                                                        BlueTickBadge(size = 12.dp)
                                                    }
                                                }
                                                Text(member.handle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        Switch(
                                            checked = member.isVerified,
                                            onCheckedChange = { verified ->
                                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                                repository.adminSetVerification(currentUser.id, member.id, verified)
                                            }
                                        )
                                    }
                                }
                            }

                            // 2. Secret Vault Access
                            item {
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onOpenVault()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Inspect Owner Quarantine Vault", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Room Database & Offline Queue Engine Tab
                    "offline" -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.heightIn(max = 380.dp)
                        ) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Simulated Offline Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = if (isOffline) "Device offline: writes stored in Room DB with 0ms latency" else "Device online: live network sync active",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = isOffline,
                                            onCheckedChange = {
                                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                repository.toggleOfflineMode()
                                            }
                                        )
                                    }
                                }
                            }

                            // Room Database Queue Section
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("Room Database Pending Queue", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFF38BDF8).copy(alpha = 0.2f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("SQLite", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                                                }
                                            }
                                            Text("${roomTasks.size} tasks", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }

                                        Text(
                                            text = "Pending chat messages are safely stored in Room SQLite on disk and auto-flush once network is restored.",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (roomTasks.isNotEmpty()) {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                roomTasks.take(5).forEach { task ->
                                                    Surface(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(8.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text("To: ${task.recipientId} • ${task.text.take(24)}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                                Text("Status: ${task.status} | Type: ${task.taskType}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                                coroutineScope.launch {
                                                    OfflineQueueManager.getInstance(context).flushQueue()
                                                    repository.flushPendingQueue()
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Flush Offline Queue Automatically", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Chat Backups & Google Account Storage Tab
                    "backups", "drive_vault" -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.heightIn(max = 380.dp)
                        ) {
                            // 1. Google Account Link & Backup Status
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                Text("Google Account Storage", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                            if (isGoogleSignedIn) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Linked", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                                }
                                            }
                                        }

                                        // Subtle Status Indicator (Strictly inside Settings -> Chat Backups)
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(
                                                    text = lastBackupStatus,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        if (isGoogleSignedIn) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Connected Account", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(googleEmail ?: "Authenticated", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                                        authManager.signOut()
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text("Sign Out", fontSize = 10.sp)
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = "Sign in to enable silent encrypted backups directly into your Google Drive appDataFolder with zero cross-syncing.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                                        coroutineScope.launch {
                                                            authManager.signInWithGoogle(context)
                                                        }
                                                    },
                                                    enabled = !isAuthenticating,
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (isAuthenticating) "Connecting..." else "Sign in with Google", fontSize = 10.sp)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                                        authManager.simulateSignIn()
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.weight(0.7f)
                                                ) {
                                                    Text("Demo Sign-In", fontSize = 10.sp)
                                                }
                                            }
                                        }

                                        if (authError != null) {
                                            Text(
                                                text = authError ?: "",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }

                            // 2. Single-Tap Prompt upon Google Sign-In
                            if (promptRestoreDialog) {
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                                                Text("Restore chat history from Google Drive", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            }
                                            Text(
                                                text = "Google Account connected! Merge your cloud backup with current chats. Unsent offline messages will be protected.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(
                                                    onClick = { authManager.dismissRestorePrompt() }
                                                ) {
                                                    Text("Dismiss", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Button(
                                                    onClick = {
                                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                                        coroutineScope.launch {
                                                            repository.restoreChatHistoryFromDrive(context)
                                                            authManager.dismissRestorePrompt()
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Restore Now", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. Backup and Restore Actions
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Cloud Backup Actions", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                                    coroutineScope.launch {
                                                        repository.backupToDriveSilent(context)
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Back up now to Google Drive", fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                                    coroutineScope.launch {
                                                        repository.restoreChatHistoryFromDrive(context)
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Restore chat history from Google Drive", fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. Storage Classification Matrix
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Storage Classification Matrix", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text("Client-side partitioning: Public Social vs Private Google Account Vault", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                        // Server DB Card
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF38BDF8)))
                                                    Text("Server DB (Public / Social State)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Text(
                                                    text = StorageClassificationMatrix.STORAGE_ROUTING[StorageTarget.SERVER_DB]?.joinToString(", ") ?: "",
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        // Google Drive appDataFolder Card
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                                                    Text("Google Account Vault (appDataFolder)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Text(
                                                    text = StorageClassificationMatrix.STORAGE_ROUTING[StorageTarget.GOOGLE_DRIVE_APP_DATA]?.joinToString(", ") ?: "",
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                                            Text("Zero Cross-Syncing: Private chat backups are shielded from central tables.", fontSize = 9.sp, color = Color(0xFF10B981))
                                        }
                                    }
                                }
                            }

                            // 3. Firebase Cloud Messaging (FCM) Real-Time Push Notifications
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Firebase Cloud Messaging (FCM)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF10B981).copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("Active", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                            }
                                        }

                                        Text(
                                            text = "Token: ${fcmToken?.take(28) ?: "Fetching..."}...",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Button(
                                            onClick = {
                                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                                FcmNotificationManager.simulateIncomingPush(
                                                    context = context,
                                                    senderName = "Elena Rostova",
                                                    message = "Hey! Testing real-time FCM push notification delivery."
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Send Test FCM Push Notification", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Themes Tab (Includes Theme Preset 6: Velvet Rose Gold)
                    "themes" -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(AppTheme.entries.toList()) { t ->
                                val isSelected = activeTheme == t
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { repository.setTheme(t) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = t.displayName,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(text = t.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Privacy Tab
                    "privacy" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Ghost Mode Toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Ghost Mode (Incognito)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "Hide profile and presence from crew discovery", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = isGhostMode,
                                    onCheckedChange = {
                                        isGhostMode = it
                                        repository.setGhostMode(it)
                                    }
                                )
                            }

                            // Edit Profile Portal Shortcut
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onOpenEditProfile()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Edit Profile Portal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
