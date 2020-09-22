package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.checkers.hasAnnotatedAnnotations
import com.ivianuu.injekt.compiler.checkers.hasAnnotation
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

@Given
class GivenIndexingGenerator : KtGenerator {

    private val indexer = given<Indexer>()

    override fun generate(files: List<KtFile>) {
        files.forEach { file ->
            file.accept(
                object : KtTreeVisitorVoid() {
                    override fun visitClass(klass: KtClass) {
                        super.visitClass(klass)
                        val descriptor = klass.descriptor<ClassDescriptor>()
                        if ((descriptor.hasAnnotation(InjektFqNames.Given) ||
                                    descriptor.hasAnnotatedAnnotations(InjektFqNames.Effect)) ||
                            descriptor.constructors
                                .any {
                                    it.hasAnnotation(InjektFqNames.Given) ||
                                            it.hasAnnotatedAnnotations(InjektFqNames.Effect)
                                }
                        ) {
                            indexer.index(descriptor)
                        }
                    }

                    override fun visitNamedFunction(function: KtNamedFunction) {
                        super.visitNamedFunction(function)
                        val descriptor = function.descriptor<FunctionDescriptor>()
                        if (!descriptor.isInGivenSet() &&
                            (descriptor.hasAnnotation(InjektFqNames.Given) ||
                                    descriptor.hasAnnotation(InjektFqNames.GivenMapEntries) ||
                                    descriptor.hasAnnotation(InjektFqNames.GivenSetElements))
                        ) {
                            indexer.index(descriptor)
                        }
                    }

                    override fun visitProperty(property: KtProperty) {
                        super.visitProperty(property)
                        val descriptor = property.descriptor<VariableDescriptor>()
                        if (descriptor.hasAnnotation(InjektFqNames.Given) &&
                            !descriptor.isInGivenSet()
                        ) {
                            indexer.index(descriptor)
                        }
                    }
                }
            )
        }
    }

    private fun DeclarationDescriptor.isInGivenSet(): Boolean {
        var current: DeclarationDescriptor? = containingDeclaration

        while (current != null) {
            if (current.hasAnnotation(InjektFqNames.GivenSet)) return true
            current = current.containingDeclaration
        }

        return false
    }

}
