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

    private val _scopes = mutableSetOf<Scope>()
    val scopes: Set<Scope> get() = _scopes

    private val _parents = mutableListOf<Component>()
    val parents: List<Component> get() = _parents

    private val _bindings = mutableMapOf<Key<*>, Binding<*>>()
    val bindings: Map<Key<*>, Binding<*>> get() = _bindings

    private val _jitFactories = mutableListOf<(Key<Any?>, Component) -> Binding<Any?>?>()
    val jitFactories: List<(Key<Any?>, Component) -> Binding<Any?>?> get() = _jitFactories

    private var onPreBuildBlocks = emptyList<() -> Boolean>()
    private var onBuildBlocks = emptyList<(Component) -> Unit>()
    private var onBindingAddedBlocks = emptyList<(Binding<Any?>) -> Unit>()
    private var onScopeAddedBlocks = emptyList<(Scope) -> Unit>()
    private var onParentAddedBlocks = emptyList<(Component) -> Unit>()
    private var bindingInterceptors = emptyList<(Binding<Any?>) -> Binding<Any?>?>()

    init {
        Modules.get(AnyScope).forEach { it(this) }
    }

    fun scopes(vararg scopes: Scope) {
        scopes.forEach { scopes(it) }
    }

    /**
     * Adds the [scope] this allows generated [Binding]s
     * to be associated with components.
     *
     * @see ScopeMarker
     */
    fun scopes(scope: Scope) {
        check(scope !in this._scopes) { "Duplicated scope $scope" }
        this._scopes += scope
        onScopeAddedBlocks.forEach { it(scope) }
        Modules.get(scope).forEach { it(this) }
    }

    /**
     * Replaces all existing scopes with [scopes]
     */
    fun setScopes(scopes: List<Scope>) {
        _scopes.clear()
        scopes.forEach { scopes(it) }
    }

    /**
     * Removes the [scope]
     */
    fun removeScope(scope: Scope) {
        _scopes -= scope
    }

    fun parents(vararg parents: Component) {
        parents.forEach { parents(it) }
    }

    /**
     * Adds the [parent] to the component if this component cannot resolve a instance
     * it will ask it's parents
     */
    fun parents(parent: Component) {
        check(parent !in this._parents) { "Duplicated parent $parent" }
        this._parents += parent
        onParentAddedBlocks.forEach { it(parent) }
    }

    /**
     * Replaces all existing parents with [parents]
     */
    fun setParents(parents: List<Component>) {
        _parents.clear()
        parents(*parents.toTypedArray())
    }

    /**
     * Removes the [parent]
     */
    fun removeParent(parent: Component) {
        _parents -= parent
    }

    fun jitFactory(factory: (Key<Any?>, Component) -> Binding<Any?>?) {
        jitFactories(factory)
    }

    /**
     * Adds the [factories]
     */
    fun jitFactories(vararg factories: (Key<Any?>, Component) -> Binding<Any?>?) {
        factories.forEach { jitFactories(it) }
    }

    /**
     * Adds the [factory]
     */
    fun jitFactories(factory: (Key<Any?>, Component) -> Binding<Any?>?) {
        _jitFactories += factory
    }

    /**
     * Replaces all existing jit factories with [factories]
     */
    fun setJitFactories(factories: List<(Key<Any?>, Component) -> Binding<Any?>?>) {
        _jitFactories.clear()
        jitFactories(*factories.toTypedArray())
    }

    /**
     * Removes the [factory]
     */
    fun removeJitFactory(factory: (Key<Any?>, Component) -> Binding<Any?>?) {
        _jitFactories -= factory
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
