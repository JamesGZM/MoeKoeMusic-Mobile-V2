package cn.james.music

import cn.james.music.feature.login.TencentCaptchaResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskCaptchaActivityTest {
    @Test
    fun navigationPolicyAllowsOnlyTheExactHttpsOrigin() {
        assertTrue(CaptchaNavigationPolicy.isAllowed("https://turing.captcha.qcloud.com/path"))
        assertFalse(CaptchaNavigationPolicy.isAllowed("http://turing.captcha.qcloud.com/path"))
        assertFalse(CaptchaNavigationPolicy.isAllowed("https://evil.turing.captcha.qcloud.com/path"))
        assertFalse(CaptchaNavigationPolicy.isAllowed("https://turing.captcha.qcloud.com.evil.test/path"))
        assertFalse(CaptchaNavigationPolicy.isAllowed("https://user@turing.captcha.qcloud.com/path"))
    }

    @Test
    fun payloadParserReturnsOnlyTypedSanitizedResults() {
        assertEquals(
            TencentCaptchaResult.Success("fixture-ticket", "fixture-random"),
            TencentCaptchaPayloadParser.parse("""{"ret":0,"ticket":"fixture-ticket","randstr":"fixture-random"}"""),
        )
        assertEquals(TencentCaptchaResult.Cancelled, TencentCaptchaPayloadParser.parse("""{"ret":2}"""))
        assertEquals(TencentCaptchaResult.Failure, TencentCaptchaPayloadParser.parse("""{"ret":0,"ticket":""}"""))
        assertEquals(TencentCaptchaResult.Failure, TencentCaptchaPayloadParser.parse("not-json"))
    }

    @Test
    fun generatedDocumentUsesAFullSizeHostWhileKeepingTheOfficialOriginBoundary() {
        val html = RiskCaptchaActivity.captchaHtml("123456789")

        assertTrue(html.contains("https://turing.captcha.qcloud.com/TCaptcha.js"))
        assertTrue(html.contains("new TencentCaptcha('123456789'"))
        assertTrue(html.contains("html,body{width:100%;height:100%;margin:0;overflow:hidden;background:#fff}"))
        assertTrue(html.contains("#status{position:fixed;inset:0;display:flex"))
        assertTrue(html.contains("#tcaptcha_transform_dy,[id^=\"tcaptcha_transform\"],.tcaptcha_transform"))
        assertTrue(html.contains("width:100%!important;height:100%!important"))
        assertTrue(html.contains("frame-src https://turing.captcha.qcloud.com"))
        assertTrue(html.contains("{type:'',showHeader:false}"))
        assertFalse(html.contains("addJavascriptInterface"))
        assertTrue(runCatching { RiskCaptchaActivity.captchaHtml("123');alert(1)//") }.isFailure)
    }
}
