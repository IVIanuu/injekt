package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.FactoryKind
import com.ivianuu.injekt.SingleKind
import com.ivianuu.injekt.common.ReusableKind
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName

data class BindingDescriptor(
    val target: ClassName,
    val factoryName: ClassName,
    val kind: Kind,
    val scope: ClassName?,
    val constructorParams: List<ParamDescriptor>
) {
    enum class Kind(val impl: ClassName) {
        FACTORY(FactoryKind::class.asClassName()),
        REUSABLE(ReusableKind::class.asClassName()),
        SINGLE(SingleKind::class.asClassName())
    }
}

data class ParamDescriptor(
    val kind: Kind,
    val name: String?,
    val qualifier: ClassName?,
    val paramIndex: Int
) {
    enum class Kind { VALUE, LAZY, PROVIDER }
}