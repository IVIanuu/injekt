package com.ivianuu.injekt

@BehaviorMarker
val Unbound = InterceptingBehavior { binding ->
    binding.copy(duplicateStrategy = DuplicateStrategy.Override)
} + AnyScope
