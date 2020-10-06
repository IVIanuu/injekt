package com.ivianuu.injekt.compiler

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.visitor.KSTopDownVisitor
import com.ivianuu.injekt.Binding

@Binding(GeneratorComponent::class)
class IndexGenerator(
    private val codeGenerator: CodeGenerator,
    private val declarationStore: DeclarationStore,
    private val injektTypes: InjektTypes
) : Generator {

    override fun generate(files: List<KSFile>) {
        files.forEach { file ->
            val indices = mutableSetOf<Index>()
            file.accept(
                object : KSTopDownVisitor<Nothing?, Unit>() {
                    var inModuleLikeScope = false

                    override fun defaultHandler(node: KSNode, data: Nothing?) {
                    }

                    override fun visitClassDeclaration(
                        classDeclaration: KSClassDeclaration,
                        data: Nothing?
                    ) {
                        super.visitClassDeclaration(classDeclaration, data)
                        println("visit class ${classDeclaration.simpleName.asString()}")
                        val prevInModuleLikeScope = inModuleLikeScope
                        inModuleLikeScope = classDeclaration.hasAnnotation(injektTypes.module) ||
                                classDeclaration.hasAnnotation(injektTypes.component) ||
                                classDeclaration.hasAnnotation(injektTypes.childComponent) ||
                                classDeclaration.hasAnnotation(injektTypes.mergeComponent) ||
                                classDeclaration.hasAnnotation(injektTypes.mergeChildComponent)
                        super.visitClassDeclaration(classDeclaration, data)
                        inModuleLikeScope = prevInModuleLikeScope
                    }

                    override fun visitDeclaration(declaration: KSDeclaration, data: Nothing?) {
                        super.visitDeclaration(declaration, data)
                        if (((declaration is KSClassDeclaration ||
                                    (declaration is KSFunctionDeclaration && declaration.isConstructor) ||
                                    !inModuleLikeScope) &&
                                    (declaration.hasAnnotation(injektTypes.binding) ||
                                            declaration.hasAnnotation(injektTypes.mapEntries) ||
                                            declaration.hasAnnotation(injektTypes.setElements))) ||
                            declaration.hasAnnotation(injektTypes.mergeComponent) ||
                            declaration.hasAnnotation(injektTypes.mergeChildComponent) ||
                            declaration.hasAnnotation(injektTypes.mergeInto)) {
                            val index = Index(
                                declaration.qualifiedName!!.asString(),
                                when (declaration) {
                                    is KSClassDeclaration -> "class"
                                    is KSFunctionDeclaration -> "function"
                                    is KSPropertyDeclaration -> "property"
                                    else -> error("Unexpected declaration $declaration")
                                }
                            )
                            indices += index
                            declarationStore.addInternalIndex(index)
                        }
                    }
                },
                null
            )

            if (indices.isNotEmpty()) {
                val fileName = file.packageName.asString().split(".").joinToString("_") + "_${file.fileName}"
                codeGenerator.generateFile(
                    InjektTypes.IndexPackage,
                    fileName,
                    buildCodeString {
                        emitLine("package ${InjektTypes.IndexPackage}")
                        indices
                            .forEach { index ->
                                val indexName = index.fqName.split(".")
                                    .joinToString("_") + "__${index.type}"
                                emitLine("internal val $indexName = Unit")
                            }
                    }
                )
            }
        }
    }
}