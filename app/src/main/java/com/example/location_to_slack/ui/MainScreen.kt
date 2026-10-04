package com.example.location_to_slack.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/**
 * 権限警告カードに表示する文言。必要な権限がすべて許可済みなら null
 * バックグラウンド位置情報は、フォアグラウンドの位置情報の取得後に別途要求する
 */
internal fun permissionWarning(
    hasFineLocation: Boolean,
    hasNotification: Boolean,
    hasBackgroundLocation: Boolean
): String? = when {
    !hasFineLocation || !hasNotification ->
        "位置情報と通知権限を許可してください（バックグラウンド通知およびジオフェンス検知に使用します）。"
    !hasBackgroundLocation ->
        "位置情報を「常に許可」に設定してください（未設定の場合、バックグラウンドでのジオフェンス検知が働きません）。"
    else -> null
}

/**
 * メイン画面
 * 動作ON/OFFの切り替えおよびシンプルな状態表示
 */
@Composable
fun MainScreen(viewModel: CheckpointViewModel) {
    val context = LocalContext.current
    val isRunning by viewModel.isServiceRunning.collectAsState()
    val checkpoints by viewModel.checkpoints.collectAsState()
    val activeCount = checkpoints.count { it.isEnabled }

    fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    // 権限状態の保持（通知権限は Android 13 未満では不要）
    var hasFineLocationPermission by remember {
        mutableStateOf(isGranted(Manifest.permission.ACCESS_FINE_LOCATION))
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                isGranted(Manifest.permission.POST_NOTIFICATIONS)
        )
    }

    var hasBackgroundLocationPermission by remember {
        mutableStateOf(isGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
    }

    // バックグラウンド位置情報のリクエストランチャー（Android 10 のみダイアログで要求できる）
    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBackgroundLocationPermission = granted
    }

    // アプリの設定画面から戻ったときに権限状態を取り直す
    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasFineLocationPermission = isGranted(Manifest.permission.ACCESS_FINE_LOCATION)
        hasNotificationPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            isGranted(Manifest.permission.POST_NOTIFICATIONS)
        hasBackgroundLocationPermission = isGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    // Android 11 以降はダイアログで「常に許可」を選べないため、設定画面へ誘導する
    fun requestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            settingsLauncher.launch(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
            )
        } else {
            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    // 権限リクエストランチャー
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasFineLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        }
        // Android 10 はフォアグラウンド権限の取得後に続けて要求する（Android 11 以降は警告カードから設定画面へ）
        if (hasFineLocationPermission && !hasBackgroundLocationPermission &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R
        ) {
            requestBackgroundLocation()
        }
    }

    fun requestPermissions() {
        permissionLauncher.launch(
            listOfNotNull(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
            ).toTypedArray()
        )
    }

    val needsForegroundPermissions = !hasFineLocationPermission || !hasNotificationPermission
    val warning = permissionWarning(
        hasFineLocationPermission,
        hasNotificationPermission,
        hasBackgroundLocationPermission
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        // 権限警告カード
        AnimatedVisibility(visible = warning != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "権限の許可が必要です",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = warning.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (needsForegroundPermissions) requestPermissions() else requestBackgroundLocation()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(if (needsForegroundPermissions) "権限を許可する" else "「常に許可」に設定する")
                    }
                }
            }
        }

        // 状態表示インジケーター（大きなサークル）
        Box(
            modifier = Modifier
                .size(180.dp)
                .background(
                    color = if (isRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isRunning) "動作中" else "停止中",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isRunning) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isRunning) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${activeCount}件の地点を監視中",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // ON/OFF切り替えボタン
        Button(
            onClick = {
                if (hasFineLocationPermission) viewModel.toggleMonitoringService() else requestPermissions()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp)
        ) {
            Text(
                text = if (isRunning) "監視を停止する" else "監視を開始する",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isRunning) {
                "設定されたチェックポイントの検知半径内に入るとSlackに自動通知されます。"
            } else {
                "監視を開始するとバックグラウンドで進入検知を行います。"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
