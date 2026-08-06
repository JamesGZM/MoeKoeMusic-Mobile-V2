package cn.james.music.core.database.lyrics

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lyrics_cache")
data class LyricsCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "source_key")
    val sourceKey: String,
    @ColumnInfo(name = "krc_text")
    val krcText: String,
    @ColumnInfo(name = "parser_version")
    val parserVersion: Int,
    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long,
) {
    override fun toString(): String =
        "LyricsCacheEntity(sourceKey=<redacted>, krcBytes=${krcText.encodeToByteArray().size}, " +
            "parserVersion=$parserVersion, updatedAtEpochMs=$updatedAtEpochMs)"
}
