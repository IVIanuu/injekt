package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object InjektFqNames {
    private fun FqName.child(name: String) = child(Name.identifier(name))
    val InjektPackage = FqName("com.ivianuu.injekt")
    val InjektInternalPackage = InjektPackage.child("internal")
    val InjektComponentsPackage = InjektInternalPackage.child("components")
    val InjektBindingsPackage = InjektInternalPackage.child("bindings")
    val InjektModulesPackage = InjektInternalPackage.child("modules")

    val BindingMetadata = InjektInternalPackage.child("BindingMetadata")
    val Component = InjektPackage.child("Component")
    val ComponentMetadata = InjektInternalPackage.child("ComponentMetadata")
    val ComponentOwner = InjektPackage.child("ComponentOwner")
    val Factory = InjektPackage.child("Factory")
    val Module = InjektPackage.child("Module")
    val ModuleMetadata = InjektInternalPackage.child("ModuleMetadata")
    val Provider = InjektPackage.child("Provider")
    val ProviderDsl = InjektPackage.child("ProviderDsl")
    val ProviderFieldMetadata = InjektInternalPackage.child("ProviderFieldMetadata")
    val ProviderMetadata = InjektInternalPackage.child("ProviderMetadata")
    val Qualifier = InjektPackage.child("Qualifier")
    val Scope = InjektPackage.child("Scope")
    val Single = InjektPackage.child("Single")
    val SingleProvider = InjektInternalPackage.child("SingleProvider")
}
