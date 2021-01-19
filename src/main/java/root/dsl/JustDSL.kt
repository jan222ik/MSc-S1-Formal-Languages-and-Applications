package root.dsl

fun main() {
    Program {
        name = "test"
        Function<JustTypes.Void> {
            name = "main"

            VariableDeclaration<JustTypes.Boolean> {
                name = "b"
                value = JustTypes.Boolean.False
            }
        }
    }.also { println(it) }
}


interface ValueProvider<T> {
    fun getValueAsString(): String?
}

sealed class JustTypes {
    open class Boolean : JustTypes() {
        object True : JustTypes.Boolean(), ValueProvider<JustTypes.Boolean> {
            override fun getValueAsString() = true.toString()
        }

        object False : JustTypes.Boolean(), ValueProvider<JustTypes.Boolean> {
            override fun getValueAsString() = false.toString()
        }
    }

    open class Int : JustTypes() {
        data class Val(val value: kotlin.Int) : JustTypes.Int(), ValueProvider<JustTypes.Int> {
            override fun getValueAsString() = value.toString()
        }
    }

    /*
    open class Float : JustTypes() {
        data class Val(val value: kotlin.Float) : JustTypes.Float(), ValueProvider<JustTypes.Float> {
            override fun getValueAsString() = value.toString()
        }
    }
     */

    object Void : JustTypes()

    companion object {
        fun <T : JustTypes> resolve(clazz: Class<T>): JustTypes {
            return when (clazz) {
                JustTypes.Boolean::class.java -> JustTypes.Boolean()
                JustTypes.Int::class.java -> JustTypes.Int()
                //JustTypes.Float::class.java -> JustTypes.Float()
                JustTypes.Void::class.java -> JustTypes.Void
                else -> {
                    throw IllegalArgumentException("Unrecognized Type: $clazz")
                }
            }
        }

    }

    fun codeString(): String {
        return when (this) {
            is Boolean -> "boolean"
            is Int -> "int"
            //is Float -> "float"
            Void -> "void"
        }
    }
}

interface JustBuilder {
    fun addRaw(raw: String)
    fun addBuilder(builder: JustBuilder)
    fun build(indentLevel: Int = 0): String
}


data class Raw(val raw: String) : AbstractJustBuilder() {
    override fun addRaw(raw: String) = throw UnsupportedOperationException()
    override fun addBuilder(builder: JustBuilder) = throw UnsupportedOperationException()
    override fun build(indentLevel: Int): String {
        return raw
    }
}

fun Program(name: String = "unnamedProgram", content: (ProgramBuilder.() -> Unit)? = null): String {
    return ProgramBuilder(name).let {
        content?.invoke(it)
        it.build()
    }
}

interface FunctionLevelDeclarationBuilder : JustBuilder
class FunctionLevelDeclarationBuilderInstance : AbstractJustBuilder(), FunctionLevelDeclarationBuilder {
    override fun build(indentLevel: Int): String {
        return contentString(indentLevel)
    }

}

fun FunctionLevelDeclarationBuilder.WhileLoop(
    condition: String,
    block: FunctionLevelDeclarationBuilder.() -> Unit
) {
    WhileLoopBuilder(condition).let {
        block.invoke(it)
        addBuilder(it)
    }
}

data class WhileLoopBuilder(
    val condition: String
) : AbstractJustBuilder(), FunctionLevelDeclarationBuilder {
    override fun build(indentLevel: Int): String {
        val blockString = contentString(1)
        return """
            |while ($condition) {
            |   $blockString
            |}
        """.trimMargin().indentAll(indentLevel)
    }
}

fun FunctionLevelDeclarationBuilder.ifBlock(
    condition: String,
    thenBlock: FunctionLevelDeclarationBuilder.() -> Unit
): IfBlockBuilder {
    return IfBlockBuilder(condition).also {
        FunctionLevelDeclarationBuilderInstance().let { inner ->
            thenBlock.invoke(inner)
            it.thenBlock = inner
        }
        this.addBuilder(it)
    }
}

fun IfBlockBuilder.elseBlock(elzeBlock: FunctionLevelDeclarationBuilder.() -> Unit) {
    FunctionLevelDeclarationBuilderInstance().let {
        elzeBlock.invoke(it)
        this.elzeBlock = it
    }
}

