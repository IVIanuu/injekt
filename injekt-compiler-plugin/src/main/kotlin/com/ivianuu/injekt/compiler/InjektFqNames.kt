package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object InjektFqNames {
    val InjektPackage = FqName("com.ivianuu.injekt")
    val InternalPackage = InjektPackage.child("internal")
    val AggregatePackage = InternalPackage.child("aggregate")

    val Component = InjektPackage.child("Component")
    val Factory = InjektPackage.child("Factory")
    val JitBindingLookup = InternalPackage.child("JitBindingLookup")
    val JitBindingMetadata = InternalPackage.child("JitBindingMetadata")
    val JitBindingRegistry = InternalPackage.child("JitBindingRegistry")
    val Key = InjektPackage.child("Key")
    val Linker = InjektPackage.child("Linker")
    val ParameterizedKey = Key.child("ParameterizedKey")
    val Provider = InjektPackage.child("Provider")
    val Qualifier = InjektPackage.child("Qualifier")
    val Scope = InjektPackage.child("Scope")
    val SimpleKey = Key.child("SimpleKey")

    private fun FqName.child(name: String) = child(Name.identifier(name))

}