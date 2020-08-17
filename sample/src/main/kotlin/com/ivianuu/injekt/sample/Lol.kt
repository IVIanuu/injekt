package com.ivianuu.injekt.sample

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.GivenSet
import java.util.logging.Logger

@GivenSet
class NetworkGivens {

    @GivenSet
    val utilGivens = UtilGivens()

}

@GivenSet
class UtilGivens {

    @Given
    fun logger(): Logger = error("")

}
