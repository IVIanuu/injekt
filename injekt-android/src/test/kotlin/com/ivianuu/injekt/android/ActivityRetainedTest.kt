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

package com.ivianuu.injekt.android

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.ivianuu.injekt.scope.element
import com.ivianuu.injekt.scope.initializeApp
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.ivianuu.injekt.scope.*

@Config(sdk = [28])
@RunWith(RobolectricTestRunner::class)
class ActivityRetainedTest {

    @Test
    fun testActivityRetainedGivenScopeLifecycle() {
        initializeApp(initializers = { emptySet() })
        val scenario = ActivityScenario.launch(AndroidTestActivity::class.java)
        lateinit var disposable: TestGivenScopeDisposable<ActivityRetainedGivenScope>
        scenario.onActivity { disposable = it.activityRetainedGivenScope.element() }
        disposable.disposed.shouldBeFalse()
        scenario.recreate()
        disposable.disposed.shouldBeFalse()
        scenario.moveToState(Lifecycle.State.DESTROYED)
        disposable.disposed.shouldBeTrue()
    }

}
