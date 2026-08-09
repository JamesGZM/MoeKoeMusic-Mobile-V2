import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Properties

class AgentExecutionPolicyTest {
    @Test
    fun `worktree record 要精确覆盖授权范围内的实际文件`() {
        val record = AgentExecutionPolicy.parse(recordProperties())

        assertTrue(
            AgentExecutionPolicy
                .validate(
                    record,
                    AgentExecutionPhase.Worktree,
                    setOf("feature/home/Home.kt", "feature/home/HomeTest.kt"),
                ).isEmpty(),
        )
        assertTrue(
            AgentExecutionPolicy
                .validate(record, AgentExecutionPhase.Worktree, setOf("feature/home/Home.kt"))
                .single()
                .contains("精确一致"),
        )
    }

    @Test
    fun `记录拒绝越权文件和 staged 遗留变更`() {
        val record = AgentExecutionPolicy.parse(recordProperties("actualFiles" to "feature/home/Home.kt,feature/search/Search.kt"))

        val violations =
            AgentExecutionPolicy.validate(
                record,
                AgentExecutionPhase.Staged,
                setOf("feature/home/Home.kt", "feature/search/Search.kt"),
                unstagedFiles = setOf("feature/home/Unstaged.kt"),
            )

        assertTrue(violations.any { it.contains("越过授权") })
        assertTrue(violations.any { it.contains("未暂存") })
    }

    @Test
    fun `committed record 需要匹配 trailer 且身份始终只是 trace`() {
        val record = AgentExecutionPolicy.parse(recordProperties("committedByAgent" to "/root"))
        val body =
            """
            build: enforce execution governance

            Implemented-By-Agent: /root/child
            Committed-By-Agent: /root
            """.trimIndent()

        assertTrue(
            AgentExecutionPolicy
                .validate(
                    record,
                    AgentExecutionPhase.Committed,
                    setOf("feature/home/Home.kt", "feature/home/HomeTest.kt"),
                    commitBody = body,
                ).isEmpty(),
        )
        assertFalse(AgentExecutionPolicy.identityProofIsStrong(record))
    }

    @Test
    fun `committed 阶段必须匹配 staged 顺序收据的基线文件与 patch`() {
        val receipt =
            AgentExecutionReceipt(
                recordContentSha256 = AgentExecutionPolicy.sha256("record"),
                baseHead = "base",
                stagedFiles = setOf("feature/home/Home.kt", "feature/home/HomeTest.kt"),
                stagedPatchSha256 = AgentExecutionPolicy.sha256("patch"),
            )

        assertTrue(
            AgentExecutionPolicy
                .validateReceipt(
                    receipt,
                    recordContentSha256 = AgentExecutionPolicy.sha256("record"),
                    commitParent = "base",
                    committedFiles = setOf("feature/home/Home.kt", "feature/home/HomeTest.kt"),
                    committedPatchSha256 = AgentExecutionPolicy.sha256("patch"),
                ).isEmpty(),
        )
        assertEquals(
            4,
            AgentExecutionPolicy
                .validateReceipt(
                    receipt,
                    recordContentSha256 = AgentExecutionPolicy.sha256("forged-record"),
                    commitParent = "other-base",
                    committedFiles = setOf("feature/home/Home.kt"),
                    committedPatchSha256 = AgentExecutionPolicy.sha256("other-patch"),
                ).size,
        )
    }

    private fun recordProperties(vararg overrides: Pair<String, String>): Properties =
        Properties().apply {
            setProperty("schemaVersion", "1")
            setProperty("task", "/root/child")
            setProperty("authorizedPaths", "feature/home")
            setProperty("actualFiles", "feature/home/Home.kt,feature/home/HomeTest.kt")
            setProperty("childChecks", "./gradlew :feature:home:test")
            setProperty("unverifiedItems", "none")
            setProperty("implementedByAgent", "/root/child")
            setProperty("committedByAgent", "pending")
            setProperty("identityProof", "trace-only")
            overrides.forEach { (key, value) -> setProperty(key, value) }
        }
}
