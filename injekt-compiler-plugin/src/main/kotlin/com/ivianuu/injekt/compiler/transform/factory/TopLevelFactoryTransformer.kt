package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class TopLevelFactoryTransformer(
    context: IrPluginContext,
    private val declarationStore: InjektDeclarationStore
) : AbstractInjektTransformer(context) {

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val factoryFunctions = mutableListOf<IrFunction>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.annotations.hasAnnotation(InjektFqNames.Factory)) {
                    factoryFunctions += declaration
                }

                return super.visitFunction(declaration)
            }
        })

        factoryFunctions.forEach { function ->
            DeclarationIrBuilder(context, function.symbol).run {
                val moduleCall = function.body?.statements?.single() as? IrCall

                val moduleFqName = moduleCall?.symbol?.owner?.fqNameForIrSerialization
                    ?.parent()
                    ?.child(InjektNameConventions.getModuleNameForModuleFunction(moduleCall.symbol.owner.name))

                val moduleClass = if (moduleFqName != null) declarationStore.getModule(moduleFqName)
                else null

                val implementation =
                    FactoryImplementation(
                        parent = null,
                        irParent = function.file,
                        name = InjektNameConventions.getImplNameForFactoryFunction(function.name),
                        superType = function.returnType,
                        moduleClass = moduleClass,
                        context = this@TopLevelFactoryTransformer.context,
                        symbols = symbols,
                        factoryTransformer = this@TopLevelFactoryTransformer,
                        declarationStore = declarationStore
                    )

                println("impl ${implementation.clazz.dump()}")

                function.file.addChild(implementation.clazz)
                (function.file as IrFileImpl).metadata =
                    MetadataSource.File(function.file.declarations.map { it.descriptor })

                val implementationConstructor = implementation.clazz.constructors.single()
                function.body = irExprBody(
                    irCall(implementationConstructor).apply {
                        if (implementationConstructor.valueParameters.isNotEmpty()) {
                            putValueArgument(
                                0,
                                irCall(moduleClass!!.constructors.single()).apply {
                                    copyTypeArgumentsFrom(moduleCall!!)
                                    (0 until moduleCall.valueArgumentsCount).forEach {
                                        putValueArgument(it, moduleCall.getValueArgument(it))
                                    }
                                }
                            )
                        }
                    }
                )
            }
        }

        return super.visitModuleFragment(declaration)
    }

}
