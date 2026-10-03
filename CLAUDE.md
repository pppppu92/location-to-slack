# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## よく使うコマンド

ビルド・テスト・実行（Windows 環境）:
- `.\gradlew.bat assembleDebug` - APK のビルド
- `.\gradlew.bat installDebug` - emulator/device へのインストール
- `.\gradlew.bat testDebugUnitTest` - ユニットテスト実行
- `.\gradlew.bat testDebugUnitTest --tests "com.example.location_to_slack.ExampleUnitTest"` - 単一テスト実行
- `.\gradlew.bat connectedDebugAndroidTest` - インストルメンテーションテスト（端末必須）
- `.\gradlew.bat lintDebug` - lint 実行

## アーキテクチャ概要

### 全体フロー
**チェックポイント登録 → ジオフェンス登録 → 進入検知 → Slack 通知**

1. **UI層（Compose）** - MainActivity → MainScreen (ON/OFF), SettingsScreen (CRUD)
2. **ViewModel層** - CheckpointViewModel: サービス/ジオフェンス更新指示
3. **データ層** - Room DB: Checkpoint エンティティ（id, name, 緯度, 経度, 検知半径, 有効フラグ）
4. **Background層** - LocationForegroundService: CheckpointViewModel.toggleMonitoringService() から ACTION_START で起動、有効なチェックポイントを GeofenceManager で登録し継続監視
5. **Geofence API** - GeofenceManager: Play Services で登録/解除（フラグ: FLAG_MUTABLE, ENTER/EXIT監視）
6. **受信層** - GeofenceBroadcastReceiver: 進入/退出イベント受信、goAsync() で pendingResult 取得し Dispatchers.IO 上で DB 検索 → SlackNotifier 呼び出し
7. **Network層** - SlackNotifier: OkHttp で Webhook 送信

### Slack メッセージ形式
```
{チェックポイント名}を通過しました。
https://www.google.com/maps/search/?api=1&query={緯度},{経度}
```
タイムスタンプは含まれない。JSONペイロードは `{"text": "..."}` の形式のみ。

### 連続通知防止
- **isInsideAreaMap**: チェックポイントID ごとのエリア内/外フラグ。すでにエリア内かつ最後の通知から 30 分未満 (COOLDOWN_MILLIS) なら送信スキップ
- **lastNotifiedTimeMap**: チェックポイントID ごとの最後の通知時刻を記録。最後の通知から 5 分未満 (MIN_RENOTIFICATION_INTERVAL_MILLIS) なら過剰通知を防ぐ
- **メモリのみ**: 両マップはプロセス終了時にリセット

### Slack Webhook URL
- `local.properties` の `SLACK_WEBHOOK_URL` を build.gradle.kts で読み込み
- BuildConfig.SLACK_WEBHOOK_URL 経由で SlackNotifier が参照
- 未設定時はログ出力のみ

### パーミッション
- **Runtime 要求**: MainScreen で ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, POST_NOTIFICATIONS (Android 13+) を rememberLauncherForActivityResult で要求
- **ACCESS_BACKGROUND_LOCATION**: AndroidManifest で宣言済みだが、コードでは要求しない（ユーザーが手動で「常に許可」を設定する必要がある）
- **その他の権限**: FOREGROUND_SERVICE, FOREGROUND_SERVICE_LOCATION は AndroidManifest で宣言済み（FOREGROUND_SERVICE_LOCATION は Android 14 (API 34)+ 向け）

### DI・初期化方式
- **DI フレームワーク無し**: Hilt/Dagger 不使用
- **手動注入** - MainActivity: AppDatabase.getDatabase() → CheckpointRepository 生成 → viewModelFactory { initializer { ... } } で CheckpointViewModel へ
- **Singleton** - SlackNotifier: Kotlin `object`（context 不要）

### 注意点・Gotchas
- **PendingIntent フラグ** - FLAG_UPDATE_CURRENT | FLAG_MUTABLE: Geofencing API がインテントの extras を埋め込む必要があるため
- **再起動後のジオフェンス** - BootReceiver 無し：再起動後はサービス再起動まで登録されない
- **RoomDatabase** - fallbackToDestructiveMigration() でスキーマ変更により登録済みチェックポイントが削除される

## Git 運用
- **Issue 着手時のコミット先**: `origin/main` から `issue-{番号}-{英語の要約}` ブランチを新規に切ってコミット・push し、`main` 向けのドラフト PR を作成する。既存の worktree ブランチ（`worktree-*`）には直接コミットしない。この手順はユーザーへの確認なしで進めてよい。
- **未マージのブランチに依存する場合**: 着手する Issue が未マージのブランチの変更を前提とするときだけ、そのブランチから分岐し、PR のベースもそのブランチにする。PR 本文に依存関係を 1 行書く。