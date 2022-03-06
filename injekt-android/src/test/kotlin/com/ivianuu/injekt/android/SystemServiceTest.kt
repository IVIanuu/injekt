/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.android

import android.os.*
import androidx.test.platform.app.*
import com.ivianuu.injekt.*
import org.junit.*
import org.junit.runner.*
import org.robolectric.*
import org.robolectric.annotation.*

@Config(sdk = [28])
@RunWith(RobolectricTestRunner::class)
class SystemServiceTest {
  @Test fun testCanRequestSystemService() {
    @Provide val context = InstrumentationRegistry.getInstrumentation().context
    inject<SystemService<PowerManager>>().value
  }
}
