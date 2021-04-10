package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.multiCodegen
import org.junit.Test

class TypeInferenceTest {
    @Test
    fun testFunctionExpressionInference() = multiCodegen(
        """
            class ChildGivenScopeModule<P : GivenScope, T, S : T> {
                @Given
                fun factory(
                    @Given scopeFactory: S
                ): @InstallElement<P> @ChildScopeFactory T = scopeFactory
            }
            fun <P : GivenScope, P1, C : GivenScope> ChildGivenScopeModule1() = 
                ChildGivenScopeModule<P, 
                (P1) -> C, (@Given @InstallElement<C> P1) -> C>()

            typealias MyGivenScope = DefaultGivenScope
            """,
        """
            @Given
            fun myGivenScopeModule() = 
                ChildGivenScopeModule1<AppGivenScope, Foo, MyGivenScope>()

            @Given fun bar(@Given foo: Foo) = Bar(foo)

            fun invoke(@Given appGivenScope: AppGivenScope) = 
                given<@ChildScopeFactory (Foo) -> MyGivenScope>()
                """
    )

    @Test
    fun testPropertyExpressionInference() = multiCodegen(
        """
            class ChildGivenScopeModule<P : GivenScope, T, S : T> {
                @Given
                fun factory(
                    @Given scopeFactory: S
                ): @InstallElement<P> @ChildScopeFactory T = scopeFactory
            }
            fun <P : GivenScope, P1, C : GivenScope> ChildGivenScopeModule1() = 
                ChildGivenScopeModule<P, 
                (P1) -> C, (@Given @InstallElement<C> P1) -> C>()

            typealias MyGivenScope = DefaultGivenScope
            """,
        """
            @Given
            val myGivenScopeModule = 
                ChildGivenScopeModule1<AppGivenScope, Foo, MyGivenScope>()

            @Given fun bar(@Given foo: Foo) = Bar(foo)

            fun invoke(@Given appGivenScope: AppGivenScope) = 
                given<@ChildScopeFactory (Foo) -> MyGivenScope>()
                """
    )
}