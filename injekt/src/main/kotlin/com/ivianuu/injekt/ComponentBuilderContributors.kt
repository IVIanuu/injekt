package com.ivianuu.injekt

internal object ComponentBuilderContributors {

    internal val contributorsByScope =
        mutableMapOf<Scope?, MutableList<ComponentBuilderContributor>>()

    fun get(scope: Scope? = null): List<ComponentBuilderContributor> =
        contributorsByScope.getOrElse(scope) { emptyList() }

    fun register(contributor: ComponentBuilderContributor) {
        contributorsByScope.getOrPut(contributor.scope) { mutableListOf() }.run {
            this += contributor
            sortByDescending { it.invokeOnInit }
        }
    }
}
