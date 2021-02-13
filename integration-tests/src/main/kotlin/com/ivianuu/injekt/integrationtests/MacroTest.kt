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

import com.ivianuu.injekt.test.*
import junit.framework.Assert.*
import org.junit.Test

class MacroTest {

    @Test
    fun testMacroGiven() = codegen(
        """
            @Qualifier annotation class Trigger
            @Macro @Given fun <T : @Trigger S, S> macroImpl(@Given instance: T): S = instance

            @Trigger @Given fun foo() = Foo()

            fun invoke() = given<Foo>()
        """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testMacroSetElement() = codegen(
        """
            @Qualifier annotation class Trigger
            @Macro @GivenSetElement fun <T : @Trigger S, S> macroImpl(@Given instance: T): S = instance

            @Trigger @Given fun foo() = Foo()

            fun invoke() = given<Set<Foo>>()
        """
    ) {
        assertEquals(1, invokeSingleFile<Set<Foo>>().size)
    }

    @Test
    fun testMacroWithoutTypeParameter() = codegen(
        """
            @Macro @Given fun macroImpl() = Unit
        """
    ) {
        assertCompileError("type parameter")
    }

    @Test
    fun testMacroWithQualifierWithTypeParameter() = codegen(
        """
            @Qualifier annotation class Trigger<S>
            @Macro @Given fun <@ForKey T : @Trigger<S> Any?, @ForKey S> macroImpl() = 
                keyOf<S>()

            @Trigger<Bar> @Given fun foo() = Foo()

            fun invoke() = given<Key<Bar>>().value
        """
    ) {
        assertEquals("com.ivianuu.injekt.test.Bar", invokeSingleFile())
    }

    @Test
    fun testMacroWithQualifierWithTypeParameterMulti() = multiCodegen(
        listOf(
            source(
                """
                    @Qualifier annotation class Trigger<S>
                    @Macro @Given fun <@ForKey T : @Trigger<S> Any?, @ForKey S> macroImpl() = 
                        keyOf<S>()
                """
            )
        ),
        listOf(
            source(
                """
                    @Trigger<Bar> @Given fun foo() = Foo()
                """
            )
        ),
        listOf(
            source(
                """
                    fun <T> givenKeyOf(@Given value: () -> Key<T>) = value()
                    fun invoke() = givenKeyOf<Bar>().value
                """,
                name = "File.kt"
            )
        )
    ) {
        assertEquals("com.ivianuu.injekt.test.Bar", it.last().invokeSingleFile())
    }

    @Test
    fun testMacroWithQualifierWithTypeParameterMulti2() = multiCodegen(
        listOf(
            source(
                """
                    @Qualifier annotation class Trigger<S>
                    @Macro @Given fun <@ForKey T : @Trigger<S> Any?, @ForKey S> macroImpl() = 
                        keyOf<S>()
                """
            )
        ),
        listOf(
            source(
                """
                    @Module val fooModule: @Given () -> @Trigger<Bar> Foo = { Foo() }
                """
            )
        ),
        listOf(
            source(
                """
                    fun <T> givenKeyOf(@Given value: () -> Key<T>) = value()
                    fun invoke() = givenKeyOf<Bar>().value
                """,
                name = "File.kt"
            )
        )
    ) {
        assertEquals("com.ivianuu.injekt.test.Bar", it.last().invokeSingleFile())
    }

    @Test
    fun testMacroClass() = codegen(
        """
            @Qualifier annotation class Trigger
            @Macro @Given fun <T : @Trigger S, S> macroImpl(@Given instance: T): S = instance

            @Trigger @Given class NotAny

            fun invoke() = given<NotAny>()
        """
    ) {
        invokeSingleFile()
    }

    @Test
    fun testMacroChain() = codegen(
        """
            @Qualifier annotation class Trigger

            @Macro @FooTrigger @Given fun <T : @Trigger Any?> triggerImpl() = 0

            @Qualifier annotation class FooTrigger
            @Macro @Given fun <T : @FooTrigger Any?> fooTriggerImpl() = Foo()

            @Trigger @Given fun dummy() = 0L
            
            fun invoke() = given<Foo>()
        """
    ) {
        assertTrue(invokeSingleFile() is Foo)
    }

    @Test
    fun testMultipleMacroTargetsOfTheSameType() = codegen(
        """
            @Qualifier annotation class Trigger
            @Macro @GivenSetElement fun <T : @Trigger String> triggerImpl(@Given instance: T): String = instance

            @Trigger @Given fun a() = "a"
            @Trigger @Given fun b() = "b"            

            fun invoke() = given<Set<String>>()
        """
    ) {
        assertEquals(setOf("a", "b"), invokeSingleFile())
    }

    @Test
    fun testScoped() = multiCodegen(
        listOf(
            source(
                """
                    typealias ActivityComponent = Component
                    @Given fun activityComponent(
                        @Given builder: Component.Builder<ActivityComponent>
                    ): ActivityComponent = builder.build()
                    @Given fun appComponent(
                        @Given builder: Component.Builder<AppComponent>
                    ): AppComponent = builder.build()
                """
            )
        ),
        listOf(
            source(
                """
                    @Scoped<AppComponent> @Given fun foo() = Foo()
                    fun invoke() = given<Foo>()
                """,
                name = "File.kt"
            )
        )
    ) {
        assertTrue(it.last().invokeSingleFile() is Foo)
    }

    @Test
    fun testMultipleMacrosWithSameType() = codegen(
        """
            @Qualifier annotation class Trigger
            @Macro @GivenSetElement fun <T : @Trigger String> macroImpl(@Given instance: T): String = instance

            @Trigger @Given fun a() = "a"
            @Trigger @Given fun b() = "b"

            fun invoke() = given<Set<String>>()
        """
    ) {
        assertEquals(setOf("a", "b"), invokeSingleFile())
    }

    @Test
    fun testMacroWithoutContributionsFails() = codegen(
        """
            @Qualifier annotation class Trigger
            @Macro fun <T : @Trigger String> macroImpl(): String = ""
        """
    ) {
        assertCompileError("@Macro declaration must have 1 contribution annotation")
    }

    @Test
    fun testMacroTypeParameterNotMarkedAsUnused() = codegen(
        """
            @Qualifier annotation class Trigger
            @Macro @GivenSetElement fun <T : @Trigger String> macroImpl(): String = ""
        """
    ) {
        assertNoMessage("Type parameter \"T\" is never used")
    }

    @Test
    fun testNoFinalTypeWarningOnMacroTypeParameter() = codegen(
        """
            @Qualifier annotation class Trigger
            @Macro @GivenSetElement fun <T : @Trigger String> macroImpl(): String = ""
        """
    ) {
        assertFalse(
            "'String' is a final type, and thus a value of the type parameter is predetermined" in messages
        )
    }

}
