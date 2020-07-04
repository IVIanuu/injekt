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

package com.ivianuu.injekt.compiler.transform.composition

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.child
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.getClassFromSingleValueAnnotation
import com.ivianuu.injekt.compiler.getClassFromSingleValueAnnotationOrNull
import com.ivianuu.injekt.compiler.getJoinedName
import com.ivianuu.injekt.compiler.getReaderConstructor
import com.ivianuu.injekt.compiler.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ReadableCompositionClassTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val classes = mutableListOf<IrClass>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Reader) &&
                    (declaration.hasAnnotation(InjektFqNames.Scoped) ||
                            declaration.hasAnnotatedAnnotations(InjektFqNames.BindingAdapter))
                ) {
                    check(declaration.typeParameters.isEmpty()) {
                        "Cannot have type params"
                    }
                    classes += declaration
                }
                return super.visitClass(declaration)
            }
        })

        classes.forEach { clazz ->
            val compositionType = clazz.getClassFromSingleValueAnnotationOrNull(
                InjektFqNames.Scoped,
                pluginContext
            ) ?: clazz.getAnnotatedAnnotations(InjektFqNames.BindingAdapter)
                .single()
                .type
                .classOrNull!!
                .owner
                .getClassFromSingleValueAnnotation(InjektFqNames.BindingAdapter, pluginContext)

            val contextType = clazz.getReaderConstructor()!!
                .valueParameters.last().type

            clazz.file.addChild(
                InjektDeclarationIrBuilder(pluginContext, clazz.file.symbol)
                    .entryPointModule(
                        getJoinedName(
                            clazz.getPackageFragment()!!.fqName,
                            clazz.descriptor.fqNameSafe.child("ReaderContextEntryPoint")
                        ),
                        compositionType.defaultType,
                        listOf(contextType)
                    )
            )
        }

        return super.visitModuleFragment(declaration)
    }

}