package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import com.ivianuu.injekt.test.compilationShouldHaveFailed
import com.ivianuu.injekt.test.invokeSingleFile
import com.ivianuu.injekt.test.singleAndMultiCodegen
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import org.junit.Test

class NotGivenTest {
    @Test
    fun testFallsBackToNotGivenIfConditionIsMet() = singleAndMultiCodegen(
        """
            @Given
            @NotGiven<String>
            fun maybeFoo() = Foo()
        """,
        """
            fun invoke() = given<Foo>()
        """
    )

    @Test
    fun testCannotResolveNotGivenIfConditionIsNotMet() = singleAndMultiCodegen(
        """
            @Given
            @NotGiven<String>
            fun maybeFoo() = Foo()
            @Given val string = ""
        """,
        """
            fun invoke() = given<Foo>()
        """
    ) {
        compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.given")
    }

    @Test
    fun testNotGivenWithSelfNotGiven() = singleAndMultiCodegen(
        """
            @Given
            @NotGiven<Boolean>
            fun notGivenBoolean() = true
        """,
        """
            fun invoke() = given<Boolean>()
        """
    ) {
        invokeSingleFile<Boolean>().shouldBeTrue()
    }

    @Test
    fun testNotGivenWithExistingSelfNotGiven() = singleAndMultiCodegen(
        """
            @Given
            @NotGiven<Boolean>
            fun notGivenBoolean() = true
            @Given val givenBoolean = false
        """,
        """
            fun invoke() = given<Boolean>()
        """
    ) {
        invokeSingleFile<Boolean>().shouldBeFalse()
    }

    @Test
    fun testMultipleNotGivens() = singleAndMultiCodegen(
        """
            @Given
            @NotGiven<String>
            @NotGiven<Int>
            fun maybeFoo() = Foo()
        """,
        """
            fun invoke() = given<Foo>()
        """
    )

    @Test
    fun testNotGivenOnFunctionType() = singleAndMultiCodegen(
        """
            @Given fun maybeFoo(): @NotGiven<String> Foo = Foo()
            @Given val string = ""
        """,
        """
            fun invoke() = given<Foo>()
        """
    ) {
        compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.given")
    }

    @Test
    fun testNotGivenOnClass() = singleAndMultiCodegen(
        """
            @Given
            @NotGiven<String>
            class Dep
            @Given val string = ""
        """,
        """
            fun invoke() = given<Dep>()
        """
    ) {
        compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.integrationtests.Dep for parameter value of function com.ivianuu.injekt.given")
    }

    @Test
    fun testNotGivenOnClassConstructor() = singleAndMultiCodegen(
        """
            @Given
            class Dep @NotGiven<String> constructor()
            @Given val string = ""
        """,
        """
            fun invoke() = given<Dep>()
        """
    ) {
        compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.integrationtests.Dep for parameter value of function com.ivianuu.injekt.given")
    }

    @Test
    fun testNotGivenOnProperty() = singleAndMultiCodegen(
        """
            @NotGiven<String> @Given val maybeFoo = Foo()
            @Given val string = ""
        """,
        """
            fun invoke() = given<Foo>()
        """
    ) {
        compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.given")
    }

    @Test
    fun testNotGivenOnPropertyType() = singleAndMultiCodegen(
        """
            @Given val maybeFoo: @NotGiven<String> Foo = Foo()
            @Given val string = ""
        """,
        """
            fun invoke() = given<Foo>()
        """
    ) {
        compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.given")
    }

    @Test
    fun testNotGivenOnLocalVariable() = codegen(
        """
            fun invoke() {
                @Given @NotGiven<String> val maybeFoo = Foo()
                @Given val string = ""
                given<Foo>()
            }
        """
    ) {
        compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.given")
    }

    @Test
    fun testNotGivenOnLocalVariableType() = codegen(
        """
            fun invoke() {
                @Given val maybeFoo: @NotGiven<String> Foo = Foo()
                @Given val string = ""
                given<Foo>()
            }
        """
    ) {
        compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.given")
    }

    @Test
    fun testNotGivenOnValueParameter() = codegen(
        """
            fun invoke(@Given @NotGiven<String> maybeFoo: Foo) {
                @Given val string = ""
                given<Foo>()
            }
        """
    ) {
        compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.given")
    }

    @Test
    fun testNotGivenOnValueParameterType() = codegen(
        """
            fun invoke(@Given maybeFoo: @NotGiven<String> Foo) {
                @Given val string = ""
                given<Foo>()
            }
        """
    ) {
        compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.test.Foo for parameter value of function com.ivianuu.injekt.given")
    }

    @Test
    fun testNotGivenOnAbstractGiven() = singleAndMultiCodegen(
        """
            @Given
            @NotGiven<String>
            interface Dep
            @Given val string = ""
        """,
        """
            fun invoke() = given<Dep>()
        """
    ) {
        compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.integrationtests.Dep for parameter value of function com.ivianuu.injekt.given")
    }

    @Test
    fun testNotGivenWithSet() = singleAndMultiCodegen(
        """
            @Given @NotGiven<String> val foo = Foo()
        """,
        """
            fun invoke() = given<Set<Foo>>()
        """
    ) {
        compilationShouldHaveFailed("no given argument found of type kotlin.collections.Set<com.ivianuu.injekt.test.Foo> for parameter value of function com.ivianuu.injekt.given")
    }

    @Test
    fun testNotGivenOnConstrainedGiven() = singleAndMultiCodegen(
        """
            @Given val foo = Foo()
            @Given @NotGiven<String> fun <@Given T : Foo> fooToBar(@Given foo: T) = Bar(foo)
            @Given val string = ""
        """,
        """
            fun invoke() = given<Bar>()
        """
    ) {
        compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.test.Bar for parameter value of function com.ivianuu.injekt.given")
    }

    @Test
    fun testConstrainedGivenWithNotGivenCandidate() = singleAndMultiCodegen(
        """
            @Given val maybeFoo: @NotGiven<String> Foo = Foo()
            @Given fun <@Given T : Foo> fooToBar(@Given foo: T) = Bar(foo)
            @Given val string = ""
        """,
        """
            fun invoke() = given<Bar>()
        """
    ) {
        compilationShouldHaveFailed("no given argument found of type com.ivianuu.injekt.test.Bar for parameter value of function com.ivianuu.injekt.given")
    }
}
