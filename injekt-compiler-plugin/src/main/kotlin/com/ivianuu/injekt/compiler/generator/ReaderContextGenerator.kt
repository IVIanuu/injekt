package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektAttributes
import com.ivianuu.injekt.compiler.frontend.isReader
import com.ivianuu.injekt.compiler.getContextName
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Given(KtGenerationContext::class)
class ReaderContextGenerator : KtGenerator {

    private val fileManager = given<KtFileManager>()
    private val promisedReaderContextDescriptor = mutableSetOf<PromisedReaderContextDescriptor>()
    private val contexts = mutableMapOf<DeclarationDescriptor, ReaderContextDescriptor>()

    fun getContextForDescriptor(descriptor: DeclarationDescriptor) = contexts[descriptor]

    override fun generate(files: List<KtFile>) {
        val descriptorCollector = given<ReaderContextDescriptorCollector>(contexts)
        files.forEach { file -> file.accept(descriptorCollector) }
        val givensCollector = given<ReaderContextGivensCollector>(
            { descriptor: DeclarationDescriptor -> contexts[descriptor] }
        )
        files.forEach { file -> file.accept(givensCollector) }
        contexts.values.forEach { generateReaderContext(it) }
        promisedReaderContextDescriptor
            .map { promised ->
                ReaderContextDescriptor(promised.fqName).apply {
                    givenTypes +=
                        FqNameTypeRef(contexts[promised.callee]!!.fqName)
                }
            }
            .forEach { generateReaderContext(it) }
    }

    fun addPromisedReaderContextDescriptor(
        descriptor: PromisedReaderContextDescriptor
    ) {
        promisedReaderContextDescriptor += descriptor
    }

    private fun generateReaderContext(descriptor: ReaderContextDescriptor) {
        val code = buildCodeString {
            emitLine("// injekt-generated")
            emitLine("package ${descriptor.fqName.parent()}")
            emitLine("import com.ivianuu.injekt.internal.ContextMarker")
            emitLine("@ContextMarker")
            emit("interface ${descriptor.fqName.shortName()} ")
            braced {
                descriptor.givenTypes.forEach { typeRef ->
                    val (name, returnType) = when (typeRef) {
                        is KotlinTypeRef -> typeRef.kotlinType.uniqueTypeName() to typeRef.kotlinType.render()
                        is FqNameTypeRef -> typeRef.fqName.pathSegments()
                            .joinToString("_") to typeRef.fqName
                    }
                    emitLine("fun $name(): $returnType")
                }
            }
        }

        fileManager.generateFile(
            packageFqName = descriptor.fqName.parent(),
            fileName = "${descriptor.fqName.shortName()}.kt",
            code = code,
            originatingDeclarations = emptyList<DeclarationDescriptor>() // todo
        )
    }

}

data class PromisedReaderContextDescriptor(
    val fqName: FqName,
    val callee: DeclarationDescriptor
)

data class ReaderContextDescriptor(val fqName: FqName) {
    val givenTypes = mutableSetOf<TypeRef>()
}

@Given
class ReaderContextDescriptorCollector(
    private val contexts: MutableMap<DeclarationDescriptor, ReaderContextDescriptor>
) : KtTreeVisitorVoid() {

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val resolvedCall = expression.getResolvedCall(given())
        if (resolvedCall!!.resultingDescriptor.fqNameSafe.asString() == "com.ivianuu.injekt.runReader") {
            generateContextIfNeeded(
                resolvedCall.valueArguments.values.single()
                    .arguments
                    .single()
                    .getArgumentExpression()!!
                    .let { it as KtLambdaExpression }
                    .functionLiteral
                    .descriptor(),
                fromRunReaderCall = true
            )
        }
    }

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        generateContextIfNeeded(klass.descriptor())
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        generateContextIfNeeded(function.descriptor())
    }

    private fun generateContextIfNeeded(
        descriptor: DeclarationDescriptor,
        fromRunReaderCall: Boolean = false
    ) {
        if (descriptor in contexts) return
        if (!descriptor.isReader() && !fromRunReaderCall) return
        contexts[descriptor] = ReaderContextDescriptor(
            fqName = descriptor.findPackage().fqName.child(descriptor.getContextName())
        )
    }

}

@Given
class ReaderContextGivensCollector(
    private val contextProvider: (DeclarationDescriptor) -> ReaderContextDescriptor?
) : KtTreeVisitorVoid() {

    inner class ReaderScope(
        val contextDescriptor: ReaderContextDescriptor
    ) {
        fun recordGivenType(type: TypeRef) {
            contextDescriptor.givenTypes += type
        }
    }

    private var readerScope: ReaderScope? = null
    private inline fun <R> withReaderScope(
        scope: ReaderScope,
        block: () -> R
    ): R {
        val prev = readerScope
        readerScope = scope
        val result = block()
        readerScope = prev
        return result
    }

    override fun visitClass(klass: KtClass) {
        val descriptor = klass.descriptor<ClassDescriptor>()
        if (descriptor.isReader()) {
            withReaderScope(ReaderScope(contextProvider(descriptor)!!)) {
                super.visitClass(klass)
            }
        } else {
            super.visitClass(klass)
        }
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        val descriptor = function.descriptor<FunctionDescriptor>()
        if (descriptor.isReader()) {
            withReaderScope(ReaderScope(contextProvider(descriptor)!!)) {
                super.visitNamedFunction(function)
            }
        } else {
            super.visitNamedFunction(function)
        }
    }

    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        val descriptor = lambdaExpression.functionLiteral.descriptor<FunctionDescriptor>()
        val contextDescriptor = contextProvider(descriptor)
        if (contextDescriptor != null) {
            withReaderScope(ReaderScope(contextDescriptor)) {
                super.visitLambdaExpression(lambdaExpression)
            }
        } else {
            super.visitLambdaExpression(lambdaExpression)
        }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val resolvedCall = expression.getResolvedCall(given())
            ?: error("No resolved call found for ${expression.text}")
        val resulting = resolvedCall.resultingDescriptor
        if (!resulting.isReader()) return
        if (resulting.fqNameSafe.asString() == "com.ivianuu.injekt.given") {
            readerScope!!.recordGivenType(KotlinTypeRef(resolvedCall.typeArguments.values.single()))
        } else if (resulting.fqNameSafe.asString() == "com.ivianuu.injekt.childContext") {
            val factoryFqName = given<InjektAttributes>()[InjektAttributes.ContextFactoryKey(
                expression.containingKtFile.virtualFilePath, expression.startOffset
            )]!!
            readerScope!!.recordGivenType(FqNameTypeRef(factoryFqName))
        }
    }

}
