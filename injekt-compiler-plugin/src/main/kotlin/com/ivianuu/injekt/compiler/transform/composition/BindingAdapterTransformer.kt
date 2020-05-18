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
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.getClassFromSingleValueAnnotation
import com.ivianuu.injekt.compiler.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
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
import org.jetbrains.kotlin.ir.declarations.MetadataSource
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
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class BindingAdapterTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val classes = mutableListOf<IrClass>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.hasAnnotatedAnnotations(InjektFqNames.BindingAdapter)) {
                    classes += declaration
                }
                return super.visitClass(declaration)
            }
        })

        classes.forEach { clazz ->
            val module = bindingAdapterModule(clazz)
            clazz.file.addChild(module)
        }

        return super.visitModuleFragment(declaration)
    }

    private fun bindingAdapterModule(clazz: IrClass) = buildFun {
        name = InjektNameConventions.getBindingAdapterModuleName(
            clazz.getPackageFragment()!!.fqName,
            clazz.descriptor.fqNameSafe
        )
        visibility = clazz.visibility
        returnType = irBuiltIns.unitType
    }.apply {
        annotations += InjektDeclarationIrBuilder(pluginContext, symbol)
            .noArgSingleConstructorCall(symbols.module)

        metadata = MetadataSource.Function(descriptor)

        body = DeclarationIrBuilder(pluginContext, symbol).run {
            irBlockBody {
                val bindingAdapterClass =
                    clazz.getAnnotatedAnnotations(InjektFqNames.BindingAdapter)
                        .single()
                        .type
                        .classOrNull!!
                        .owner

                val installIn =
                    bindingAdapterClass.getClassFromSingleValueAnnotation(
                        InjektFqNames.BindingAdapter,
                        pluginContext
                    )

                +irCall(
                    pluginContext.referenceFunctions(
                        FqName("com.ivianuu.injekt.composition.installIn")
                    ).single()
                ).apply {
                    putTypeArgument(0, installIn.defaultType)
                }

                val bindingAdapterCompanion = bindingAdapterClass
                    .companionObject() as IrClass

                val adapterModule = bindingAdapterCompanion
                    .functions
                    .single { it.hasAnnotation(InjektFqNames.Module) }

                +irCall(adapterModule).apply {
                    dispatchReceiver = irGetObject(bindingAdapterCompanion.symbol)
                    putTypeArgument(0, clazz.defaultType)
                }
            }
        }
    }

}
