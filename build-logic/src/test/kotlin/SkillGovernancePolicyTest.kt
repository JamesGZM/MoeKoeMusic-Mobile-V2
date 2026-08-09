import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Properties

class SkillGovernancePolicyTest {
    @Test
    fun `incident 与 eval 必须共享同一组稳定断言`() {
        val incident = properties("assertionIds" to "strict-regions,long-input,switch-state")
        val evaluation =
            properties(
                "assertionIds" to "strict-regions,long-input,switch-state",
                "assertions" to "严格页面必须有 region;长输入不得裁切;开启 Switch 必须可交互",
            )

        assertTrue(verifyAssertionLink("incident.properties", incident, evaluation).isEmpty())

        evaluation.setProperty("assertionIds", "strict-regions,long-input")
        assertEquals(1, verifyAssertionLink("incident.properties", incident, evaluation).size)
    }

    @Test
    fun `eval 断言文字数量不能落后于稳定 ID`() {
        val incident = properties("assertionIds" to "strict-regions,switch-state")
        val evaluation =
            properties(
                "assertionIds" to "strict-regions,switch-state",
                "assertions" to "严格页面必须有 region",
            )

        assertEquals(1, verifyAssertionLink("incident.properties", incident, evaluation).size)
    }

    private fun properties(vararg values: Pair<String, String>): Properties =
        Properties().apply { values.forEach { (key, value) -> setProperty(key, value) } }
}
