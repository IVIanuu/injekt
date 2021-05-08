package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.utils.addToStdlib.*

data class CustomErrorMessages(
    val notFoundMessage: String?,
    val ambiguousMessage: String?
)

fun List<Annotations>.customErrorMessages(
    typeParameters: List<ClassifierRef>
): CustomErrorMessages? {
    val replacements = typeParameters
        .map { "[${it.fqName.shortName()}]" to "[${it.fqName}]" }
    fun String.replaceTypeParameterRefs(): String =
        replacements.fold(this) { acc, nextReplacement ->
            acc.replace(nextReplacement.first, nextReplacement.second)
        }
    val notFoundMessage = (firstNotNullResult { it.findAnnotation(InjektFqNames.GivenNotFound) })
        ?.allValueArguments
        ?.get("message".asNameId())
        ?.value
        ?.cast<String>()
        ?.replaceTypeParameterRefs()
    val ambiguousMessage = (firstNotNullResult { it.findAnnotation(InjektFqNames.GivenAmbiguous) })
        ?.allValueArguments
        ?.get("message".asNameId())
        ?.value
        ?.cast<String>()
        ?.replaceTypeParameterRefs()
    return if (notFoundMessage == null && ambiguousMessage == null) null
    else CustomErrorMessages(notFoundMessage, ambiguousMessage)
}
