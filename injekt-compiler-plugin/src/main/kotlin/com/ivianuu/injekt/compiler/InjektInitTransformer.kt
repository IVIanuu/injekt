package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class InjektInitTransformer(
    pluginContext: IrPluginContext,
    private val thisModules: List<IrClass>
) : AbstractInjektTransformer(pluginContext) {

    private val aggregatePackage =
        pluginContext.moduleDescriptor.getPackage(FqName("com.ivianuu.injekt.aggregate"))
    private val moduleImpl = getClass(InjektClassNames.Module)
        .unsubstitutedMemberScope.getContributedClassifier(Name.identifier("Impl"), NoLookupLocation.FROM_BACKEND)!! as ClassDescriptor
    private val injekt = getClass(InjektClassNames.Injekt)

    override fun visitCall(expression: IrCall): IrExpression {
        super.visitCall(expression)

        if (expression.symbol.descriptor.name.asString() != "initializeEndpoint") return expression

        val allModules = aggregatePackage
            .memberScope
            .getClassifierNames()!!
            .map { FqName(it.asString().replace("_", ".")) }
            .map { fqName ->
                try {
                    getClass(fqName)
                } catch (e: Exception) {
                    null
                }
                    ?: error("Not found for $fqName this desc ${thisModules.map { it.descriptor.fqNameSafe }}")
            } + thisModules.map { it.descriptor }

        val addModules = injekt.unsubstitutedMemberScope
            .findSingleFunction(Name.identifier("modules"))

        return DeclarationIrBuilder(pluginContext, expression.symbol).irCall(
            callee = symbolTable.referenceSimpleFunction(addModules),
            type = pluginContext.irBuiltIns.unitType
        ).apply {
            dispatchReceiver = expression.dispatchReceiver

            putValueArgument(
                0,
                IrVarargImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    pluginContext.irBuiltIns.arrayClass
                        .typeWith(moduleImpl.defaultType.toIrType()),
                    moduleImpl.defaultType.toIrType(),
                    allModules.map { module ->
                        DeclarationIrBuilder(pluginContext, expression.symbol)
                            .irGetObject(symbolTable.referenceClass(module))
                    }
                )
            )
        }
    }
}