data class IfBlockBuilder(
    var condition: String = "missingCondition",
    var thenBlock: FunctionLevelDeclarationBuilder? = null,
    var elzeBlock: FunctionLevelDeclarationBuilder? = null
) : AbstractJustBuilder(), FunctionLevelDeclarationBuilder {
    override fun build(indentLevel: Int): String {
        val then = thenBlock?.build(1) ?: ""
        val elze = if (elzeBlock != null) {
            val block = elzeBlock?.build(1) ?: ""
            """else {
                |$block
                |}
            """.trimMargin()
        } else ""
        return """
            |if ($condition) {
            |   $then
            |} $elze
        """.trimMargin().indentAll(indentLevel)
    }

}


inline fun <reified T : JustTypes> ProgramBuilder.Function(
    name: String = "unnamedFunction",
    noinline content: (FunctionBuilder.() -> Unit)? = null
) {
    val type = JustTypes.resolve(T::class.java)
    FunctionBuilder(name, type).let {
        content?.invoke(it)
        addBuilder(it)
    }
}

inline fun <reified T : JustTypes> JustBuilder.VariableDeclaration(
    name: String = "unnamedVariable",
    value: ValueProvider<T>? = null,
    noinline block: (VariableBuilder<T>.() -> Unit)? = null
): VariableBuilder<T> {
    val type = JustTypes.resolve(T::class.java)
    return VariableBuilder(name, type, value, this).also {
        block?.invoke(it)
        addBuilder(it)
    }
}


inline fun <reified T : JustTypes> JustBuilder.VariableAssignment(
    variable: VariableBuilder<T>,
    value: ValueProvider<T>? = null,
    noinline block: (VariableAssignmentBuilder<T>.() -> Unit)? = null
) {
    VariableAssignmentBuilder(variable, value).also {
        block?.invoke(it)
        addBuilder(it)
    }
}

data class VariableAssignmentBuilder<T>(
    var assignment: VariableBuilder<T>,
    var value: ValueProvider<T>?
) : AbstractJustBuilder() {
    override fun build(indentLevel: Int): String {
        val indent = indent(indentLevel)
        return "$indent${assignment.name} = ${value?.getValueAsString()};"
    }
}

data class VariableBuilder<T>(
    var name: String,
    var returnType: JustTypes,
    var value: ValueProvider<T>?,
    val scope: JustBuilder
) : AbstractJustBuilder() {
    override fun build(indentLevel: Int): String {
        val indent = indent(indentLevel)
        val type = returnType.codeString()
        if (value == null) {
            return "$indent$type $name;"
        }
        return "$indent$type $name = ${value?.getValueAsString()};"
    }

}

const val INDENT = "\t"

abstract class AbstractJustBuilder : JustBuilder {
    private val content = mutableListOf<JustBuilder>()

    fun contentString(indentLevel: Int): String {
        return content.joinToString(separator = "\n") {
            it.build(indentLevel)
        }
    }

    fun indent(indentLevel: Int) = INDENT.repeat(indentLevel)

    override fun addRaw(raw: String) {
        content.add(Raw(raw))
    }

    override fun addBuilder(builder: JustBuilder) {
        content.add(builder)
    }
}

typealias ParamPair = Pair<String, JustTypes>

inline fun <reified T : JustTypes> FunctionBuilder.param(name: String) {
    val type = JustTypes.resolve(T::class.java)
    this.addToParamList(name to type)
}

data class ParamBuilder(
    val parameters: MutableList<ParamPair> = mutableListOf()
) : AbstractJustBuilder() {
    override fun build(indentLevel: Int): String {
        return parameters.joinToString(", ") { "${it.second.codeString()} ${it.first}" }
    }



}


data class FunctionBuilder(
    var name: String,
    private var returnType: JustTypes,
) : AbstractJustBuilder(),
    FunctionLevelDeclarationBuilder {

    private val paramBuilder: ParamBuilder = ParamBuilder()

    override fun build(indentLevel: Int): String {
        val paramString = paramBuilder.build(0)
        return """
            |${returnType.codeString()} $name($paramString) {
            |${contentString(1)}
            |}
        """.trimMargin().indentAll(indentLevel)
    }

    fun addToParamList(pair: ParamPair) {
        paramBuilder.parameters.add(pair)
    }
}


data class ProgramBuilder(var name: String) : AbstractJustBuilder() {

    override fun build(indentLevel: Int): String {
        return """
            |program $name {
            |${contentString(1)}
            |}
        """.trimMargin().indentAll(indentLevel)
    }
}

fun String.indentAll(indentLevel: Int): String {
    val indent = INDENT.repeat(indentLevel)
    return this.split("\n").joinToString(separator = "\n") { indent + it }
}
