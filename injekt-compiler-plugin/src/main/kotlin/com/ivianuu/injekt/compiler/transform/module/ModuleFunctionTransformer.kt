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

package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.addToParentOrAbove
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.deepCopyWithPreservingQualifiers
import com.ivianuu.injekt.compiler.hasTypeAnnotation
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.setClassKind
import com.ivianuu.injekt.compiler.transform.AbstractFunctionTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ModuleFunctionTransformer(
    pluginContext: IrPluginContext,
    private val declarationStore: InjektDeclarationStore
) : AbstractFunctionTransformer(pluginContext, TransformOrder.BottomUp) {

    fun getModuleFunctionForClass(moduleClass: IrClass): IrFunction =
        transformedFunctions.values
            .single { it.returnType.classOrNull!!.owner == moduleClass }

    fun getModuleClassForFunction(moduleFunction: IrFunction): IrClass =
        transformFunctionIfNeeded(moduleFunction).returnType.classOrNull!!.owner

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        super.visitModuleFragment(declaration)
        transformedFunctions
            .filterNot { it.key.isExternalDeclaration() }
            .forEach {
                it.value.returnType.classOrNull!!.owner.addToParentOrAbove(
                    it.value
                )
            }
        return declaration
    }

    override fun needsTransform(function: IrFunction): Boolean =
        function.hasTypeAnnotation(InjektFqNames.Module, pluginContext.bindingContext)

    override fun transform(function: IrFunction): IrFunction {
        val transformedFunction = function.deepCopyWithPreservingQualifiers(wrapDescriptor = true)

        transformedFunction.addMetadataIfNotLocal()

        val moduleDescriptor = ModuleDescriptor(
            transformedFunction,
            pluginContext,
            symbols
        )

        val moduleClass = buildClass {
            name = InjektNameConventions.getModuleClassNameForModuleFunction(transformedFunction)
            visibility = transformedFunction.visibility
        }.apply clazz@{
            moduleDescriptor.moduleClass = this
            parent = transformedFunction.file
            createImplicitParameterDeclarationWithWrappedDescriptor()
            addMetadataIfNotLocal()
            copyTypeParametersFrom(transformedFunction)
            addChild(moduleDescriptor.clazz)
        }

        transformedFunction.addValueParameter(
            "\$moduleMarker",
            irBuiltIns.anyNType
        )

        transformedFunction.returnType = moduleClass.typeWith(
            transformedFunction.typeParameters.map { it.defaultType }
        )

        val nameProvider = NameProvider()

        val allDeclarations = mutableListOf<ModuleDeclaration>()
        val moduleDeclarationFactory = ModuleDeclarationFactory(
            transformedFunction,
            moduleClass,
            pluginContext,
            declarationStore,
            nameProvider,
            symbols
        )
        val variableByDeclaration = mutableMapOf<ModuleDeclaration, IrVariable>()

        transformedFunction.rewriteTransformedFunctionCalls()

        val bodyStatements = transformedFunction.body?.statements?.toList() ?: emptyList()

        transformedFunction.body =
            DeclarationIrBuilder(pluginContext, transformedFunction.symbol).irBlockBody {
                bodyStatements.forEach { stmt ->
                    if (stmt !is IrCall) {
                        +stmt
                        return@forEach
                    }

                    val callee = stmt.symbol.owner
                    val declarations = moduleDeclarationFactory.createDeclarations(
                        callee, stmt
                    )
                    allDeclarations += declarations
                    moduleDescriptor.addDeclarations(declarations)

                    if (declarations.isEmpty()) {
                        +stmt
                    } else {
                        declarations.forEach { declaration ->
                            if (declaration is ModuleDeclarationWithProperty) {
                                variableByDeclaration[declaration] =
                                    irTemporary(declaration.variableExpression)
                            }
                        }
                    }
            }

                var isStatic = false

                val moduleConstructor = moduleClass.addConstructor {
                    returnType = moduleClass.defaultType
                    isPrimary = true
                    visibility = Visibilities.PUBLIC
                }.apply {
                    val declarationsWithProperties = allDeclarations
                        .filterIsInstance<ModuleDeclarationWithProperty>()

                    isStatic = declarationsWithProperties
                        .none {
                            var captures = false
                            it.variableExpression.transform(
                                object : IrElementTransformerVoid() {
                                    override fun visitGetValue(expression: IrGetValue): IrExpression {
                                        if (expression.symbol.owner.parent == transformedFunction) {
                                            captures = true
                                        }
                                        return super.visitGetValue(expression)
                                    }
                                }, null
                            )
                            captures
                        } && moduleClass.typeParameters.isEmpty()

                    val valueParametersByProperties = if (isStatic) {
                        emptyMap()
                    } else {
                        declarationsWithProperties
                            .map { it.property }
                            .associateWith {
                                addValueParameter(
                                    it.name.asString(),
                                    it.getter!!.returnType
                                )
                            }
                    }

                    body = InjektDeclarationIrBuilder(pluginContext, symbol).run {
                        builder.irBlockBody {
                            initializeClassWithAnySuperClass(moduleClass.symbol)
                            if (!isStatic) {
                                valueParametersByProperties.forEach { (property, valueParameter) ->
                                    +irSetField(
                                        irGet(moduleClass.thisReceiver!!),
                                        property.backingField!!,
                                        irGet(valueParameter)
                                    )
                                }
                            } else {
                                declarationsWithProperties
                                    .forEach { declaration ->
                                        +irSetField(
                                            irGet(moduleClass.thisReceiver!!),
                                            declaration.property.backingField!!,
                                            declaration.variableExpression
                                                .deepCopyWithVariables()
                                        )
                                    }
                            }
                        }
                    }
                }

                if (isStatic) {
                    moduleClass.setClassKind(ClassKind.OBJECT)
                    doBuild().statements.clear()
                }

                +irReturn(
                    if (isStatic) {
                        irGetObject(moduleClass.symbol)
                    } else {
                        irCall(moduleConstructor).apply {
                            variableByDeclaration.values.forEachIndexed { index, variable ->
                                putValueArgument(index, irGet(variable))
                            }
                        }
                    }
                )
            }

        return transformedFunction
    }

    override fun transformExternal(function: IrFunction): IrFunction {
        if (function.valueParameters.lastOrNull()?.name?.asString() == "\$moduleMarker") return function
        return pluginContext.referenceFunctions(function.descriptor.fqNameSafe)
            .map { it.owner }
            .single { other ->
                other.valueParameters.size == function.valueParameters.size + 1 &&
                        other.valueParameters.all { otherValueParameter ->
                            val thisValueParameter =
                                function.valueParameters.getOrNull(otherValueParameter.index)
                            otherValueParameter.name == thisValueParameter?.name ||
                                    otherValueParameter.name.asString() == "\$moduleMarker"
                }
            }
    }

    override fun createDecoy(original: IrFunction, transformed: IrFunction): IrFunction =
        original.deepCopyWithPreservingQualifiers(wrapDescriptor = false)
            .also { decoy ->
                decoy.body = InjektDeclarationIrBuilder(pluginContext, decoy.symbol).run {
                    builder.irExprBody(irInjektIntrinsicUnit())
                }
            }

    override fun transformCall(transformed: IrFunction, expression: IrCall): IrCall {
        return super.transformCall(transformed, expression).apply {
            putValueArgument(
                valueArgumentsCount - 1,
                DeclarationIrBuilder(pluginContext, symbol).irNull()
            )
        }
    }

}
