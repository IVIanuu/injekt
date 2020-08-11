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
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class UniqueNameProvider(
    private val existsPredicate: (Name, FqName) -> Boolean
) {

    operator fun invoke(
        base: Name,
        fqName: FqName = FqName.ROOT
    ): Name {
        val finalBase = base.asString().removeIllegalChars().asNameId()
        var name = finalBase
        var differentiator = 2
        while (existsPredicate(name, fqName)) {
            name = (finalBase.asString() + differentiator).asNameId()
            differentiator++
        }
        return name
    }

}

fun SimpleUniqueNameProvider(): UniqueNameProvider {
    val uniqueNames = mutableSetOf<FqName>()
    return UniqueNameProvider { base, fqName -> !uniqueNames.add(fqName.child(base)) }
}

fun ClassUniqueNameProvider(
    injektContext: InjektContext
): UniqueNameProvider {
    val uniqueNames = mutableSetOf<FqName>()
    return UniqueNameProvider { base, fqName ->
        val fullFqName = fqName.child(base)
        (injektContext.referenceClass(fullFqName) != null) or
                !uniqueNames.add(fullFqName)
    }
}
