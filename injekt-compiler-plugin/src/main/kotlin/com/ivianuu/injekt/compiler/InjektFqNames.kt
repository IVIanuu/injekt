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
    val Component = InjektPackage.child("Component".asNameId())
    val ComponentFactory = Component.child("Factory".asNameId())
    val Distinct = InjektPackage.child("Distinct".asNameId())
    val Effect = InjektPackage.child("Effect".asNameId())
    val Given = InjektPackage.child("Given".asNameId())
    val MapEntries = InjektPackage.child("MapEntries".asNameId())
    val Reader = InjektPackage.child("Reader".asNameId())
    val SetElements = InjektPackage.child("SetElements".asNameId())

    val InternalPackage = InjektPackage.child("internal".asNameId())
    val ComponentFactories = InternalPackage.child("ComponentFactories".asNameId())
    val EntryPoint = InternalPackage.child("EntryPoint".asNameId())
    val Implicit = InternalPackage.child("Implicit".asNameId())
    val Implicits = InternalPackage.child("Implicits".asNameId())
    val Index = InternalPackage.child("Index".asNameId())
    val Name = InternalPackage.child("Name".asNameId())
    val Qualifier = InternalPackage.child("Qualifier".asNameId())

    val IndexPackage = InternalPackage.child("index".asNameId())
}
