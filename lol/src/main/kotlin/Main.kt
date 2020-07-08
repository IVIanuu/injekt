import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.Scoped
import com.ivianuu.injekt.buildComponents
import com.ivianuu.injekt.componentFactory
import com.ivianuu.injekt.get
import com.ivianuu.injekt.runReader

/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class Foo
class Bar(foo: Foo)

@Component
interface TestComponent {
    @Component.Factory
    interface Factory {
        fun create(): TestComponent
    }
}

@Scoped(TestComponent::class)
@Reader
fun foo() = Foo()

val component by lazy {
    buildComponents()
    componentFactory<TestComponent.Factory>().create()
}

fun invoke() = component.runReader { get<() -> Foo>() }
