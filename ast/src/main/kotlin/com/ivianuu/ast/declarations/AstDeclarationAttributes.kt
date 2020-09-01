package com.ivianuu.ast.declarations

import com.ivianuu.ast.utils.AttributeArrayOwner
import com.ivianuu.ast.utils.NullableArrayMapAccessor
import com.ivianuu.ast.utils.TypeRegistry
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

abstract class AstDeclarationDataKey

class AstDeclarationAttributes : AttributeArrayOwner<AstDeclarationDataKey, Any>() {
    override val typeRegistry: TypeRegistry<AstDeclarationDataKey, Any>
        get() = AstDeclarationDataRegistry

    internal operator fun set(key: KClass<out AstDeclarationDataKey>, value: Any?) {
        if (value == null) {
            removeComponent(key)
        } else {
            registerComponent(key, value)
        }
    }
}

/*
 * Example of adding new attribute for declaration:
 *
 *    object SomeKey : AstDeclarationDataKey()
 *    var AstDeclaration.someString: String? by AstDeclarationDataRegistry.data(SomeKey)
 */
object AstDeclarationDataRegistry : TypeRegistry<AstDeclarationDataKey, Any>() {
    fun <K : AstDeclarationDataKey, V : Any> data(key: K): ReadWriteProperty<AstDeclaration, V?> {
        val kClass = key::class
        return DeclarationDataAccessor(generateNullableAccessor(kClass), kClass)
    }

    private class DeclarationDataAccessor<V : Any>(
        val dataAccessor: NullableArrayMapAccessor<AstDeclarationDataKey, Any, V>,
        val key: KClass<out AstDeclarationDataKey>
    ) : ReadWriteProperty<AstDeclaration, V?> {
        override fun getValue(thisRef: AstDeclaration, property: KProperty<*>): V? {
            return dataAccessor.getValue(thisRef.attributes, property)
        }

        override fun setValue(thisRef: AstDeclaration, property: KProperty<*>, value: V?) {
            thisRef.attributes[key] = value
        }
    }
}
