package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.backend.asNameId
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Given
class RunReaderCallIndexingGenerator : KtGenerator {

    private val indexer = given<KtIndexer>()

    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            file.accept(
                object : KtTreeVisitorVoid() {
                    override fun visitReferenceExpression(expression: KtReferenceExpression) {
                        super.visitReferenceExpression(expression)
                        val resolvedCall = expression.getResolvedCall(given()) ?: return
                        if (resolvedCall.resultingDescriptor.fqNameSafe.asString() ==
                            "com.ivianuu.injekt.runReader"
                        ) {
                            generateIndexForRunReaderCall(resolvedCall)
                        }
                    }
                }
            )
        }
    }

    private fun generateIndexForRunReaderCall(
        call: ResolvedCall<*>
    ) {
        val callElement = call.call.callElement
        val file = callElement.containingKtFile

        val contextType = call.extensionReceiver!!.type
        val blockContextFqName = call.valueArguments.values.single()
            .arguments
            .single()
            .getArgumentExpression()!!
            .let { it as KtLambdaExpression }
            .functionLiteral
            .descriptor<FunctionDescriptor>()
            .let { given<ReaderContextGenerator>().getContextForDescriptor(it)!! }
            .fqName

        indexer.index(
            fqName = file.packageFqName.child(
                "runReaderCall${callElement.startOffset}".asNameId()
            ),
            type = "class",
            indexIsDeclaration = true,
            annotations = listOf(
                InjektFqNames.RunReaderCall to
                        "@RunReaderCall(calleeContext = ${contextType.render()}::class, blockContext = $blockContextFqName::class)"
            )
        )
    }

}
