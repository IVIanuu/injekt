/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt

import java.util.ServiceLoader

/**
 * Create a [Component] configured by [block]
 *
 * @see Component
 */
inline fun Component(block: ComponentBuilder.() -> Unit = {}): Component =
    ComponentBuilder().apply(block).build()

/**
 * Builder for a [Component]
 *
 * @see Component
 */
class ComponentBuilder {

    private val _scopes = mutableListOf<Scope>()
    val scopes: List<Scope> get() = _scopes

    private val _dependencies = mutableListOf<Component>()
    val dependencies: List<Component> get() = _dependencies

    private val _bindings = mutableMapOf<Key<*>, Binding<*>>()
    val bindings: Map<Key<*>, Binding<*>> get() = _bindings

    private val _justInTimeBindingFactories = mutableListOf<JustInTimeBindingFactory>()
    val justInTimeBindingFactories: List<JustInTimeBindingFactory> get() = _justInTimeBindingFactories

    private val onPreBuildBlocks = mutableListOf<() -> Boolean>()
    private val onBuildBlocks = mutableListOf<(Component) -> Unit>()
    private val onBindingAddedBlocks = mutableListOf<(Binding<*>) -> Unit>()
    private val onScopeAddedBlocks = mutableListOf<(Scope) -> Unit>()
    private val onDependencyAddedBlocks = mutableListOf<(Component) -> Unit>()

    init {
        ComponentBuilderContributors.implementations
            .filter { it.invokeOnInit }
            .forEach { it.apply(this) }
    }

    /**
     * Adds the [scopes] this allows generated [Binding]s
     * to be associated with components.
     *
     * @see ScopeMarker
     */
    fun scopes(vararg scopes: Scope) {
        scopes.fastForEach { scope ->
            check(scope !in this._scopes) { "Duplicated scope $scope" }
            this._scopes += scope
            onScopeAddedBlocks.toList().forEach { it(scope) }
        }
    }

    /**
     * Adds the [dependencies] to the component if this component cannot resolve a instance
     * it will ask it's dependencies
     */
    fun dependencies(vararg dependencies: Component) {
        dependencies.fastForEach { dependency ->
            check(dependency !in this._dependencies) { "Duplicated dependency $dependency" }
            this._dependencies += dependency
            onDependencyAddedBlocks.toList().forEach { it(dependency) }
        }
    }

    /**
     * Adds the [factories]
     */
    fun justInTimeBindingFactories(vararg factories: JustInTimeBindingFactory) {
        _justInTimeBindingFactories += factories
    }

    inline fun <reified T> bind(
        qualifier: Qualifier = Qualifier.None,
        behavior: Behavior = Behavior.None,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
        noinline provider: BindingProvider<T>
    ) = bind(
        key = keyOf(qualifier = qualifier),
        behavior = behavior,
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )

    fun <T> bind(
        key: Key<T>,
        behavior: Behavior = Behavior.None,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
        provider: BindingProvider<T>
    ) {
        bind(
            Binding(
                key = key,
                behavior = behavior,
                duplicateStrategy = duplicateStrategy,
                provider = provider
            )
        )
    }

    /**
     * Adds the [binding] which can be retrieved by [Binding.key]
     *
     * @see factory
     * @see single
     */
    fun <T> bind(binding: Binding<T>) {
        if (binding.duplicateStrategy.check(
                existsPredicate = { binding.key in _bindings },
                errorMessage = { "Already declared binding for ${binding.key}" }
            )
        ) {
            _bindings[binding.key] = binding
            onBindingAddedBlocks.toList().forEach { it(binding) }
        }
    }

    /**
     * Invokes the [block] for every binding
     */
    fun onBindingAdded(block: (Binding<*>) -> Unit) {
        onBindingAddedBlocks += block
    }

    fun onScopeAdded(block: (Scope) -> Unit) {
        onScopeAddedBlocks += block
    }

    fun onDependencyAdded(block: (Component) -> Unit) {
        onDependencyAddedBlocks += block
    }

    /**
     * Invokes the [block] before building the [Component] until it returns false
     */
    fun onPreBuild(block: () -> Boolean) {
        onPreBuildBlocks += block
    }

    fun onBuild(block: (Component) -> Unit) {
        onBuildBlocks += block
    }

    /**
     * Create a new [Component] instance.
     */
    fun build(): Component {
        ComponentBuilderContributors.implementations
            .filter { !it.invokeOnInit }
            .forEach { it.apply(this) }

        runPreBuildBlocks()

        checkScopes()

        val dependencyBindings = mutableMapOf<Key<*>, Binding<*>>()

        _dependencies.fastForEach { dependency ->
            val bindings = dependency.getAllBindings()
            for ((key, binding) in bindings) {
                if (binding.duplicateStrategy.check(
                        existsPredicate = { key in dependencyBindings },
                        errorMessage = { "Already declared binding for $key" }
                    )
                ) {
                    dependencyBindings[key] = binding
                }
            }
        }

        val finalBindings = mutableMapOf<Key<*>, Binding<*>>()

        _bindings.forEach { (key, binding) ->
            if (binding.duplicateStrategy.check(
                    existsPredicate = { key in dependencyBindings },
                    errorMessage = { "Already declared binding for $key" })
            ) {
                finalBindings[key] = binding
            }
        }

        val component = Component(
            scopes = _scopes,
            dependencies = _dependencies,
            justInTimeBindingFactories = _justInTimeBindingFactories,
            bindings = finalBindings
        )

        onBuildBlocks.toList().forEach { it(component) }

        return component
    }

    private fun runPreBuildBlocks() {
        var run = true
        while (run) {
            run = false
            onPreBuildBlocks.toList().fastForEach {
                val result = it()
                if (!result) onPreBuildBlocks -= it
                run = run || result
            }
        }
    }

    private fun checkScopes() {
        val dependencyScopes = mutableListOf<Scope>()

        fun addScope(scope: Scope) {
            check(scope !in dependencyScopes) {
                "Duplicated scope $scope"
            }

            dependencyScopes += scope
        }

        _dependencies.fastForEach { dependency ->
            dependency.scopes.fastForEach { scope ->
                addScope(scope)
            }
        }

        _scopes.fastForEach { addScope(it) }
    }

    private fun Component.getAllBindings(): Map<Key<*>, Binding<*>> =
        mutableMapOf<Key<*>, Binding<*>>().also { collectBindings(it) }

    private fun Component.collectBindings(bindings: MutableMap<Key<*>, Binding<*>>) {
        dependencies.fastForEach { it.collectBindings(bindings) }
        bindings += this.bindings
    }
}

/**
 * Bridge for @IntoComponent functions
 *
 * @see IntoComponent
 */
interface ComponentBuilderContributor {
    val invokeOnInit: Boolean get() = false
    fun apply(builder: ComponentBuilder)
}

private object ComponentBuilderContributors {
    val implementations by lazy {
        ServiceLoader.load(ComponentBuilderContributor::class.java, null)
            .toList()
    }
}
