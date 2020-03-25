package com.ivianuu.injekt

internal object ComponentBuilderContributors {

    private val allContributors = mutableListOf<ComponentBuilderContributor>()

    init {
        allContributors += FastServiceLoader.load(
            ComponentBuilderContributor::class,
            ClassLoader.getSystemClassLoader()
        )
    }

    fun getUnscopedInit(): List<ComponentBuilderContributor> =
        allContributors.filter { it.invokeOnInit && it.scope == null }

    fun getNonInitUnscoped(): List<ComponentBuilderContributor> =
        allContributors.filter { !it.invokeOnInit && it.scope == null }

    fun getForScope(scope: Scope): List<ComponentBuilderContributor> =
        allContributors.filter { !it.invokeOnInit && it.scope == scope }

    fun register(contributor: ComponentBuilderContributor) {
        allContributors += contributor
    }

}
