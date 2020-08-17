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

import com.ivianuu.injekt.compiler.transform.InjektContext

class InjektSymbols(private val injektContext: InjektContext) {
    val context = injektContext.referenceClass(InjektFqNames.Context)!!
    val effect = injektContext.referenceClass(InjektFqNames.Effect)!!
    val given = injektContext.referenceClass(InjektFqNames.Given)!!
    val givenMapEntries = injektContext.referenceClass(InjektFqNames.GivenMapEntries)!!
    val givenSet = injektContext.referenceClass(InjektFqNames.GivenSet)!!
    val givenSetElements = injektContext.referenceClass(InjektFqNames.GivenSetElements)!!
    val reader = injektContext.referenceClass(InjektFqNames.Reader)!!

    val genericContext = injektContext.referenceClass(InjektFqNames.GenericContext)!!
    val givenContext = injektContext.referenceClass(InjektFqNames.GivenContext)!!
    val index = injektContext.referenceClass(InjektFqNames.Index)!!
    val qualifier = injektContext.referenceClass(InjektFqNames.Qualifier)!!
    val readerCall = injektContext.referenceClass(InjektFqNames.ReaderCall)!!
    val readerImpl = injektContext.referenceClass(InjektFqNames.ReaderImpl)!!
    val rootContext = injektContext.referenceClass(InjektFqNames.RootContext)!!
    val runReaderCall = injektContext.referenceClass(InjektFqNames.RunReaderCall)!!
    val signature = injektContext.referenceClass(InjektFqNames.Signature)!!
}
