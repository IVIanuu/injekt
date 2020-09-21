package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektAttributes
import com.ivianuu.injekt.compiler.frontend.isReader
import com.ivianuu.injekt.compiler.getContextName
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

@Given(KtGenerationContext::class)
class ReaderContextGenerator : KtGenerator {

    private val fileManager = given<KtFileManager>()
    private val promisedReaderContextDescriptor = mutableSetOf<PromisedReaderContextDescriptor>()
    private val contexts = mutableMapOf<DeclarationDescriptor, ReaderContextDescriptor>()
    private val externalContexts = mutableMapOf<DeclarationDescriptor, ReaderContextDescriptor>()

    fun getContextForDescriptor(descriptor: DeclarationDescriptor): ReaderContextDescriptor? {
        return contexts[descriptor.original] ?: externalContexts[descriptor.original]
        ?: descriptor.findPackage()
            .getMemberScope()
            .getContributedClassifier(
                descriptor.getContextName(),
                NoLookupLocation.FROM_BACKEND
            )
            ?.let { it as ClassDescriptor }
            ?.let {
                ReaderContextDescriptor(
                    it.fqNameSafe,
                    it.declaredTypeParameters
                        .map { typeParameter ->
                            ReaderContextTypeParameter(
                                typeParameter.name,
                                typeParameter.upperBounds
                                    .map { KotlinTypeRef(it) }
                            )
                        }
                )
            }
            ?.also { externalContexts[descriptor.original] = it }
    }

