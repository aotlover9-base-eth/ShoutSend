package app.shoutsend.modem.crypto

/**
 * Strength rating of a user-provided encryption passphrase.
 */
public enum class PassphraseStrengthClass {
    WEAK,
    OK,
    STRONG
}

/**
 * Result of evaluating passphrase security against offline dictionary and brute-force attacks.
 *
 * @property isValid True if the passphrase satisfies the minimum length requirement (>= 8 characters).
 * @property strength Evaluated security level.
 * @property message Explanatory security guidance without leaking passphrase contents.
 */
public data class PassphraseEvaluation(
    public val isValid: Boolean,
    public val strength: PassphraseStrengthClass,
    public val message: String
)

/**
 * Evaluates passphrase strength for ShoutSend Secure Mode.
 *
 * Note: Acoustic transmissions can be recorded by any nearby bystander or room microphone.
 * A recorded chirp can be analyzed offline with unlimited dictionary and brute-force attempts.
 * Weak passphrases offer negligible resistance to offline attacks.
 */
public object PassphraseEvaluator {

    public const val MIN_LENGTH: Int = 8

    /**
     * Evaluates security characteristics of [passphrase].
     * Never stores or prints the passphrase.
     */
    public fun evaluate(passphrase: CharArray): PassphraseEvaluation {
        if (passphrase.size < MIN_LENGTH) {
            return PassphraseEvaluation(
                isValid = false,
                strength = PassphraseStrengthClass.WEAK,
                message = "Passphrase must be at least $MIN_LENGTH characters"
            )
        }

        var hasUpper = false
        var hasLower = false
        var hasDigit = false
        var hasSymbol = false

        for (c in passphrase) {
            when {
                c.isUpperCase() -> hasUpper = true
                c.isLowerCase() -> hasLower = true
                c.isDigit() -> hasDigit = true
                else -> hasSymbol = true
            }
        }

        val varietyScore = (if (hasUpper) 1 else 0) +
            (if (hasLower) 1 else 0) +
            (if (hasDigit) 1 else 0) +
            (if (hasSymbol) 1 else 0)

        val strength = when {
            passphrase.size >= 14 && varietyScore >= 3 -> PassphraseStrengthClass.STRONG
            passphrase.size >= 10 && varietyScore >= 2 -> PassphraseStrengthClass.OK
            varietyScore >= 4 -> PassphraseStrengthClass.OK
            else -> PassphraseStrengthClass.WEAK
        }

        val message = when (strength) {
            PassphraseStrengthClass.WEAK ->
                "Weak passphrase: susceptible to offline dictionary search against recorded acoustic chirps"
            PassphraseStrengthClass.OK ->
                "Moderate passphrase strength: adequate for casual ad-hoc sharing"
            PassphraseStrengthClass.STRONG ->
                "Strong passphrase: high resistance against offline dictionary and brute-force attacks"
        }

        return PassphraseEvaluation(
            isValid = true,
            strength = strength,
            message = message
        )
    }
}
