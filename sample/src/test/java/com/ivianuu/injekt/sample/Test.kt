package com.ivianuu.injekt.sample

import org.junit.Test
import kotlin.system.measureNanoTime

/**
 * @author Manuel Wrage (IVIanuu)
 */
class Test {

    @Test
    fun testClassForName() {
        val classForName = measureNanoTime {
            Class.forName(AppDependency::class.java.name + "Factory")
        }.let { it / 1000000.0 }.format()
        println("Class for name took $classForName")
    }

    @Test
    fun testLoadClass() {
        val loadClass = measureNanoTime {
            javaClass.classLoader.loadClass(AppDependency::class.java.name + "Factory")
        }.let { it / 1000000.0 }.format()
        println("Load class took $loadClass")
    }


    private fun Double?.format(): String = String.format("%.2f ms", this)

}