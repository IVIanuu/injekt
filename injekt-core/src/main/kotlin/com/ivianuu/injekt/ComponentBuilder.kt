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

    var scopes = emptySet<Scope>()
        private set

    var parents = emptyList<Component>()
        private set

    private val _bindings = mutableMapOf<Key<*>, Binding<*>>()
    val bindings: Map<Key<*>, Binding<*>> get() = _bindings

    var jitFactories = emptyList<(Component, Key<Any?>) -> BindingProvider<Any?>?>()
        private set

    private var onPreBuildBlocks = emptyList<() -> Boolean>()
    private var onBuildBlocks = emptyList<(Component) -> Unit>()
    private var onBindingAddedBlocks = emptyList<(Binding<Any?>) -> Unit>()
    private var onScopeAddedBlocks = emptyList<(Scope) -> Unit>()
    private var onParentAddedBlocks = emptyList<(Component) -> Unit>()
    private var bindingInterceptors = emptyList<(Binding<Any?>) -> Binding<Any?>?>()

    init {
        Modules.get(AnyScope).forEach { it(this) }
    }

    /**
     * Adds the [scope] this allows generated [Binding]s
     * to be associated with components.
     *
     * @see ScopeMarker
     */
    fun scopes(vararg scopes: Scope) {
        scopes.forEach { scope ->
            check(scope !in this.scopes) { "Duplicated scope $scope" }
            this.scopes = this.scopes + scope
            onScopeAddedBlocks.forEach { it(scope) }
            Modules.get(scope).forEach { it(this) }
        }
    }

    /**
     * Replaces all existing scopes with [scopes]
     */
    fun setScopes(scopes: Set<Scope>) {
        this.scopes = emptySet()
        scopes.forEach { scopes(it) }
    }

    /**
     * Removes the [scope]
     */
    fun removeScope(scope: Scope) {
        scopes = scopes - scope
    }

    /**
     * Adds the [parents] to the component if this component cannot resolve a instance
     * it will ask it's parents
     */
    fun parents(vararg parents: Component) {
        parents.forEach { parent ->
            check(parent !in this.parents) { "Duplicated parent $parent" }
            this.parents = this.parents + parent
            onParentAddedBlocks.forEach { it(parent) }
        }
    }

    /**
     * Replaces all existing parents with [parents]
     */
    fun setParents(parents: List<Component>) {
        this.parents = emptyList()
        parents(*parents.toTypedArray())
    }

    /**
     * Removes the [parent]
     */
    fun removeParent(parent: Component) {
        this.parents = this.parents - parent
    }

    /**
     * Invokes the [factories] when ever a [Binding] request cannot be fulfilled
     * If a factory returns a non null [Binding] it will be returned
     */
    fun jitFactory(factory: (Component, Key<Any?>) -> BindingProvider<Any?>?) {
        jitFactories(factory)
    }

    fun jitFactories(vararg factories: (Component, Key<Any?>) -> BindingProvider<Any?>?) {
        jitFactories = jitFactories + factories
    }

    /**
     * Replaces all existing jit factories with [factories]
     */
    fun setJitFactories(factories: List<(Component, Key<Any?>) -> BindingProvider<Any?>?>) {
        jitFactories = emptyList()
        jitFactories(*factories.toTypedArray())
    }

    /**
     * Removes the [factory]
     */
    fun removeJitFactory(factory: (Component, Key<Any?>) -> BindingProvider<Any?>?) {
        jitFactories = jitFactories - factory
    }

    @KeyOverload
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
     */
    fun <T> bind(binding: Binding<T>) {
        var finalBinding: Binding<Any?> = binding as Binding<Any?>
        bindingInterceptors.forEach {
            finalBinding = it(finalBinding) ?: return
        }
        if (finalBinding.duplicateStrategy.check(
                existsPredicate = { finalBinding.key in _bindings },
                errorMessage = { "Already declared binding for ${finalBinding.key}" }
            )
        ) {
            _bindings[finalBinding.key] = finalBinding
            onBindingAddedBlocks.forEach { it(finalBinding) }
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
     * Removes the binding for [key]
     */
    fun removeBinding(key: Key<*>) {
        _bindings -= key
    }

    /**
     * Invokes the [block] for every binding which gets added
     */
    fun onBindingAdded(block: (Binding<Any?>) -> Unit) {
        onBindingAddedBlocks = onBindingAddedBlocks + block
    }

    /**
     * Invokes the [block] when ever a binding gets added
     *
     * Returning null means that the binding won't get added
     */
    fun bindingInterceptor(block: (Binding<Any?>) -> Binding<Any?>?) {
        bindingInterceptors = bindingInterceptors + block
    }

    /**
     * Invokes the [block] for every scope which gets added
     */
    fun onScopeAdded(block: (Scope) -> Unit) {
        onScopeAddedBlocks = onScopeAddedBlocks + block
    }

    /**
     * Invokes the [block] for every parent which gets added
     */
    fun onParentAdded(block: (Component) -> Unit) {
        onParentAddedBlocks = onParentAddedBlocks + block
    }

    /**
     * Invokes the [block] before building the [Component] until it returns false
     */
    fun onPreBuild(block: () -> Boolean) {
        onPreBuildBlocks = onPreBuildBlocks + block
    }

    /**
     * Invokes the [block] right after [Component] gets build
     */
    fun onBuild(block: (Component) -> Unit) {
        onBuildBlocks = onBuildBlocks + block
    }

    /**
     * Create a new [Component] instance.
     */
    fun build(): Component {
        runPreBuildBlocks()

        checkScopes()

        val parentBindings = mutableMapOf<Key<*>, Binding<*>>()

        parents.forEach { parent ->
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
            scopes = scopes.toSet(),
            parents = parents.toList(),
            jitFactories = jitFactories.toList(),
            bindings = finalBindings.toMap()
        )

        onBuildBlocks.forEach { it(component) }

        return component
    }

    private fun runPreBuildBlocks() {
        var run = true
        while (run) {
            run = false
            onPreBuildBlocks.forEach {
                val result = it()
                if (!result) onPreBuildBlocks = onPreBuildBlocks - it
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

        parents.forEach { parent ->
            parent.scopes.forEach { scope ->
                addScope(scope)
            }
        }

        scopes.forEach { addScope(it) }
    }

    private fun Component.getAllBindings(): Map<Key<*>, Binding<*>> =
        mutableMapOf<Key<*>, Binding<*>>().also { collectBindings(it) }

    private fun Component.collectBindings(bindings: MutableMap<Key<*>, Binding<*>>) {
        parents.forEach { it.collectBindings(bindings) }
        bindings += this.bindings
    }
}
