class CollectionsTest {

    /*
    @Test
    fun testSimpleMap() = codegen(
        """
            @Binding
            fun commandA() = CommandA()

            @MapEntries fun commandAIntoMap(
                commandA: CommandA
            ): Map<KClass<out Command>, Command> = mapOf(CommandA::class to commandA)

            @Component abstract class MapComponent {
                abstract val map: Map<KClass<out Command>, Command>

                @Binding
                protected fun commandB() = CommandB()

                @MapEntries
                protected fun commandBIntoMap(
                    commandB: CommandB
                ): Map<KClass<out Command>, Command> = mapOf(CommandB::class to commandB)
            }

            fun invoke(): Map<KClass<out Command>, Command> {
                return component<MapComponent>().map
            }
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
            @Component abstract class ParentMapComponent {
                abstract val map: Map<KClass<out Command>, Command>
                abstract val childMapComponentFactory: () -> ChildMapComponent

                @Binding protected fun commandA() = CommandA()

                @MapEntries
                protected fun commandAIntoMap(commandA: CommandA): Map<KClass<out Command>, Command> =
                    mapOf(CommandA::class to commandA)
            }

            @ChildComponent
            abstract class ChildMapComponent {
                abstract val map: Map<KClass<out Command>, Command>

                @Binding protected fun commandB() = CommandB()

                @MapEntries
                protected fun commandBIntoMap(commandB: CommandB): Map<KClass<out Command>, Command> =
                    mapOf(CommandB::class to commandB)
            }

            fun invoke(): Pair<Map<KClass<out Command>, Command>, Map<KClass<out Command>, Command>> {
                val parent = component<ParentMapComponent>()
                return parent.map to parent.childMapComponentFactory().map
            }
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
    fun testAssistedMap() = codegen(
        """
            @Component abstract class MapComponent {
                abstract val map: Map<KClass<out Command>, (String) -> Command>

                @Binding
                protected fun commandA(arg: String) = CommandA()

                @MapEntries
                protected fun commandAIntoMap(
                    commandAFactory: (String) -> CommandA
                ): Map<KClass<out Command>, (String) -> Command> = mapOf(CommandA::class to commandAFactory)

                @Binding
                protected fun commandB(arg: String) = CommandB()

                @MapEntries
                protected fun commandBIntoMap(
                    commandBFactory: (String) -> CommandB
                ): Map<KClass<out Command>, (String) -> Command> = mapOf(CommandB::class to commandBFactory)
            }

            fun invoke(): Map<KClass<out Command>, (String) -> Command> {
                return component<MapComponent>().map
            }
        """
    ) {
        val map =
            invokeSingleFile<Map<KClass<out Command>, (String) -> Command>>()
        assertEquals(2, map.size)
        assertTrue(map[CommandA::class]!!("a") is CommandA)
        assertTrue(map[CommandB::class]!!("b") is CommandB)
    }

    @Test
    fun testDefaultMap() = codegen(
        """
            @Default @MapEntries fun defaultMap() = mapOf<KClass<out Command>, Command>()
            @Component abstract class TestComponent {
                abstract val map: Map<KClass<out Command>, Command>
            }
        """
    )

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

    @Test
    fun testScopedMap() = codegen(
        """
            @Binding
            fun commandA() = CommandA()

            @Scoped(MapComponent::class)
            @MapEntries fun commandAIntoMap(
                commandA: CommandA
            ): Map<KClass<out Command>, Command> = mapOf(CommandA::class to commandA)

            @Component abstract class MapComponent {
                abstract val map: Map<KClass<out Command>, Command>

                @Binding protected fun commandB() = CommandB()

                @MapEntries protected fun commandBIntoMap(
                    commandB: CommandB
                ): Map<KClass<out Command>, Command> = mapOf(CommandB::class to commandB)
            }

            fun invoke(): Pair<Map<KClass<out Command>, Command>, Map<KClass<out Command>, Command>> {
                val component = component<MapComponent>()
                return component.map to component.map
            }
        """
    ) {
        val (a, b) = invokeSingleFile<Pair<Map<KClass<out Command>, Command>, Map<KClass<out Command>, Command>>>()
        assertSame(a[CommandA::class], b[CommandA::class])
        assertNotSame(a[CommandB::class], b[CommandB::class])
    }*/

}