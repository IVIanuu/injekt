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
import io.kotest.assertions.throwables.*
import io.kotest.matchers.*
import org.junit.*

class TypeKeyTest {
    @Test
    fun testTypeKeyOf() = codegen(
        """
           fun invoke() = typeKeyOf<String>() 
        """
    ) {
        invokeSingleFile() shouldBe "kotlin.String"
    }

    @Test
    fun testNullableTypeKeyOf() = codegen(
        """
           fun invoke() = typeKeyOf<String?>() 
        """
    ) {
        invokeSingleFile() shouldBe "kotlin.String?"
    }

    @Test
    fun testForTypeKeyTypeParameter() = singleAndMultiCodegen(
        """
            inline fun <@ForTypeKey T> listTypeKeyOf() = typeKeyOf<List<T>>()
        """,
        """
           fun invoke() = listTypeKeyOf<String>()  
        """
    ) {
        invokeSingleFile() shouldBe "kotlin.collections.List<kotlin.String>"
    }

    @Test
    fun testTypeKeyOfWithTypeAliasWithNullableExpandedType() = codegen(
        """
            typealias MyAlias = String?
            fun invoke() = typeKeyOf<MyAlias>() 
        """
    ) {
        invokeSingleFile() shouldBe "com.ivianuu.injekt.integrationtests.MyAlias"
    }

    @Test
    fun testTypeKeyOfWithTypeAlias() = codegen(
        """
            typealias MyAlias = String
            fun invoke() = typeKeyOf<MyAlias>() 
        """
    ) {
        invokeSingleFile() shouldBe "com.ivianuu.injekt.integrationtests.MyAlias"
    }

    @Test
    fun testTypeKeyOfWithNullableTypeAlias() = codegen(
        """
            typealias MyAlias = String
            fun invoke() = typeKeyOf<MyAlias?>()
        """
    ) {
        invokeSingleFile() shouldBe "com.ivianuu.injekt.integrationtests.MyAlias?"
    }

    @Test
    fun testTypeKeyOfWithComposableType() = codegen(
        """
            fun invoke() = typeKeyOf<@Composable () -> Unit>() 
        """
    ) {
        invokeSingleFile() shouldBe "[@androidx.compose.runtime.Composable]kotlin.Function0<kotlin.Unit>"
    }
    @Test
    fun testTypeKeyOfWithTypeAliasWithComposableExpandedType() = codegen(
        """
            typealias MyAlias = @Composable () -> Unit
            fun invoke() = typeKeyOf<MyAlias>() 
        """
    ) {
        invokeSingleFile() shouldBe "com.ivianuu.injekt.integrationtests.MyAlias"
    }

    @Test
    fun testTypeKeyOfWithQualifiers() = codegen(
        """
            fun invoke() = typeKeyOf<@Qualifier2 String>() 
        """
    ) {
        invokeSingleFile() shouldBe "[@com.ivianuu.injekt.test.Qualifier2]kotlin.String"
    }

    @Test
    fun testTypeKeyOfWithTypeAliasWithQualifiedExpandedType() = codegen(
        """
            typealias MyAlias = @Qualifier2 String
            fun invoke() = typeKeyOf<MyAlias>() 
        """
    ) {
        invokeSingleFile() shouldBe "com.ivianuu.injekt.integrationtests.MyAlias"
    }

    @Test
    fun testTypeKeyOfWithParameterizedQualifiers() = codegen(
        """
            @Qualifier 
            annotation class MyQualifier<T>
            fun invoke() = typeKeyOf<@MyQualifier<String> String>() 
        """
    ) {
        invokeSingleFile() shouldBe "[@com.ivianuu.injekt.integrationtests.MyQualifier<kotlin.String>]kotlin.String"
    }
    
