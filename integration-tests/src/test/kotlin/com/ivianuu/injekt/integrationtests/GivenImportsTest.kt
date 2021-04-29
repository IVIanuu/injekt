/*
 * Copyright 2021 Manuel Wrage
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

import com.ivianuu.injekt.test.*
import io.kotest.matchers.*
import io.kotest.matchers.types.*
import org.jetbrains.kotlin.name.*
import org.junit.*

class GivenImportsTest {
    @Test
    fun testUnresolvedImport() = codegen(
        """
            @GivenImports("a")
            fun invoke() {
            }
        """
    ) {
        compilationShouldHaveFailed("Unresolved given import")
    }

    @Test
    fun testUnresolvedStarImport() = codegen(
        """
            @GivenImports("a.*")
            fun invoke() {
            }
        """
    ) {
        compilationShouldHaveFailed("Unresolved given import")
    }

    @Test
    fun testMalformedImport() = codegen(
        """
            @GivenImports("-_;-")
            fun invoke() {
            }
        """
    ) {
        compilationShouldHaveFailed("Cannot read given import")
    }

    @Test
    fun testCompileTimeConstant() = codegen(
        """
            fun invoke() = withGivenImports(if (true) "com.ivianuu.injekt.common.*" else "com.ivianuu.injekt.scope.*") {
                
            }
        """
    ) {
        compilationShouldHaveFailed("Given import must be a compile time constant")
    }

    @Test
    fun testDuplicatedImports() = codegen(
        """
            @GivenImports("kotlin.collections.*", "kotlin.collections.*")
            fun invoke() {
            }
        """
    ) {
        compilationShouldHaveFailed("Duplicated given import")
    }

    @Test
    fun testNestedDuplicatedImports() = codegen(
        """
            @GivenImports("kotlin.collections.*")
            fun invoke() {
                withGivenImports("kotlin.collections.*") {
                    
                }
            }
        """
    ) {
        compilationShouldHaveFailed("Duplicated given import")
    }

    @Test
    fun testImportIsNotGiven() = codegen(
        listOf(
            source(
                """
                    val foo = Foo()
                """,
                packageFqName = FqName("givens")
            ),
            source(
                """
                    @GivenImports("givens.foo")
                    fun invoke() = Unit
                    """,
                name = "File.kt"
            )
        )
    ) {
        compilationShouldHaveFailed("Given import does not contain given declarations")
    }

    @Test
    fun testStarImportHasNoGivens() = codegen(
        listOf(
            source(
                """
                    val foo = Foo()
                """,
                packageFqName = FqName("givens")
            ),
            source(
                """
                    @GivenImports("givens.foo")
                    fun invoke() = Unit
                    """,
                name = "File.kt"
            )
        )
    ) {
        compilationShouldHaveFailed("Given import does not contain given declarations")
    }

    @Test
    fun testUnusedImport() = codegen(
        """
            @GivenImports("kotlin.collections.*")
            fun invoke() {
            }
        """
    ) {
        shouldContainMessage("Unused given import")
    }

    @Test
    fun testUsedImport() = codegen(
        listOf(
            source(
                """
                    @Given val foo = Foo()
                """,
                packageFqName = FqName("givens")
            ),
            source(
                """
                    @GivenImports("givens.foo")
                    fun invoke() = given<Foo>()
                    """,
                name = "File.kt"
            )
        )
    ) {
        shouldNotContainMessage("Unused given import")
    }

    @Test
    fun testUsedStarImport() = codegen(
        listOf(
            source(
                """
                    @Given val foo = Foo()
                """,
                packageFqName = FqName("givens")
            ),
            source(
                """
                    @GivenImports("givens.*")
                    fun invoke() = given<Foo>()
                    """,
                name = "File.kt"
            )
        )
    ) {
        shouldNotContainMessage("Unused given import")
    }

    @Test
    fun testStarImportSamePackage() = codegen(
        """
            @GivenImports("com.ivianuu.injekt.integrationtests.*")
            fun invoke() {
            }
        """
    ) {
        compilationShouldHaveFailed("Givens of the same package are automatically imported")
    }

    @Test
    fun testImportGivenSamePackage() = codegen(
        """
            @Given val foo = Foo()
            @GivenImports("com.ivianuu.injekt.integrationtests.foo")
            fun invoke() {
            }
        """
    ) {
        compilationShouldHaveFailed("Givens of the same package are automatically imported")
    }

    @Test
    fun testClassWithGivenImports() = multiCodegen(
        listOf(
            listOf(
                source(
                    """
                    @Given val foo = Foo()
                """,
                    packageFqName = FqName("givens")
                )
            ),
            listOf(
                source(
                    """
                    @GivenImports("givens.*")
                    class MyClass {
                        fun invoke() = given<Foo>()
                    }
                    fun invoke() = MyClass().invoke()
                    """,
                    name = "File.kt"
                )
            )
        )
    ) {
        invokeSingleFile().shouldBeTypeOf<Foo>()
    }

    @Test
    fun testFunctionWithGivenImports() = multiCodegen(
        listOf(
            listOf(
                source(
                    """
                    @Given val foo = Foo()
                """,
                    packageFqName = FqName("givens")
                )
            ),
            listOf(
                source(
                    """
                    @GivenImports("givens.*")
                    fun invoke() = given<Foo>()
                    """,
                    name = "File.kt"
                )
            )
        )
    ) {
        invokeSingleFile().shouldBeTypeOf<Foo>()
    }

    @Test
    fun testPropertyWithGivenImports() = multiCodegen(
        listOf(
            listOf(
                source(
                    """
                    @Given val foo = Foo()
                """,
                    packageFqName = FqName("givens")
                )
            ),
            listOf(
                source(
                    """
                    @GivenImports("givens.*")
                    val givenFoo = given<Foo>()
                    fun invoke() = givenFoo
                    """,
                    name = "File.kt"
                )
            )
        )
    ) {
        invokeSingleFile().shouldBeTypeOf<Foo>()
    }

    @Test
    fun testWithGivenImports() = codegen(
        listOf(
            source(
                """
                    fun invoke() = withGivenImports("com.ivianuu.injekt.common.*") {
                        given<TypeKey<Foo>>().value
                    }
                """,
                name = "File.kt",
                givenImports = emptyList()
            )
        )
    ) {
        invokeSingleFile() shouldBe "com.ivianuu.injekt.test.Foo"
    }
}
