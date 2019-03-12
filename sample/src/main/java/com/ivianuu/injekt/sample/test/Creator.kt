/*
 * Copyright 2018 Manuel Wrage
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

package com.ivianuu.injekt.sample.test

/**
import android.content.Context
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.BindingFactory
import com.ivianuu.injekt.Definition
import com.ivianuu.injekt.annotations.Name
import com.ivianuu.injekt.annotations.Param
import com.ivianuu.injekt.createSingle
import com.ivianuu.injekt.get
import kotlin.reflect.KClass

/**
 * @author Manuel Wrage (IVIanuu)
*/

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Creator(val value: KClass<out BindingCreator>)

interface BindingCreator {
fun <T> create(
type: KClass<*>,
definition: Definition<T>,
args: Map<String, Any>
): Binding<T>

val Map<String, Any>.name: String? get() = get("name") as? String
val Map<String, Any>.scope: Scope? get() = get("scope") as? String
val Map<String, Any>.override: Boolean get() = get("override") as? Boolean ?: false
val Map<String, Any>.eager: Boolean get() = get("eager") as? Boolean ?: false
}

//
// USAGE
//

/**
 * Generates a factory for the annotated type
*/
@Target(AnnotationTarget.CLASS)
@Creator(AppServiceBindingCreator::class)
annotation class AppService(
val name: String = "",
val scope: String = "",
val override: Boolean = false
)

object AppServiceBindingCreator : BindingCreator {
override fun <T> create(
type: KClass<*>,
definition: Definition<T>,
args: Map<String, Any>
): Binding<T> {
val name = args.name
val scope = args.scope
val override = args.override
val eager = args.eager
return Binding.createSingle(type, name, scope,
override = override, eager = eager, definition = definition)
}
}

@AppService
class MyAppService(
@Param val something: Boolean,
@Name("appContext") val appContext: Context
)

object MyAppService__Factory : BindingFactory<MyAppService> {

private val creator = AppServiceBindingCreator

private val definition: Definition<MyAppService> = { params ->
MyAppService(params[0], get("appContext"))
}

private val args = mapOf(
"name" to "",
"scope" to "",
"override" to false,
"eager" to false
)

override fun create(): Binding<MyAppService> =
creator.create(MyAppService::class, definition, args)

}*/