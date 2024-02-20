package com.ivianuu.injekt.integrationtests

import org.junit.*

class ModuleTest {
  @Test fun testClassModule() = singleAndMultiCodegen(
    """
      @Provide val foo = Foo()
      @Provide class BarModule(private val foo: Foo) {
        @Provide val bar get() = Bar(foo)
      }
    """,
    """
      fun invoke() = inject<Bar>() 
    """
  )
}
