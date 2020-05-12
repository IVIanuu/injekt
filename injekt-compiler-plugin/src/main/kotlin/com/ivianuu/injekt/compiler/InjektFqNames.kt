package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object InjektFqNames {
    val InjektPackage = FqName("com.ivianuu.injekt")
    val InternalPackage = InjektPackage.child("internal")

    val InjektAst = InternalPackage.child("InjektAst")
    val AstAlias = InjektAst.child("Alias")
    val AstAssisted = InjektPackage.child("Assisted")
    val AstBinding = InjektAst.child("Binding")
    val AstChildFactory = InjektAst.child("ChildFactory")
    val AstDependency = InjektAst.child("Dependency")
    val AstInline = InjektAst.child("Inline")
    val AstImplFactory = InjektAst.child("ImplFactory")
    val AstInstanceFactory = InjektAst.child("InstanceFactory")
    val AstMap = InjektAst.child("Map")
    val AstMapEntry = AstMap.child("Entry")
    val AstMapClassKey = AstMap.child("ClassKey")
    val AstMapTypeParameterClassKey = AstMap.child("TypeParameterClassKey")
    val AstMapIntKey = AstMap.child("IntKey")
    val AstMapLongKey = AstMap.child("LongKey")
    val AstMapStringKey = AstMap.child("StringKey")
    val AstModule = InjektAst.child("Module")
    val AstPath = InjektAst.child("Path")
    val AstClassPath = AstPath.child("Class")
    val AstPropertyPath = AstPath.child("Property")
    val AstTypeParameterPath = AstPath.child("TypeParameter")
    val AstValueParameterPath = AstPath.child("ValueParameter")
    val AstScope = InjektAst.child("Scope")
    val AstScoped = InjektAst.child("Scoped")
    val AstSet = InjektAst.child("Set")
    val AstSetElement = AstSet.child("Element")
    val AstTyped = InjektAst.child("Typed")

    val Assisted = InjektPackage.child("Assisted")
    val AssistedParameters = InjektPackage.child("AssistedParameters")

    val ChildFactory = InjektPackage.child("ChildFactory")

    val DoubleCheck = InternalPackage.child("DoubleCheck")

    val Factory = InjektPackage.child("Factory")

    val Inject = InjektPackage.child("Inject")

    val InstanceProvider = InternalPackage.child("InstanceProvider")

    val MapDsl = InjektPackage.child("MapDsl")
    val MapProvider = InternalPackage.child("MapProvider")

    val Module = InjektPackage.child("Module")

    val Lazy = InjektPackage.child("Lazy")
    val MembersInjector = InjektPackage.child("MembersInjector")

    val Provider = InjektPackage.child("Provider")
    val ProviderDefinition = InjektPackage.child("ProviderDefinition")
    val ProviderDsl = InjektPackage.child("ProviderDsl")
    val ProviderOfLazy = InternalPackage.child("ProviderOfLazy")

    val Qualifier = InjektPackage.child("Qualifier")

    val Scope = InjektPackage.child("Scope")

    val SetDsl = InjektPackage.child("SetDsl")
    val SetProvider = InternalPackage.child("SetProvider")

    val Transient = InjektPackage.child("Transient")

    val TypeAnnotation = InternalPackage.child("TypeAnnotation")

    private fun FqName.child(name: String) = child(Name.identifier(name))

}
