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

    open class Float : JustTypes() {
        data class Val(val value: kotlin.Float) : JustTypes.Float(), ValueProvider<JustTypes.Float> {
            override fun getValueAsString() = value.toString()
        }
    }

    object Void : JustTypes()

    companion object {
        fun <T : JustTypes> resolve(clazz: Class<T>): JustTypes {
            return when (clazz) {
                JustTypes.Boolean::class.java -> JustTypes.Boolean()
                JustTypes.Int::class.java -> JustTypes.Int()
                JustTypes.Float::class.java -> JustTypes.Float()
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
            is Float -> "float"
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

data class FunctionBuilder(var name: String, private var returnType: JustTypes) : AbstractJustBuilder() {

    override fun build(indentLevel: Int): String {
        return """
            |${returnType.codeString()} $name() {
            |${contentString(1)}
            |}
        """.trimMargin().indentAll(indentLevel)
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
