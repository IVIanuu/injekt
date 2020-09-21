package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.ApplicationContext
import com.ivianuu.injekt.Given
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.name.FqName

@Given(ApplicationContext::class)
class InjektAttributes {
    private val map = mutableMapOf<Any, Any>()
    operator fun <K : Key<V>, V : Any> get(key: K): V? = map[key] as? V
    operator fun <K : Key<V>, V : Any> set(key: K, value: V) {
        map[key] = value
    }

    interface Key<V : Any>

    data class ContextFactoryKey(val filePath: String, val startOffset: Int) : Key<FqName>
    data class IrFunctionTypeParametersMapKey(val attributesId: Any) :
        Key<Map<IrTypeParameter, IrTypeParameter>>
}
