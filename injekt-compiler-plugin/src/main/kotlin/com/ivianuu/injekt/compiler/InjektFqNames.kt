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
    val Context = InjektPackage.child("Context".asNameId())
    val Effect = InjektPackage.child("Effect".asNameId())
    val Given = InjektPackage.child("Given".asNameId())
    val GivenMapEntries = InjektPackage.child("GivenMapEntries".asNameId())
    val GivenSet = InjektPackage.child("GivenSet".asNameId())
    val GivenSetElements = InjektPackage.child("GivenSetElements".asNameId())
    val InitializeInjekt = InjektPackage.child("InitializeInjekt".asNameId())
    val Reader = InjektPackage.child("Reader".asNameId())

    val InternalPackage = InjektPackage.child("internal".asNameId())
    val ChildContextFactory = InternalPackage.child("ChildContextFactory".asNameId())
    val ContextMarker = InternalPackage.child("ContextMarker".asNameId())
    val GenericContext = InternalPackage.child("GenericContext".asNameId())
    val Index = InternalPackage.child("Index".asNameId())
    val Qualifier = InternalPackage.child("Qualifier".asNameId())
    val ReaderCall = InternalPackage.child("ReaderCall".asNameId())
    val ReaderImpl = InternalPackage.child("ReaderImpl".asNameId())
    val RootContextFactory = InternalPackage.child("RootContextFactory".asNameId())
    val RunReaderCall = InternalPackage.child("RunReaderCall".asNameId())
    val Signature = InternalPackage.child("Signature".asNameId())

    val IndexPackage = InternalPackage.child("index".asNameId())
}
