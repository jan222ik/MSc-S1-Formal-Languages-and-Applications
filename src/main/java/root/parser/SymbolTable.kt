package root.parser

import java.lang.RuntimeException

class SymbolTable {

    private var curScope: Scope? = null

    fun enterScope(name: String = "") {
        val scope = Scope(
            name = name,
            outer = curScope,
            level = curScope?.level?.inc() ?: 0,
            nrOfParams = 0,
            nrOfLocals = 0
        )
        curScope = scope
        println("Entered Scope $scope")
    }

    fun leaveScope() {
        println("Exit Scope $curScope")
        curScope = curScope?.outer
    }

    fun insert(symbol: Symbol) {
        curScope?.addSymbol(symbol) ?: throw RuntimeException("There is no scope a symbol could be added to.")
    }

    fun lookup() {

    }
}
