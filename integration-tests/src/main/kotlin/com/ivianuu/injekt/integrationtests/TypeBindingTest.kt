package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.test.codegen
import org.junit.Test

class TypeBindingTest {

    @Test
    fun testPropertyTypeBinding() = codegen(
        """
            @TypeBinding
            val userName = "user"
            
            @Component
            abstract class MyComponent {
                abstract val userName: UserName
            }
        """
    )

    @Test
    fun testComposablePropertyTypeBinding() = codegen(
        """
            @TypeBinding
            @Composable
            val userName get() = "user"
            
            @Component
            abstract class MyComponent {
                @Composable
                abstract val userName: UserName
            }
        """
    )

    @Test
    fun testExtensionPropertyTypeBinding() = codegen(
        """
            @TypeBinding
            val String.userName get() = "user"
            
            @Component
            abstract class MyComponent {
                abstract val userName: UserName
                @Binding protected val string = ""
            }
        """
    )

    @Test
    fun testFunctionTypeBinding() = codegen(
        """
            @TypeBinding
            fun userName() = "user"
            
            @Component
            abstract class MyComponent {
                abstract val userName: UserName
            }
        """
    )

    @Test
    fun testExtensionFunctionTypeBinding() = codegen(
        """
            @TypeBinding
            fun String.userName() = "user"
            
            @Component
            abstract class MyComponent {
                abstract val userName: UserName
                @Binding protected val string = ""
            }
        """
    )

    @Test
    fun testSuspendFunctionTypeBinding() = codegen(
        """
            @TypeBinding
            suspend fun userName() = "user"
            
            @Component
            abstract class MyComponent {
                abstract suspend fun userName(): UserName
            }
        """
    )

    @Test
    fun testComposableFunctionTypeBinding() = codegen(
        """
            @TypeBinding
            @Composable
            fun userName() = "user"
            
            @Component
            abstract class MyComponent {
                @Composable
                abstract fun userName(): UserName
            }
        """
    )

}
