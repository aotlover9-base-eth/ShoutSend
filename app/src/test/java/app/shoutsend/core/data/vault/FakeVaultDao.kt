package app.shoutsend.core.data.vault

import app.shoutsend.core.data.vault.dao.VaultDao
import app.shoutsend.core.data.vault.entity.VaultEntity
import app.shoutsend.core.data.vault.model.Direction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicLong

/**
 * In-memory test double for [VaultDao].
 */
class FakeVaultDao : VaultDao {

    private val idCounter = AtomicLong(1L)
    private val entriesFlow = MutableStateFlow<Map<Long, VaultEntity>>(emptyMap())

    override fun observeAllNewestFirst(): Flow<List<VaultEntity>> {
        return entriesFlow.map { map ->
            map.values.sortedWith(
                compareByDescending<VaultEntity> { it.timestamp }
                    .thenByDescending { it.id }
            )
        }
    }

    override fun observeByDirection(direction: Direction): Flow<List<VaultEntity>> {
        return entriesFlow.map { map ->
            map.values
                .filter { it.direction == direction }
                .sortedWith(
                    compareByDescending<VaultEntity> { it.timestamp }
                        .thenByDescending { it.id }
                )
        }
    }

    override suspend fun insert(entity: VaultEntity): Long {
        val assignedId = if (entity.id == 0L) idCounter.getAndIncrement() else entity.id
        val finalEntity = entity.copy(id = assignedId)
        val current = entriesFlow.value.toMutableMap()
        current[assignedId] = finalEntity
        entriesFlow.value = current
        return assignedId
    }

    override suspend fun deleteById(id: Long) {
        val current = entriesFlow.value.toMutableMap()
        current.remove(id)
        entriesFlow.value = current
    }

    override suspend fun deleteAll() {
        entriesFlow.value = emptyMap()
    }

    override suspend fun count(): Int {
        return entriesFlow.value.size
    }

    override suspend fun setStarred(id: Long, isStarred: Boolean) {
        val current = entriesFlow.value.toMutableMap()
        val existing = current[id] ?: return
        current[id] = existing.copy(isStarred = isStarred)
        entriesFlow.value = current
    }

    override suspend fun prune(keepCount: Int) {
        val safeKeep = keepCount.coerceAtLeast(0)
        val current = entriesFlow.value.toMutableMap()

        val unstarredSorted = current.values
            .filter { !it.isStarred }
            .sortedWith(
                compareByDescending<VaultEntity> { it.timestamp }
                    .thenByDescending { it.id }
            )

        val unstarredToKeep = unstarredSorted.take(safeKeep).map { it.id }.toSet()

        val iterator = current.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (!entry.value.isStarred && entry.key !in unstarredToKeep) {
                iterator.remove()
            }
        }
        entriesFlow.value = current
    }
}
