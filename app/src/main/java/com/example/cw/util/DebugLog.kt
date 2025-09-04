package com.example.cw.util

/**
 * Minimal debug logger for tests and observability.
 * Stores entries in-memory and prints to stdout. All messages are prefixed with [DEBUG_LOG].
 */
object DebugLog {
    private val _entries: MutableList<String> = mutableListOf()
    val entries: List<String> get() = _entries

    @Synchronized
    fun d(message: String) {
        val line = "[DEBUG_LOG] $message"
        _entries.add(line)
        println(line)
    }

    @Synchronized
    fun clear() {
        _entries.clear()
    }
}
