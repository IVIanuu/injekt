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

import com.ivianuu.injekt.compiler.generator.asNameId
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.FqName

object InjektFqNames {
    val InjektPackage = FqName("com.ivianuu.injekt")

    val Binding = InjektPackage.child("Binding".asNameId())
    val Bound = InjektPackage.child("Bound".asNameId())
    val Component = InjektPackage.child("Component".asNameId())
    val Default = InjektPackage.child("Default".asNameId())
    val Eager = InjektPackage.child("Eager".asNameId())
    val Interceptor = InjektPackage.child("Interceptor".asNameId())
    val MapEntries = InjektPackage.child("MapEntries".asNameId())
    val Module = InjektPackage.child("Module".asNameId())
    val Qualifier = InjektPackage.child("Qualifier".asNameId())
    val Scoped = InjektPackage.child("Scoped".asNameId())
    val SetElements = InjektPackage.child("SetElements".asNameId())

    val create = InjektPackage.child("create".asNameId())

    val InternalPackage = InjektPackage.child("internal".asNameId())
    val Index = InternalPackage.child("Index".asNameId())

    val IndexPackage = InternalPackage.child("index".asNameId())

    val MergePackage = InjektPackage.child("merge".asNameId())

    val MergeComponent = MergePackage.child("MergeComponent".asNameId())
    val MergeInto = MergePackage.child("MergeInto".asNameId())
    val get = MergePackage.child("get".asNameId())

    val Composable = FqName("androidx.compose.runtime.Composable")

    val Mutex = FqName("kotlinx.coroutines.sync.Mutex")

    val Any = StandardNames.FqNames.any.toSafe()
}
