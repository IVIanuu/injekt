package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.hasAnnotatedAnnotations
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class ClassProviderTransformer(
    context: IrPluginContext
) : AbstractInjektTransformer(context) {

    val providersByClass = mutableMapOf<IrClass, IrClass>()

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val classes = mutableListOf<IrClass>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.descriptor.hasAnnotatedAnnotations(InjektFqNames.Scope)) {
                    classes += declaration
                }
                return super.visitClass(declaration)
            }
        })

        classes.forEach { clazz ->
            val file = clazz.file
            val constructor = clazz.constructors.singleOrNull()
            file.addChild(
                DeclarationIrBuilder(context, clazz.symbol)
                    .provider(
                        name = InjektNameConventions.getProviderNameForClass(clazz.name),
                        dependencies = constructor?.valueParameters
                            ?.mapIndexed { index, valueParameter ->
                                "p$index" to valueParameter.type
                            }
                            ?.toMap() ?: emptyMap(),
                        type = clazz.defaultType,
                        createBody = { createFunction ->
                            irExprBody(
                                if (clazz.kind == ClassKind.OBJECT) {
                                    irGetObject(clazz.symbol)
                                } else {
                                    irCall(constructor!!).apply {
                                        createFunction.valueParameters.forEach { valueParameter ->
                                            putValueArgument(
                                                valueParameter.index,
                                                irGet(valueParameter)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    )
                    .also { providersByClass[clazz] = it }
            )
        }

        classes
            .map { it.file }
            .distinct()
            .forEach {
                (it as IrFileImpl).metadata =
                    MetadataSource.File(it.declarations.map { it.descriptor })
            }

        return super.visitModuleFragment(declaration)
    }

}
