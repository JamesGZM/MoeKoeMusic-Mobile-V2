package cn.james.music

import android.content.ComponentName
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RiskCaptchaActivityDeviceTest {
    @Test
    fun activityIsNotExported() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val activityInfo = context.packageManager.getActivityInfo(ComponentName(context, RiskCaptchaActivity::class.java), 0)
        assertFalse(activityInfo.exported)
    }
}
