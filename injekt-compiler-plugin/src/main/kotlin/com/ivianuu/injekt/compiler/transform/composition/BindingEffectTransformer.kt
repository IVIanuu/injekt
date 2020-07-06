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
import com.ivianuu.injekt.compiler.dumpSrc
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.getClassFromSingleValueAnnotationOrNull
import com.ivianuu.injekt.compiler.getJoinedName
import com.ivianuu.injekt.compiler.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.tmpSuspendFunction
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.transform.InjektOrigin
import com.ivianuu.injekt.compiler.transform.reader.isReader
import com.ivianuu.injekt.compiler.uniqueName
import com.ivianuu.injekt.compiler.withAnnotations
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class BindingEffectTransformer(
    pluginContext: IrPluginContext,
    private val declarationStore: InjektDeclarationStore
) : AbstractInjektTransformer(pluginContext) {

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val classes = mutableListOf<IrClass>()
        val functions = mutableListOf<IrFunction>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.hasAnnotatedAnnotations(InjektFqNames.BindingAdapter) ||
                    declaration.hasAnnotatedAnnotations(InjektFqNames.BindingEffect)
                ) {
                    classes += declaration
                }
                return super.visitClass(declaration)
            }

            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.hasAnnotatedAnnotations(InjektFqNames.BindingEffect)) {
                    functions += declaration
                }
                return super.visitFunction(declaration)
            }
        })

        classes.forEach { clazz ->
            val nameProvider = NameProvider()
            val bindingEffects = clazz
                .getAnnotatedAnnotations(InjektFqNames.BindingEffect) + clazz
                .getAnnotatedAnnotations(InjektFqNames.BindingAdapter)

            bindingEffects.forEach { effect ->
                val effectModule = bindingEffectModuleForClass(
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

        functions.forEach { function ->
            val nameProvider = NameProvider()
            val bindingEffects = function
                .getAnnotatedAnnotations(InjektFqNames.BindingEffect)

            bindingEffects.forEach { effect ->
                val effectModule = bindingEffectModuleForFunction(
                    function,
                    nameProvider.allocateForGroup(
                        getJoinedName(
                            function.getPackageFragment()!!.fqName,
                            function.descriptor.fqNameSafe.child("BindingEffect")
                        )
                    ),
                    effect.type.classOrNull!!.descriptor.fqNameSafe,
                    effect.startOffset,
                    effect.endOffset
                )

                function.file.addChild(effectModule)
            }
        }

        return super.visitModuleFragment(declaration)
    }

    private fun bindingEffectModuleForClass(
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
        parent = clazz.file

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

    private fun bindingEffectModuleForFunction(
        function: IrFunction,
        name: Name,
        effectFqName: FqName,
        startOffset: Int,
        endOffset: Int
    ): IrFunctionImpl {
        return buildFun {
            this.name = name
            visibility = function.visibility
            returnType = irBuiltIns.unitType
            origin = InjektOrigin
        }.apply function@{
            parent = function.file
            // todo check is top level / static

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

                    val parametersSize = function.valueParameters
                        .count { it.name.asString() != "_context" }

                    val functionType =
                        (if (function.isSuspend) pluginContext.tmpSuspendFunction(parametersSize)
                        else pluginContext.tmpFunction(parametersSize))
                            .typeWith(
                                function.valueParameters
                                    .take(parametersSize)
                                    .map { it.type } + function.returnType
                            )
                            .withAnnotations(
                                listOf(
                                    irCall(symbols.astName.constructors.single()).apply {
                                        putValueArgument(
                                            0,
                                            irString(function.uniqueName())
                                        )
                                    }
                                )
                            )
                            .let {
                                if (function.hasAnnotation(FqName("androidx.compose.Composable"))) {
                                    it.withAnnotations(
                                        listOf(
                                            irCall(
                                                pluginContext.referenceConstructors(FqName("androidx.compose.Composable"))
                                                    .single()
                                            )
                                        )
                                    )
                                } else {
                                    it
                                }
                            }

                    +irCall(
                        pluginContext.referenceFunctions(
                            FqName("com.ivianuu.injekt.composition.installIn")
                        ).single()
                    ).apply {
                        putTypeArgument(0, installIn.defaultType)
                    }

                    +IrCallImpl(
                        startOffset + 5,
                        startOffset + 6,
                        irBuiltIns.unitType,
                        pluginContext.referenceFunctions(
                            FqName("com.ivianuu.injekt.unscoped")
                        ).single()
                    ).apply {
                        putTypeArgument(0, functionType)
                        putValueArgument(
                            0,
                            InjektDeclarationIrBuilder(pluginContext, this@function.symbol)
                                .factoryLambda(
                                    declarationStore.readerTransformer,
                                    emptyList(),
                                    functionType,
                                    startOffset + 1,
                                    startOffset + 2
                                ) { factoryLambda, _ ->
                                    InjektDeclarationIrBuilder(
                                        pluginContext,
                                        factoryLambda.symbol
                                    ).irLambda(functionType) { lambda ->
                                        if (function.returnType.isUnit()) {
                                            +IrCallImpl(
                                                startOffset + 3,
                                                startOffset + 4,
                                                function.returnType,
                                                function.symbol
                                            ).apply {
                                                lambda.valueParameters.forEachIndexed { index, param ->
                                                    putValueArgument(index, irGet(param))
                                                }
                                            }
                                        } else {
                                            +irReturn(
                                                IrCallImpl(
                                                    startOffset + 3,
                                                    startOffset + 4,
                                                    function.returnType,
                                                    function.symbol
                                                ).apply {
                                                    lambda.valueParameters.forEachIndexed { index, param ->
                                                        putValueArgument(index, irGet(param))
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                        )
                    }

                    val effectCompanion = effectClass.companionObject() as IrClass

                    val effectModule = effectCompanion
                        .functions
                        .first { it.hasAnnotation(InjektFqNames.Module) }

                    +IrCallImpl(
                        startOffset + 7,
                        endOffset + 8,
                        irBuiltIns.unitType,
                        effectModule.symbol
                    ).apply {
                        dispatchReceiver = irGetObject(effectCompanion.symbol)
                        putTypeArgument(0, functionType)
                    }
                }
            }
        }
    }

}
