package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.assertInternalError
import com.ivianuu.injekt.test.codegen
import org.junit.Test

class CircularDependencyTest {

    @Test
    fun testCircularDependencyFails() = codegen(
        """
            @Binding class A(b: B)
            @Binding class B(a: A)

            @Component abstract class MyComponent {
                abstract val b: B
            }
        """
    ) {
        assertInternalError("circular")
    }

    @Test
    fun testProviderBreaksCircularDependency() = codegen(
        """
            @Binding class A(b: B)
            @Scoped(MyComponent::class) @Binding class B(a: () -> A)
            
            @Component abstract class MyComponent {
                abstract val b: B
            }
        """
    )

    @Test
    fun testIrrelevantProviderInChainDoesNotBreakCircularDependecy() = codegen(
        """
            @Binding class A(b: () -> B)
            @Binding class B(b: C)
            @Binding class C(b: B)
            
            @Component abstract class MyComponent {
                abstract val c: C
            }
        """
    ) {
        assertInternalError("circular")
    }

    @Test
    fun testAssistedBreaksCircularDependency() = codegen(
        """
            @Binding class A(b: B)
            @Scoped(MyComponent::class) @Binding class B(a: (B) -> A)
            
            @Component abstract class MyComponent {
                abstract val b: B
            }
        """
    )

    @Test
    fun testFunBindingBreaksCircularDependency() = codegen(
        """
            @FunBinding fun A(b: B) {
            }
            
            @FunBinding fun B(a: A) {
            }
            
            @Component abstract class MyComponent {
                abstract val b: B
            }
        """
    )

    @Test
    fun testLazyRequestInSetBreaksCircularDependency() = codegen(
        """
            @FunBinding fun A(b: B) {
            }
            
            @FunBinding fun B(a: A) {
            }
            
            @SetElements fun set(a: A, b: B): Set<Any> = setOf(a, b)
            
            @Component abstract class MyComponent {
                abstract val set: Set<Any>
            }
        """
    )

    @Test
    fun testLazyRequestInMapBreaksCircularDependency() = codegen(
        """
            @FunBinding fun A(b: B) {
            }
            
            @FunBinding fun B(a: A) {
            }
            
            @MapEntries fun map(a: A, b: B): Map<String, Any> = mapOf("a" to a, "b" to b)
            
            @Component abstract class MyComponent {
                abstract val map: Map<String, Any>
            }
        """
    )

    @Test
    fun testLazyRequestInInterceptorBreaksCircularDependency() = codegen(
        """
            @Interceptor fun interceptor(a: A, factory: () -> B): () -> B {
                return factory
            }
            
            @FunBinding fun A(b: B) {
            }
            
            @FunBinding fun B(a: A) {
            }
            
            @MapEntries fun map(a: A, b: B): Map<String, Any> = mapOf("a" to a, "b" to b)
            
            @Component abstract class MyComponent {
                abstract val map: Map<String, Any>
            }
        """
    )

}
