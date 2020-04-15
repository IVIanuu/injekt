package com.ivianuu.injekt

interface Qualifier {
    fun plus(qualifier: Qualifier): Qualifier = stub()
}

annotation class ExampleQualifier {
    companion object : Qualifier
}
