import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Effect
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.given
import com.ivianuu.injekt.initializeComponents
import com.ivianuu.injekt.rootComponent
import com.ivianuu.injekt.runReader

class Foo
class Bar(foo: Foo)

@Component
interface TestComponent

typealias FooFactory = () -> Foo

@Effect
annotation class BindFooFactory {
    companion object {
        @Given
        operator fun <T : FooFactory> invoke(): FooFactory = given<T>()
    }
}

@BindFooFactory
@Reader
fun fooFactory(): Foo {
    return Foo()
}

fun main() {
    initializeComponents()
    val component = rootComponent<TestComponent>()
    component.runReader { given<FooFactory>()() }
}
