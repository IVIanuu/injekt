package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.addMember
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.storage.LockBasedStorageManager

// todo nullify dsl block
class ModuleTransformer(pluginContext: IrPluginContext) : AbstractInjektTransformer(pluginContext) {

    private val module = getClass(InjektClassNames.ModuleDsl)
    private val provider = getClass(InjektClassNames.Provider)
    private val providerDsl = getClass(InjektClassNames.ProviderDsl)

    override fun visitClass(declaration: IrClass): IrStatement {
        super.visitClass(declaration)

        if (declaration.descriptor == module ||
            !declaration.descriptor.isSubclassOf(module)
        ) return declaration

        val factoryExpressions = mutableListOf<IrCall>()

        val constructor = declaration.constructors.single()

        constructor.transformChildrenVoid(
            object : IrElementTransformerVoid() {
                override fun visitCall(expression: IrCall): IrExpression {
                    if (expression.symbol.descriptor.name.asString() == "factory") {
                        factoryExpressions += expression
                    }
                    return super.visitCall(expression)
                }
            }
        )

        factoryExpressions.forEach { factoryExpression ->
            val provider = factoryExpression.getValueArgument(0)!! as IrFunctionExpressionImpl
            declaration.addMember(moduleProvider(declaration, provider.function))
        }

        return declaration
    }

    private fun moduleProvider(
        module: IrClass,
        function: IrFunction
    ): IrClass {
        val qualifier: IrExpression? = null
        val name =
            "_${keyHash(function.returnType.toKotlinType(), qualifier?.type?.toKotlinType())}"

        val dependencies = mutableListOf<IrCall>()

        function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol.descriptor.name.asString() == "get" &&
                    expression.dispatchReceiver?.type?.toKotlinType()?.constructor?.declarationDescriptor == providerDsl
                ) {
                    dependencies += expression
                }
                return super.visitCall(expression)
            }
        })

        val classDescriptor = ClassDescriptorImpl(
            module.descriptor,
            Name.identifier(name),
            Modality.FINAL,
            ClassKind.OBJECT,
            emptyList(),
            SourceElement.NO_SOURCE,
            false,
            LockBasedStorageManager.NO_LOCKS
        )

        return IrClassImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            InjektOrigin,
            IrClassSymbolImpl(classDescriptor)
        ).apply {
            addConstructor {

            }.apply {

            }
        }
    }
}
