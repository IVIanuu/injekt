package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.getModuleName
import com.ivianuu.injekt.compiler.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class ModuleAggregateGenerator(
    private val project: Project,
    pluginContext: IrPluginContext
) : AbstractInjektTransformer(pluginContext) {

    val aggregateModules = mutableMapOf<FqName, MutableSet<Name>>()

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val modules = mutableListOf<IrFunction>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.annotations.hasAnnotation(InjektFqNames.Module) &&
                    declaration.descriptor.hasAnnotatedAnnotations(InjektFqNames.Scope)
                ) {
                    modules += declaration
                }
                return super.visitFunction(declaration)
            }
        })

        modules.forEach { module ->
            val moduleFqName = getModuleName(module.descriptor)

            val scope = module.descriptor.getAnnotatedAnnotations(InjektFqNames.Scope)
                .single()
            val packageFqName = InjektFqNames
                .InjektModulesPackage
                .child(Name.identifier(scope.fqName!!.asString().replace(".", "_")))

            val aggregateName = Name.identifier(
                "${moduleFqName.asString().replace(".", "_")}"
            )

            aggregateModules.getOrPut(packageFqName) { mutableSetOf() }
                .add(aggregateName)

            declaration.addEmptyClass(
                pluginContext,
                project,
                aggregateName,
                packageFqName
            )
        }

        return super.visitModuleFragment(declaration)
    }

}