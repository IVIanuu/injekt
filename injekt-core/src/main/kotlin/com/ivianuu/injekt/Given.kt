package com.ivianuu.injekt

inline fun <reified T> Context.givenOrNull(): T? =
    givenOrNull(keyOf())

fun <T> Context.givenOrNull(key: Key<T>): T? =
    givenProviderOrNull(key)?.let { runReader { it() } }

inline fun <reified T> Context.given(): T = given(keyOf())

fun <T> Context.given(key: Key<T>): T = givenProviderOrNull(key)?.let { runReader { it() } }
    ?: error("No given found for '$key'")

@JvmName("readerGiven")
@Reader
inline fun <reified T> given(): T = currentContext.given()

@JvmName("readerGiven")
@Reader
fun <T> given(key: Key<T>): T = currentContext.given(key)
