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
import com.ivianuu.injekt.compiler.LookupManager
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
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
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.classOrNull
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
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Given
class EffectTransformer : IrLowering {

    override fun lower() {
        val newDeclarations = mutableListOf<IrDeclarationWithName>()
        module.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.hasAnnotatedAnnotations(InjektFqNames.Effect)) {
                    newDeclarations += createEffectModuleForDeclaration(declaration)
                }
                return super.visitClass(declaration)
            }

            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.hasAnnotatedAnnotations(InjektFqNames.Effect)) {
                    newDeclarations += createEffectModuleForDeclaration(declaration)
                }
                return super.visitFunction(declaration)
            }
        })
        newDeclarations.forEach {
            (it.parent as IrFile).addChildAndUpdateMetadata(it)
        }
    }

    private fun createEffectModuleForDeclaration(declaration: IrDeclarationWithName): IrClass {
        val effects = declaration
            .getAnnotatedAnnotations(InjektFqNames.Effect)
            .map { it.symbol.owner.constructedClass }
        return buildClass {
            name = getJoinedName(
                declaration.file.fqName,
                FqName(declaration.descriptor.fqNameSafe.asString() + "Effects")
            )
            kind = ClassKind.OBJECT
            visibility = Visibilities.INTERNAL
        }.apply clazz@{
            parent = declaration.file
            createImplicitParameterDeclarationWithWrappedDescriptor()
            addMetadataIfNotLocal()

            effects.forEach { given<LookupManager>().recordLookup(declaration, it) }

            addConstructor {
                returnType = defaultType
                isPrimary = true
                visibility = Visibilities.PUBLIC
            }.apply {
                addMetadataIfNotLocal()
                body = irBuilder().irBlockBody {
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
                        (if (declaration.isSuspend) pluginContext.tmpSuspendFunction(parametersSize)
                        else pluginContext.tmpFunction(parametersSize))
                            .owner
                            .typeWith(
                                declaration.valueParameters
                                    .take(parametersSize)
                                    .map { it.type } + declaration.returnType
                            )
                            .let {
                                if (declaration.hasAnnotation(FqName("androidx.compose.runtime.Composable"))) {
                                    it.withAnnotations(
                                        listOf(
                                            declaration.irBuilder().irCall(
                                                pluginContext.referenceConstructors(FqName("androidx.compose.runtime.Composable"))
                                                    .single()
                                            )
                                        )
                                    )
                                } else it
                            }
                            .let {
                                it.withAnnotations(
                                    listOf(
                                        declaration.irBuilder().run {
                                            irCall(injektSymbols.qualifier.constructors.single()).apply {
                                                putValueArgument(
                                                    0,
                                                    irString(declaration.uniqueKey())
                                                )
                                            }
                                        }
                                    )
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

                    annotations += irBuilder()
                        .irCall(injektSymbols.given.constructors.single())

                    body = irBuilder().run {
                        irExprBody(
                            irLambda(givenType) {
                                irCall(declaration.symbol).apply {
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
                .flatMap { effectGivenSet ->
                    effectGivenSet.declarations
                        .filter {
                            it.hasAnnotation(InjektFqNames.Given) ||
                                    it.hasAnnotation(InjektFqNames.GivenMapEntries) ||
                                    it.hasAnnotation(InjektFqNames.GivenSetElements)
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
                        addMetadataIfNotLocal()
                        dispatchReceiverParameter = thisReceiver!!.copyTo(this)

                        annotations += effectFunction.annotations
                            .map { it.deepCopyWithSymbols() }

                        body = irBuilder().run {
                            irExprBody(
                                irCall(effectFunction.symbol).apply {
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
}
