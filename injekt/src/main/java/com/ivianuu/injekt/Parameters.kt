package com.ivianuu.injekt

/**
 * Parameters which will be used for assisted injection
 */
@Suppress("UNCHECKED_CAST")
inline class Parameters(val values: List<Any?>) {

    operator fun <T> component1(): T = elementAt(0)
    operator fun <T> component2(): T = elementAt(1)
    operator fun <T> component3(): T = elementAt(2)
    operator fun <T> component4(): T = elementAt(3)
    operator fun <T> component5(): T = elementAt(4)

    /**
     * Returns the element [i]
     */
    operator fun <T> get(i: Int): T = values[i] as T

    private fun <T> elementAt(i: Int): T =
        if (values.size > i) values[i] as T else throw IllegalArgumentException("Can't get parameter value #$i from $this")

}

/**
 * Number of contained elements
 */
val Parameters.size: Int get() = values.size

/**
 * Defines [Parameters]
 */
typealias ParametersDefinition = () -> Parameters

/**
 * Returns new [Parameters] which contains all [values]
 */
fun parametersOf(vararg values: Any?): Parameters = Parameters(listOf(*values))

/**
 * Returns new [Parameters] which contains the [value]
 */
fun parametersOf(value: Any?): Parameters =
    Parameters(listOf(value))

/**
 * Returns new empty parameters
 */
fun parametersOf(): Parameters =
    Parameters(emptyList())

/**
 * Returns empty [Parameters]
 */
fun emptyParameters(): Parameters = parametersOf()