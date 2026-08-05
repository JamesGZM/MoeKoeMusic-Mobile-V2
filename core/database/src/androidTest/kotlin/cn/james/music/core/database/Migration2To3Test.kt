package cn.james.music.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration2To3Test {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MoeKoeDatabase::class.java,
        )

    @Test
    fun migrationPreservesQueueAndAddsNullableArtworkColumns() {
        helper.createDatabase(DATABASE_NAME, 2).apply {
            execSQL(
                "INSERT INTO playback_snapshot (id, current_index, position_ms, mode, updated_at_epoch_ms) VALUES (1, 0, 4200, 'RepeatAll', 99)",
            )
            execSQL(
                "INSERT INTO playback_queue_item (snapshot_id, queue_index, media_id, title, artist, album_title, source_type, source_value) VALUES (1, 0, 'fixture', 'Fixture', 'Artist', NULL, 'kugou', 'hash')",
            )
            close()
        }

        helper.runMigrationsAndValidate(DATABASE_NAME, 3, true, MIGRATION_2_3).use { database ->
            database
                .query(
                    "SELECT media_id, source_value, artwork_type, artwork_value FROM playback_queue_item",
                ).use { cursor ->
                    check(cursor.moveToFirst())
                    assertEquals("fixture", cursor.getString(0))
                    assertEquals("hash", cursor.getString(1))
                    assertNull(cursor.getString(2))
                    assertNull(cursor.getString(3))
                }
        }
    }

    @Test
    fun completeMigrationPathFromVersionOneIsValid() {
        helper.createDatabase(FULL_PATH_DATABASE_NAME, 1).close()

        helper
            .runMigrationsAndValidate(
                FULL_PATH_DATABASE_NAME,
                3,
                true,
                MIGRATION_1_2,
                MIGRATION_2_3,
            ).close()
    }

    private companion object {
        const val DATABASE_NAME = "migration-2-3-test"
        const val FULL_PATH_DATABASE_NAME = "migration-1-3-test"
    }
}
