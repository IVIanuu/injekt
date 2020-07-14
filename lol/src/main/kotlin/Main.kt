import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Effect
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.componentFactory
import com.ivianuu.injekt.given
import com.ivianuu.injekt.initializeComponents
import com.ivianuu.injekt.runReader
import kotlinx.coroutines.runBlocking

class Foo
class Bar(foo: Foo)

@Component
interface TestComponent {
    @Component.Factory
    interface Factory {
        fun create(): TestComponent
    }
}

typealias FooFactory = suspend () -> Foo

@Effect
annotation class BindFooFactory {
    companion object {
        @Given
        @Reader
        operator fun <T : FooFactory> invoke(): FooFactory = given<T>()
    }
}

@BindFooFactory
@Reader
suspend fun fooFactory(): Foo {
    return Foo()
}

fun main() {
    initializeComponents()
    val component = componentFactory<TestComponent.Factory>().create()
    return component.runReader { runBlocking { given<FooFactory>()() } }
}