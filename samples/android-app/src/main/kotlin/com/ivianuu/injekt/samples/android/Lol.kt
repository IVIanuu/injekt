package com.ivianuu.injekt.samples.android

import com.ivianuu.injekt.ApplicationContext
import com.ivianuu.injekt.Context
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.given
import com.ivianuu.injekt.rootContext

fun main() {
    val context = rootContext<ApplicationContext>()
    a(0, context)
    a(0)
}

class ApplicationContextImpl {
    fun string(): String = ""
}

class ReaderClass {
    init {
        given<String>()
    }
}

fun ReaderClass(string: String = given()): ReaderClass = error("Stub")

fun ReaderClass(context: Context): ReaderClass = error("Stub")

@Reader
val readerProperty: String
    get() = given()

@Reader
fun readerProperty(string: String = given()): String = error("Stub")

@Reader
fun readerProperty(context: Context): String = error("Stub")

@Reader
fun c() {
    a(0, string = "")
}

@Reader
fun c(context: Context) {
    error("")
}

@Reader
fun a(int: Int) {
    b()
}

@Reader
fun a(int: Int, string: String = given()) {
    b(string)
}

fun a(int: Int, context: Context) {
    error("Stub")
}

@Reader
fun b(string: String = given()) {
}

fun b(context: Context) {
    error("Stub")
}
