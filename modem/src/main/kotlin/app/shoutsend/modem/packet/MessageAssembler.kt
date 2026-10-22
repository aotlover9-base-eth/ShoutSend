package app.shoutsend.modem.packet

/**
 * Immutable progress snapshot for UI display during transmission reception.
 *
 * @property messageId The active message ID.
 * @property receivedFragments Count of distinct fragments received so far.
 * @property totalFragments Expected total fragment count.
 * @property type Payload message type.
 */
public data class AssemblyProgress(
    public val messageId: Int,
    public val receivedFragments: Int,
    public val totalFragments: Int,
    public val type: MessageType
) {
    public val fraction: Float
        get() = if (totalFragments > 0) receivedFragments.toFloat() / totalFragments else 0f

    public val isComplete: Boolean
        get() = totalFragments > 0 && receivedFragments >= totalFragments
}

/**
 * Reassembled complete message ready for application payload decoding.
 */
public data class ReceivedMessage(
    public val type: MessageType,
    public val flags: MessageFlags,
    public val messageId: Int,
    public val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReceivedMessage) return false
        if (type != other.type) return false
        if (flags != other.flags) return false
        if (messageId != other.messageId) return false
        if (!payload.contentEquals(other.payload)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + flags.hashCode()
        result = 31 * result + messageId
        result = 31 * result + payload.contentHashCode()
        return result
    }
}

/**
 * Outcome of feeding a decoded frame into the assembler.
 */
public sealed interface AssemblerResult {
    public data class Incomplete(public val progress: AssemblyProgress) : AssemblerResult
    public data class Complete(public val message: ReceivedMessage) : AssemblerResult
    public data class Duplicate(public val progress: AssemblyProgress) : AssemblerResult
    public data class Conflict(public val messageId: Int, public val newProgress: AssemblyProgress) : AssemblerResult
}

/**
 * Reassembles message frames into full payload buffers.
 *
 * Handles:
 * - Grouping by 8-bit message ID.
 * - Out-of-order fragment delivery.
 * - Duplicate frame suppression.
 * - Conflicting frame stream detection (same messageId with different type/count resets state).
 * - Expiration timeout for incomplete messages.
 * - Exactly-once delivery guarantee per transmission.
 *
 * @param timeoutMs Timeout duration in milliseconds for incomplete messages (default 60,000 ms).
 * @param clock Injectable time source returning current epoch/monotonic milliseconds.
 */
