package com.example.location_to_slack.data

import org.junit.Test
import java.io.File

class AppDatabaseSchemaTest {

    @Test
    fun testSchemaIsExported() {
        // ユニットテストの作業ディレクトリは app モジュール直下
        val schema = File("schemas/com.example.location_to_slack.data.AppDatabase/1.json")

        assert(schema.exists())
        assert(schema.readText().contains("\"tableName\": \"checkpoints\""))
    }
}
