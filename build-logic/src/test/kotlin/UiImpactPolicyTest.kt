import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.Properties

class UiImpactPolicyTest {
    @Test
    fun `任一公共组件 source 变更返回全部登记消费者`() {
        val entries = UiImpactPolicy.parse(registry(), setOf("component", "home", "search"))

        assertEquals(
            setOf("component", "home", "search"),
            UiImpactPolicy.affectedContracts(entries, setOf("core/designsystem/MoeSongRowModel.kt")),
        )
        assertEquals(
            setOf("component", "home", "search"),
            UiImpactPolicy.affectedContracts(entries, setOf("core/designsystem/MoeSongRow.kt")),
        )
        assertEquals(emptySet<String>(), UiImpactPolicy.affectedContracts(entries, setOf("feature/home/HomeScreen.kt")))
    }

    @Test
    fun `registry 兼容单 source 并拒绝非法 source`() {
        val singleSource =
            registry().apply {
                remove("component.songRow.sources")
                setProperty("component.songRow.source", "core/designsystem/MoeSongRow.kt")
            }
        assertEquals(
            1,
            UiImpactPolicy
                .parse(singleSource, setOf("component", "home", "search"))
                .single()
                .sources.size,
        )

        val both = registry().apply { setProperty("component.songRow.source", "core/designsystem/MoeSongRow.kt") }
        assertThrows(IllegalArgumentException::class.java) { UiImpactPolicy.parse(both, setOf("component", "home", "search")) }

        val traversal = registry().apply { setProperty("component.songRow.sources", "../MoeSongRow.kt") }
        assertThrows(IllegalArgumentException::class.java) { UiImpactPolicy.parse(traversal, setOf("component", "home", "search")) }
    }

    @Test
    fun `registry 拒绝未知 contract 和跨组件重复 source`() {
        val unknown = registry().apply { setProperty("component.songRow.contracts", "component,missing") }
        assertThrows(IllegalArgumentException::class.java) { UiImpactPolicy.parse(unknown, setOf("component")) }

        val duplicate =
            registry().apply {
                setProperty("component.other.sources", "core/designsystem/MoeSongRow.kt")
                setProperty("component.other.contracts", "home")
            }
        assertThrows(IllegalArgumentException::class.java) { UiImpactPolicy.parse(duplicate, setOf("component", "home", "search")) }
    }

    private fun registry(): Properties =
        Properties().apply {
            setProperty("schemaVersion", "1")
            setProperty("component.songRow.sources", "core/designsystem/MoeSongRow.kt,core/designsystem/MoeSongRowModel.kt")
            setProperty("component.songRow.contracts", "component,home,search")
        }
}
