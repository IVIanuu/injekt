package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.file
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
                if (declaration.hasAnnotation(InjektFqNames.Factory)) {
                    factoryFunctions += declaration
                }

                return super.visitFunction(declaration)
            }
        })

        factoryFunctions.forEach { function ->
            val moduleCall = function.body!!.statements[0] as IrCall
            val moduleClass = declarationStore.getModuleClass(moduleCall.symbol.owner)

            val createCall = (function.body!!.statements[1] as IrReturn).value as IrCall

            when {
                createCall.symbol.descriptor.name.asString() == "createImpl" -> {
                    FactoryImplementation(
                        factoryFunction = function,
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
                }
                createCall.symbol.descriptor.name.asString() == "createInstance" -> {
                    FactoryInstance(
                        factoryFunction = function,
                        moduleClass = moduleClass,
                        context = this@TopLevelFactoryTransformer.context,
                        symbols = symbols,
                        factoryTransformer = this@TopLevelFactoryTransformer,
                        declarationStore = declarationStore
                    )
                }
                else -> {
                    error("Unexpected factory body ${function.dump()}")
                }
            }
        }

        factoryFunctions
            .map { it.file }
            .distinct()
            .forEach {
                (it as IrFileImpl).metadata =
                    MetadataSource.File(it.declarations.map { it.descriptor })
            }

        return super.visitModuleFragment(declaration)
    }

}
