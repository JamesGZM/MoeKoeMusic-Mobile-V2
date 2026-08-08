package cn.james.music.feature.my

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MyUiStateContractTest {
    @Test
    fun authenticatedAndAnonymousStatesKeepDistinctAccountRules() {
        val authenticated =
            MyAccountUiState.Authenticated(
                MyProfileUi(
                    userId = "fixture-user",
                    nickname = "Fixture",
                    avatarUrl = null,
                    vipLabel = null,
                    vipUnavailable = false,
                ),
            )

        assertNotEquals(authenticated, MyAccountUiState.Anonymous)
        assertTrue(authenticated.profile.userId.isNotBlank())
    }
}
