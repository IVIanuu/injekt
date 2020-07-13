import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.componentFactory
import com.ivianuu.injekt.given
import com.ivianuu.injekt.initializeComponents
import com.ivianuu.injekt.runReader

class Foo
class Bar(foo: Foo)

@Component
interface TestComponent {
    @Component.Factory
    interface Factory {
        fun create(): TestComponent
    }
}

@Given
fun foo() = Foo()

@Reader
fun createFoo(foo: Foo = given()): Foo = foo

fun invoke(): Foo {
    initializeComponents()
    val component = componentFactory<TestComponent.Factory>().create()
    return component.runReader { createFoo() }
}
