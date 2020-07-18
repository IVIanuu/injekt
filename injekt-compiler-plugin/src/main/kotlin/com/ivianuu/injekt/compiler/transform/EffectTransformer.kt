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
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.copy
import com.ivianuu.injekt.compiler.flatMapFix
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.getJoinedName
import com.ivianuu.injekt.compiler.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.irLambda
import com.ivianuu.injekt.compiler.substitute
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.tmpSuspendFunction
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class EffectTransformer(pluginContext: IrPluginContext) : AbstractInjektTransformer(pluginContext) {

    override fun lower() {
        val classes = mutableListOf<IrClass>()
        val functions = mutableListOf<IrFunction>()

        module.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.hasAnnotatedAnnotations(InjektFqNames.Effect)) {
                    classes += declaration
                }
                return super.visitClass(declaration)
            }

            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.hasAnnotatedAnnotations(InjektFqNames.Effect)) {
                    functions += declaration
                }
                return super.visitFunction(declaration)
            }
        })

        classes.forEach { clazz ->
            val effects = clazz
                .getAnnotatedAnnotations(InjektFqNames.Effect)
                .map { it.symbol.owner.constructedClass }
            val effectModule = effectModuleForClass(
                clazz,
                "${clazz.name}Effects".asNameId(),
                effects
            )

            clazz.file.addChild(effectModule)
        }

        functions.forEach { function ->
            val effects = function
                .getAnnotatedAnnotations(InjektFqNames.Effect)
                .map { it.symbol.owner.constructedClass }
            val effectModule = effectDeclarationsForFunction(
                function,
                "${function.name}Effects".asNameId(),
                effects
            )

            function.file.addChild(effectModule)
        }
    }

    private fun effectModuleForClass(
        clazz: IrClass,
        name: Name,
        effects: List<IrClass>
    ) = buildClass {
        this.name = name
        kind = ClassKind.OBJECT
        visibility = clazz.visibility
    }.apply clazz@{
        parent = clazz.file
        createImplicitParameterDeclarationWithWrappedDescriptor()
        addMetadataIfNotLocal()

        addConstructor {
            returnType = defaultType
            isPrimary = true
            visibility = Visibilities.PUBLIC
        }.apply {
            body = DeclarationIrBuilder(
                pluginContext,
                symbol
            ).irBlockBody {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.constructors.single().owner)
                +IrInstanceInitializerCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    this@clazz.symbol,
                    context.irBuiltIns.unitType
                )
            }
        }

        effects
            .map { it.companionObject() as IrClass }
            .flatMapFix {
                it.declarations
                    .filter {
                        it.hasAnnotation(InjektFqNames.Given) ||
                                it.hasAnnotation(InjektFqNames.MapEntries) ||
                                it.hasAnnotation(InjektFqNames.SetElements)
                    }
                    .filterIsInstance<IrFunction>()
            }
            .map { effectFunction ->
                addFunction(
                    getJoinedName(
                        effectFunction.getPackageFragment()!!.fqName,
                        effectFunction.descriptor.fqNameSafe
                    ).asString(),
                    effectFunction.returnType
                        .substitute(
                            mapOf(
                                effectFunction.typeParameters
                                    .single().symbol to clazz.defaultType
                            )
                        ),
                    isSuspend = effectFunction.isSuspend
                ).apply {
                    dispatchReceiverParameter = thisReceiver!!.copyTo(this)

                    annotations += effectFunction.annotations
                        .map { it.deepCopyWithSymbols() }

                    body = DeclarationIrBuilder(pluginContext, symbol).run {
                        irExprBody(
                            irCall(effectFunction).apply {
                                dispatchReceiver =
                                    irGetObject(effectFunction.dispatchReceiverParameter!!.type.classOrNull!!)
                                putTypeArgument(0, clazz.defaultType)
                            }
                        )
                    }
                }
            }
    }

    private fun effectDeclarationsForFunction(
        function: IrFunction,
        moduleName: Name,
        effects: List<IrClass>
    ) = buildClass {
        this.name = moduleName
        kind = ClassKind.OBJECT
        visibility = function.visibility
    }.apply clazz@{
        parent = function.file
        createImplicitParameterDeclarationWithWrappedDescriptor()
        addMetadataIfNotLocal()

        addConstructor {
            returnType = defaultType
            isPrimary = true
            visibility = Visibilities.PUBLIC
        }.apply {
            body = DeclarationIrBuilder(
                pluginContext,
                symbol
            ).irBlockBody {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.constructors.single().owner)
                +IrInstanceInitializerCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    this@clazz.symbol,
                    context.irBuiltIns.unitType
                )
            }
        }

        val parametersSize = function.valueParameters.size

        val functionType =
            (if (function.isSuspend) pluginContext.tmpSuspendFunction(parametersSize)
            else pluginContext.tmpFunction(parametersSize))
                .typeWith(
                    function.valueParameters
                        .take(parametersSize)
                        .map { it.type } + function.returnType
                )
                .let {
                    if (function.hasAnnotation(FqName("androidx.compose.Composable"))) {
                        it.copy(
                            annotations = it.annotations + DeclarationIrBuilder(
                                pluginContext,
                                function.symbol
                            ).irCall(
                                pluginContext.referenceConstructors(FqName("androidx.compose.Composable"))
                                    .single()
                            )
                        )
                    } else {
                        it
                    }
                }
                .let {
                    it.copy(
                        annotations = it.annotations + DeclarationIrBuilder(
                            pluginContext,
                            function.symbol
                        ).run {
                            irCall(symbols.qualifier.constructors.single()).apply {
                                putValueArgument(
                                    0,
                                    irString(function.descriptor.fqNameSafe.asString())
                                )
                            }
                        }
                    )
                }

        addFunction("function", functionType).apply {
            addMetadataIfNotLocal()

            dispatchReceiverParameter = thisReceiver!!.copyTo(this)

            DeclarationIrBuilder(pluginContext, symbol).run {
                annotations += irCall(symbols.given.constructors.single())
                annotations += irCall(symbols.reader.constructors.single())
            }

            body = DeclarationIrBuilder(pluginContext, symbol).run {
                irExprBody(
                    irLambda(functionType) {
                        irCall(function).apply {
                            if (function.dispatchReceiverParameter != null) {
                                dispatchReceiver =
                                    irGetObject(function.dispatchReceiverParameter!!.type.classOrNull!!)
                            }
                            valueParameters.forEachIndexed { index, param ->
                                putValueArgument(index, irGet(param))
                            }
                        }
                    }
                )
            }
        }

        effects
            .map { it.companionObject() as IrClass }
            .flatMapFix {
                it.declarations
                    .filter {
                        it.hasAnnotation(InjektFqNames.Given) ||
                                it.hasAnnotation(InjektFqNames.MapEntries) ||
                                it.hasAnnotation(InjektFqNames.SetElements)
                    }
                    .filterIsInstance<IrFunction>()
            }
            .map { effectFunction ->
                addFunction(
                    getJoinedName(
                        effectFunction.getPackageFragment()!!.fqName,
                        effectFunction.descriptor.fqNameSafe
                    ).asString(),
                    effectFunction.returnType
                        .substitute(
                            mapOf(
                                effectFunction.typeParameters
                                    .single().symbol to functionType
                            )
                        ),
                    isSuspend = effectFunction.isSuspend
                ).apply {
                    dispatchReceiverParameter = thisReceiver!!.copyTo(this)

                    annotations += effectFunction.annotations
                        .map { it.deepCopyWithSymbols() }

                    body = DeclarationIrBuilder(pluginContext, symbol).run {
                        irExprBody(
                            irCall(effectFunction).apply {
                                dispatchReceiver =
                                    irGetObject(effectFunction.dispatchReceiverParameter!!.type.classOrNull!!)
                                putTypeArgument(0, functionType)
                            }
                        )
                    }
                }
            }
    }

}
