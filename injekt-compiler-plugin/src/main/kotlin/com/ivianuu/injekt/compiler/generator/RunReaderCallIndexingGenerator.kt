package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektTrace
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.checkers.isReader
import com.ivianuu.injekt.compiler.filePositionOf
import com.ivianuu.injekt.compiler.irtransform.asNameId
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Given
class RunReaderCallIndexingGenerator : Generator {

    private val declarationStore = given<DeclarationStore>()
    private val indexer = given<Indexer>()
    private val capturedTypeParameters = mutableListOf<TypeParameterDescriptor>()

    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            file.accept(
                object : KtTreeVisitorVoid() {
                    override fun visitClass(klass: KtClass) {
                        val descriptor = klass.descriptor<ClassDescriptor>()
                        withCapturedTypeParametersIfNeeded(
                            descriptor,
                            descriptor.declaredTypeParameters
                        ) {
                            super.visitClass(klass)
                        }
                    }

                    override fun visitNamedFunction(function: KtNamedFunction) {
                        val descriptor = function.descriptor<FunctionDescriptor>()
                        withCapturedTypeParametersIfNeeded(descriptor, descriptor.typeParameters) {
                            super.visitNamedFunction(function)
                        }
                    }

                    override fun visitProperty(property: KtProperty) {
                        val descriptor = property.descriptor<VariableDescriptor>()
                        withCapturedTypeParametersIfNeeded(descriptor, descriptor.typeParameters) {
                            super.visitProperty(property)
                        }
                    }

                    override fun visitReferenceExpression(expression: KtReferenceExpression) {
                        super.visitReferenceExpression(expression)
                        val resolvedCall = expression.getResolvedCall(given()) ?: return
                        if (resolvedCall.resultingDescriptor.fqNameSafe.asString() ==
                            "com.ivianuu.injekt.runReader" || resolvedCall.resultingDescriptor.fqNameSafe.asString() ==
                            "com.ivianuu.injekt.runChildReader"
                        ) {
                            generateIndexForRunReaderCall(resolvedCall)
                        }
                    }
                }
            )
        }
    }

    private inline fun <R> withCapturedTypeParametersIfNeeded(
        owner: DeclarationDescriptor,
        typeParameters: List<TypeParameterDescriptor>,
        block: () -> R
    ): R {
        val isReader = owner.isReader(given())
        if (isReader) capturedTypeParameters += typeParameters
        val result = block()
        if (isReader) capturedTypeParameters -= typeParameters
        return result
    }

    private fun generateIndexForRunReaderCall(call: ResolvedCall<*>) {
        val callElement = call.call.callElement
        val file = callElement.containingKtFile

        val contextType = call.extensionReceiver?.type?.toTypeRef() ?: run {
            given<InjektTrace>()[
                    InjektWritableSlices.CONTEXT_FACTORY,
                    filePositionOf(
                        file.virtualFilePath,
                        callElement.startOffset
                    )
            ]!!.contextType
        }
        val blockContextType = call.valueArguments
            .entries
            .single { it.key.name.asString() == "block" }
            .value
            .arguments
            .single()
            .getArgumentExpression()!!
            .let { it as KtLambdaExpression }
            .functionLiteral
            .descriptor<FunctionDescriptor>()
            .let { declarationStore.getReaderContextForDeclaration(it)!! }
            .type

        declarationStore
            .addInternalRunReaderContext(
                contextType.classifier.fqName,
                blockContextType.classifier.fqName
            )

        indexer.index(
            path = runReaderPathOf(contextType.classifier.fqName),
            fqName = file.packageFqName.child(
                "runReaderCall${callElement.startOffset}".asNameId()
            ),
            type = "class",
            indexIsDeclaration = true,
            annotations = listOf(
                InjektFqNames.RunReaderCall to
                        "@RunReaderCall(calleeContext = ${contextType.classifier.fqName}::class, blockContext = ${blockContextType.classifier.fqName}::class)"
            )
        )
    }

}
