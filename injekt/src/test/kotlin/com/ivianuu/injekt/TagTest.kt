package com.ivianuu.injekt

import junit.framework.Assert.assertTrue
import org.junit.Test

class TagTest {

    @Test
    fun testContains() {
        val tag = Factory + Single + Eager + Bound
        assertTrue(Factory in tag)
        assertTrue(Single in tag)
        assertTrue(Eager in tag)
        assertTrue(Bound in tag)
    }

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