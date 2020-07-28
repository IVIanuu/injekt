import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.given
import com.ivianuu.injekt.initializeComponents
import com.ivianuu.injekt.rootComponent
import com.ivianuu.injekt.runReader
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class Foo
class Bar(foo: Foo)

@Component
interface TestComponent

@Given
fun foo() = Foo()

@Reader
suspend fun func(foo: Foo = given()): Foo {
    delay(1000)
    return foo
}

@Reader
suspend fun other() {
    delay(1000)
}

@Reader
suspend fun <R> withFoo(block: @Reader suspend (Foo) -> R): R = block(func())

fun main() {
    initializeComponents()
    val component = rootComponent<TestComponent>()
    return runBlocking {
        component.runReader {
            withFoo {
                other()
                it
            }
        }
    }
}