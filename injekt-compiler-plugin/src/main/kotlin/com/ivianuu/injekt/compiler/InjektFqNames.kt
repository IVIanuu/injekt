package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object InjektFqNames {
    val InjektPackage = FqName("com.ivianuu.injekt")
    val InternalPackage = InjektPackage.child("internal")

    val InjektAst = InternalPackage.child("InjektAst")
    val AstChildFactory = InjektAst.child("ChildFactory")
    val AstDependency = InjektAst.child("Dependency")
    val AstModule = InjektAst.child("Module")
    val AstScope = InjektAst.child("Scope")

    val ChildFactory = InjektPackage.child("ChildFactory")

    val Factory = InjektPackage.child("Factory")

    val Module = InjektPackage.child("Module")

    val Lazy = InjektPackage.child("Lazy")
    val MembersInjector = InjektPackage.child("MembersInjector")
    val Provider = InjektPackage.child("Provider")
    val ProviderDefinition = InjektPackage.child("ProviderDefinition")
    val ProviderDsl = InjektPackage.child("ProviderDsl")

    val Qualifier = InjektPackage.child("Qualifier")
    val Scope = InjektPackage.child("Scope")

    val SetDsl = InjektPackage.child("SetDsl")

    private fun FqName.child(name: String) = child(Name.identifier(name))

}
