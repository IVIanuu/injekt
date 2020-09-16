package com.ivianuu.injekt.compiler.codegen

import org.jetbrains.kotlin.name.FqName
import java.io.File

class ModuleRegistrarManager(
    private val serviceLoaderFile: File
) {

    private val impls = (if (serviceLoaderFile.exists()) serviceLoaderFile.readText() else "")
        .split("\n")
        .filter { it.isNotEmpty() }
        .toMutableSet()

    fun addImpl(impl: FqName) {
        impls += impl.asString()
    }

    fun removeImpl(impl: FqName) {
        impls -= impl.asString()
    }

    fun flush() {
        if (impls.isNotEmpty()) {
            serviceLoaderFile.parentFile.mkdirs()
            serviceLoaderFile.createNewFile()
            serviceLoaderFile.writeText(
                impls.joinToString("\n")
            )
        } else {
            serviceLoaderFile.delete()
        }
    }

}
