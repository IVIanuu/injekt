package com.ivianuu.injekt.compiler

import io.github.classgraph.ClassGraph
import org.junit.Test
import java.io.File

class Lol {

    @Test
    fun testDifferentCompilations() {
        val compilation1 = compile {
            this.sources = listOf(
                source(
                    """
                    @Target(AnnotationTarget.TYPE)
                    annotation class TypeAnnotation

                    class AnnotatedClass(foo: @TypeAnnotation Foo)
                """
                )
            )
        }.also {
            it.assertOk()
        }

        val compilation2 = compile {
            this.sources = listOf(
                source(
                    """
                    class Dummy
                """
                )
            )
            val classGraph = ClassGraph()
                .addClassLoader(compilation1.classLoader)
            val classpaths = classGraph.classpathFiles
            val modules = classGraph.modules.mapNotNull { it.locationFile }
            this.classpaths += (classpaths + modules).distinctBy(File::getAbsolutePath)
        }.also { it.assertOk() }
    }

}