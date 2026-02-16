import kotlinx.coroutines.sync.Semaphore

/**
 * Simple semaphore-based rate limiter.
 * Limits how many coroutines can make HTTP requests simultaneously.
 *
 * This is your first concurrency primitive — the semaphore.
 * Think of it as a bouncer at a club: only N people in at a time.
 */
class RateLimiter(concurrency: Int) {

    private val semaphore = Semaphore(concurrency)

    /**
     * Executes [block] only when a permit is available.
     * If all permits are taken, this suspends (not blocks!) until one frees up.
     */
    suspend fun <T> withPermit(block: suspend () -> T): T {
        semaphore.acquire()
        try {
            return block()
        } finally {
            semaphore.release()
        }
    }
}