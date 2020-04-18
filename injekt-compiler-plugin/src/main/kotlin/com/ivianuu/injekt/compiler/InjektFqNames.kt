package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object InjektFqNames {
    private fun FqName.child(name: String) = child(Name.identifier(name))
    val InjektPackage = FqName("com.ivianuu.injekt")
    val InjektInternalPackage = InjektPackage.child("internal")
    val InjektComponentsPackage = InjektInternalPackage.child("components")
    val InjektModulesPackage = InjektInternalPackage.child("modules")

    val Component = InjektPackage.child("Component")
    val ComponentMetadata = InjektInternalPackage.child("ComponentMetadata")
    val ComponentOwner = InjektPackage.child("ComponentOwner")
    val Module = InjektPackage.child("Module")
    val ModuleMetadata = InjektInternalPackage.child("ModuleMetadata")
    val Provider = InjektPackage.child("Provider")
    val ProviderDsl = InjektPackage.child("ProviderDsl")
    val ProviderMetadata = InjektInternalPackage.child("ProviderMetadata")
    val Scope = InjektPackage.child("Scope")
    val SingleProvider = InjektInternalPackage.child("SingleProvider")
}