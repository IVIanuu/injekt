package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.ApplicationContext
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.generator.ClassifierRef
import com.ivianuu.injekt.compiler.generator.ContextFactoryDescriptor
import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.util.slicedMap.BasicWritableSlice
import org.jetbrains.kotlin.util.slicedMap.RewritePolicy

object InjektWritableSlices {
    val IS_READER = BasicWritableSlice<DeclarationDescriptor, Boolean>(RewritePolicy.DO_NOTHING)
    val IS_RUN_READER_FUNCTION = BasicWritableSlice<Any, Boolean>(RewritePolicy.DO_NOTHING)
    val IS_TRANSFORMED_READER = BasicWritableSlice<Any, Boolean>(RewritePolicy.DO_NOTHING)
    val CONTEXT_TYPE_PARAMETERS_WITH_ORIGIN =
        BasicWritableSlice<String, Map<ClassifierRef, TypeParameterDescriptor>>(
            RewritePolicy.DO_NOTHING
        )
    val CONTEXT_FACTORY = BasicWritableSlice<String, ContextFactoryDescriptor>(
        RewritePolicy.DO_NOTHING
    )
}

typealias InjektTrace = BindingTrace

@Given(ApplicationContext::class)
fun givenInjektTrace(): InjektTrace = CliBindingTrace()
