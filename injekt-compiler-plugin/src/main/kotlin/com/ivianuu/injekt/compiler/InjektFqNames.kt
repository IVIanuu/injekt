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
import org.jetbrains.kotlin.name.FqName

object InjektFqNames {
    val InjektPackage = FqName("com.ivianuu.injekt")

    val Assisted = InjektPackage.child("Assisted".asNameId())
    val ChildFactory = InjektPackage.child("ChildFactory".asNameId())
    val RootFactory = InjektPackage.child("RootFactory".asNameId())

    val Given = InjektPackage.child("Given".asNameId())
    val GivenMapEntries = InjektPackage.child("GivenMapEntries".asNameId())
    val GivenSetElements = InjektPackage.child("GivenSetElements".asNameId())
    val Module = InjektPackage.child("Module".asNameId())

    val rootFactoryFun = InjektPackage.child("rootFactory".asNameId())

    val InternalPackage = InjektPackage.child("internal".asNameId())
    val FunctionAlias = InternalPackage.child("FunctionAlias".asNameId())

    val MergePackage = InjektPackage.child("Merge".asNameId())

    val Effect = InjektPackage.child("Effect".asNameId())
    val MergeFactory = MergePackage.child("MergeFactory".asNameId())
    val mergeFactoryFun = MergePackage.child("mergeFactory".asNameId())

    val Composable = FqName("androidx.compose.runtime.Composable")
}
