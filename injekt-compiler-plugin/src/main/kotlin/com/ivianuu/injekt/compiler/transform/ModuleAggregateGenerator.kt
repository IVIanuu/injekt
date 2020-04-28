package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotatedAnnotations
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class ModuleAggregateGenerator(
    context: IrPluginContext,
    private val project: Project
) : AbstractInjektTransformer(context) {

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val modules = mutableListOf<IrFunction>()
        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.descriptor.hasAnnotatedAnnotations(InjektFqNames.Scope) &&
                    declaration.extensionReceiverParameter?.type == symbols.componentDsl.defaultType
                ) {
                    modules += declaration
                }
                return super.visitFunction(declaration)
            }
        })

        modules.forEach {
            declaration.addClass(
                context,
                project,
                Name.identifier(
                    "${it.fqNameWhenAvailable!!.asString().replace(".", "_")}\$ModuleAccessor"
                ),
                symbols.aggregatePackage.fqName
            )
        }

        return super.visitModuleFragment(declaration)
    }

}
