@file:Suppress("ktlint:standard:max-line-length")

package cn.james.music.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `local_music` (`id` TEXT NOT NULL, `storageKey` TEXT NOT NULL, `displayName` TEXT NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `albumTitle` TEXT, `durationMs` INTEGER NOT NULL, `mimeType` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, `sha256` TEXT NOT NULL, `artworkKey` TEXT, `source` TEXT NOT NULL, `importedAtEpochMs` INTEGER NOT NULL, `pendingDeletion` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))",
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_local_music_sizeBytes_sha256` ON `local_music` (`sizeBytes`, `sha256`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `local_import_batch` (`id` TEXT NOT NULL, `source` TEXT NOT NULL, `completionAction` TEXT NOT NULL, `state` TEXT NOT NULL, `totalCount` INTEGER NOT NULL, `completedCount` INTEGER NOT NULL, `failedCount` INTEGER NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, `updatedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `local_import_entry` (`id` TEXT NOT NULL, `batchId` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, `sourceUri` TEXT NOT NULL, `displayName` TEXT, `expectedSizeBytes` INTEGER, `state` TEXT NOT NULL, `copiedBytes` INTEGER NOT NULL, `localMusicId` TEXT, `errorCode` TEXT, PRIMARY KEY(`id`))",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_local_import_entry_batchId` ON `local_import_entry` (`batchId`)")
        }
    }
