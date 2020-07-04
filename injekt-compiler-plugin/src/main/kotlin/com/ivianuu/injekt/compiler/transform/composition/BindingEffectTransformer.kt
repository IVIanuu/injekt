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
import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.child
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.getClassFromSingleValueAnnotationOrNull
import com.ivianuu.injekt.compiler.getJoinedName
import com.ivianuu.injekt.compiler.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.InjektOrigin
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class BindingEffectTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val classes = mutableListOf<IrClass>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.hasAnnotatedAnnotations(InjektFqNames.BindingAdapter) ||
                    declaration.hasAnnotatedAnnotations(InjektFqNames.BindingEffect)
                ) {
                    classes += declaration
                }
                return super.visitClass(declaration)
            }
        })

        classes.forEach { clazz ->
            val nameProvider = NameProvider()
            val bindingEffects = clazz
                .getAnnotatedAnnotations(InjektFqNames.BindingEffect) + clazz
                .getAnnotatedAnnotations(InjektFqNames.BindingAdapter)

            bindingEffects.forEach { effect ->
                val effectModule = bindingEffectModule(
                    clazz,
                    nameProvider.allocateForGroup(
                        getJoinedName(
                            clazz.getPackageFragment()!!.fqName,
                            clazz.descriptor.fqNameSafe.child("BindingEffect")
                        )
                    ),
                    effect.type.classOrNull!!.descriptor.fqNameSafe,
                    effect.startOffset,
                    effect.endOffset
                )

                clazz.file.addChild(effectModule)
            }
        }

        return super.visitModuleFragment(declaration)
    }

    private fun bindingEffectModule(
        clazz: IrClass,
        name: Name,
        effectFqName: FqName,
        startOffset: Int,
        endOffset: Int
    ) = buildFun {
        this.name = name
        visibility = clazz.visibility
        returnType = irBuiltIns.unitType
        origin = InjektOrigin
    }.apply {
        annotations += InjektDeclarationIrBuilder(pluginContext, symbol)
            .noArgSingleConstructorCall(symbols.module)

        addMetadataIfNotLocal()

        body = DeclarationIrBuilder(pluginContext, symbol).run {
            irBlockBody {
                val effectClass = pluginContext.referenceClass(effectFqName)!!
                    .owner

                val installIn =
                    effectClass.getClassFromSingleValueAnnotationOrNull(
                        InjektFqNames.BindingAdapter,
                        pluginContext
                    ) ?: effectClass.getClassFromSingleValueAnnotationOrNull(
                        InjektFqNames.BindingEffect,
                        pluginContext
                    )!!

                +irCall(
                    pluginContext.referenceFunctions(
                        FqName("com.ivianuu.injekt.composition.installIn")
                    ).single()
                ).apply {
                    putTypeArgument(0, installIn.defaultType)
                }

                val effectCompanion = effectClass.companionObject() as IrClass

                val effectModule = effectCompanion
                    .functions
                    .first { it.hasAnnotation(InjektFqNames.Module) }

                +IrCallImpl(
                    startOffset,
                    endOffset,
                    irBuiltIns.unitType,
                    effectModule.symbol
                ).apply {
                    dispatchReceiver = irGetObject(effectCompanion.symbol)
                    putTypeArgument(0, clazz.defaultType)
                }
            }
        }
    }

}
