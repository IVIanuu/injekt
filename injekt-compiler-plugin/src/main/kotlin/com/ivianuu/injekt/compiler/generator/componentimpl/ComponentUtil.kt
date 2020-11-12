package com.ivianuu.injekt.compiler.generator.componentimpl

import com.ivianuu.injekt.compiler.generator.Callable
import com.ivianuu.injekt.compiler.generator.ClassifierRef
import com.ivianuu.injekt.compiler.generator.TypeRef
import com.ivianuu.injekt.compiler.generator.ValueParameterRef
import com.ivianuu.injekt.compiler.generator.callableKind
import com.ivianuu.injekt.compiler.generator.replaceTypeParametersWithStars
import com.ivianuu.injekt.compiler.generator.substitute

fun ValueParameterRef.toBindingRequest(
    callable: Callable,
    substitutionMap: Map<ClassifierRef, TypeRef>
) = BindingRequest(
    if (callable.isFunBinding) {
        type
            .typeArguments
            .first()
            .substitute(substitutionMap)
            .replaceTypeParametersWithStars()
    } else {
        type.substitute(substitutionMap)
            .replaceTypeParametersWithStars()
    },
    callable.fqName.child(name),
    !hasDefault,
    if (callable.isFunBinding) type.callableKind
    else callable.callableKind
)