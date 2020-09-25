package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektTrace
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.checkers.isReader
import com.ivianuu.injekt.compiler.filePositionOf
import com.ivianuu.injekt.compiler.getContextName
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
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
import java.io.File

@Given(GenerationContext::class)
class ReaderContextGenerator : Generator {

    private val declarationStore = given<DeclarationStore>()
    private val fileManager = given<KtFileManager>()
    private val promisedReaderContextDescriptor = mutableSetOf<PromisedReaderContextDescriptor>()

    override fun generate(files: List<KtFile>) {
        // collect contexts
        val descriptorCollector = given<ReaderContextDescriptorCollector>()
        files.forEach { file -> file.accept(descriptorCollector) }

        // collect given types for contexts
        val givensCollector =
            given<((DeclarationDescriptor) -> ReaderContextDescriptor?) -> ReaderContextGivensCollector>()
                .invoke { declarationStore.getReaderContextForDeclaration(it) }
        files.forEach { file -> file.accept(givensCollector) }

        // generate contexts
        declarationStore.internalReaderContextsByFqName.values.forEach { generateReaderContext(it) }
        promisedReaderContextDescriptor
            .map { promised ->
                ReaderContextDescriptor(
                    promised.type,
                    promised.origin,
                    promised.originatingFiles
                ).apply {
                    declarationStore.addInternalReaderContext(this)
                    givenTypes +=
                        SimpleTypeRef(
                            classifier = declarationStore.getReaderContextForDeclaration(promised.callee)!!
                                .type.classifier,
                            isContext = true,
                            typeArguments = promised.calleeTypeArguments
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
            emitLine("package ${descriptor.type.classifier.fqName.parent()}")
            emitLine("import com.ivianuu.injekt.internal.ContextMarker")
            emitLine("import com.ivianuu.injekt.internal.Origin")
            emitLine("@Origin(\"${descriptor.origin}\")")
            emitLine("@ContextMarker")
            emit("interface ${descriptor.type.classifier.fqName.shortName()}")
            if (descriptor.type.classifier.typeParameters.isNotEmpty()) {
                emit("<")
                descriptor.type.classifier.typeParameters.forEachIndexed { index, typeParameter ->
                    emit(typeParameter.fqName.shortName())
                    if (index != descriptor.type.classifier.typeParameters.lastIndex) emit(", ")
                }
                emit(">")
            }

            emitSpace()

            if (descriptor.type.classifier.typeParameters.isNotEmpty()) {
                emit("where ")
                val typeParametersWithUpperBounds = descriptor.type.classifier.typeParameters
                    .flatMap { typeParameter ->
                        typeParameter.upperBounds.map { typeParameter to it }
                    }

                typeParametersWithUpperBounds.forEachIndexed { index, (typeParameter, upperBound) ->
                    emit("${typeParameter.fqName.shortName()} : ${upperBound.render()}")
                    if (index != typeParametersWithUpperBounds.lastIndex) emit(", ")
                }

                emitSpace()
            }

            braced {
                descriptor.givenTypes.forEach { typeRef ->
                    val name = typeRef.uniqueTypeName()
                    val returnType = typeRef.render()
                    emitLine("fun $name(): $returnType")
                }
            }
        }

        fileManager.generateFile(
            packageFqName = descriptor.type.classifier.fqName.parent(),
            fileName = "${descriptor.type.classifier.fqName.shortName()}.kt",
            code = code,
            originatingFiles = descriptor.originatingFiles
        )
    }

}

data class PromisedReaderContextDescriptor(
    val type: TypeRef,
    val callee: DeclarationDescriptor,
    val calleeTypeArguments: List<TypeRef>,
    val origin: FqName,
    val originatingFiles: List<File>
)

data class ReaderContextDescriptor(
    val type: TypeRef,
    val origin: FqName,
    val originatingFiles: List<File>,
    val givenTypes: MutableSet<TypeRef> = mutableSetOf()
)

@Given
class ReaderContextDescriptorCollector : KtTreeVisitorVoid() {

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
        val isReader = owner.isReader(given())
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
        declaration: DeclarationDescriptor,
        fromRunReaderCall: Boolean = false
    ) {
        val declarationStore = given<DeclarationStore>()
        if (!declaration.isReader(given()) && !fromRunReaderCall) return
        if (declarationStore
                .getReaderContextForDeclaration(declaration) != null
        ) return
        val contextName = declaration.getContextName()
        val contextFqName = declaration.findPackage().fqName.child(contextName)

        val typeParametersWithOrigin = (when (declaration) {
            is ClassifierDescriptorWithTypeParameters -> declaration.declaredTypeParameters
            is CallableDescriptor -> declaration.typeParameters
            else -> emptyList()
        } + capturedTypeParameters)
            .map { typeParameter ->
                ClassifierRef(
                    contextFqName.child(typeParameter.name),
                    upperBounds = typeParameter.upperBounds.map { it.toTypeRef() },
                    isTypeParameter = true
                ) to typeParameter
            }

        given<InjektTrace>().record(
            InjektWritableSlices.CONTEXT_TYPE_PARAMETERS_WITH_ORIGIN,
            declaration.uniqueKey(),
            typeParametersWithOrigin.toMap()
        )

        val typeParameters = typeParametersWithOrigin.map { it.first }

        val context = ReaderContextDescriptor(
            type = SimpleTypeRef(
                classifier = ClassifierRef(
                    fqName = contextFqName,
                    typeParameters = typeParameters
                ),
                typeArguments = typeParameters.map { it.defaultType },
                isContext = true
            ),
            origin = declaration.fqNameSafe,
            originatingFiles = listOf(File((declaration.findPsi()!!.containingFile as KtFile).virtualFilePath))
        )

        declarationStore.addInternalReaderContext(context)
    }

}

@Given
class ReaderContextGivensCollector(
    private val contextProvider: (DeclarationDescriptor) -> ReaderContextDescriptor?
) : KtTreeVisitorVoid() {

    inner class ReaderScope(
        val declaration: DeclarationDescriptor,
        val contextDescriptor: ReaderContextDescriptor
    ) {
        private val substitutionMap = run {
            val typeParametersWithOrigin = given<InjektTrace>()[
                    InjektWritableSlices.CONTEXT_TYPE_PARAMETERS_WITH_ORIGIN,
                    declaration.uniqueKey()
            ]!!
            contextDescriptor.type.classifier.typeParameters
                .map {
                    typeParametersWithOrigin[it]!!
                        .toClassifierRef() to it.defaultType
                }
                .toMap()
        }

        fun recordGivenType(type: TypeRef) {
            contextDescriptor.givenTypes += type
                .substitute(substitutionMap)
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
        if (descriptor.isReader(given())) {
            withReaderScope(ReaderScope(descriptor, contextProvider(descriptor)!!)) {
                super.visitClass(klass)
            }
        } else {
            super.visitClass(klass)
        }
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        val descriptor = function.descriptor<FunctionDescriptor>()
        if (descriptor.isReader(given())) {
            withReaderScope(ReaderScope(descriptor, contextProvider(descriptor)!!)) {
                super.visitNamedFunction(function)
            }
        } else {
            super.visitNamedFunction(function)
        }
    }

    override fun visitProperty(property: KtProperty) {
        val descriptor = property.descriptor<VariableDescriptor>()
        if (descriptor.isReader(given())) {
            withReaderScope(ReaderScope(descriptor, contextProvider(descriptor)!!)) {
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
            withReaderScope(ReaderScope(descriptor, contextDescriptor)) {
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
        if (!resulting.isReader(given())) return
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
                realType.toTypeRef()
            }
            resulting.fqNameSafe.asString() == "com.ivianuu.injekt.childContext" -> {
                val factoryDescriptor = given<InjektTrace>()[
                        InjektWritableSlices.CONTEXT_FACTORY,
                        filePositionOf(
                            expression.containingKtFile.virtualFilePath,
                            expression.startOffset
                        )
                ]!!
                factoryDescriptor.factoryType
                    .substitute(
                        factoryDescriptor.factoryType.classifier.typeParameters
                            .zip(readerScope!!.contextDescriptor.type.classifier.typeParameters
                                .map { it.defaultType })
                            .toMap()
                    )
            }
            else -> {
                val calleeContext = contextProvider(resulting)
                    ?: error("Null for $resulting")
                calleeContext.type.typeWith(
                    resolvedCall.typeArguments.values.map { it.toTypeRef() }
                )
            }
        }

        readerScope!!.recordGivenType(givenType)
    }

}
