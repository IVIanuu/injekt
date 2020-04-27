package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object InjektFqNames {
    val InjektPackage = FqName("com.ivianuu.injekt")
    val InternalPackage = InjektPackage.child("internal")
    val AggregatePackage = InternalPackage.child("aggregate")

    val Binding = InjektPackage.child("Binding")
    val BindingDefinition = InjektPackage.child("BindingDefinition")
    val BindingDsl = InjektPackage.child("BindingDsl")
    val Component = InjektPackage.child("Component")
    val Factory = InjektPackage.child("Factory")
    val HasScope = InternalPackage.child("HasScope")
    val JitBindingMetadata = InternalPackage.child("JitBindingMetadata")
    val JitBindingRegistry = InternalPackage.child("JitBindingRegistry")
    val Key = InjektPackage.child("Key")
    val LinkedBinding = InjektPackage.child("LinkedBinding")
    val Linker = InjektPackage.child("Linker")
    val Module = InjektPackage.child("Module")
    val ModuleRegistry = InternalPackage.child("ModuleRegistry")
    val Param = InjektPackage.child("Param")
    val Parameters = InjektPackage.child("Parameters")
    val ParameterizedKey = Key.child("ParameterizedKey")
    val Provider = InjektPackage.child("Provider")
    val Qualifier = InjektPackage.child("Qualifier")
    val Scope = InjektPackage.child("Scope")
    val SimpleKey = Key.child("SimpleKey")
    val UnlinkedBinding = InjektPackage.child("UnlinkedBinding")

    private fun FqName.child(name: String) = child(Name.identifier(name))

}