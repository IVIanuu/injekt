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

import com.ivianuu.injekt.BindingCreator
import com.ivianuu.injekt.Definition
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.annotations.Creator
import com.ivianuu.injekt.annotations.CreatorsRegistry
import com.ivianuu.injekt.module
import com.ivianuu.injekt.multibinding.bindIntoMap
import com.ivianuu.injekt.single
import com.ivianuu.injekt.withContext
import kotlin.reflect.KClass

@CreatorsRegistry([AppService::class])
private class Creators

/**
 * Generates a factory for the annotated type
 */
@Target(AnnotationTarget.CLASS)
@Creator(AppServiceBindingCreator::class)
annotation class AppService(
    val name: String = "",
    val scopeName: String = "",
    val override: Boolean = false,
    val serviceImpl: KClass<out Any> = String::class,
    val stringArray: Array<String> = ["one", "two"],
    val typeArray: Array<KClass<*>> = [String::class, Int::class]
)

class AppServiceBindingCreator : BindingCreator {
    override fun <T> create(
        type: KClass<*>,
        definition: Definition<T>,
        args: Map<String, Any>
    ): Module {
        return module {
            single(
                type,
                args.name,
                args.scopeName,
                args.override,
                args.eager,
                definition
            ) withContext {
                bindIntoMap("app_services", type)
            }
        }
    }
}

@AppService
class MyAppService