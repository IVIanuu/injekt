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
    val Module = InjektPackage.child("Module".asNameId())
    val Qualifier = InjektPackage.child("Qualifier".asNameId())

    val InternalPackage = InjektPackage.child("internal".asNameId())
    val CallableInfo = InternalPackage.child("CallableInfo".asNameId())
    val ClassifierInfo = InternalPackage.child("ClassifierInfo".asNameId())
    val Index = InternalPackage.child("Index".asNameId())

    val IndexPackage = InternalPackage.child("index".asNameId())

    val CommonPackage = InjektPackage.child("common".asNameId())
    val ForTypeKey = CommonPackage.child("ForTypeKey".asNameId())
    val TypeKey = CommonPackage.child("TypeKey".asNameId())
    val typeKeyOf = CommonPackage.child("typeKeyOf".asNameId())

    val Composable = FqName("androidx.compose.runtime.Composable")

    val Any = StandardNames.FqNames.any.toSafe()
}
