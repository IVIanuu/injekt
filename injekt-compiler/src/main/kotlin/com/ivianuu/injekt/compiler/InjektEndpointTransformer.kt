package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

class InjektEndpointTransformer(
    val generatedClasses: List<ClassDescriptor>,
    pluginContext: IrPluginContext
) :
    AbstractInjektTransformer(pluginContext) {

    private val componentBuilderContributor = getClass(InjektClassNames.ComponentBuilderContributor)
    private val injekt = getClass(InjektClassNames.Injekt)

    override fun visitCall(expression: IrCall): IrExpression {
        super.visitCall(expression)
        val descriptor = expression.symbol.descriptor

        if (descriptor.name.asString() != "initializeEndpoint") return expression

        val aggregatePackage =
            pluginContext.moduleDescriptor.getPackage(FqName("com.ivianuu.injekt.aggregate"))

        return DeclarationIrBuilder(pluginContext, expression.symbol).irCall(
            callee = symbolTable.referenceSimpleFunction(
                injekt.unsubstitutedMemberScope.findSingleFunction(Name.identifier("setComponentBuilderContributors"))
            ),
            type = pluginContext.irBuiltIns.unitType
        ).apply {
            val listOf =
                pluginContext.moduleDescriptor.getPackage(FqName("kotlin.collections"))
                    .memberScope
                    .findFirstFunction("listOf") {
                        it.valueParameters.singleOrNull()?.isVararg ?: false
                    }
            putValueArgument(
                0,
                DeclarationIrBuilder(pluginContext, symbol).irCall(
                    symbolTable.referenceSimpleFunction(listOf),
                    type = KotlinTypeFactory.simpleType(
                        pluginContext.builtIns.list.defaultType,
                        arguments = listOf(
                            componentBuilderContributor.defaultType.asTypeProjection()
                        )
                    ).toIrType()
                ).apply {
                    putTypeArgument(0, componentBuilderContributor.defaultType.toIrType())
                    val arrayType =
                        pluginContext.symbols.array.typeWith(componentBuilderContributor.defaultType.toIrType())

                    putValueArgument(
                        0,
                        IrVarargImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            arrayType,
                            listOf.valueParameters.single().varargElementType!!.toIrType(),
                            (aggregatePackage.memberScope.getClassifierNames() ?: emptySet())
                                .map {
                                    val fqName = FqName(it.asString().replace("_", "."))
                                    try {
                                        getClass(fqName)
                                    } catch (e: Exception) {
                                        try {
                                            generatedClasses.first {
                                                it.fqNameSafe == fqName
                                            }
                                        } catch (e: Exception) {
                                            error("Couldn't find class for '$fqName' name was '$it' generated classes $generatedClasses")
                                        }
                                    }
                                }
                                .map { classDescriptor ->
                                    DeclarationIrBuilder(pluginContext, symbol)
                                        .irCall(
                                            symbolTable.referenceSimpleFunction(
                                                classDescriptor.constructors.singleOrNull()
                                                    ?: error("Broken class $classDescriptor")
                                            ),
                                            classDescriptor.defaultType.toIrType()
                                        )
                                }
                        )
                    )
                }
            )
        }
    }
}
