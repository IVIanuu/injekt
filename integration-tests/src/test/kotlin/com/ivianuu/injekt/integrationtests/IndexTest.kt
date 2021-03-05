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

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.source
import org.junit.Test

class IndexTest {

    @Test
    fun testCanIndexDeclarationsWithTheSameNameInTheSameFile() = codegen(
        """
            @Given val foo get() = Foo()
            
            @Given fun foo() = Foo()
        """
    )

    @Test
    fun testCanIndexDeclarationsWithTheSameNameInTheSamePackage() = codegen(
        source(
            """
                    @Given val foo get() = Foo()
                """
        ),
        source(
            """
                    @Given fun foo() = Foo()
                """
        )
    )

}
