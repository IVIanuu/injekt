package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.getAnnotatedAnnotations
import com.ivianuu.injekt.compiler.getProviderFqName
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class AnnotatedBindingAggregateGenerator(
    private val project: Project,
    context: IrPluginContext
) : AbstractInjektTransformer(context) {

    val aggregatedBindings = mutableMapOf<FqName, MutableSet<Name>>()

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val classes = mutableListOf<IrClass>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                if (declaration.annotations.hasAnnotation(InjektFqNames.Factory) ||
                    declaration.annotations.hasAnnotation(InjektFqNames.Single)
                ) {
                    classes += declaration
                }
                return super.visitClass(declaration)
            }
        })

        classes.forEach { clazz ->
            val bindingFqName = getProviderFqName(clazz.descriptor)

            val scope = clazz.descriptor.getAnnotatedAnnotations(InjektFqNames.Scope)
                .singleOrNull()

            val packageFqName = InjektFqNames
                .InjektBindingsPackage
                .let {
                    if (scope != null)
                        it.child(Name.identifier(scope.fqName!!.asString().replace(".", "_")))
                    else it
                }

            val aggregateName = Name.identifier(
                bindingFqName.asString().replace(".", "_")
            )

            aggregatedBindings.getOrPut(packageFqName) { mutableSetOf() }
                .add(aggregateName)

            declaration.addEmptyClass(
                context,
                project,
                aggregateName,
                packageFqName
            )
        }

        return super.visitModuleFragment(declaration)
    }

}