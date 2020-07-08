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
    val InfoPackage = InternalPackage.child("info")

    val Component = InjektPackage.child("Component")
    val ComponentFactory = Component.child("Factory")
    val ComponentFactories = InjektPackage.child("ComponentFactories")
    val ContextMarker = InternalPackage.child("ContextMarker")
    val DistinctType = InternalPackage.child("DistinctType")
    val DoubleCheck = InternalPackage.child("DoubleCheck")
    val EntryPoint = InternalPackage.child("EntryPoint")
    val InjektInfo = InternalPackage.child("InjektInfo")
    val MapEntries = InjektPackage.child("MapEntries")
    val Name = InternalPackage.child("Name")
    val Reader = InjektPackage.child("Reader")
    val Scoped = InjektPackage.child("Scoped")
    val SetElements = InjektPackage.child("SetElements")
    val Unscoped = InjektPackage.child("Unscoped")
    val TypeAnnotation = InternalPackage.child("TypeAnnotation")
}
