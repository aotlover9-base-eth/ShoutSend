package app.shoutsend.modem.payload

/**
 * Diagnostic validation error detailing field and invariant failure without exposing sensitive data.
 *
 * @property field Name of the failing field (e.g. "ssid", "password", "url", "email").
 * @property reason Safe explanation of why validation failed. NEVER contains user payload values.
 */
public data class PayloadValidationError(
    public val field: String,
    public val reason: String
)

/**
 * Result of validating a [Payload] instance against protocol limits.
 */
public sealed interface PayloadValidationResult {
    public data object Valid : PayloadValidationResult
    public data class Invalid(public val error: PayloadValidationError) : PayloadValidationResult
}
