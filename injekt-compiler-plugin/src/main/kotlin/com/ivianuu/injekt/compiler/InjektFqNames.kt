package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object InjektFqNames {
    val InjektPackage = FqName("com.ivianuu.injekt")

    val Component = InjektPackage.child("Component")
    val Key = InjektPackage.child("Key")
    val Linkable = InjektPackage.child("Linkable")
    val Linker = InjektPackage.child("Linker")
    val ParameterizedKey = Key.child("ParameterizedKey")
    val Provider = InjektPackage.child("Provider")
    val Qualifier = InjektPackage.child("Qualifier")
    val Scope = InjektPackage.child("Scope")
    val SimpleKey = Key.child("SimpleKey")

    private fun FqName.child(name: String) = child(Name.identifier(name))

}