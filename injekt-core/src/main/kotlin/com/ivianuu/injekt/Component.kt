package com.ivianuu.injekt

open class Component(block: ComponentDsl.() -> Unit) {
    open fun <T> get(key: Int): T = stub()
}

class ComponentDsl {
    inline fun <reified T : Component> parents(component: T): Unit = stub()

    inline fun <reified T : Module> modules(modules: T): Unit = stub()

    inline fun <reified T> instance(instance: T, qualifier: Qualifier? = null): Unit = stub()
}
