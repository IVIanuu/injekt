import com.ivianuu.injekt.test.Command
import com.ivianuu.injekt.test.CommandA
import com.ivianuu.injekt.test.CommandB
import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.invokeSingleFile
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.KClass

class GivenMapTest {

    @Test
    fun testSimpleMap() = codegen(
        """
            @Given fun commandA() = CommandA()
            @GivenMap fun commandAIntoMap(
                commandA: CommandA = given
            ): Map<KClass<out Command>, Command> = mapOf(CommandA::class to commandA)
            @Given fun commandB() = CommandB()
            @GivenMap
            fun commandBIntoMap(
                commandB: CommandB = given
            ): Map<KClass<out Command>, Command> = mapOf(CommandB::class to commandB)

            fun invoke() = given<Map<KClass<out Command>, Command>>()
        """
    ) {
        val map = invokeSingleFile<Map<KClass<out Command>, Command>>()
        assertEquals(2, map.size)
        assertTrue(map[CommandA::class] is CommandA)
        assertTrue(map[CommandB::class] is CommandB)
    }

    @Test
    fun testNestedMap() = codegen(
        """
            @Given fun commandA() = CommandA()
            @GivenMap fun commandAIntoMap(
                commandA: CommandA = given
            ): Map<KClass<out Command>, Command> = mapOf(CommandA::class to commandA)
            class InnerObject {
                @Given fun commandB() = CommandB()
                @GivenMap
                fun commandBIntoMap(
                    commandB: CommandB = given
                ): Map<KClass<out Command>, Command> = mapOf(CommandB::class to commandB)

                val map = given<Map<KClass<out Command>, Command>>()
            }

            fun invoke() = given<Map<KClass<out Command>, Command>>() to InnerObject().map
        """
    ) {
        val (parentMap, childMap) =
            invokeSingleFile<Pair<Map<KClass<out Command>, Command>, Map<KClass<out Command>, Command>>>()
        assertEquals(1, parentMap.size)
        assertTrue(parentMap[CommandA::class] is CommandA)
        assertEquals(2, childMap.size)
        assertTrue(childMap[CommandA::class] is CommandA)
        assertTrue(childMap[CommandB::class] is CommandB)
    }

    /*

    @Test
    fun testChildMapOverridesParent() = codegen(
        """
            @Component abstract class ParentMapComponent {
                abstract val map: Map<String, String>
                abstract val childMapComponentFactory: () -> ChildMapComponent

                @Binding protected fun value() = "parent"

                @MapEntries
                protected fun valueIntoMap(value: String): Map<String, String> =
                    mapOf("key" to value)
            }

            @ChildComponent
            abstract class ChildMapComponent {
                abstract val map: Map<String, String>
                @Binding protected fun value() = "child"

                @MapEntries
                protected fun valueIntoMap(value: String): Map<String, String> =
                    mapOf("key" to value)
            }

            fun invoke(): Map<String, String> {
                val parent = component<ParentMapComponent>()
                return parent.childMapComponentFactory().map
            }
        """
    ) {
        val map = invokeSingleFile<Map<String, String>>()
        assertEquals("child", map["key"])
    }

    @Test
    fun testUndeclaredMap() = codegen(
        """
            @Component abstract class TestComponent {
                abstract val map: Map<KClass<out Command>, Command>
            }
        """
    ) {
        assertInternalError("no binding")
    }

    @Test
    fun testGenericMap() = codegen(
        """
            @Binding fun string() = ""

            @Binding fun int() = 0

            @MapEntries fun <V> genericMap(instance: V): Map<Int, V> = mapOf(instance.hashCode() to instance)

            @Component abstract class MapComponent {
                abstract val stringMap: Map<Int, String>
                abstract val intMap: Map<Int, Int>
            }
        """
    )
*/

}