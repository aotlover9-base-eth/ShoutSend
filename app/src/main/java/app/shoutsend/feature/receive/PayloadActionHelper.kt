package app.shoutsend.feature.receive

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import android.provider.ContactsContract
import android.provider.Settings
import app.shoutsend.modem.payload.Payload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URI

/**
 * Helper providing safe, permission-minimal Android intents and clipboard operations
 * for incoming acoustic payload action cards (Step 42).
 *
 * Security and privacy rules:
 * - Never auto-execute actions (links, dials, or contacts).
 * - Mark Wi-Fi passwords as sensitive on Android 13+ (ClipDescription.EXTRA_IS_SENSITIVE).
 * - Auto-clear sensitive clipboard contents after 60 seconds.
 * - Zero extra permissions required (uses ACTION_DIAL instead of ACTION_CALL, ACTION_SENDTO for SMS,
 *   ACTION_INSERT for Contacts, and ACTION_WIFI_SETTINGS for Wi-Fi).
 * - Never log sensitive payload contents (SSIDs, passwords, contact records).
 */
object PayloadActionHelper {

    /**
     * Copies text to the system clipboard, optionally marking it sensitive and scheduling auto-clear.
     */
    fun copyToClipboard(
        context: Context,
        text: String,
        label: String = "ShoutSend",
        isSensitive: Boolean = false,
        scope: CoroutineScope? = null,
        autoClearSeconds: Long = 60L
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clipData = ClipData.newPlainText(label, text)

        if (isSensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
            clipData.description.extras = extras
        }

        clipboard.setPrimaryClip(clipData)

        if (isSensitive && autoClearSeconds > 0 && scope != null) {
            scope.launch {
                delay(autoClearSeconds * 1000L)
                try {
                    if (clipboard.hasPrimaryClip()) {
                        val currentText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                        if (currentText == text) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                clipboard.clearPrimaryClip()
                            } else {
                                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Ignored on headless or restricted environments
                }
            }
        }
    }

    /**
     * Sanitizes a phone number for dialer or SMS intent, retaining digits, '+', '*', and '#'.
     */
    fun sanitizePhoneNumber(phone: String): String {
        return phone.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
    }

    /**
     * Formats a clean plaintext summary of a contact card suitable for clipboard or sharing.
     */
    fun formatContactSummary(contact: Payload.Contact): String {
        val parts = mutableListOf<String>()
        if (contact.displayName.isNotBlank()) {
            parts.add(contact.displayName)
        }
        parts.addAll(contact.phones)
        val email = contact.email
        if (!email.isNullOrBlank()) {
            parts.add(email)
        }
        return parts.joinToString("\n").trim()
    }

    /**
     * Creates an intent to open Android's Wi-Fi network settings screen.
     */
    fun createWifiSettingsIntent(): Intent {
        return Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Creates an intent to insert a received contact into the phone's Contacts app.
     * Pre-fills name, phone numbers, and email without requesting contact write permissions.
     */
    fun createSaveContactIntent(contact: Payload.Contact): Intent {
        return Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.NAME, contact.displayName)
            if (contact.phones.isNotEmpty()) {
                putExtra(ContactsContract.Intents.Insert.PHONE, contact.phones[0])
            }
            if (contact.phones.size > 1) {
                putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, contact.phones[1])
            }
            val email = contact.email
            if (!email.isNullOrBlank()) {
                putExtra(ContactsContract.Intents.Insert.EMAIL, email)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Creates an intent to open the phone dialer pre-filled with a telephone number.
     * Uses ACTION_DIAL so the user explicitly initiates the call (zero CALL_PHONE permission needed).
     */
    fun createDialerIntent(phone: String): Intent {
        val sanitized = sanitizePhoneNumber(phone)
        return Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitized")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Creates an intent to open the SMS compose app pre-filled with a recipient.
     * Uses ACTION_SENDTO with smsto: scheme (zero SEND_SMS permission needed).
     */
    fun createSmsIntent(phone: String): Intent {
        val sanitized = sanitizePhoneNumber(phone)
        return Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$sanitized")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Creates an intent to open an email compose client.
     */
    fun createEmailIntent(email: String): Intent {
        return Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Creates an intent to open a web URL in the external browser.
     */
    fun createBrowserIntent(url: String): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Creates a system chooser intent to share plaintext across external apps.
     */
    fun createShareIntent(text: String, chooserTitle: String? = null): Intent {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        return Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Structured decomposition of a URL for secure visual presentation.
     */
    data class ParsedUrl(
        val scheme: String,
        val host: String,
        val pathAndQuery: String,
        val isHttps: Boolean
    )

    /**
     * Parses a raw URL string into host, scheme, and path components to allow safe domain highlighting.
     */
    fun parseUrl(rawUrl: String): ParsedUrl {
        val trimmed = rawUrl.trim()
        return try {
            val uri = URI(trimmed)
            val scheme = (uri.scheme ?: "http").lowercase()
            val host = uri.host ?: trimmed
            val path = trimmed.substringAfter(host, "")
            ParsedUrl(
                scheme = scheme,
                host = host,
                pathAndQuery = path,
                isHttps = scheme == "https"
            )
        } catch (_: Exception) {
            ParsedUrl(
                scheme = "http",
                host = trimmed,
                pathAndQuery = "",
                isHttps = false
            )
        }
    }
}