    override fun generate(files: List<KtFile>) {
        val descriptorCollector = given<ReaderContextDescriptorCollector>(contexts)
        files.forEach { file -> file.accept(descriptorCollector) }
        val givensCollector =
            given<((DeclarationDescriptor) -> ReaderContextDescriptor?) -> ReaderContextGivensCollector>()
                .invoke(::getContextForDescriptor)
        files.forEach { file -> file.accept(givensCollector) }
        contexts.values.forEach { generateReaderContext(it) }
        promisedReaderContextDescriptor
            .map { promised ->
                ReaderContextDescriptor(promised.fqName, emptyList()).apply {
                    givenTypes +=
                        FqNameTypeRef(
                            getContextForDescriptor(promised.callee)!!.fqName,
                            promised.calleeTypeArguments
                        )
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
            emit("interface ${descriptor.fqName.shortName()}")
            if (descriptor.typeParameters.isNotEmpty()) {
                emit("<")
                descriptor.typeParameters.forEachIndexed { index, typeParameter ->
                    emit(typeParameter.name)
                    if (index != descriptor.typeParameters.lastIndex) emit(", ")
                }
                emit(">")
            }

            emitSpace()
            braced {
                descriptor.givenTypes.forEach { typeRef ->
                    val name = typeRef.uniqueTypeName()
                    val returnType = typeRef.render()
                    emitLine("fun $name(): $returnType")
                }
            }
        }

        fileManager.generateFile(
            packageFqName = descriptor.fqName.parent(),
            fileName = "${descriptor.fqName.shortName()}.kt",
            code = code,
            originatingDeclarations = emptyList<DeclarationDescriptor>(), // todo
            originatingFiles = emptyList()
        )
    }

}

data class PromisedReaderContextDescriptor(
    val fqName: FqName,
    val callee: DeclarationDescriptor,
    val calleeTypeArguments: List<TypeRef>
)

data class ReaderContextDescriptor(
    val fqName: FqName,
    val typeParameters: List<ReaderContextTypeParameter>
) {
    val givenTypes = mutableSetOf<TypeRef>()
}

data class ReaderContextTypeParameter(
    val name: Name,
    val upperBounds: List<TypeRef>
)

@Given
class ReaderContextDescriptorCollector(
    private val contexts: MutableMap<DeclarationDescriptor, ReaderContextDescriptor>
) : KtTreeVisitorVoid() {

    private val capturedTypeParameters = mutableListOf<TypeParameterDescriptor>()

    override fun visitReferenceExpression(expression: KtReferenceExpression) {
        super.visitReferenceExpression(expression)
        val resolvedCall = expression.getResolvedCall(given()) ?: return
        if (resolvedCall.resultingDescriptor.fqNameSafe.asString() == "com.ivianuu.injekt.runReader") {
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

    private inline fun <R> withCapturedTypeParametersIfNeeded(
        owner: DeclarationDescriptor,
        typeParameters: List<TypeParameterDescriptor>,
        block: () -> R
    ): R {
        val isReader = owner.isReader()
        if (isReader) capturedTypeParameters += typeParameters
        val result = block()
        if (isReader) capturedTypeParameters -= typeParameters
        return result
    }

    override fun visitClass(klass: KtClass) {
        val descriptor = klass.descriptor<ClassDescriptor>()
        withCapturedTypeParametersIfNeeded(descriptor, descriptor.declaredTypeParameters) {
            super.visitClass(klass)
        }
        generateContextIfNeeded(descriptor)
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        val descriptor = function.descriptor<FunctionDescriptor>()
        withCapturedTypeParametersIfNeeded(descriptor, descriptor.typeParameters) {
            super.visitNamedFunction(function)
        }
        generateContextIfNeeded(descriptor)
    }

    override fun visitProperty(property: KtProperty) {
        val descriptor = property.descriptor<VariableDescriptor>()
        withCapturedTypeParametersIfNeeded(descriptor, descriptor.typeParameters) {
            super.visitProperty(property)
        }
        generateContextIfNeeded(descriptor)
    }

    private fun generateContextIfNeeded(
        descriptor: DeclarationDescriptor,
        fromRunReaderCall: Boolean = false
    ) {
        if (descriptor in contexts) return
        if (!descriptor.isReader() && !fromRunReaderCall) return
        contexts[descriptor.original] = ReaderContextDescriptor(
            fqName = descriptor.findPackage().fqName.child(descriptor.getContextName()),
            typeParameters = (when (descriptor) {
                is ClassDescriptor -> descriptor.declaredTypeParameters
                is FunctionDescriptor -> descriptor.typeParameters
                is PropertyDescriptor -> descriptor.typeParameters
                else -> emptyList()
            } + capturedTypeParameters).map { typeParameter ->
                ReaderContextTypeParameter(
                    typeParameter.name,
                    typeParameter.upperBounds.map { KotlinTypeRef(it) }
                )
            }
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

    override fun visitProperty(property: KtProperty) {
        val descriptor = property.descriptor<VariableDescriptor>()
        if (descriptor.isReader()) {
            withReaderScope(ReaderScope(contextProvider(descriptor)!!)) {
                super.visitProperty(property)
            }
        } else {
            super.visitProperty(property)
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

    override fun visitReferenceExpression(expression: KtReferenceExpression) {
        super.visitReferenceExpression(expression)
        val resolvedCall = expression.getResolvedCall(given())
            ?: return
        val resulting = resolvedCall.resultingDescriptor
            .let { if (it is ClassConstructorDescriptor) it.constructedClass else it }
        if (!resulting.isReader()) return
        val givenType = when {
            resulting.fqNameSafe.asString() == "com.ivianuu.injekt.given" -> {
                val arguments = resolvedCall.valueArguments.values
                    .singleOrNull()
                    ?.let { it as VarargValueArgument }
                    ?.arguments
                    ?.mapNotNull { it.getArgumentExpression()?.getType(given()) }
                    ?: emptyList()
                val rawType = resolvedCall.typeArguments.values.single()
                val realType = when {
                    arguments.isNotEmpty() -> moduleDescriptor.builtIns.getFunction(arguments.size)
                        .defaultType
                        .replace(
                            newArguments = arguments.map { it.asTypeProjection() } + rawType.asTypeProjection()
                        )
                    else -> rawType
                }
                KotlinTypeRef(realType)
            }
            resulting.fqNameSafe.asString() == "com.ivianuu.injekt.childContext" -> {
                val factoryFqName = given<InjektAttributes>()[InjektAttributes.ContextFactoryKey(
                    expression.containingKtFile.virtualFilePath, expression.startOffset
                )]!!
                FqNameTypeRef(factoryFqName, emptyList())
            }
            else -> {
                val calleeContext = contextProvider(resulting)!!
                FqNameTypeRef(
                    calleeContext.fqName,
                    resolvedCall.typeArguments.values.map { KotlinTypeRef(it) }
                )
            }
        }

        readerScope!!.recordGivenType(givenType)
    }

}
