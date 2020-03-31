package com.ivianuu.injekt

internal object ComponentBuilderContributors {

    internal val allContributors = mutableListOf<ComponentBuilderContributor>()

    init {
        allContributors += FastServiceLoader.load(
            ComponentBuilderContributor::class
        )
    }

    fun get(scope: Scope? = null): List<ComponentBuilderContributor> =
        allContributors.filter { it.scope == scope }
            .sortedByDescending { it.invokeOnInit }

    fun register(contributor: ComponentBuilderContributor) {
        allContributors += contributor
    }

}
