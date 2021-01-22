package root

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import org.junit.jupiter.api.fail
import root.dsl.Function
import root.dsl.FunctionBuilder
import root.dsl.JustTypes
import root.dsl.Program
import root.dsl.Return
import root.dsl.VariableAssignment
import root.dsl.VariableDeclaration
import root.dsl.WhileLoop
import root.dsl.elseBlock
import root.dsl.ifBlock
import root.dsl.param


@Throws(Throwable::class)
private fun applyParser(input: String) {
    val lines = input.split("\n")
    val numLength = lines.size.toString().length
    val template = "%${numLength}d | %s"
    println("input: \n" + lines.mapIndexed { i, s -> template.format(i, s) }.joinToString(separator = "\n"))
    Main.createParser(input)
}

@Throws(Throwable::class)
private fun applyParser(supplier: () -> String) {
    applyParser(supplier.invoke())
}

private fun testForParseExceptionText(text: String, content: () -> Unit) {
    try {
        content()
        fail("Exception did not contain expected message text")
    } catch (e: Exception) {
        if (e.message?.contains(text) != true) {
            fail("Exception did not contain expected message text")
        }
    }
}


class ParserTests {

    @BeforeEach
    fun resetParser(info: TestInfo) {
        Main.restParser()
        println("=".repeat(80))
        println("Test: ${info.displayName}")
        println("-".repeat(80))
    }

    @AfterEach
    fun afterEach() {
        println("=".repeat(80))
        println()
    }

    @Test
    fun `Empty Program`() {
        applyParser {
            Program("ATest")
        }
    }

    @Test
    fun `Top-Level Function Declarations`() {
        applyParser {
            Program {
                name = "ATest"
                Function<JustTypes.Void> {
                    name = "main"
                }

                Function<JustTypes.Boolean>(name = "funBool")
                Function<JustTypes.Int>(name = "funInt")

            }
        }
    }

    @Test
    fun `Top-Level Variable Declaration`() {
        applyParser {
            Program("ATest") {
                VariableDeclaration<JustTypes.Int> {
                    name = "a"
                }
            }
        }
    }

    @Test
    fun `Top-Level Variable Declaration with Assignment`() {
        applyParser {
            Program("ATest") {
                VariableDeclaration<JustTypes.Boolean> {
                    name = "a"
                    value = JustTypes.Boolean.True
                }
            }
        }
    }

    @Test
    fun `Top-Level Variable Declaration with split Assignment`() {
        applyParser {
            Program("ATest") {
                val v1 = VariableDeclaration<JustTypes.Int>("a")
                VariableAssignment(v1) {
                    value = JustTypes.Int.Val(12)
                }
            }
        }
    }

    @Test
    fun `Function with code`() {
        applyParser {
            Program {
                Function<JustTypes.Void> {
                    val v1 = VariableDeclaration<JustTypes.Int> {
                        name = "v1"
                        value = JustTypes.Int.Val(47)
                    }
                    VariableAssignment(v1) {
                        value = JustTypes.Int.Val(4711)
                    }
                }
            }
        }
    }

    @Test
    fun `Functions with parameters`() {
        applyParser {
            Program {
                Function<JustTypes.Void> {
                    param<JustTypes.Int>("param1")
                }
                Function<JustTypes.Void> {
                    param<JustTypes.Int>("param1")
                    param<JustTypes.Boolean>("param2")
                }
            }
        }
    }

    @Test
    fun `Functions with return`() {
        applyParser {
            Program {
                Function<JustTypes.Boolean> {
                    param<JustTypes.Int>("param1")
                    Return(JustTypes.Boolean.True)
                }
                Function<JustTypes.Int> {
                    param<JustTypes.Int>("param1")
                    param<JustTypes.Boolean>("param2")
                    Return(JustTypes.Int.Val(4711))
                }
                Function<JustTypes.Int> {
                    val var01 = VariableDeclaration(value = JustTypes.Int.Val(4711))
                    Return(var01)
                }

                Function<JustTypes.Void> {
                    Return()
                }
            }
        }
    }


    @Test
    fun `If Then Else`() {
        applyParser {
            Program {
                Function<JustTypes.Void> {
                    val cond = VariableDeclaration<JustTypes.Boolean> {
                        name = "isEnabled"
                        value = JustTypes.Boolean.False
                    }
                    ifBlock(cond.name) {
                        VariableDeclaration<JustTypes.Int> { value = JustTypes.Int.Val(636) }
                    }

                    ifBlock("true") {
                        VariableDeclaration<JustTypes.Int> { value = JustTypes.Int.Val(666) }
                    }.elseBlock {
                        VariableDeclaration<JustTypes.Boolean> { value = JustTypes.Boolean.False }
                    }
                }
            }
        }
    }

    @Test
    fun `While Loops`() {
        applyParser {
            Program {
                Function<JustTypes.Void> {
                    val cond = VariableDeclaration<JustTypes.Boolean> {
                        name = "isEnabled"
                        value = JustTypes.Boolean.False
                    }
                    WhileLoop(cond.name) {
                        VariableDeclaration<JustTypes.Boolean>()
                    }
                    WhileLoop("true") {
                        VariableDeclaration<JustTypes.Int>()
                    }
                    WhileLoop("true || false && true") {
                        VariableDeclaration<JustTypes.Int>()
                    }
                    WhileLoop("${cond.name} && ${cond.name} || false")
                }
            }
        }
    }

    @Nested
    @Suppress("ClassName")
    inner class `Test for Invalid Code Examples` {
        @Test
        fun `(Fail) - Function Declaration inside top-level function declaration`() {
            testForParseExceptionText(
                text = "Encountered \" \"void\" \"void \"\" at line 3, column 3."
            ) {
                applyParser {
                    Program {
                        Function<JustTypes.Void> {
                            addRaw(FunctionBuilder("insideFuncFunc", JustTypes.Void).build(1))
                        }
                    }
                }
            }
        }

        @Test
        fun `(Fail) - Empty String`() {
            testForParseExceptionText(
                text = "Encountered \"<EOF>\" at line 0, column 0."
            ) {
                val input = ""
                applyParser(input)
            }
        }
    }
}
