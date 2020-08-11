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
import com.ivianuu.injekt.compiler.irCallAndRecordLookup
import com.ivianuu.injekt.compiler.irLambda
import com.ivianuu.injekt.compiler.substitute
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.tmpSuspendFunction
import com.ivianuu.injekt.compiler.uniqueKey
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
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class EffectTransformer(injektContext: InjektContext) : AbstractInjektTransformer(injektContext) {

    override fun lower() {
        val declarations = mutableListOf<IrDeclarationWithName>()

        injektContext.module.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.hasAnnotatedAnnotations(InjektFqNames.Effect)) {
                    declarations += declaration
                }
                return super.visitClass(declaration)
            }

            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.hasAnnotatedAnnotations(InjektFqNames.Effect)) {
                    declarations += declaration
                }
                return super.visitFunction(declaration)
            }
        })

        declarations.forEach { clazz ->
            val effects = clazz
                .getAnnotatedAnnotations(InjektFqNames.Effect)
                .map { it.symbol.owner.constructedClass }
            val effectModule = effectModule(
                clazz,
                "${clazz.name}Effects".asNameId(),
                effects
            )

            clazz.file.addChild(effectModule)
        }
    }

    private fun effectModule(
        declaration: IrDeclarationWithName,
        name: Name,
        effects: List<IrClass>
    ) = buildClass {
        this.name = name
        kind = ClassKind.OBJECT
        visibility = Visibilities.INTERNAL
    }.apply clazz@{
        parent = declaration.file
        createImplicitParameterDeclarationWithWrappedDescriptor()
        addMetadataIfNotLocal()

        addConstructor {
            returnType = defaultType
            isPrimary = true
            visibility = Visibilities.PUBLIC
        }.apply {
            body = DeclarationIrBuilder(
                injektContext,
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

        val givenType = when (declaration) {
            is IrClass -> declaration.defaultType
            is IrFunction -> {
                if (declaration.hasAnnotation(InjektFqNames.Given)) {
                    declaration.returnType
                } else {
                    val parametersSize = declaration.valueParameters.size
                    (if (declaration.isSuspend) injektContext.tmpSuspendFunction(parametersSize)
                    else injektContext.tmpFunction(parametersSize))
                        .typeWith(
                            declaration.valueParameters
                                .take(parametersSize)
                                .map { it.type } + declaration.returnType
                        )
                        .let {
                            if (declaration.hasAnnotation(FqName("androidx.compose.runtime.Composable"))) {
                                it.copy(
                                    annotations = it.annotations + DeclarationIrBuilder(
                                        injektContext,
                                        declaration.symbol
                                    ).irCall(
                                        injektContext.referenceConstructors(FqName("androidx.compose.runtime.Composable"))
                                            .single()
                                    )
                                )
                            } else if (declaration.hasAnnotation(FqName("androidx.compose.Composable"))) {
                                it.copy(
                                    annotations = it.annotations + DeclarationIrBuilder(
                                        injektContext,
                                        declaration.symbol
                                    ).irCall(
                                        injektContext.referenceConstructors(FqName("androidx.compose.Composable"))
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
                                    injektContext,
                                    declaration.symbol
                                ).run {
                                    irCall(injektContext.injektSymbols.qualifier.constructors.single()).apply {
                                        putValueArgument(
                                            0,
                                            irString(declaration.uniqueKey())
                                        )
                                    }
                                }
                            )
                        }
                }
            }
            is IrProperty -> declaration.getter!!.returnType
            is IrField -> declaration.type
            else -> error("Unexpected given declaration ${declaration.dump()}")
        }

        if (declaration is IrFunction && !declaration.hasAnnotation(InjektFqNames.Given)) {
            addFunction("function", givenType).apply function@{
                addMetadataIfNotLocal()

                dispatchReceiverParameter = thisReceiver!!.copyTo(this)

                DeclarationIrBuilder(injektContext, symbol).run {
                    annotations += irCall(injektContext.injektSymbols.given.constructors.single())
                }

                body = DeclarationIrBuilder(injektContext, symbol).run {
                    irExprBody(
                        irLambda(givenType) {
                            irCallAndRecordLookup(this@function, declaration.symbol).apply {
                                if (declaration.dispatchReceiverParameter != null) {
                                    dispatchReceiver =
                                        irGetObject(declaration.dispatchReceiverParameter!!.type.classOrNull!!)
                                }
                                valueParameters.forEachIndexed { index, param ->
                                    putValueArgument(index, irGet(param))
                                }
                            }
                        }
                    )
                }
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
                                    .single().symbol to givenType
                            )
                        ),
                    isSuspend = effectFunction.isSuspend
                ).apply function@{
                    dispatchReceiverParameter = thisReceiver!!.copyTo(this)

                    annotations += effectFunction.annotations
                        .map { it.deepCopyWithSymbols() }

                    body = DeclarationIrBuilder(injektContext, symbol).run {
                        irExprBody(
                            irCallAndRecordLookup(this@function, effectFunction.symbol).apply {
                                dispatchReceiver =
                                    irGetObject(effectFunction.dispatchReceiverParameter!!.type.classOrNull!!)
                                putTypeArgument(0, givenType)
                            }
                        )
                    }
                }
            }
    }
}
