package com.ivianuu.injekt.gradle

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import kotlin.reflect.KProperty

internal operator fun <T> Provider<T>.getValue(thisRef: Any?, property: KProperty<*>) = get()

internal operator fun <T> Property<T>.setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    set(value)
}

internal inline fun <reified T : Task> Project.locateTask(name: String): TaskProvider<T>? =
    try {
        tasks.withType(T::class.java).named(name)
    } catch (e: UnknownTaskException) {
        null
    }