public class MessageAssembler(
    public val timeoutMs: Long = 60_000L,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    /**
     * Total number of detected message stream conflicts.
     */
    public var conflictCount: Int = 0
        private set

    private val pendingMessages = mutableMapOf<Int, PendingState>()
    private val completedMessageIds = mutableMapOf<Int, Long>()

    /**
     * Feeds a decoded [frame] into the reassembler.
     *
     * @param frame Decoded and CRC-verified frame.
     * @return [AssemblerResult] detailing the resulting state.
     */
    public fun feed(frame: Frame): AssemblerResult {
        val now = clock()
        pruneExpired(now)

        val header = frame.header
        val id = header.messageId

        // Check if recently completed
        val completedAt = completedMessageIds[id]
        if (completedAt != null) {
            if (now - completedAt < timeoutMs) {
                val progress = AssemblyProgress(
                    messageId = id,
                    receivedFragments = header.fragmentCount,
                    totalFragments = header.fragmentCount,
                    type = header.type
                )
                return AssemblerResult.Duplicate(progress)
            } else {
                completedMessageIds.remove(id)
            }
        }

        val existing = pendingMessages[id]
        if (existing != null) {
            // Check for conflict: same ID but changed type or fragmentCount
            if (existing.type != header.type || existing.fragmentCount != header.fragmentCount) {
                conflictCount++
                pendingMessages.remove(id)

                val fresh = PendingState(
                    type = header.type,
                    flags = header.flags,
                    messageId = id,
                    fragmentCount = header.fragmentCount,
                    lastActivityMs = now
                )
                fresh.addFragment(header.fragmentIndex, frame.payload)

                if (fresh.isComplete()) {
                    completedMessageIds[id] = now
                    return AssemblerResult.Complete(
                        ReceivedMessage(
                            type = fresh.type,
                            flags = fresh.flags,
                            messageId = id,
                            payload = fresh.assemblePayload()
                        )
                    )
                }

                pendingMessages[id] = fresh
                return AssemblerResult.Conflict(messageId = id, newProgress = fresh.toProgress())
            }

            // Normal pending state continuation
            existing.lastActivityMs = now
            val added = existing.addFragment(header.fragmentIndex, frame.payload)
            if (!added) {
                return AssemblerResult.Duplicate(existing.toProgress())
            }

            if (existing.isComplete()) {
                pendingMessages.remove(id)
                completedMessageIds[id] = now
                return AssemblerResult.Complete(
                    ReceivedMessage(
                        type = existing.type,
                        flags = existing.flags,
                        messageId = id,
                        payload = existing.assemblePayload()
                    )
                )
            }

            return AssemblerResult.Incomplete(existing.toProgress())
        }

        // New pending message
        val fresh = PendingState(
            type = header.type,
            flags = header.flags,
            messageId = id,
            fragmentCount = header.fragmentCount,
            lastActivityMs = now
        )
        fresh.addFragment(header.fragmentIndex, frame.payload)

        if (fresh.isComplete()) {
            completedMessageIds[id] = now
            return AssemblerResult.Complete(
                ReceivedMessage(
                    type = fresh.type,
                    flags = fresh.flags,
                    messageId = id,
                    payload = fresh.assemblePayload()
                )
            )
        }

        pendingMessages[id] = fresh
        return AssemblerResult.Incomplete(fresh.toProgress())
    }

    /**
     * Returns the active progress for [messageId], or null if not active.
     */
    public fun getProgress(messageId: Int): AssemblyProgress? {
        return pendingMessages[messageId]?.toProgress()
    }

    /**
     * Returns an immutable list of all in-progress message progress snapshots.
     */
    public fun getAllProgress(): List<AssemblyProgress> {
        return pendingMessages.values.map { it.toProgress() }
    }

    /**
     * Returns the progress snapshot of the most recently updated active message, if any.
     */
    public val currentProgress: AssemblyProgress?
        get() = pendingMessages.values.maxByOrNull { it.lastActivityMs }?.toProgress()

    /**
     * Clears all pending state and history.
     */
    public fun reset() {
        pendingMessages.clear()
        completedMessageIds.clear()
        conflictCount = 0
    }

    private fun pruneExpired(now: Long) {
        val expiredPending = pendingMessages.filterValues { now - it.lastActivityMs >= timeoutMs }.keys
        for (k in expiredPending) {
            pendingMessages.remove(k)
        }

        val expiredCompleted = completedMessageIds.filterValues { now - it >= timeoutMs }.keys
        for (k in expiredCompleted) {
            completedMessageIds.remove(k)
        }
    }

    private class PendingState(
        val type: MessageType,
        val flags: MessageFlags,
        val messageId: Int,
        val fragmentCount: Int,
        var lastActivityMs: Long
    ) {
        val fragments = arrayOfNulls<ByteArray>(fragmentCount)
        var receivedCount: Int = 0

        fun addFragment(index: Int, payload: ByteArray): Boolean {
            if (fragments[index] == null) {
                fragments[index] = payload
                receivedCount++
                return true
            }
            return false
        }

        fun isComplete(): Boolean = receivedCount == fragmentCount

        fun assemblePayload(): ByteArray {
            var total = 0
            for (f in fragments) {
                total += f?.size ?: 0
            }
            val out = ByteArray(total)
            var offset = 0
            for (f in fragments) {
                if (f != null) {
                    System.arraycopy(f, 0, out, offset, f.size)
                    offset += f.size
                }
            }
            return out
        }

        fun toProgress(): AssemblyProgress = AssemblyProgress(
            messageId = messageId,
            receivedFragments = receivedCount,
            totalFragments = fragmentCount,
            type = type
        )
    }
}
