package cn.james.music.kugou.api

import cn.james.music.kugou.api.session.KugouCookies
import cn.james.music.kugou.api.transport.EpochSecondsProvider
import cn.james.music.kugou.api.transport.KugouCleartextPolicy
import cn.james.music.kugou.api.transport.KugouHttpMethod
import cn.james.music.kugou.api.transport.KugouRequestContext
import cn.james.music.kugou.api.transport.KugouRequestFactory
import cn.james.music.kugou.api.transport.KugouRequestSpec
import cn.james.music.kugou.api.transport.KugouSignatureMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class KugouRequestFactoryTest {
    private val factory = KugouRequestFactory(clock = EpochSecondsProvider { 1_700_000_000L })

    @Test
    fun searchRequestMatchesFixedNodeSnapshot() {
        val request =
            factory.prepare(
                spec =
                    KugouRequestSpec(
                        id = "search.song",
                        method = KugouHttpMethod.Get,
                        path = "/v3/search/song",
                        params =
                            linkedMapOf(
                                "albumhide" to "0",
                                "iscorrection" to "1",
                                "keyword" to "MoeKoe 测试",
                                "nocollect" to "0",
                                "page" to "1",
                                "pagesize" to "30",
                                "platform" to "AndroidFilter",
                            ),
                        headers = mapOf("x-router" to "complexsearch.kugou.com"),
                    ),
                context = KugouRequestContext(mid = "fixture-mid", dfid = "fixture-dfid"),
            )

        assertEquals("https://gateway.kugou.com", request.baseUrl)
        assertEquals("/v3/search/song", request.path)
        assertEquals("d81aa00a9d419ef44605e93cf2056a90", request.query["signature"])
        assertEquals("1700000000", request.query["clienttime"])
        assertEquals("1005", request.query["appid"])
        assertEquals("complexsearch.kugou.com", request.headers["x-router"])
        assertEquals("fixture-mid", request.headers["mid"])
        assertEquals("fixture-dfid", request.headers["dfid"])
        assertFalse("Cookie" in request.headers)
    }

    @Test
    fun customParamsOverrideDefaultsBeforeSigning() {
        val request =
            factory.prepare(
                spec =
                    KugouRequestSpec(
                        id = "override",
                        method = KugouHttpMethod.Get,
                        path = "/fixture",
                        params = mapOf("clienttime" to "42", "appid" to "3116"),
                    ),
                context = KugouRequestContext(mid = "fixture-mid"),
            )

        assertEquals("42", request.query["clienttime"])
        assertEquals("42", request.headers["clienttime"])
        assertEquals("3116", request.query["appid"])
    }

    @Test
    fun authenticatedContextAddsParamsAndSortedCookieHeader() {
        val request =
            factory.prepare(
                spec = KugouRequestSpec(id = "auth", method = KugouHttpMethod.Get, path = "/fixture"),
                context =
                    KugouRequestContext(
                        mid = "fixture-mid",
                        token = "fixture-token",
                        userId = "42",
                        cookies = KugouCookies.from(mapOf("z" to "last", "a" to "first")),
                    ),
            )

        assertEquals("fixture-token", request.query["token"])
        assertEquals("42", request.query["userid"])
        assertEquals("a=first; z=last", request.headers["Cookie"])
        assertFalse(request.toString().contains("fixture-token"))
        assertFalse(request.toString().contains("a=first"))
    }

    @Test
    fun protocolHeadersReplaceCaseVariants() {
        val request =
            factory.prepare(
                spec =
                    KugouRequestSpec(
                        id = "headers",
                        method = KugouHttpMethod.Get,
                        path = "/fixture",
                        headers = mapOf("MID" to "wrong", "DFID" to "wrong"),
                    ),
                context = KugouRequestContext(mid = "fixture-mid", dfid = "fixture-dfid"),
            )

        assertEquals(1, request.headers.keys.count { it.equals("mid", ignoreCase = true) })
        assertEquals(1, request.headers.keys.count { it.equals("dfid", ignoreCase = true) })
        assertEquals("fixture-mid", request.headers["mid"])
        assertEquals("fixture-dfid", request.headers["dfid"])
    }

    @Test
    fun unsignedRequestDoesNotAddSignature() {
        val request =
            factory.prepare(
                spec =
                    KugouRequestSpec(
                        id = "unsigned",
                        method = KugouHttpMethod.Post,
                        path = "/fixture",
                        signatureMode = KugouSignatureMode.None,
                    ),
                context = KugouRequestContext(mid = "fixture-mid"),
            )

        assertFalse("signature" in request.query)
    }

    @Test
    fun cleartextEndpointIsRejectedBeforeTransport() {
        assertThrows(IllegalArgumentException::class.java) {
            factory.prepare(
                spec =
                    KugouRequestSpec(
                        id = "cleartext",
                        method = KugouHttpMethod.Get,
                        path = "/fixture",
                        baseUrl = "http://example.test",
                    ),
                context = KugouRequestContext(mid = "fixture-mid"),
            )
        }
    }

    @Test
    fun fixedMobileCodeOriginRequiresExplicitPolicy() {
        val request =
            factory.prepare(
                spec =
                    KugouRequestSpec(
                        id = "captcha_sent",
                        method = KugouHttpMethod.Post,
                        path = "/v7/send_mobile_code",
                        baseUrl = "http://login.user.kugou.com",
                        cleartextPolicy = KugouCleartextPolicy.LoginMobileCode,
                    ),
                context = KugouRequestContext(mid = "fixture-mid"),
            )

        assertEquals("http://login.user.kugou.com", request.baseUrl)

        listOf(
            "http://login.user.kugou.com:80",
            "http://sub.login.user.kugou.com",
            "http://example.test",
            "https://login.user.kugou.com",
        ).forEach { origin ->
            assertThrows(IllegalArgumentException::class.java) {
                factory.prepare(
                    spec =
                        KugouRequestSpec(
                            id = "captcha_sent",
                            method = KugouHttpMethod.Post,
                            path = "/v7/send_mobile_code",
                            baseUrl = origin,
                            cleartextPolicy = KugouCleartextPolicy.LoginMobileCode,
                        ),
                    context = KugouRequestContext(mid = "fixture-mid"),
                )
            }
        }
    }

    @Test
    fun baseUrlPathAndHeaderInjectionAreRejectedBeforeTransport() {
        listOf(
            KugouRequestSpec(id = "base-path", method = KugouHttpMethod.Get, path = "/fixture", baseUrl = "https://example.test/api"),
            KugouRequestSpec(id = "path-query", method = KugouHttpMethod.Get, path = "/fixture?unsafe=true"),
            KugouRequestSpec(id = "header", method = KugouHttpMethod.Get, path = "/fixture", headers = mapOf("Bad\r\nName" to "value")),
        ).forEach { spec ->
            assertThrows(IllegalArgumentException::class.java) {
                factory.prepare(spec = spec, context = KugouRequestContext(mid = "fixture-mid"))
            }
        }
    }
}
