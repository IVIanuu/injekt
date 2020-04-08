package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.resolve.calls.components.isVararg

class ModuleScopeAnnotationTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val scope = getClass(InjektClassNames.Scope)

    override fun visitProperty(declaration: IrProperty): IrStatement {
        super.visitProperty(declaration)

        if (!declaration.annotations.hasAnnotation(InjektClassNames.ModuleMarker)) return declaration

        val scope = declaration.descriptor.getSyntheticAnnotationPropertiesOfType(scope.defaultType)
            .singleOrNull()
            ?: return declaration

        declaration.backingField?.initializer?.transform(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val callee = expression.symbol.ensureBound().owner
                if ((callee.fqNameForIrSerialization.asString() == "com.ivianuu.injekt.Module" ||
                            callee.fqNameForIrSerialization.asString() == "com.ivianuu.injekt.ModuleKt.Module") &&
                    expression.getValueArgument(0) == null
                ) {
                    val listOf =
                        pluginContext.moduleDescriptor.getPackage(FqName("kotlin.collections"))
                            .memberScope
                            .findFirstFunction("listOf") {
                                it.valueParameters.singleOrNull()?.isVararg != true
                            }
                    val builder = DeclarationIrBuilder(pluginContext, expression.symbol)
                    expression.putValueArgument(
                        0,
                        builder.irCall(
                            symbolTable.referenceSimpleFunction(listOf),
                            pluginContext.builtIns.list
                                .defaultType
                                .toIrType()
                                .classOrNull!!
                                .typeWith(this@ModuleScopeAnnotationTransformer.scope.defaultType.toIrType())
                        ).apply {
                            putValueArgument(
                                0,
                                builder.irCall(
                                    symbolTable.referenceSimpleFunction(scope.getter!!),
                                    scope.type.toIrType()
                                )
                            )
                        }
                    )
                }

                return super.visitCall(expression)
            }
        }, null)

        return declaration
    }

}