package au.edu.jcu.codequest.util

/** Source of the current time in milliseconds. Injected so tests can use a fixed clock. */
fun interface TimeProvider {
    fun now(): Long
}
