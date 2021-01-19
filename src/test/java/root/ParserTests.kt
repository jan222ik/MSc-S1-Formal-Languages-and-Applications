package root

import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import root.dsl.Function
import root.dsl.FunctionBuilder
import root.dsl.JustTypes
import root.dsl.Program
import root.dsl.VariableAssignment
import root.dsl.VariableDeclaration
import root.dsl.WhileLoop
import root.dsl.elseBlock
import root.dsl.ifBlock
import root.dsl.param

class ParserTests {
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

    @Before
    fun resetParser() {
        Main.restParser()
    }

    @Test
    fun validEmptyProgram() {
        applyParser {
            Program("ATest")
        }
    }

    @Test
    fun emptyProgram() {
        testForParseExceptionText(
            text = "Encountered \"<EOF>\" at line 0, column 0."
        ) {
            val input = ""
            applyParser(input)
        }
    }

    @Test
    fun programWithFunctions() {
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
    fun programWithDeclaration() {
        applyParser {
            Program("ATest") {
                VariableDeclaration<JustTypes.Int> {
                    name = "a"
                }
            }
        }
    }

    @Test
    fun programWithDeclarationWithAssignment() {
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
    fun programWithSplitDeclarationAndAssignment() {
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
    fun programWithCodeInFunctions() {
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
    fun functionInFunctionFailing() {
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
    fun functionWithParams() {
        applyParser {
            Program {
                Function<JustTypes.Void> {
                    param<JustTypes.Int>("param1")
                    param<JustTypes.Boolean>("param2")
                }
            }
        }
    }

    @Test
    fun functionWithIf() {
        applyParser {
            Program {
                Function<JustTypes.Void> {
                    val cond = VariableDeclaration<JustTypes.Boolean> {
                        name = "isEnabled"
                        value = JustTypes.Boolean.False
                    }
                    ifBlock(cond.name) {
                        VariableDeclaration<JustTypes.Int> { value = JustTypes.Int.Val(636)}
                    }

                    ifBlock("true") {
                        VariableDeclaration<JustTypes.Int> { value = JustTypes.Int.Val(666)}
                    }.elseBlock {
                        VariableDeclaration<JustTypes.Boolean> { value = JustTypes.Boolean.False }
                    }
                }
            }
        }
    }

    @Test
    fun functionWithWhile() {
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
                }
            }
        }
    }
}
