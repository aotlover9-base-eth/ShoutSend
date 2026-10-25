package app.shoutsend.core.modem

import kotlinx.coroutines.flow.StateFlow

/**
 * Seam providing reactive access to runtime [ModemConfig].
 *
 * Implemented as [FixedModemConfigProvider] in `src/user` (fixed to user Settings + default tuning),
 * and as [OverridableModemConfigProvider] in `src/dev` (allowing live DSP Lab overrides in Step 50).
 */
interface ModemConfigProvider {
    /**
     * Observable stream emitting current acoustic modem configuration.
     */
    val config: StateFlow<ModemConfig>
}
