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

package com.ivianuu.injekt.compiler.backend

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.unsafeLazy
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Given(IrContext::class)
class IrIndexedDeclarations {

    val classIndices by unsafeLazy {
        allExternalIndices
            .filter { it.type == "class" }
            .mapNotNull { index ->
                if (index.indexIsDeclaration) index.indexClass
                else pluginContext.referenceClass(index.fqName)?.owner
            }
    }

    val functionIndices by unsafeLazy {
        allExternalIndices
            .filter { it.type == "function" }
            .flatMap { index ->
                pluginContext.referenceFunctions(index.fqName)
                    .map { it.owner }
            }
            .distinct()
    }

    val propertyIndices by unsafeLazy {
        allExternalIndices
            .filter { it.type == "property" }
            .flatMap { index ->
                pluginContext.referenceProperties(index.fqName)
                    .map { it.owner }
            }
    }

    private val allExternalIndices by unsafeLazy {
        val memberScope = irModule.descriptor.getPackage(InjektFqNames.IndexPackage).memberScope
        (memberScope.getClassifierNames() ?: emptySet())
            .mapNotNull {
                memberScope.getContributedClassifier(
                    it,
                    NoLookupLocation.FROM_BACKEND
                )
            }
            .mapNotNull { pluginContext.referenceClass(it.fqNameSafe)?.owner }
            .map {
                Index(
                    FqName(
                        it.getConstantFromAnnotationOrNull<String>(
                            InjektFqNames.Index,
                            1
                        )!!
                    ),
                    it,
                    it.getConstantFromAnnotationOrNull<String>(InjektFqNames.Index, 0)!!,
                    it.getConstantFromAnnotationOrNull<Boolean>(InjektFqNames.Index, 2)!!
                )
            }
    }

    private data class Index(
        val fqName: FqName,
        val indexClass: IrClass,
        val type: String,
        val indexIsDeclaration: Boolean
    )

}
