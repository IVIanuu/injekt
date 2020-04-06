package com.ivianuu.injekt

import junit.framework.Assert.assertTrue
import org.junit.Test

class TagTest {

    @Test
    fun testSideEffectTag() {
        var called = false
        val tag = sideEffectTag("test") {
            called = true
        }
        Component {
            bind(tag = tag) { }
        }

        assertTrue(called)
    }

    @Test
    fun testInterceptingTag() {
        var called = false
        val tag = interceptingTag("test") {
            called = true
            it
        }
        Component {
            bind(tag = tag) { }
        }

        assertTrue(called)
    }

}