package app.shoutsend.core.designsystem

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Stream of token overrides.
 */
interface TokenOverridesSource {
    val overrides: Flow<TokenOverrides>
}

/**
 * Mutator for token overrides.
 */
interface TokenOverridesWriter {
    suspend fun update(transform: (TokenOverrides) -> TokenOverrides)
    suspend fun resetAll()
    suspend fun resetColor(role: String)
    suspend fun resetRadius(name: String)
}

/**
 * Unified repository combining source and writer capabilities.
 */
interface TokenOverridesRepository : TokenOverridesSource, TokenOverridesWriter

/**
 * No-op implementation used in User flavor to completely omit persistence overhead.
 */
class NoOpTokenOverridesRepository : TokenOverridesRepository {
    override val overrides: Flow<TokenOverrides> = flowOf(TokenOverrides())

    override suspend fun update(transform: (TokenOverrides) -> TokenOverrides) {}

    override suspend fun resetAll() {}

    override suspend fun resetColor(role: String) {}

    override suspend fun resetRadius(name: String) {}
}
