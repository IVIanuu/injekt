package com.ivianuu.injekt.compiler

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.visitor.KSTopDownVisitor
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.compiler.componentimpl.ComponentImpl

@Binding
class ComponentGenerator(
    private val codeGenerator: CodeGenerator,
    private val declarationStore: DeclarationStore,
    private val componentImplFactory: (
        TypeRef,
        String,
        ComponentImpl?,
    ) -> ComponentImpl,
    private val injektTypes: InjektTypes
) : Generator {
    override fun generate(files: List<KSFile>) {
        var generateMergeComponents = false
        files.forEach { file ->
            file.accept(
                object : KSTopDownVisitor<Nothing?, Unit>() {
                    override fun defaultHandler(node: KSNode, data: Nothing?) {
                    }

                    override fun visitDeclaration(declaration: KSDeclaration, data: Nothing?) {
                        super.visitDeclaration(declaration, data)
                        generateMergeComponents = (generateMergeComponents ||
                                declaration.hasAnnotation(injektTypes.generateMergeComponents))
                        if (declaration is KSClassDeclaration &&
                            declaration.hasAnnotation(injektTypes.component)
                        ) {
                            generateComponent(declaration.asType().toTypeRef(injektTypes))
                        }
                    }
                },
                null
            )
        }
        if (generateMergeComponents) {
            declarationStore.mergeComponents
                .forEach { generateComponent(it) }
        }
    }

    private fun generateComponent(componentType: TypeRef) {
        val componentImplFqName = componentType.classifier.fqName.toComponentImplFqName()
        val componentImpl = componentImplFactory(
            componentType,
            componentImplFqName.shortName(),
            null
        )
        componentImpl.initialize()

        val code = buildCodeString {
            emitLine("package ${componentImplFqName.parent()}")
            with(componentImpl) { emit() }

            /*emit("fun ${componentType.classifier.fqName.shortName()}(")
            componentImpl.constructorParameters.forEachIndexed { index, param ->
                emit("${param.name}: ${param.type.render()}")
                if (index != componentImpl.constructorParameters.lastIndex) emit(", ")
            }
            emit("): ${componentType.render()} ")
            braced {
                emitLine("return ${componentImplFqName.shortName()}(")
                componentImpl.constructorParameters.forEachIndexed { index, param ->
                    emit("${param.name}")
                    if (index != componentImpl.constructorParameters.lastIndex) emit(", ")
                }
                emitLine(")")
            }*/
        }

        codeGenerator.generateFile(
            componentType.classifier.fqName.parent(),
            componentImplFqName.shortName(),
            code
        )
    }
}
