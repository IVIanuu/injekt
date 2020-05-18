/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object InjektFqNames {
    val InjektPackage = FqName("com.ivianuu.injekt")
    val InternalPackage = InjektPackage.child("internal")
    val CompositionsPackage = InternalPackage.child("compositions")

    val InjektAst = InternalPackage.child("InjektAst")
    val AstAlias = InjektAst.child("Alias")
    val AstAssisted = InjektPackage.child("Assisted")
    val AstBinding = InjektAst.child("Binding")
    val AstChildFactory = InjektAst.child("ChildFactory")
    val AstDependency = InjektAst.child("Dependency")
    val AstEntryPoint = InjektAst.child("EntryPoint")
    val AstInline = InjektAst.child("Inline")
    val AstMap = InjektAst.child("Map")
    val AstMapEntry = AstMap.child("Entry")
    val AstMapClassKey = AstMap.child("ClassKey")
    val AstMapTypeParameterClassKey = AstMap.child("TypeParameterClassKey")
    val AstMapIntKey = AstMap.child("IntKey")
    val AstMapLongKey = AstMap.child("LongKey")
    val AstMapStringKey = AstMap.child("StringKey")
    val AstModule = InjektAst.child("Module")
    val AstObjectGraphFunction = InjektAst.child("ObjectGraphFunction")
    val AstPath = InjektAst.child("Path")
    val AstParents = InjektAst.child("Parents")
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

    val BindingAdapter = InjektPackage.child("BindingAdapter")

    val ChildFactory = InjektPackage.child("ChildFactory")

    val CompositionComponent = InjektPackage.child("CompositionComponent")
    val CompositionFactory = InjektPackage.child("CompositionFactory")
    val CompositionFactories = InjektPackage.child("CompositionFactories")

    val DoubleCheck = InternalPackage.child("DoubleCheck")

    val Factory = InjektPackage.child("Factory")

    val InjectProperty = InjektPackage.child("InjectProperty")

    val InstanceFactory = InjektPackage.child("InstanceFactory")
    val InstanceProvider = InternalPackage.child("InstanceProvider")

    val MapDsl = InjektPackage.child("MapDsl")
    val MapProvider = InternalPackage.child("MapProvider")

    val Module = InjektPackage.child("Module")

    val Lazy = InjektPackage.child("Lazy")
    val MembersInjector = InjektPackage.child("MembersInjector")

    val NoOpMembersInjector = InternalPackage.child("NoOpMembersInjector")

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

    val Uninitialized = InternalPackage.child("Uninitialized")

    private fun FqName.child(name: String) = child(Name.identifier(name))

    val CompositionFactoryDslNames = listOf(
        InjektPackage.child("parent")
    )

    val ModuleDslNames = listOf(
        InjektPackage.child("map"),
        InjektPackage.child("set"),
        InjektPackage.child("scope"),
        InjektPackage.child("dependency"),
        InjektPackage.child("childFactory"),
        InjektPackage.child("alias"),
        InjektPackage.child("transient"),
        InjektPackage.child("scoped"),
        InjektPackage.child("instance")
    )

}
