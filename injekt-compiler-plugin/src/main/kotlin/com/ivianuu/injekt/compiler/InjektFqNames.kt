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

object InjektFqNames {
    val InjektPackage = FqName("com.ivianuu.injekt")
    val InternalPackage = InjektPackage.child("internal")
    val CompositionPackage = InjektPackage.child("composition")
    val CompositionsPackage = CompositionPackage.child("internal")

    val AndroidPackage = InjektPackage.child("android")

    val AndroidEntryPoint = AndroidPackage.child("AndroidEntryPoint")

    val InjektAst = InternalPackage.child("InjektAst")
    val AstAlias = InjektAst.child("Alias")
    val AstAssisted = InjektPackage.child("Assisted")
    val AstBinding = InjektAst.child("Binding")
    val AstChildFactory = InjektAst.child("ChildFactory")
    val AstDependency = InjektAst.child("Dependency")
    val AstEntryPoints = InjektAst.child("EntryPoints")
    val AstInstance = InjektAst.child("Instance")
    val AstMap = InjektAst.child("Map")
    val AstMapEntry = AstMap.child("Entry")
    val AstMapClassKey = AstMap.child("ClassKey")
    val AstMapTypeParameterClassKey = AstMap.child("TypeParameterClassKey")
    val AstMapIntKey = AstMap.child("IntKey")
    val AstMapLongKey = AstMap.child("LongKey")
    val AstMapStringKey = AstMap.child("StringKey")
    val AstModule = InjektAst.child("Module")
    val AstName = InjektAst.child("Name")
    val AstObjectGraph = InjektAst.child("ObjectGraph")
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

    val Assisted = InjektPackage.child("Assisted")

    val BindingAdapter = CompositionPackage.child("BindingAdapter")
    val BindingEffect = CompositionPackage.child("BindingEffect")

    val ChildFactory = InjektPackage.child("ChildFactory")

    val CompositionComponent = CompositionPackage.child("CompositionComponent")
    val CompositionFactory = CompositionPackage.child("CompositionFactory")
    val CompositionFactories = CompositionPackage.child("CompositionFactories")

    val DoubleCheck = InternalPackage.child("DoubleCheck")

    val Factory = InjektPackage.child("Factory")

    val Inject = InjektPackage.child("Inject")
    val InjectProperty = InjektPackage.child("InjectProperty")

    val InstanceFactory = InjektPackage.child("InstanceFactory")

    val LateinitFactory = InternalPackage.child("LateinitFactory")

    val MapDsl = InjektPackage.child("MapDsl")
    val MapOfValueFactory = InternalPackage.child("MapOfValueFactory")
    val MapOfProviderFactory = InternalPackage.child("MapOfProviderFactory")
    val MapOfLazyFactory = InternalPackage.child("MapOfLazyFactory")

    val Module = InjektPackage.child("Module")

    val Lazy = InjektPackage.child("Lazy")

    val MembersInjector = InjektPackage.child("MembersInjector")
    val NoOpMembersInjector = InternalPackage.child("NoOpMembersInjector")

    val Provider = InjektPackage.child("Provider")
    val ProviderOfLazy = InternalPackage.child("ProviderOfLazy")

    val Qualifier = InjektPackage.child("Qualifier")

    val Scope = InjektPackage.child("Scope")

    val SetDsl = InjektPackage.child("SetDsl")
    val SetOfValueFactory = InternalPackage.child("SetOfValueFactory")
    val SetOfProviderFactory = InternalPackage.child("SetOfProviderFactory")
    val SetOfLazyFactory = InternalPackage.child("SetOfLazyFactory")

    val SingleInstanceFactory = InternalPackage.child("SingleInstanceFactory")

    val Transient = InjektPackage.child("Transient")

    val TypeAnnotation = InternalPackage.child("TypeAnnotation")

    val Uninitialized = InternalPackage.child("Uninitialized")

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
