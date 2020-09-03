package com.ivianuu.injekt.integrationtests.ast

import com.ivianuu.injekt.test.compile
import com.ivianuu.injekt.test.source
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList

class AstTest {

    @Test
    fun testAll() {
        Files.walk(Paths.get("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText"))
            .filter { it.toString().endsWith(".kt") }.toList().map { filePath ->
                compile {
                    sources += source(
                        source = filePath.toFile().readText(),
                        injektImports = false,
                        initializeInjekt = false
                    )
                }
            }
    }

}
