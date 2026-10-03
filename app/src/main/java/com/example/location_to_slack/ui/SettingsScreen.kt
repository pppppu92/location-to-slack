package com.example.location_to_slack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.location_to_slack.data.Checkpoint

/**
 * 設定画面（チェックポイント管理）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: CheckpointViewModel) {
    val checkpoints by viewModel.checkpoints.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editingCheckpoint by remember { mutableStateOf<Checkpoint?>(null) }
    var checkpointToDelete by remember { mutableStateOf<Checkpoint?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("チェックポイント設定") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCheckpoint = null
                    showDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "チェックポイント追加")
            }
        }
    ) { paddingValues ->
        if (checkpoints.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "登録されたチェックポイントはありません。\n右下の ＋ ボタンから追加してください。",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(checkpoints, key = { it.id }) { checkpoint ->
                    CheckpointCard(
                        checkpoint = checkpoint,
                        onToggle = { viewModel.toggleCheckpoint(checkpoint) },
                        onEdit = {
                            editingCheckpoint = checkpoint
                            showDialog = true
                        },
                        onDelete = { checkpointToDelete = checkpoint }
                    )
                }
            }
        }
    }

    // 追加・編集ダイアログ
    if (showDialog) {
        val editing = editingCheckpoint
        CheckpointDialog(
            initialCheckpoint = editing,
            onDismiss = { showDialog = false },
            onConfirm = { name, lat, lon, radius ->
                if (editing != null) {
                    viewModel.updateCheckpoint(
                        editing.copy(name = name, latitude = lat, longitude = lon, radius = radius)
                    )
                } else {
                    viewModel.addCheckpoint(name, lat, lon, radius)
                }
                showDialog = false
            }
        )
    }

    // 削除確認ダイアログ
    checkpointToDelete?.let { checkpoint ->
        AlertDialog(
            onDismissRequest = { checkpointToDelete = null },
            title = { Text("削除の確認") },
            text = { Text("「${checkpoint.name}」を削除してもよろしいですか？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCheckpoint(checkpoint)
                        checkpointToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("削除")
                }
            },
            dismissButton = {
                TextButton(onClick = { checkpointToDelete = null }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
private fun CheckpointCard(
    checkpoint: Checkpoint,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (checkpoint.isEnabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (checkpoint.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = checkpoint.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "緯度: ${checkpoint.latitude}, 経度: ${checkpoint.longitude}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "検知半径: ${checkpoint.radius.toInt()}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 有効/無効スイッチ
            Switch(
                checked = checkpoint.isEnabled,
                onCheckedChange = { onToggle() }
            )

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "編集",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "削除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
