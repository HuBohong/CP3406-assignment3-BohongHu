package au.edu.jcu.codequest.util

import javax.inject.Inject
import kotlinx.coroutines.delay

/** Waits between animation frames. Injected so tests can replay a run instantly. */
fun interface FrameTimer {
    suspend fun await(millis: Long)
}

class DelayFrameTimer @Inject constructor() : FrameTimer {
    override suspend fun await(millis: Long) = delay(millis)
}
