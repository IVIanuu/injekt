package com.ivianuu.injekt.integrationtests.ast

import com.ivianuu.injekt.compiler.removeIllegalChars
import com.ivianuu.injekt.test.compile
import com.ivianuu.injekt.test.source
import java.nio.file.Files
import java.nio.file.Paths

fun main() {
    println(
        buildString {
            appendLine("package com.ivianuu.injekt.integrationtests.ast")
            appendLine("import com.ivianuu.injekt.test.compile")
            appendLine("import com.ivianuu.injekt.test.source")
            appendLine("import org.junit.Test")
            appendLine("import com.ivianuu.ast.psi2ast.astEnabled")
            appendLine("import com.ivianuu.injekt.test.assertOk")
            appendLine("import java.io.File")

            appendLine("class GeneratedAstTests {")

            appendLine(
                """
                        init {
                            astEnabled = true
                        }
                """
            )

            Files.walk(Paths.get("/home/ivianuu/other-projects/kotlin/compiler/testData/ir/irText"))
                .filter { it.toString().endsWith(".kt") }
                .forEach { filePath ->
                    appendLine(
                        """
                                @Test
                                fun test${filePath.toString().replace("/", "_")
                            .replace(".", "").replace("-", "")}() {
                                    compile {
                                        sources += source(
                                            source = File("$filePath").readText(),
                                            injektImports = false,
                                            initializeInjekt = false
                                        )
                                    }.apply {
                                        assertOk()
                                    }
                                }
                        """
                    )

                }

            appendLine("}")
        }
    )
}
