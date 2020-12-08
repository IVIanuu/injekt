package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzer
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Binding class MergeAccessorGenerator(
    private val bindingContext: BindingContext,
    private val declarationStore: DeclarationStore,
    private val fileManager: FileManager,
    private val lazyTopDownAnalyzer: LazyTopDownAnalyzer,
) : Generator {

    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            file.accept(
                object : KtTreeVisitorVoid() {
                    override fun visitCallExpression(expression: KtCallExpression) {
                        super.visitCallExpression(expression)
                        if (expression.calleeExpression?.text == "get") {
                            val scope = expression.parents
                                .first {
                                    (it is KtNamedFunction ||
                                            it is KtProperty ||
                                            it is KtClass) &&
                                            (it !is KtProperty || !it.isLocal)
                                } as KtNamedDeclaration
                            lazyTopDownAnalyzer.analyzeDeclarations(
                                TopDownAnalysisMode.LocalDeclarations,
                                listOf(scope)
                            )
                            val resolvedCall = expression.getResolvedCall(bindingContext)!!
                            if (resolvedCall.resultingDescriptor.fqNameSafe ==
                                InjektFqNames.get
                            ) {
                                try {
                                    generateAccessor(resolvedCall, scope)
                                } catch (e: Exception) {
                                    error("Failed to create merge accessor $resolvedCall ${expression.text} ${scope.text} ${scope.javaClass}")
                                }
                            }
                        }
                    }
                }
            )
        }
    }

    private fun generateAccessor(call: ResolvedCall<*>, scope: KtNamedDeclaration) {
        val file = scope.containingKtFile
        val componentType = call.extensionReceiver!!.type.toTypeRef()
        val requestedType = call.typeArguments.values.single().toTypeRef()
        val accessorName = "${
            scope.fqName!!.pathSegments().joinToString("_")
        }_${call.call.callElement.startOffset}"
        fileManager.generateFile(
            packageFqName = file.packageFqName,
            fileName = "$accessorName.kt",
            originatingFile = file,
            code = buildCodeString {
                emitLine("package ${file.packageFqName}")
                emitLine("@${InjektFqNames.MergeInto}(${componentType.classifier.fqName}::class)")
                emit("interface $accessorName ")
                braced {
                    emitLine("val ${requestedType.uniqueTypeName()}: ${requestedType.render()}")
                }
            }
        )
        declarationStore.addInternalMergeAccessor(
            MergeAccessorDescriptor(
                componentType.classifier.fqName,
                ClassifierRef(file.packageFqName.child(accessorName.asNameId())).defaultType,
                listOf(
                    Callable(
                        file.packageFqName,
                        file.packageFqName.child(accessorName.asNameId())
                            .child(requestedType.uniqueTypeName()),
                        requestedType.uniqueTypeName(),
                        requestedType,
                        requestedType,
                        emptyList(),
                        emptyList(),
                        null,
                        false,
                        false,
                        false,
                        null,
                        false,
                        Callable.CallableKind.DEFAULT,
                        false,
                        Modality.ABSTRACT
                    )
                )
            )
        )
    }

}
