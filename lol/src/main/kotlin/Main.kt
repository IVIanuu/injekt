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
@Reader
fun foo() = Foo()

@Reader
fun func(): Foo {
    return given()
}

fun main() {
    initializeComponents()
    val component = componentFactory<TestComponent.Factory>().create()
    component.runReader { func() }
}
