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

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.addClassFile
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.flatMapFix
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class Indexer(
    private val pluginContext: IrPluginContext,
    private val module: IrModuleFragment,
    private val symbols: InjektSymbols
) {

    val indices: List<FqName>
        get() {
            val memberScope = pluginContext.moduleDescriptor.getPackage(InjektFqNames.IndexPackage)
                .memberScope

            return ((module.files
                .filter { it.fqName == InjektFqNames.IndexPackage }
                .flatMapFix { it.declarations }
                .filterIsInstance<IrClass>()) + ((memberScope.getClassifierNames()
                ?: emptySet()).mapNotNull {
                memberScope.getContributedClassifier(
                    it,
                    NoLookupLocation.FROM_BACKEND
                )
            }.map { pluginContext.referenceClass(it.fqNameSafe)!!.owner }))
                .map {
                    it.getAnnotation(InjektFqNames.Index)!!
                        .getValueArgument(0)!!
                        .let { it as IrConst<String> }
                        .value
                        .let { FqName(it) }
                }
        }

    fun index(fqName: FqName) {
        module.addClassFile(
            pluginContext,
            InjektFqNames.IndexPackage,
            buildClass {
                name = "${
                fqName
                    .pathSegments()
                    .joinToString("_")
                }Index".asNameId()
                kind = ClassKind.INTERFACE
                visibility = Visibilities.INTERNAL
            }.apply {
                createImplicitParameterDeclarationWithWrappedDescriptor()
                addMetadataIfNotLocal()
                annotations += DeclarationIrBuilder(pluginContext, symbol).run {
                    irCall(symbols.index.constructors.single()).apply {
                        putValueArgument(
                            0,
                            irString(fqName.asString())
                        )
                    }
                }
            }
        )
    }

}
