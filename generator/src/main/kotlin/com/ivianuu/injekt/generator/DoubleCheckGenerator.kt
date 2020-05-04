package com.ivianuu.injekt.generator

fun main() {
    fun doubleCheck(parameterCount: Int) = """
        |class DoubleCheck$parameterCount<${commaSeparated(parameterCount) { "P$it" } + if (parameterCount != 0) ", " else ""}R>(private var delegate: ((${commaSeparated(
        parameterCount
    ) { "P$it" }}) -> R)?) : (${commaSeparated(parameterCount) { "P$it" }}) -> R {
        |   private var value: Any? = this
        |   override fun invoke(${commaSeparated(parameterCount) { "p$it: P$it" }}): R {
        |       var value = this.value
        |       if (value === this) {
        |           synchronized(this) {
        |               value = this.value
        |               if (value === this) {
        |                   value = delegate!!(${commaSeparated(parameterCount) { "p$it" }})
        |                   this.value = value
        |                   delegate = null
        |               }
        |           }
        |       }
        |       return value as R
        |   }
        |}
    """.trimMargin()

    (0..MAX_PARAMETERS).forEach {
        println(doubleCheck(it))
        println()
    }
}