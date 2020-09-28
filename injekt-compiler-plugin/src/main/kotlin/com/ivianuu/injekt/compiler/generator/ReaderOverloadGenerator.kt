package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektTrace
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.checkers.isReader
import com.ivianuu.injekt.compiler.filePositionOf
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import java.io.File

@Given(GenerationContext::class)
class ReaderOverloadGenerator : Generator {

    private val declarationStore = given<DeclarationStore>()
    private val fileManager = given<KtFileManager>()
    private val promisedInfos = mutableSetOf<PromisedReaderInfo>()

    override fun generate(files: List<KtFile>) {
        // collect infos
        val infoCollector = given<ReaderInfoCollector>()
        files.forEach { file -> file.accept(infoCollector) }

        // collect given types for infos
        val givensCollector =
            given<((DeclarationDescriptor) -> ReaderInfo?) -> ReaderInfoGivensCollector>()
                .invoke { declarationStore.getReaderInfoForDeclaration(it) }
        files.forEach { file -> file.accept(givensCollector) }

        val infosByFile = declarationStore.internalInfosByFqName.values
            .groupBy { it.originatingFile }

        // generate overloads
        infosByFile.forEach { (file, infos) ->
            generateOverloads(file, infos)
        }

        /*promisedInfos
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
            .forEach { generateReaderContext(it) }*/
    }

    fun addPromisedReaderContextDescriptor(
        descriptor: PromisedReaderInfo
    ) {
        promisedInfos += descriptor
    }

    private fun generateOverloads(
        file: KtFile,
        infos: List<ReaderInfo>
    ) {
        val fileName = "${file.name.removeSuffix(".kt")}ReaderOverloads.kt"
        val code = buildCodeString {
            emitLine("// injekt-generated")
            emitLine("package ${file.packageFqName}")
            emitLine("import com.ivianuu.injekt.internal.ReaderOverload")

            infos.forEach { info ->
                emit("fun ")
            }

            /*
            emit("interface ${info.type.classifier.fqName.shortName()}")
            if (info.type.classifier.typeParameters.isNotEmpty()) {
                emit("<")
                info.type.classifier.typeParameters.forEachIndexed { index, typeParameter ->
                    emit(typeParameter.fqName.shortName())
                    if (index != info.type.classifier.typeParameters.lastIndex) emit(", ")
                }
                emit(">")
            }

            emitSpace()

            if (info.type.classifier.typeParameters.isNotEmpty()) {
                emit("where ")
                val typeParametersWithUpperBounds = info.type.classifier.typeParameters
                    .flatMap { typeParameter ->
                        typeParameter.superTypes.map { typeParameter to it }
                    }

                typeParametersWithUpperBounds.forEachIndexed { index, (typeParameter, upperBound) ->
                    emit("${typeParameter.fqName.shortName()} : ${upperBound.render()}")
                    if (index != typeParametersWithUpperBounds.lastIndex) emit(", ")
                }

                emitSpace()
            }

            braced {
                info.givenTypes.forEach { typeRef ->
                    val name = typeRef.uniqueTypeName()
                    val returnType = typeRef.render()
                    emitLine("fun $name(): $returnType")
                }
            }*/
        }

        fileManager.generateFile(
            packageFqName = info.type.classifier.fqName.parent(),
            fileName = "${info.type.classifier.fqName.shortName()}.kt",
            code = code,
            originatingFiles = info.originatingFiles
        )
    }

}

class PromisedReaderInfo

data class ReaderInfo(
    val callable: CallableRef,
    val originatingFile: File,
    val givenTypes: MutableSet<TypeRef> = mutableSetOf()
) {

}

fun ReaderInfo.getFullSignatureOverload() {

}

fun ReaderInfo.getContextOverload() {

}

@Given
class ReaderInfoCollector : KtTreeVisitorVoid() {

    private val declarationStore = given<DeclarationStore>()
    private val injektTrace = given<InjektTrace>()

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        generateInfoIfNeeded(klass.descriptor())
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)
        generateInfoIfNeeded(function.descriptor())
    }

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)
        generateInfoIfNeeded(property.descriptor())
    }

    private fun generateInfoIfNeeded(declaration: DeclarationDescriptor) {
        if (!declaration.isReader(given())) return
        if (declarationStore
                .getReaderInfoForDeclaration(declaration) != null
        ) return
        val info = ReaderInfo(
            callable = when (declaration) {
                is ClassDescriptor -> declaration.getReaderConstructor(given())!!.toCallableRef()
                is FunctionDescriptor -> declaration.toCallableRef()
                is PropertyDescriptor -> declaration.getter!!.toCallableRef()
                else -> error("Unexpected declaration $declaration")
            },
            originatingFile = File((declaration.findPsi()!!.containingFile as KtFile).virtualFilePath)
        )

        declarationStore.addInternalReaderInfo(info)
    }

}

@Given
class ReaderInfoGivensCollector(
    private val infoProvider: (DeclarationDescriptor) -> ReaderInfo?
) : KtTreeVisitorVoid() {

    private val injektTrace = given<InjektTrace>()

    inner class ReaderScope(
        val declaration: DeclarationDescriptor,
        val info: ReaderInfo
    ) {
        fun recordGivenType(type: TypeRef) {
            info.givenTypes += type
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
            withReaderScope(ReaderScope(descriptor, infoProvider(descriptor)!!)) {
                super.visitClass(klass)
            }
        } else {
            super.visitClass(klass)
        }
    }

    override fun visitNamedFunction(function: KtNamedFunction) {
        val descriptor = function.descriptor<FunctionDescriptor>()
        if (descriptor.isReader(given())) {
            withReaderScope(ReaderScope(descriptor, infoProvider(descriptor)!!)) {
                super.visitNamedFunction(function)
            }
        } else {
            super.visitNamedFunction(function)
        }
    }

    override fun visitProperty(property: KtProperty) {
        val descriptor = property.descriptor<VariableDescriptor>()
        if (descriptor.isReader(given())) {
            withReaderScope(ReaderScope(descriptor, infoProvider(descriptor)!!)) {
                super.visitProperty(property)
            }
        } else {
            super.visitProperty(property)
        }
    }

    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
        val descriptor = lambdaExpression.functionLiteral.descriptor<FunctionDescriptor>()
        val contextDescriptor = infoProvider(descriptor)
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
        when {
            resulting.fqNameSafe.asString() == "com.ivianuu.injekt.given" -> {
                readerScope!!.recordGivenType(
                    resolvedCall.typeArguments.values.single().toTypeRef()
                )
            }
            resulting.fqNameSafe.asString() == "com.ivianuu.injekt.childContext" -> {
                val factoryDescriptor = injektTrace[
                        InjektWritableSlices.CONTEXT_FACTORY,
                        filePositionOf(
                            expression.containingKtFile.virtualFilePath,
                            expression.startOffset
                        )
                ]!!
                readerScope!!.recordGivenType(factoryDescriptor.factoryType)
            }
            else -> {
                val calleeInfo = infoProvider(resulting)
                    ?: error("Null for $resulting")

                calleeInfo.givenTypes
                    .map {
                        it.substitute(
                            calleeInfo.callable.typeParameters
                                .zip(resolvedCall.typeArguments.values
                                    .map { it.toTypeRef() })
                                .toMap()
                        )
                    }
                    .forEach { readerScope!!.recordGivenType(it) }
            }
        }
    }

}
