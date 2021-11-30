/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.android

import android.os.PowerManager
import androidx.test.core.app.launchActivity
import androidx.test.platform.app.InstrumentationRegistry
import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.inject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [28])
@RunWith(RobolectricTestRunner::class)
class SystemServiceTest {
  @Test fun testCanRequestSystemService() {
    @Provide val context = InstrumentationRegistry.getInstrumentation().context
    inject<@SystemService PowerManager>()
  }
}
