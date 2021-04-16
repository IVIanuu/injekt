package com.ivianuu.injekt.ktor

import com.ivianuu.injekt.*
import com.ivianuu.injekt.scope.*
import io.kotest.matchers.booleans.*
import io.ktor.server.testing.*
import org.junit.*

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
