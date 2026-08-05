package cn.james.music.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration1To2Test {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MoeKoeDatabase::class.java,
        )

    @Test
    fun migrationPreservesPlaybackSnapshotAndCreatesLocalMusicTables() {
        helper.createDatabase(DATABASE_NAME, 1).apply {
            execSQL(
                "INSERT INTO playback_snapshot (id, current_index, position_ms, mode, updated_at_epoch_ms) VALUES (1, 0, 4200, 'RepeatAll', 99)",
            )
            close()
        }

        helper.runMigrationsAndValidate(DATABASE_NAME, 2, true, MIGRATION_1_2).use { database ->
            database.query("SELECT position_ms, mode FROM playback_snapshot").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(4_200, cursor.getLong(0))
                assertEquals("RepeatAll", cursor.getString(1))
            }
            database
                .query(
                    "SELECT name FROM sqlite_master WHERE type = 'table' AND name LIKE 'local_%' ORDER BY name",
                ).use { cursor ->
                    val tables =
                        buildList {
                            while (cursor.moveToNext()) add(cursor.getString(0))
                        }
                    assertEquals(
                        listOf("local_import_batch", "local_import_entry", "local_music"),
                        tables,
                    )
                }
        }
    }

    private companion object {
        const val DATABASE_NAME = "migration-1-2-test"
    }
}
