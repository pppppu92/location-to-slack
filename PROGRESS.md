# PROGRESS

- [GitHub Issue 未起票・認証待ち] 再起動後にジオフェンスが再登録されない（BootReceiver 無し）
- [GitHub Issue 未起票・認証待ち] START_STICKY による再起動時（intent=null）に startForeground もジオフェンス再登録も行われず、isRunning が false のままになる
- [GitHub Issue 未起票・認証待ち] 連続通知防止の状態がメモリのみで、プロセス終了で失われ重複通知しうる
- [GitHub Issue 未起票・認証待ち] fallbackToDestructiveMigration によりスキーマ変更で登録済みチェックポイントが消える（exportSchema=false）
- [GitHub Issue 未起票・認証待ち] ACCESS_BACKGROUND_LOCATION をアプリ内で要求・案内していない
- [GitHub Issue 未起票・認証待ち] Slack Webhook URL が BuildConfig 経由で APK に埋め込まれる（アプリ内設定と安全な保存へ）
- [GitHub Issue 未起票・認証待ち] Slack 送信失敗時のリトライが無い
- [GitHub Issue 未起票・認証待ち] ジオフェンス登録失敗（権限なし・上限 100 件超）が UI に伝わらず「動作中」表示のままになる
- [GitHub Issue 未起票・認証待ち] ユニットテストがテンプレートのみ（メッセージ形式・連続通知防止ロジックのテストが無い）
- [GitHub Issue 未起票・認証待ち] プロジェクトパスに非 ASCII 文字があるとビルドが止まり、overridePathCheck を付けてもユニットテストが ClassNotFoundException で失敗する
- [GitHub Issue 未起票・認証待ち] 依存関係が古い（Kotlin 1.9.0 / Compose BOM 2024.04.01 / targetSdk 34）
