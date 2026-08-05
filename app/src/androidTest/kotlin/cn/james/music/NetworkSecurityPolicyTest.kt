package cn.james.music

import android.security.NetworkSecurityPolicy
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NetworkSecurityPolicyTest {
    @Test
    fun cleartextIsLimitedToTheFixedCaptchaHost() {
        val policy = NetworkSecurityPolicy.getInstance()

        assertTrue(policy.isCleartextTrafficPermitted("login.user.kugou.com"))
        assertFalse(policy.isCleartextTrafficPermitted("example.com"))
        assertFalse(policy.isCleartextTrafficPermitted("sub.login.user.kugou.com"))
    }
}
