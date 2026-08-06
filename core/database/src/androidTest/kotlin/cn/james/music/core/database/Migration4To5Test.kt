package cn.james.music.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration4To5Test {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MoeKoeDatabase::class.java,
        )

    @Test
    fun migrationPreservesExistingDataAndCreatesEmptyHomeContentCache() {
        helper.createDatabase(DATABASE_NAME, 4).apply {
            execSQL(
                "INSERT INTO playback_snapshot (id, current_index, position_ms, mode, updated_at_epoch_ms) VALUES (1, 0, 4200, 'RepeatAll', 99)",
            )
            execSQL(
                "INSERT INTO playback_queue_item (snapshot_id, queue_index, media_id, title, artist, album_title, source_type, source_value, artwork_type, artwork_value) VALUES (1, 0, 'fixture', 'Fixture', 'Artist', NULL, 'kugou', 'hash', NULL, NULL)",
            )
            execSQL(
                "INSERT INTO local_music (id, storageKey, displayName, title, artist, albumTitle, durationMs, mimeType, sizeBytes, sha256, artworkKey, source, importedAtEpochMs, pendingDeletion) VALUES ('local', 'local.audio', 'local.wav', 'Local', 'Artist', NULL, 1000, 'audio/wav', 512, 'fixture-sha', NULL, 'DocumentPicker', 100, 0)",
            )
            execSQL(
                "INSERT INTO lyrics_cache (source_key, krc_text, parser_version, updated_at_epoch_ms) VALUES ('kugou:fixture-hash', 'fixture-krc', 1, 100)",
            )
            close()
        }

        helper.runMigrationsAndValidate(DATABASE_NAME, 5, true, MIGRATION_4_5).use { database ->
            assertSingleTextValue(database, "SELECT media_id FROM playback_queue_item", "fixture")
            assertSingleTextValue(database, "SELECT id FROM local_music", "local")
            assertSingleTextValue(database, "SELECT source_key FROM lyrics_cache", "kugou:fixture-hash")
            database.query("SELECT COUNT(*) FROM home_content_snapshots").use { cursor ->
                check(cursor.moveToFirst())
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    @Test
    fun completeMigrationPathFromVersionOneIsValid() {
        helper.createDatabase(FULL_PATH_DATABASE_NAME, 1).close()

        helper
            .runMigrationsAndValidate(
                FULL_PATH_DATABASE_NAME,
                5,
                true,
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
            ).close()
    }

    private fun assertSingleTextValue(
        database: androidx.sqlite.db.SupportSQLiteDatabase,
        query: String,
        expected: String,
    ) {
        database.query(query).use { cursor ->
            check(cursor.moveToFirst())
            assertEquals(expected, cursor.getString(0))
        }
    }

    private companion object {
        const val DATABASE_NAME = "migration-4-5-test"
        const val FULL_PATH_DATABASE_NAME = "migration-1-5-test"
    }
}
