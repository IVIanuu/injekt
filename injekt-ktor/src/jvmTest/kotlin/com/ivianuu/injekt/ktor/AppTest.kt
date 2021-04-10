package com.ivianuu.injekt.ktor

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.common.*
import com.ivianuu.injekt.scope.*
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.ktor.server.testing.withTestApplication
import org.junit.Test

class AppTest {
    @Test
    fun testServerLifecycle() {
        lateinit var listener: ScopeDisposeListener
        withTestApplication({
            initializeAppGivenScope()
            listener = appGivenScope.element()
            listener.disposed.shouldBeFalse()
        }) {
        }
        listener.disposed.shouldBeTrue()
    }
}

@Given
@InstallElement<AppGivenScope>
@Scoped<AppGivenScope>
class ScopeDisposeListener : GivenScopeDisposable {
    var disposed = false
    override fun dispose() {
        disposed = true
    }
}
