package com.ivianuu.injekt.compiler.ast.extension

import com.ivianuu.injekt.compiler.ast.psi.Psi2AstStorage
import com.ivianuu.injekt.compiler.ast.psi.Psi2AstStubGenerator
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstModuleFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstProperty
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.MemberScope

interface AstGenerationExtension {
    companion object : ProjectExtensionDescriptor<AstGenerationExtension>(
        "com.ivianuu.injekt.compiler.ast.extension.AstGenerationExtension",
        AstGenerationExtension::class.java
    )

    fun generate(moduleFragment: AstModuleFragment, pluginContext: AstPluginContext)
}

class AstPluginContext(
    private val module: ModuleDescriptor,
    private val storage: Psi2AstStorage,
    private val stubGenerator: Psi2AstStubGenerator
) {

    val builtIns = AstBuiltIns(
        module.builtIns,
        {
            with(stubGenerator.translator) {
                it.toAstClass()
            }
        },
        {
            with(stubGenerator.translator) {
                it.toAstType()
            }
        }
    )

    private val classes = mutableMapOf<FqName, AstClass?>()
    private val functions = mutableMapOf<FqName, List<AstFunction>>()
    private val constructors = mutableMapOf<FqName, List<AstFunction>>()
    private val properties = mutableMapOf<FqName, List<AstProperty>>()

    fun referenceClass(fqName: FqName): AstClass? = classes.getOrPut(fqName) {
        val scope = resolveMemberScope(fqName.parent())
        scope
            ?.getContributedClassifier(fqName.shortName(), NoLookupLocation.FROM_BACKEND)
            ?.let { it as ClassDescriptor }
            ?.let { storage.classes[it] ?: stubGenerator.get(it) as? AstClass }
    }

    fun referenceFunctions(fqName: FqName): List<AstFunction> =
        functions.getOrPut(fqName) {
            val scope = resolveMemberScope(fqName.parent())
            scope?.getContributedFunctions(fqName.shortName(), NoLookupLocation.FROM_BACKEND)
                ?.mapNotNull {
                    storage.functions[it] ?: stubGenerator.get(it) as? AstFunction
                }
                ?: emptyList()
        }

    fun referenceConstructors(classFqName: FqName): List<AstFunction> =
        constructors.getOrPut(classFqName) {
            val kclass = referenceClass(classFqName)
            kclass
                ?.declarations
                ?.filterIsInstance<AstFunction>()
                ?: emptyList()
        }

    fun referenceProperties(fqName: FqName): List<AstProperty>? = properties.getOrPut(fqName) {
        val scope = resolveMemberScope(fqName.parent())
        scope?.getContributedVariables(fqName.shortName(), NoLookupLocation.FROM_BACKEND)
            ?.mapNotNull { storage.properties[it] ?: stubGenerator.get(it) as? AstProperty }
            ?: emptyList()
    }

    private fun resolveMemberScope(fqName: FqName): MemberScope? {
        val pkg = module.getPackage(fqName)

        if (fqName.isRoot || pkg.fragments.isNotEmpty()) return pkg.memberScope

        val parentMemberScope = resolveMemberScope(fqName.parent()) ?: return null

        val classDescriptor =
            parentMemberScope.getContributedClassifier(
                fqName.shortName(),
                NoLookupLocation.FROM_BACKEND
            ) as? ClassDescriptor
                ?: return null

        return classDescriptor.unsubstitutedMemberScope
    }

}
