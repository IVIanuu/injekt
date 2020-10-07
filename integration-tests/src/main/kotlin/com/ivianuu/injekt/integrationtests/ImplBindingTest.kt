package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import org.junit.Test

class ImplBindingTest {

    @Test
    fun testImplBinding() = codegen(
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

}