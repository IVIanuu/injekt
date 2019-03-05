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

package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.FactoryInstanceFactory
import com.ivianuu.injekt.SingleInstanceFactory
import com.ivianuu.injekt.common.ReusableInstanceFactory
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.asClassName

data class BindingDescriptor(
    val target: ClassName,
    val factoryName: ClassName,
    val kind: Kind,
    val name: String?,
    val scopeName: String?,
    val constructorParams: List<ParamDescriptor>
) {
    enum class Kind {
        FACTORY {
            override val instanceFactory = FactoryInstanceFactory::class.asClassName()
            override val kindConstantName = "FACTORY_KIND"
            override fun addImport(file: FileSpec.Builder) {
                file.addImport("com.ivianuu.injekt", kindConstantName)
            }
        },
        REUSABLE {
            override val instanceFactory = ReusableInstanceFactory::class.asClassName()
            override val kindConstantName = "REUSABLE_KIND"
            override fun addImport(file: FileSpec.Builder) {
                file.addImport("com.ivianuu.injekt.common", kindConstantName)
            }
        },
        SINGLE {
            override val instanceFactory = SingleInstanceFactory::class.asClassName()
            override val kindConstantName = "SINGLE_KIND"

            override fun addImport(file: FileSpec.Builder) {
                file.addImport("com.ivianuu.injekt", kindConstantName)
            }
        };

        abstract val instanceFactory: ClassName
        abstract val kindConstantName: String

        abstract fun addImport(file: FileSpec.Builder)
    }
}