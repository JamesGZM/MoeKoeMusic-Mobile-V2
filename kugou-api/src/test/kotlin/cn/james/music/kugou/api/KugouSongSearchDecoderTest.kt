package cn.james.music.kugou.api

import cn.james.music.kugou.api.endpoint.KugouSongSearchDecodeResult
import cn.james.music.kugou.api.endpoint.KugouSongSearchDecoder
import cn.james.music.kugou.api.transport.KugouError
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KugouSongSearchDecoderTest {
    private val decoder = KugouSongSearchDecoder()

    @Test
    fun mixedStringAndNumberFieldsAreDecodedWhileMalformedRowsAreSkipped() {
        val result =
            decoder.decode(
                json(
                    """
                    {
                      "data": {
                        "total": "31",
                        "lists": [
                          {
                            "MixSongID": "mix-1",
                            "FileHash": "ABC",
                            "SongName": "Title",
                            "SingerName": "Artist",
                            "AlbumID": 7,
                            "AlbumName": "Album",
                            "Duration": "123",
                            "Image": "http://img/{size}/cover.jpg"
                          },
                          {"FileHash": "", "SongName": "invalid"}
                        ]
                      }
                    }
                    """.trimIndent(),
                ),
            )

        assertTrue(result is KugouSongSearchDecodeResult.Success)
        val page = (result as KugouSongSearchDecodeResult.Success).page
        assertEquals(31, page.totalCount)
        assertEquals(1, page.items.size)
        assertEquals("mix-1", page.items.single().id)
        assertEquals("7", page.items.single().albumId)
        assertEquals(123L, page.items.single().durationSeconds)
    }

    @Test
    fun emptyResultIsAValidPage() {
        val result = decoder.decode(json("""{"data":{"total":0,"lists":[]}}"""))

        assertEquals(
            KugouSongSearchDecodeResult.Success::class,
            result::class,
        )
        assertTrue((result as KugouSongSearchDecodeResult.Success).page.items.isEmpty())
    }

    @Test
    fun missingListAndCompletelyUnusableRowsAreProtocolFailures() {
        val missing = decoder.decode(json("""{"data":{"total":1}}"""))
        val unusable = decoder.decode(json("""{"data":{"lists":[{"SongName":"no hash"}]}}"""))

        assertMissingRequiredField(missing)
        assertMissingRequiredField(unusable)
    }

    @Test
    fun malformedRootIsAProtocolFailure() {
        val result = decoder.decode(json("""[1,2,3]"""))

        assertTrue(result is KugouSongSearchDecodeResult.Failure)
        assertEquals(
            KugouError.Protocol.Reason.MalformedResponse,
            (result as KugouSongSearchDecodeResult.Failure).error.reason,
        )
    }

    private fun assertMissingRequiredField(result: KugouSongSearchDecodeResult) {
        assertTrue(result is KugouSongSearchDecodeResult.Failure)
        assertEquals(
            KugouError.Protocol.Reason.MissingRequiredField,
            (result as KugouSongSearchDecodeResult.Failure).error.reason,
        )
    }

    private fun json(value: String) = Json.parseToJsonElement(value)
}
