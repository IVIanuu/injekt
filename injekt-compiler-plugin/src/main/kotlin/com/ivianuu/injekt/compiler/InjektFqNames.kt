package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object InjektFqNames {
    private fun FqName.child(name: String) = child(Name.identifier(name))
    val InjektPackage = FqName("com.ivianuu.injekt")
    val InjektInternalPackage = InjektPackage.child("internal")

    val Component = InjektPackage.child("Component")
    val ComponentFactory = Component.child("Factory")
    val Lazy = InjektPackage.child("Lazy")
    val Module = InjektPackage.child("Module")
    val Provide = InjektPackage.child("Provide")
    val Provider = InjektPackage.child("Provider")
    val Qualifier = InjektPackage.child("Qualifier")
    val Scope = InjektPackage.child("Scope")

    val SingleProvider = InjektInternalPackage.child("SingleProvider")
}
