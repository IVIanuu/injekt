package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import org.junit.Test

class ImplBindingTest {

    @Test
    fun testSimpleImplBinding() = codegen(
        """
            interface Repository
            
            @ImplBinding
            class RepositoryImpl : Repository
            
            @Component
            abstract class MyComponent {
                abstract val repository: Repository
            }
        """
    )

    @Test
    fun testObjectImplBinding() = codegen(
        """
            interface Repository
            
            @ImplBinding
            object RepositoryImpl : Repository
            
            @Component
            abstract class MyComponent {
                abstract val repository: Repository
            }
        """
    )

}