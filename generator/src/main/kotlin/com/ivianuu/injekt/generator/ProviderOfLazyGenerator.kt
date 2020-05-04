package com.ivianuu.injekt.generator

fun main() {
    fun doubleCheck(parameterCount: Int) = """
        |class ProviderOfLazy$parameterCount<${commaSeparated(parameterCount) { "P$it" } + if (parameterCount != 0) ", " else ""}R>(private val provider: (${commaSeparated(
        parameterCount
    ) { "P$it" }}) -> R) : (${commaSeparated(parameterCount) { "P$it" }}) -> @Lazy (${commaSeparated(
        parameterCount
    ) { "P$it" }}) -> R {
        |   override fun invoke(${commaSeparated(parameterCount) { "p$it: P$it" }}) = DoubleCheck$parameterCount(provider)
        |}
    """.trimMargin()

    (0..MAX_PARAMETERS).forEach {
        println(doubleCheck(it))
        println()
    }
}
