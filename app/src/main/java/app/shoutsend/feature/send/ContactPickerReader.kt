package app.shoutsend.feature.send

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

/**
 * Resolved contact entity from system contact picker.
 */
data class PickedContactData(
    val displayName: String = "",
    val phones: List<String> = emptyList(),
    val email: String? = null,
    val readError: String? = null
)

/**
 * Safe reader for content URIs returned by ActivityResultContracts.PickContact.
 * Reads contact name, up to 2 phone numbers, and 1 email using temporary read URI permissions.
 * Never requests READ_CONTACTS permission (Rule 24).
 * Never reads contact photos.
 */
object ContactPickerReader {

    fun read(context: Context, contactUri: Uri): PickedContactData {
        var name = ""
        val phones = mutableListOf<String>()
        var email: String? = null

        try {
            // 1. Query display name directly from the picked contact URI
            context.contentResolver.query(
                contactUri,
                arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex) ?: ""
                    }
                }
            }

            // 2. Query Data directory under the picked contact URI using temporary grant (no READ_CONTACTS required)
            val dataUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Data.CONTENT_DIRECTORY)
            context.contentResolver.query(
                dataUri,
                arrayOf(
                    ContactsContract.Data.MIMETYPE,
                    ContactsContract.Data.DATA1,
                    ContactsContract.Contacts.DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val mimeIndex = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)
                val dataIndex = cursor.getColumnIndex(ContactsContract.Data.DATA1)
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    if (name.isEmpty() && nameIndex != -1) {
                        name = cursor.getString(nameIndex) ?: ""
                    }
                    val mime = if (mimeIndex != -1) cursor.getString(mimeIndex) else null
                    val data = if (dataIndex != -1) cursor.getString(dataIndex)?.trim() else null

                    if (!data.isNullOrEmpty()) {
                        when (mime) {
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                                if (phones.size < 2 && !phones.contains(data)) {
                                    phones.add(data)
                                }
                            }
                            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE -> {
                                if (email == null) {
                                    email = data
                                }
                            }
                        }
                    }
                }
            }

            return PickedContactData(
                displayName = name,
                phones = phones,
                email = email
            )
        } catch (e: SecurityException) {
            return PickedContactData(
                displayName = name,
                readError = "Contact details restricted without permissions. Please enter numbers manually."
            )
        } catch (e: Exception) {
            return PickedContactData(
                displayName = name,
                readError = "Could not read contact: ${e.message}"
            )
        }
    }
}
