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

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.name.FqName

object InjektFqNames {
    val InjektPackage = FqName("com.ivianuu.injekt")

    val Given = InjektPackage.child("Given".asNameId())
    val givenFun = InjektPackage.child("given".asNameId())

    val InternalPackage = InjektPackage.child("internal".asNameId())
    val GivenInfo = InternalPackage.child("GivenInfo".asNameId())
    val Index = InternalPackage.child("Index".asNameId())

    val IndexPackage = InternalPackage.child("index".asNameId())

    val Composable = FqName("androidx.compose.runtime.Composable")

    val Any = StandardNames.FqNames.any.toSafe()
}
