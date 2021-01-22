package root.vm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class VM {

    private var job: Job? = null

    fun run() {
        job = GlobalScope.launch(Dispatchers.IO) {

        }
    }
}
