package com.ivianuu.injekt.compiler.generator.componentimpl

import com.ivianuu.injekt.compiler.generator.Callable
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.ValueParameterRef
import com.ivianuu.injekt.compiler.generator.replaceTypeParametersWithStars
import com.ivianuu.injekt.compiler.generator.substitute

fun ValueParameterRef.toBindingRequest(
    callable: Callable,
    substitutionMap: Map<TypeRef, TypeRef>
) = BindingRequest(
    type = this.type.substitute(substitutionMap)
        .replaceTypeParametersWithStars(),
    origin = callable.fqName.child(name),
    required = !hasDefault,
    callableKind = callable.callableKind,
    lazy = callable.isFunBinding
)
