package root

import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import root.dsl.Function
import root.dsl.JustTypes
import root.dsl.Program
import root.dsl.VariableAssignment
import root.dsl.VariableDeclaration

class ParserTests {
    @Throws(Throwable::class)
    private fun applyParser(input: String) {
        Main.createParser(input)
    }

    @Throws(Throwable::class)
    private fun applyParser(supplier: () -> String) {
        Main.createParser(supplier.invoke())
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
                Function<JustTypes.Float>(name = "funFloat")

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
                VariableDeclaration<JustTypes.Float> {
                    name = "a"
                    value = JustTypes.Float.Val(0.1f)
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
}
