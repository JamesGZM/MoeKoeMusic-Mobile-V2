import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Properties

class UiImpactPolicyTest {
    @Test
    fun `公共组件变更返回全部登记消费者`() {
        val entries = UiImpactPolicy.parse(registry(), setOf("component", "home", "search"))

        assertEquals(
            setOf("component", "home", "search"),
            UiImpactPolicy.affectedContracts(entries, setOf("core/designsystem/MoeSongRow.kt")),
        )
        assertEquals(emptySet<String>(), UiImpactPolicy.affectedContracts(entries, setOf("feature/home/HomeScreen.kt")))
    }

    @Test
    fun `registry 拒绝未知 contract 和路径越界`() {
        val unknown = registry().apply { setProperty("component.songRow.contracts", "component,missing") }
        assertThrows(IllegalArgumentException::class.java) {
            UiImpactPolicy.parse(unknown, setOf("component"))
        }

        val traversal = registry().apply { setProperty("component.songRow.source", "../MoeSongRow.kt") }
        assertThrows(IllegalArgumentException::class.java) {
            UiImpactPolicy.parse(traversal, setOf("component", "home", "search"))
        }
    }

    private fun registry(): Properties =
        Properties().apply {
            setProperty("schemaVersion", "1")
            setProperty("component.songRow.source", "core/designsystem/MoeSongRow.kt")
            setProperty("component.songRow.contracts", "component,home,search")
        }
}
