package com.ivianuu.ast.tree.generator.context

import com.ivianuu.ast.tree.generator.model.Builder
import com.ivianuu.ast.tree.generator.model.Element
import com.ivianuu.ast.tree.generator.model.FieldWithDefault
import com.ivianuu.ast.tree.generator.model.Implementation
import com.ivianuu.ast.tree.generator.model.Importable
import com.ivianuu.ast.tree.generator.model.IntermediateBuilder
import com.ivianuu.ast.tree.generator.model.LeafBuilder
import com.ivianuu.ast.tree.generator.noReceiverExpressionType
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class AbstractBuilderConfigurator<T : AbstractAstTreeBuilder>(val astTreeBuilder: T) {
    abstract class BuilderConfigurationContext {
        abstract val builder: Builder

        private fun getField(name: String): FieldWithDefault {
            return builder[name]
        }

        fun useTypes(vararg types: Importable) {
            types.forEach { builder.usedTypes += it }
        }

        fun defaultNoReceivers(notNullExplicitReceiver: Boolean = false) {
            if (!notNullExplicitReceiver) {
                defaultNull("explicitReceiver")
            }
            default("dispatchReceiver", "AstNoReceiverExpression")
            default("extensionReceiver", "AstNoReceiverExpression")
            useTypes(noReceiverExpressionType)
        }

        fun default(field: String, value: String) {
            default(field) {
                this.value = value
            }
        }

        fun defaultTrue(field: String) {
            default(field) {
                value = "true"
            }
        }

        fun defaultFalse(vararg fields: String) {
            for (field in fields) {
                default(field) {
                    value = "false"
                }
            }
        }

        fun defaultNull(vararg fields: String) {
            for (field in fields) {
                default(field) {
                    value = "null"
                }
                require(getField(field).nullable) {
                    "$field is not nullable field"
                }
            }
        }

        fun default(field: String, init: DefaultValueContext.() -> Unit) {
            DefaultValueContext(getField(field)).apply(init).applyConfiguration()
        }

        inner class DefaultValueContext(private val field: FieldWithDefault) {
            var value: String? = null

            fun applyConfiguration() {
                if (value != null) field.defaultValueInBuilder = value
            }
        }
    }


    class IntermediateBuilderConfigurationContext(override val builder: IntermediateBuilder) :
        BuilderConfigurationContext() {
        inner class Fields {
            // fields from <element>
            infix fun from(element: Element): ExceptConfigurator {
                builder.fields += element.allFields.map {
                    FieldWithDefault(it.copy())
                }
                builder.packageName = "${element.packageName}.builder"
                builder.materializedElement = element
                return ExceptConfigurator()
            }

            inner class Helper(val fieldName: String) {
                infix fun from(element: Element) {
                    val field = element[fieldName]
                        ?: throw IllegalArgumentException("Element $element doesn't have field $fieldName")
                    builder.fields += FieldWithDefault(field)
                }
            }

            // fields has <field> from <element>
            infix fun has(name: String): Helper = Helper(name)
        }

        inner class ExceptConfigurator {
            infix fun without(name: String) {
                without(listOf(name))
            }

            infix fun without(names: List<String>) {
                builder.fields.removeAll { it.name in names }
            }
        }

        val fields = Fields()
        val parents: MutableList<IntermediateBuilder> get() = builder.parents

        var materializedElement: Element
            get() = throw IllegalArgumentException()
            set(value) {
                builder.materializedElement = value
            }

    }

    inner class IntermediateBuilderDelegateProvider(
        private val name: String?,
        private val block: IntermediateBuilderConfigurationContext.() -> Unit
    ) {
        lateinit var builder: IntermediateBuilder

        operator fun provideDelegate(
            thisRef: Nothing?,
            prop: KProperty<*>
        ): ReadOnlyProperty<Nothing?, IntermediateBuilder> {
            val name = name ?: "Ast${prop.name.capitalize()}"
            builder = IntermediateBuilder(name).apply {
                astTreeBuilder.intermediateBuilders += this
                IntermediateBuilderConfigurationContext(this).block()
            }
            return DummyDelegate(builder)
        }

        private inner class DummyDelegate(val builder: IntermediateBuilder) :
            ReadOnlyProperty<Nothing?, IntermediateBuilder> {
            override fun getValue(thisRef: Nothing?, property: KProperty<*>): IntermediateBuilder {
                return builder
            }
        }
    }

    inner class LeafBuilderConfigurationContext(override val builder: LeafBuilder) :
        BuilderConfigurationContext() {
        val parents: MutableList<IntermediateBuilder> get() = builder.parents

        fun openBuilder() {
            builder.isOpen = true
        }

        fun withCopy() {
            builder.wantsCopy = true
        }
    }

    fun builder(
        name: String? = null,
        block: IntermediateBuilderConfigurationContext.() -> Unit
    ): IntermediateBuilderDelegateProvider {
        return IntermediateBuilderDelegateProvider(name, block)
    }

    fun builder(
        element: Element,
        type: String? = null,
        init: LeafBuilderConfigurationContext.() -> Unit
    ) {
        val implementation = element.extractImplementation(type)
        val builder = implementation.builder
        requireNotNull(builder)
        LeafBuilderConfigurationContext(builder).apply(init)
    }

    private fun Element.extractImplementation(type: String?): Implementation {
        return if (type == null) {
            allImplementations.filter { it.kind?.hasLeafBuilder == true }.singleOrNull()
                ?: this@AbstractBuilderConfigurator.run {
                    val message = buildString {
                        appendLine("${this@extractImplementation} has multiple implementations:")
                        for (implementation in allImplementations) {
                            appendLine("  - ${implementation.type}")
                        }
                        appendLine("Please specify implementation is needed")
                    }
                    throw IllegalArgumentException(message)
                }
        } else {
            allImplementations.firstOrNull { it.type == type }
                ?: this@AbstractBuilderConfigurator.run {
                    val message = buildString {
                        appendLine("${this@extractImplementation} has not implementation $type. Existing implementations:")
                        for (implementation in allImplementations) {
                            appendLine("  - ${implementation.type}")
                        }
                        appendLine("Please specify implementation is needed")
                    }
                    throw IllegalArgumentException(message)
                }
        }
    }

    fun noBuilder(element: Element, type: String? = null) {
        val implementation = element.extractImplementation(type)
        implementation.builder = null
    }
}