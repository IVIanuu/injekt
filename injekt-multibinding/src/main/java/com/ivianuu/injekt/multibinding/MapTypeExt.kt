package com.ivianuu.injekt.multibinding

import com.ivianuu.injekt.BeanDefinition
import com.ivianuu.injekt.ModuleContext
import kotlin.reflect.KClass

/** Calls [ModuleContext.mapBinding] */
fun <T : Any> ModuleContext.stringMapBinding(mapName: String) =
    mapBinding<String, T>(mapName)

/** Calls [BeanDefinition.intoMap] */
infix fun <T : Any, S : T> BeanDefinition<S>.intoStringMap(pair: Pair<String, String>) =
    intoMap<String, T, S>(pair)

/** Calls [ModuleContext.bindIntoMap] */
inline fun <reified T : Any, reified S : T> ModuleContext.bindIntoStringMap(
    mapName: String,
    key: String,
    declarationName: String? = null
) = bindIntoMap<String, T, S>(mapName, key, declarationName)

/** Calls [ModuleContext.mapBinding] */
fun <T : Any> ModuleContext.classMapBinding(mapName: String) =
    mapBinding<KClass<out T>, T>(mapName)

/** Calls [BeanDefinition.intoMap] */
inline infix fun <T : Any, reified S : T> BeanDefinition<S>.intoClassMap(mapName: String) =
    intoMap<KClass<out T>, T, S>(mapName to S::class)

/** Calls [ModuleContext.bindIntoMap] */
inline fun <reified T : Any, reified S : T> ModuleContext.bindIntoClassMap(
    mapName: String,
    declarationName: String? = null
) = bindIntoMap<KClass<out T>, T, S>(mapName, S::class, declarationName)

/** Calls [ModuleContext.mapBinding] */
fun <T : Any> ModuleContext.intMapBinding(mapName: String) =
    mapBinding<Int, T>(mapName)

/** Calls [BeanDefinition.intoMap] */
infix fun <T : Any, S : T> BeanDefinition<S>.intoIntMap(pair: Pair<String, Int>) =
    intoMap<Int, T, S>(pair)

/** Calls [ModuleContext.bindIntoMap] */
inline fun <reified T : Any, reified S : T> ModuleContext.bindIntoIntMap(
    mapName: String,
    key: Int,
    declarationName: String? = null
) = bindIntoMap<Int, T, S>(mapName, key, declarationName)

/** Calls [ModuleContext.mapBinding] */
fun <T : Any> ModuleContext.longMapBinding(mapName: String) =
    mapBinding<Long, T>(mapName)

/** Calls [BeanDefinition.intoMap] */
infix fun <T : Any, S : T> BeanDefinition<S>.intoLongMap(pair: Pair<String, Long>) =
    intoMap<Long, T, S>(pair)

/** Calls [ModuleContext.bindIntoMap] */
inline fun <reified T : Any, reified S : T> ModuleContext.bindIntoLongMap(
    mapName: String,
    key: Long,
    declarationName: String? = null
) = bindIntoMap<Long, T, S>(mapName, key, declarationName)