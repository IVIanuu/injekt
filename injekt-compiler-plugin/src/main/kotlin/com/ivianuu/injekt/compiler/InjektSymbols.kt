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

import com.ivianuu.injekt.compiler.transform.InjektIrContext

class InjektSymbols(private val _context: InjektIrContext) {
    val effect = _context.referenceClass(InjektFqNames.Effect)!!
    val given = _context.referenceClass(InjektFqNames.Given)!!
    val mapEntries = _context.referenceClass(InjektFqNames.MapEntries)!!
    val reader = _context.referenceClass(InjektFqNames.Reader)!!
    val scoping = _context.referenceClass(InjektFqNames.Scoping)!!
    val setElements = _context.referenceClass(InjektFqNames.SetElements)!!

    val context = _context.referenceClass(InjektFqNames.Context)!!
    val genericContext = _context.referenceClass(InjektFqNames.GenericContext)!!
    val index = _context.referenceClass(InjektFqNames.Index)!!
    val name = _context.referenceClass(InjektFqNames.Name)!!
    val qualifier = _context.referenceClass(InjektFqNames.Qualifier)!!
    val readerImpl = _context.referenceClass(InjektFqNames.ReaderImpl)!!
    val readerInvocation = _context.referenceClass(InjektFqNames.ReaderInvocation)!!
    val runReaderContext = _context.referenceClass(InjektFqNames.RunReaderContext)!!
    val signature = _context.referenceClass(InjektFqNames.Signature)!!
}
