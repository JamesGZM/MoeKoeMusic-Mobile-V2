package cn.james.music.core.database.home

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "home_content_snapshots")
data class HomeContentSnapshotEntity(
    @PrimaryKey
    @ColumnInfo(name = "cache_key")
    val cacheKey: String,
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,
    @ColumnInfo(name = "schema_version")
    val schemaVersion: Int,
    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long,
) {
    override fun toString(): String =
        "HomeContentSnapshotEntity(cacheKey=<redacted>, payloadBytes=${payloadJson.encodeToByteArray().size}, " +
            "schemaVersion=$schemaVersion, updatedAtEpochMs=$updatedAtEpochMs)"
}
