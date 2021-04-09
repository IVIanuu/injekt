package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.multiCodegen
import com.ivianuu.injekt.test.singleAndMultiCodegen
import org.junit.Test

class TypeInferenceTest {
    @Test
    fun testFunctionExpressionInference() = multiCodegen(
        """
            class ChildGivenScopeModule<P : GivenScope, T, S : T> {
                @Given
                fun factory(
                    @Given scopeFactory: S
                ): @GivenScopeElementBinding<P> @ChildGivenScopeFactory T = scopeFactory
            }
            fun <P : GivenScope, P1, C : GivenScope> ChildGivenScopeModule1() = 
                ChildGivenScopeModule<P, 
                (P1) -> C, (@Given @GivenScopeElementBinding<C> P1) -> C>()

            typealias MyGivenScope = DefaultGivenScope
            """,
        """
            @Given
            fun myGivenScopeModule() = 
                ChildGivenScopeModule1<AppGivenScope, Foo, MyGivenScope>()

            @Given fun bar(@Given foo: Foo) = Bar(foo)

            fun invoke(@Given appGivenScope: AppGivenScope) = 
                given<@ChildGivenScopeFactory (Foo) -> MyGivenScope>()
                """
    )

    @Test
    fun testPropertyExpressionInference() = multiCodegen(
        """
            class ChildGivenScopeModule<P : GivenScope, T, S : T> {
                @Given
                fun factory(
                    @Given scopeFactory: S
                ): @GivenScopeElementBinding<P> @ChildGivenScopeFactory T = scopeFactory
            }
            fun <P : GivenScope, P1, C : GivenScope> ChildGivenScopeModule1() = 
                ChildGivenScopeModule<P, 
                (P1) -> C, (@Given @GivenScopeElementBinding<C> P1) -> C>()

            typealias MyGivenScope = DefaultGivenScope
            """,
        """
            @Given
            val myGivenScopeModule = 
                ChildGivenScopeModule1<AppGivenScope, Foo, MyGivenScope>()

            @Given fun bar(@Given foo: Foo) = Bar(foo)

            fun invoke(@Given appGivenScope: AppGivenScope) = 
                given<@ChildGivenScopeFactory (Foo) -> MyGivenScope>()
                """
    )
}