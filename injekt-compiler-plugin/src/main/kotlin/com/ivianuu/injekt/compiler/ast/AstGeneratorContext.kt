package com.ivianuu.injekt.compiler.ast

import com.ivianuu.injekt.compiler.ast.psi.AstProvider
import com.ivianuu.injekt.compiler.ast.psi.Psi2AstStorage
import com.ivianuu.injekt.compiler.ast.psi.TypeMapper
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstProperty
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.MemberScope

class AstGeneratorContext(
    val builtIns: AstBuiltIns,
    val module: ModuleDescriptor,
    val provider: AstProvider,
    val bindingContext: BindingContext,
    val kotlinBuiltIns: KotlinBuiltIns,
    val typeMapper: TypeMapper,
    val storage: Psi2AstStorage
) {

    private val classes = mutableMapOf<FqName, AstClass?>()
    private val functions = mutableMapOf<FqName, List<AstFunction>>()
    private val constructors = mutableMapOf<FqName, List<AstFunction>>()
    private val properties = mutableMapOf<FqName, List<AstProperty>>()

    fun referenceClass(fqName: FqName): AstClass? = classes.getOrPut(fqName) {
        val scope = resolveMemberScope(fqName.parent())
        scope
            ?.getContributedClassifier(fqName.shortName(), NoLookupLocation.FROM_BACKEND)
            ?.let { it as ClassDescriptor }
            ?.let { provider.get(it) as? AstClass }
    }

    fun referenceFunctions(fqName: FqName): List<AstFunction> =
        functions.getOrPut(fqName) {
            val scope = resolveMemberScope(fqName.parent())
            scope?.getContributedFunctions(fqName.shortName(), NoLookupLocation.FROM_BACKEND)
                ?.mapNotNull { provider.get(it) as? AstFunction }
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
            ?.mapNotNull { provider.get(it) as? AstProperty }
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
