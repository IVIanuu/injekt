package com.ivianuu.injekt.compiler.transform.graph

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class Graph(
    val context: IrPluginContext,
    val symbols: InjektSymbols,
    val componentNode: ComponentNode,
    componentModule: ModuleNode?,
    declarationStore: InjektDeclarationStore
) {

    val factoryExpressions =
        FactoryExpressions(
            context,
            symbols,
            this,
            componentNode
        )

    val scopes = mutableSetOf<FqName>()

    private val explicitBindingResolvers = mutableListOf<BindingResolver>()
    private val annotatedClassBindingResolver =
        AnnotatedClassBindingResolver(
            this,
            context,
            declarationStore
        )
    private val resolvedBindings = mutableMapOf<Key, Binding>()

    init {
        if (componentModule != null) addModule(componentModule)
        explicitBindingResolvers += LazyOrProviderBindingResolver(
            context,
            this
        )
    }

    fun getBinding(request: BindingRequest): Binding {
        return resolvedBindings.getOrPut(request.key) {
            val explicitBindings = explicitBindingResolvers.flatMap { it(request.key) }
            if (explicitBindings.size > 1) {
                error("Multiple bindings found for ${request.key}")
            }

            val binding = explicitBindings.singleOrNull()
                ?: annotatedClassBindingResolver(request.key).singleOrNull()
                ?: error("No binding found for ${request.key}")

            if (binding.targetScope != null && binding.targetScope !in scopes) {
                error(
                    "Scope mismatch binding ${binding.key} " +
                            "with scope ${binding.targetScope} is not compatible with this component $scopes"
                )
            }

            binding
        }
    }

    private fun addScope(scope: FqName) {
        scopes += scope
    }

    private fun addExplicitBindingResolver(bindingResolver: BindingResolver) {
        explicitBindingResolvers += bindingResolver
    }

    private fun addModule(moduleNode: ModuleNode) {
        val module = moduleNode.module

        val descriptor = module.declarations.single {
            it is IrClass && it.nameForIrSerialization.asString() == "Descriptor"
        } as IrClass

        val functions = descriptor.functions

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstScope) }
            .forEach { addScope(it.returnType.classOrNull!!.descriptor.fqNameSafe) }

        // todo dependencies
        functions
            .filter { it.hasAnnotation(InjektFqNames.AstDependency) }
            .forEach { dependency ->

            }

        functions
            .filter { it.hasAnnotation(InjektFqNames.AstModule) }
            .map { it to it.returnType.classOrNull?.owner as IrClass }
            .forEach { (function, includedModule) ->
                val field = module.fields
                    .single { field ->
                        field.name.asString() == function.getAnnotation(InjektFqNames.AstFieldPath)!!
                            .getValueArgument(0)!!
                            .let { it as IrConst<String> }
                            .value
                    }
                addModule(
                    ModuleNode(
                        includedModule,
                        moduleNode.treeElement.child(field)
                    )
                )
            }

        addExplicitBindingResolver(
            ModuleBindingResolver(
                this,
                moduleNode,
                module,
                descriptor
            )
        )
    }

}
