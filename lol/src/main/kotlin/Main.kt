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
interface ParentComponent {
    @Component.Factory
    interface Factory {
        fun create(): ParentComponent
    }
}

@Component(parent = ParentComponent::class)
interface ChildComponent {
    @Component.Factory
    interface Factory {
        fun create(): ChildComponent
    }
}

@Given(ParentComponent::class)
@Reader
fun foo() = Foo()

@Given
@Reader
fun bar() = Bar(given())

fun invoke(): Bar {
    initializeComponents()
    val childComponent = componentFactory<ParentComponent.Factory>().create().runReader {
        given<ChildComponent.Factory>().create()
    }
    return childComponent.runReader { given<Bar>() }
}
