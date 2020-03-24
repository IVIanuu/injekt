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

    private val _parents = mutableListOf<Component>()
    val parents: List<Component> get() = _parents

    private val _bindings = mutableMapOf<Key<*>, Binding<*>>()
    val bindings: Map<Key<*>, Binding<*>> get() = _bindings

    private val _jitFactories = mutableListOf<JitFactory>()
    val jitFactories: List<JitFactory> get() = _jitFactories

    private val onPreBuildBlocks = mutableListOf<() -> Boolean>()
    private val onBuildBlocks = mutableListOf<(Component) -> Unit>()
    private val onBindingAddedBlocks = mutableListOf<(Binding<*>) -> Unit>()
    private val onScopeAddedBlocks = mutableListOf<(Scope) -> Unit>()
    private val onParentAddedBlocks = mutableListOf<(Component) -> Unit>()
    private val bindingInterceptors = mutableListOf<(Binding<*>) -> Binding<*>>()

    init {
        (Injekt.componentBuilderContributors +
                ComponentBuilderContributors.implementations)
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
        scopes.forEach { scope ->
            check(scope !in this._scopes) { "Duplicated scope $scope" }
            this._scopes += scope
            onScopeAddedBlocks.toList().forEach { it(scope) }
        }
    }

    /**
     * Replaces all existing scopes with [scopes]
     */
    fun setScopes(scopes: List<Scope>) {
        _scopes.clear()
        scopes(*scopes.toTypedArray())
    }

    /**
     * Adds the [parents] to the component if this component cannot resolve a instance
     * it will ask it's parents
     */
    fun parents(vararg parents: Component) {
        parents.forEach { parent ->
            check(parent !in this._parents) { "Duplicated parent $parent" }
            this._parents += parent
            onParentAddedBlocks.toList().forEach { it(parent) }
        }
    }

    /**
     * Replaces all existing parents with [parents]
     */
    fun setParents(parents: List<Component>) {
        _parents.clear()
        parents(*parents.toTypedArray())
    }

    fun jitFactory(block: (Key<*>, Component) -> Binding<*>?) {
        jitFactories(
            object : JitFactory {
                override fun <T> create(key: Key<T>, component: Component): Binding<T>? =
                    block(key, component) as? Binding<T>
            }
        )
    }

    /**
     * Adds the [factories]
     */
    fun jitFactories(vararg factories: JitFactory) {
        _jitFactories += factories
    }

    /**
     * Replaces all existing jit factories with [factories]
     */
    fun setJitFactories(factories: List<JitFactory>) {
        _jitFactories.clear()
        jitFactories(*factories.toTypedArray())
    }

    inline fun <reified T> bind(
        qualifier: Qualifier = Qualifier.None,
        behavior: Behavior = Behavior.None,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
        tags: List<Tag> = emptyList(),
        noinline provider: BindingProvider<T>
    ) = bind(
        key = keyOf(qualifier = qualifier),
        behavior = behavior,
        duplicateStrategy = duplicateStrategy,
        tags = tags,
        provider = provider
    )

    fun <T> bind(
        key: Key<T>,
        behavior: Behavior = Behavior.None,
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
        tags: List<Tag> = emptyList(),
        provider: BindingProvider<T>
    ) {
        bind(
            Binding(
                key = key,
                behavior = behavior,
                duplicateStrategy = duplicateStrategy,
                tags = tags,
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
        val finalBinding = bindingInterceptors.fold(binding) { acc, interceptor ->
            interceptor(acc) as Binding<T>
        }
        if (finalBinding.duplicateStrategy.check(
                existsPredicate = { finalBinding.key in _bindings },
                errorMessage = { "Already declared binding for ${finalBinding.key}" }
            )
        ) {
            _bindings[finalBinding.key] = finalBinding
            onBindingAddedBlocks.toList().forEach { it(finalBinding) }
        }
    }

    /**
     * Replaces all existing bindings with [bindings]
     */
    fun setBindings(bindings: List<Binding<*>>) {
        _bindings.clear()
        bindings.forEach { bind(it) }
    }

    /**
     * Invokes the [block] for every binding which gets added
     */
    fun onBindingAdded(block: (Binding<*>) -> Unit) {
        onBindingAddedBlocks += block
    }

    /**
     * Invokes the [block] when ever a binding gets added
     */
    fun bindingInterceptor(block: (Binding<*>) -> Binding<*>) {
        bindingInterceptors += block
    }

    /**
     * Invokes the [block] for every scope which gets added
     */
    fun onScopeAdded(block: (Scope) -> Unit) {
        onScopeAddedBlocks += block
    }

    /**
     * Invokes the [block] for every parent which gets added
     */
    fun onParentAdded(block: (Component) -> Unit) {
        onParentAddedBlocks += block
    }

    /**
     * Invokes the [block] before building the [Component] until it returns false
     */
    fun onPreBuild(block: () -> Boolean) {
        onPreBuildBlocks += block
    }

    /**
     * Invokes the [block] right after [Component] gets build
     */
    fun onBuild(block: (Component) -> Unit) {
        onBuildBlocks += block
    }

    /**
     * Create a new [Component] instance.
     */
    fun build(): Component {
        (Injekt.componentBuilderContributors +
                ComponentBuilderContributors.implementations)
            .filter { !it.invokeOnInit }
            .forEach { it.apply(this) }

        runPreBuildBlocks()

        checkScopes()

        val parentBindings = mutableMapOf<Key<*>, Binding<*>>()

        _parents.forEach { parent ->
            val bindings = parent.getAllBindings()
            for ((key, binding) in bindings) {
                if (binding.duplicateStrategy.check(
                        existsPredicate = { key in parentBindings },
                        errorMessage = { "Already declared binding for $key" }
                    )
                ) {
                    parentBindings[key] = binding
                }
            }
        }

        val finalBindings = mutableMapOf<Key<*>, Binding<*>>()

        _bindings.forEach { (key, binding) ->
            if (binding.duplicateStrategy.check(
                    existsPredicate = { key in parentBindings },
                    errorMessage = { "Already declared binding for $key" })
            ) {
                finalBindings[key] = binding
            }
        }

        val component = Component(
            scopes = _scopes,
            parents = _parents,
            jitFactories = _jitFactories,
            bindings = finalBindings
        )

        onBuildBlocks.toList().forEach { it(component) }

        return component
    }

    private fun runPreBuildBlocks() {
        var run = true
        while (run) {
            run = false
            onPreBuildBlocks.toList().forEach {
                val result = it()
                if (!result) onPreBuildBlocks -= it
                run = run || result
            }
        }
    }

    private fun checkScopes() {
        val parentScopes = mutableListOf<Scope>()

        fun addScope(scope: Scope) {
            check(scope !in parentScopes) {
                "Duplicated scope $scope"
            }

            parentScopes += scope
        }

        _parents.forEach { parent ->
            parent.scopes.forEach { scope ->
                addScope(scope)
            }
        }

        _scopes.forEach { addScope(it) }
    }

    private fun Component.getAllBindings(): Map<Key<*>, Binding<*>> =
        mutableMapOf<Key<*>, Binding<*>>().also { collectBindings(it) }

    private fun Component.collectBindings(bindings: MutableMap<Key<*>, Binding<*>>) {
        parents.forEach { it.collectBindings(bindings) }
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
        FastServiceLoader.load(
                ComponentBuilderContributor::class.java,
                ClassLoader.getSystemClassLoader()
            )
    }
}
