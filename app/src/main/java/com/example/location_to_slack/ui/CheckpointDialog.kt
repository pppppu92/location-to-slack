package com.example.location_to_slack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.location_to_slack.data.Checkpoint

/**
 * チェックポイントの追加・編集を行うダイアログ
 */
@Composable
fun CheckpointDialog(
    initialCheckpoint: Checkpoint? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, latitude: Double, longitude: Double, radius: Float) -> Unit
) {
    var name by remember { mutableStateOf(initialCheckpoint?.name ?: "") }
    var latitudeText by remember { mutableStateOf(initialCheckpoint?.latitude?.toString() ?: "") }
    var longitudeText by remember { mutableStateOf(initialCheckpoint?.longitude?.toString() ?: "") }
    var radiusText by remember { mutableStateOf(initialCheckpoint?.radius?.toString() ?: "100") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (initialCheckpoint == null) "チェックポイント追加" else "チェックポイント編集")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名前 (例: 大学、自宅)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = latitudeText,
                    onValueChange = { latitudeText = it },
                    label = { Text("緯度 (Latitude 例: 35.6812)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = longitudeText,
                    onValueChange = { longitudeText = it },
                    label = { Text("経度 (Longitude 例: 139.7671)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = radiusText,
                    onValueChange = { radiusText = it },
                    label = { Text("検知半径 (メートル, デフォルト 100m)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latitudeText.toDoubleOrNull()
                    val lon = longitudeText.toDoubleOrNull()
                    val radius = radiusText.toFloatOrNull() ?: 100f

                    if (name.isBlank()) {
                        errorMessage = "名前を入力してください"
                        return@Button
                    }
                    if (lat == null || lat !in -90.0..90.0) {
                        errorMessage = "正しい緯度 (-90〜90) を入力してください"
                        return@Button
                    }
                    if (lon == null || lon !in -180.0..180.0) {
                        errorMessage = "正しい経度 (-180〜180) を入力してください"
                        return@Button
                    }
                    // 0 以下の半径は Geofence.Builder が例外を投げる
                    if (radius <= 0f) {
                        errorMessage = "検知半径は 0 より大きい値を入力してください"
                        return@Button
                    }

                    onConfirm(name.trim(), lat, lon, radius)
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
