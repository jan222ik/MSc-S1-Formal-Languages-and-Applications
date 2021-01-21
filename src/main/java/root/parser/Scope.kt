package root.parser

data class Scope(var name: String, var level: Int, var nrOfParams: Int, var nrOfLocals: Int, var outer: Scope?) {
    fun addSymbol(symbol: Symbol) {
        nrOfLocals += 1
        locals.add(symbol)
    }

    val locals: MutableList<Symbol> = mutableListOf()
}

data class Symbol(var isInit: Boolean, var value: Int, var addr: Int)
