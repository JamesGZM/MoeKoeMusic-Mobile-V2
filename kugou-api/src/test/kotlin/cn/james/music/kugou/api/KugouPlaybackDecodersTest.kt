package cn.james.music.kugou.api

import cn.james.music.kugou.api.endpoint.KugouPlaybackAddressDecodeResult
import cn.james.music.kugou.api.endpoint.KugouPlaybackAddressDecoder
import cn.james.music.kugou.api.endpoint.KugouPlaybackQuality
import cn.james.music.kugou.api.endpoint.KugouPlaybackUnavailableReason
import cn.james.music.kugou.api.endpoint.KugouPrivilegeDecodeResult
import cn.james.music.kugou.api.endpoint.KugouPrivilegeDecoder
import cn.james.music.kugou.api.transport.KugouError
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KugouPlaybackDecodersTest {
    @Test
    fun privilegeDecoderFlattensGoodsFiltersUnavailableAndOrdersQuality() {
        val result =
            KugouPrivilegeDecoder().decode(
                json(
                    """
                    {"data":[{
                      "hash":"BASE","quality":"128","level":1,
                      "relate_goods":[
                        {"hash":"FLAC","quality":"flac","level":"1"},
                        {"hash":"BLOCKED","quality":"320","level":0},
                        {"hash":"UNKNOWN","quality":"future","level":1}
                      ]
                    }]}
                    """.trimIndent(),
                ),
            )

        assertTrue(result is KugouPrivilegeDecodeResult.Success)
        assertEquals(
            listOf(KugouPlaybackQuality.Lossless, KugouPlaybackQuality.Standard),
            (result as KugouPrivilegeDecodeResult.Success).candidates.map { it.quality },
        )
    }

    @Test
    fun playbackQualityUsesTheFixedSevenLevelWireOrder() {
        assertEquals(
            listOf("128", "320", "flac", "high", "viper_atmos", "viper_clear", "viper_tape"),
            KugouPlaybackQuality.entries.map(KugouPlaybackQuality::wireValue),
        )
    }

    @Test
    fun playbackDecoderCollectsSecurePrimaryAndBackupUrls() {
        val result =
            KugouPlaybackAddressDecoder().decode(
                json(
                    """
                    {
                      "status":1,
                      "url":["https://cdn.example/primary.mp3", "http://insecure.example/audio"],
                      "backupUrl":"https://cdn.example/backup.mp3",
                      "timeLength":"123000",
                      "extName":"mp3"
                    }
                    """.trimIndent(),
                ),
            )

        assertTrue(result is KugouPlaybackAddressDecodeResult.Success)
        val address = (result as KugouPlaybackAddressDecodeResult.Success).address
        assertEquals(
            listOf(
                "https://cdn.example/primary.mp3",
                "https://insecure.example/audio",
                "https://cdn.example/backup.mp3",
            ),
            address.urls,
        )
        assertEquals(123_000L, address.durationMs)
        assertEquals("mp3", address.extension)
    }

    @Test
    fun missingUrlsDistinguishCopyrightAndVip() {
        val copyright = KugouPlaybackAddressDecoder().decode(json("""{"status":3,"url":[]}"""))
        val vip = KugouPlaybackAddressDecoder().decode(json("""{"status":1,"url":[]}"""))

        assertEquals(
            KugouPlaybackUnavailableReason.NoCopyright,
            (copyright as KugouPlaybackAddressDecodeResult.Unavailable).reason,
        )
        assertEquals(
            KugouPlaybackUnavailableReason.VipRequired,
            (vip as KugouPlaybackAddressDecodeResult.Unavailable).reason,
        )
    }

    @Test
    fun malformedPrivilegeAndPlaybackBodiesAreProtocolFailures() {
        val privilege = KugouPrivilegeDecoder().decode(json("""{"status":1}"""))
        val playback = KugouPlaybackAddressDecoder().decode(json("""[]"""))

        assertEquals(
            KugouError.Protocol.Reason.MissingRequiredField,
            (privilege as KugouPrivilegeDecodeResult.Failure).error.reason,
        )
        assertEquals(
            KugouError.Protocol.Reason.MalformedResponse,
            (playback as KugouPlaybackAddressDecodeResult.Failure).error.reason,
        )
    }

    private fun json(value: String) = Json.parseToJsonElement(value)
}
