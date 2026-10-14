package uz.alimov.settingshelper

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import uz.alimov.settingshelper.ui.theme.SettingsHelperTheme

enum class LockScreenNotificationOption {
    SHOW_ALL,       // Show notifications and content
    HIDE_CONTENT,   // Show notifications, but hide content
    HIDE_ALL        // Hide notifications
}

class MainActivity : FragmentActivity() {

    private val isUnlocked = mutableStateOf(false)
    private val authError = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SettingsHelperTheme {
                if (isUnlocked.value) {
                    MainSettingsScreen(
                        onLockRequested = { isUnlocked.value = false }
                    )
                } else {
                    LockOverlayScreen(
                        authErrorText = authError.value,
                        onAuthenticateRequested = { promptBiometric() }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Automatically prompt for biometrics when activity starts or returns to foreground
        if (!isUnlocked.value) {
            promptBiometric()
        }
    }

    override fun onStop() {
        super.onStop()
        // Lock app immediately when moving to background
        isUnlocked.value = false
    }

    private fun promptBiometric() {
        val biometricManager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor = ContextCompat.getMainExecutor(this)
                val biometricPrompt = BiometricPrompt(
                    this,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            super.onAuthenticationSucceeded(result)
                            isUnlocked.value = true
                            authError.value = null
                        }

                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            super.onAuthenticationError(errorCode, errString)
                            authError.value = errString.toString()
                        }

                        override fun onAuthenticationFailed() {
                            super.onAuthenticationFailed()
                            authError.value = "Authentication failed. Please try again."
                        }
                    }
                )

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Unlock Settings Helper")
                    .setSubtitle("Authenticate using face, fingerprint, or device PIN")
                    .setAllowedAuthenticators(authenticators)
                    .build()

                biometricPrompt.authenticate(promptInfo)
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                authError.value = "No biometric hardware available on this device."
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                authError.value = "Biometric hardware is currently unavailable."
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                authError.value = "No biometrics or device PIN set up. Please set up security in system settings."
            }
            else -> {
                authError.value = "Biometric authentication is unavailable."
            }
        }
    }
}

@Composable
fun LockOverlayScreen(
    authErrorText: String?,
    onAuthenticateRequested: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Settings Helper",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "App is locked for security",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!authErrorText.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = authErrorText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onAuthenticateRequested,
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock with Biometrics")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsScreen(
    onLockRequested: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(checkWriteSecureSettingsPermission(context))
    }
    var currentOption by remember {
        mutableStateOf(readCurrentLockScreenOption(context))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Lock Screen Notifications",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Settings Helper",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            hasPermission = checkWriteSecureSettingsPermission(context)
                            currentOption = readCurrentLockScreenOption(context)
                            Toast.makeText(context, "Refreshed settings", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onLockRequested) {
                        Icon(Icons.Default.Lock, contentDescription = "Lock App")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!hasPermission) {
                PermissionRequiredCard(
                    onCheckAgain = {
                        hasPermission = checkWriteSecureSettingsPermission(context)
                        if (hasPermission) {
                            Toast.makeText(context, "Permission granted!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Notification Display Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select how notification contents are displayed on your device lock screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    NotificationOptionItem(
                        title = "Show notifications and content",
                        description = "All notification details and content are visible on the lock screen.",
                        selected = currentOption == LockScreenNotificationOption.SHOW_ALL,
                        onSelect = {
                            currentOption = LockScreenNotificationOption.SHOW_ALL
                            applyLockScreenOption(context, LockScreenNotificationOption.SHOW_ALL)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    NotificationOptionItem(
                        title = "Show notifications, but hide content",
                        description = "Notifications appear on the lock screen, but sensitive contents remain hidden until unlocked.",
                        selected = currentOption == LockScreenNotificationOption.HIDE_CONTENT,
                        onSelect = {
                            currentOption = LockScreenNotificationOption.HIDE_CONTENT
                            applyLockScreenOption(context, LockScreenNotificationOption.HIDE_CONTENT)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    NotificationOptionItem(
                        title = "Hide notifications",
                        description = "No notifications are shown on the lock screen at all.",
                        selected = currentOption == LockScreenNotificationOption.HIDE_ALL,
                        onSelect = {
                            currentOption = LockScreenNotificationOption.HIDE_ALL
                            applyLockScreenOption(context, LockScreenNotificationOption.HIDE_ALL)
                        }
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "System Settings Fallback",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Open system notification settings directly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = { openSystemNotificationSettings(context) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Settings")
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationOptionItem(
    title: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionRequiredCard(
    onCheckAgain: () -> Unit
) {
    val context = LocalContext.current
    val adbCommand = "adb shell pm grant uz.alimov.settingshelper android.permission.WRITE_SECURE_SETTINGS"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ADB Permission Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "To directly change system settings, run this one-time ADB command on a PC or via Shizuku:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = adbCommand,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("ADB Command", adbCommand)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied ADB command", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy command"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onCheckAgain) {
                    Text("Check Permission")
                }
            }
        }
    }
}

private fun checkWriteSecureSettingsPermission(context: Context): Boolean {
    return context.checkCallingOrSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED
}

private fun readCurrentLockScreenOption(context: Context): LockScreenNotificationOption {
    val showNotifications = Settings.Secure.getInt(
        context.contentResolver,
        "lock_screen_show_notifications",
        1
    )
    val allowPrivateNotifications = Settings.Secure.getInt(
        context.contentResolver,
        "lock_screen_allow_private_notifications",
        1
    )

    return when {
        showNotifications == 0 -> LockScreenNotificationOption.HIDE_ALL
        allowPrivateNotifications == 1 -> LockScreenNotificationOption.SHOW_ALL
        else -> LockScreenNotificationOption.HIDE_CONTENT
    }
}

private fun applyLockScreenOption(context: Context, option: LockScreenNotificationOption) {
    val (showNotifs, allowPrivate) = when (option) {
        LockScreenNotificationOption.SHOW_ALL -> Pair(1, 1)
        LockScreenNotificationOption.HIDE_CONTENT -> Pair(1, 0)
        LockScreenNotificationOption.HIDE_ALL -> Pair(0, 0)
    }

    try {
        Settings.Secure.putInt(
            context.contentResolver,
            "lock_screen_show_notifications",
            showNotifs
        )
        Settings.Secure.putInt(
            context.contentResolver,
            "lock_screen_allow_private_notifications",
            allowPrivate
        )
        Toast.makeText(context, "Lock screen notification setting updated", Toast.LENGTH_SHORT).show()
    } catch (_: SecurityException) {
        Toast.makeText(
            context,
            "Permission denied. Grant WRITE_SECURE_SETTINGS via ADB.",
            Toast.LENGTH_LONG
        ).show()
    }
}

private fun openSystemNotificationSettings(context: Context) {
    try {
        val intent = Intent("android.settings.ACTION_NOTIFICATION_SETTINGS")
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Unable to open system settings", Toast.LENGTH_SHORT).show()
        }
    }
}
