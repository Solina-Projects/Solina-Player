package app.solina.player.presentation.navigation

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory staging store for video playback queues passed to [Screen.VideoPlayer].
 * Avoids serializing hundreds/thousands of content:// URIs into Navigation route strings
 * (which otherwise risks TransactionTooLargeException on SavedState persistence).
 */
object VideoQueueStore {
    private const val MAX_STAGED_QUEUES = 16
    private val queues = ConcurrentHashMap<String, List<String>>()
    private val insertionOrder = ArrayDeque<String>()

    @Synchronized
    fun stage(uris: List<String>): String? {
        if (uris.isEmpty()) return null
        val key = UUID.randomUUID().toString()
        queues[key] = uris.toList()
        insertionOrder.addLast(key)
        while (insertionOrder.size > MAX_STAGED_QUEUES) {
            val oldest = insertionOrder.removeFirst()
            queues.remove(oldest)
        }
        return key
    }

    fun get(key: String?): List<String> {
        if (key.isNullOrBlank()) return emptyList()
        return queues[key].orEmpty()
    }
}