    @Test
    fun testForTypeKeyTypeParameterInInterface() = singleAndMultiCodegen(
        """
            interface KeyFactory {
                fun <@ForTypeKey T> listTypeKeyOf(): TypeKey<List<T>>
                companion object : KeyFactory {
                    override fun <@ForTypeKey T> listTypeKeyOf() = typeKeyOf<List<T>>()
                }
            }
        """,
        """
           fun invoke() = KeyFactory.listTypeKeyOf<String>()  
        """
    ) {
        invokeSingleFile() shouldBe "kotlin.collections.List<kotlin.String>"
    }

    @Test
    fun testClassWithForTypeKeyParameterInInitializer() = singleAndMultiCodegen(
        """
            class MyClass<@ForTypeKey T> {
                val typeKey = typeKeyOf<T>()
            }
        """,
        """
           fun invoke() = MyClass<String>().typeKey 
        """
    ) {
        invokeSingleFile() shouldBe "kotlin.String"
    }

    @Test
    fun testClassWithForTypeKeyParameterInFunction() = singleAndMultiCodegen(
        """
            class MyClass<@ForTypeKey T> {
                fun typeKey() = typeKeyOf<T>()
            }
        """,
        """
           fun invoke() = MyClass<String>().typeKey() 
        """
    ) {
        invokeSingleFile() shouldBe "kotlin.String"
    }

    @Test
    fun testClassWithForTypeKeyParameterSubClass() = singleAndMultiCodegen(
        """
            abstract class MySuperClass<@ForTypeKey T> {
                val typeKey = typeKeyOf<T>()
            }
        """,
        """
            class MyClass<@ForTypeKey T> : MySuperClass<T>()
            fun invoke() = MyClass<String>().typeKey 
        """
    ) {
        invokeSingleFile() shouldBe "kotlin.String"
    }

    @Test
    fun testTypeKeyFromGivenCall() = singleAndMultiCodegen(
        """
            @Given fun <@ForTypeKey T> listKey(): TypeKey<List<T>> = typeKeyOf<List<T>>()
        """,
        """
           fun invoke() = given<TypeKey<List<@Qualifier1 Foo>>>() 
        """
    ) {
        invokeSingleFile() shouldBe
                "kotlin.collections.List<[@com.ivianuu.injekt.test.Qualifier1]com.ivianuu.injekt.test.Foo>"
    }

    @Test
    fun testNonForTypeKeyTypeParameterOverride() = singleAndMultiCodegen(
        """
            abstract class MySuperClass {
                abstract fun <@ForTypeKey T> func()
            }
        """,
        """
           class MySubClass : MySuperClass() {
                override fun <T> func() {
                }
            } 
        """
    ) {
        compilationShouldHaveFailed("Conflicting overloads")
    }

    @Test
    fun testPropertyWithForTypeKeyParameter() = singleAndMultiCodegen(
        """
            val <@ForTypeKey T> T.typeKey: TypeKey<T> get() = typeKeyOf<T>()
        """,
        """
           fun invoke() = "".typeKey 
        """
    ) {
        invokeSingleFile() shouldBe "kotlin.String"
    }

    @Test
    fun testNonTopLevelInlineForTypeKeyFunction() = singleAndMultiCodegen(
        """
            @Given object MyClass {
                @Given
                inline fun <@ForTypeKey T> myKey(): @Qualifier1 TypeKey<T> = typeKeyOf()
            }
        """,
        """
           fun invoke() = given<@Qualifier1 TypeKey<String>>()
        """
    ) {
        invokeSingleFile() shouldBe "kotlin.String"
    }

    @Test
    fun testTypeKeyWithStar() = codegen(
        """
           fun invoke() = typeKeyOf<List<*>>() 
        """
    ) {
        invokeSingleFile() shouldBe "kotlin.collections.List<*>"
    }

    @Test
    fun testTypeKeyWithStar2() = codegen(
        """
            @GivenImports("com.ivianuu.injekt.common.*", "com.ivianuu.injekt.scope.*")
            val scope = given<(@Given @InstallElement<AppGivenScope> List<*>) -> AppGivenScope>()
                .invoke(emptyList<Any?>())
            fun invoke() = scope.element<List<*>>()
        """
    ) {
        shouldNotThrow<IllegalStateException> { invokeSingleFile() }
    }
}
