package app.shoutsend.modem.payload

import java.io.ByteArrayOutputStream

/**
 * Compact binary encoder and decoder for Wi-Fi credentials.
 */
public object WifiCompactor {

    /**
     * Compacts [wifi] into binary form.
     */
    public fun compact(wifi: Payload.WiFi): ByteArray {
        val out = ByteArrayOutputStream()

        val ssid = wifi.ssid
        val ssidIs7Bit = SevenBitPacker.is7BitAscii(ssid)

        val pass = wifi.password
        val hasPass = !pass.isNullOrEmpty() && wifi.security != WifiSecurity.OPEN
        val passIs7Bit = if (hasPass && pass != null) SevenBitPacker.is7BitAscii(pass) else false

        val secBits = when (wifi.security) {
            WifiSecurity.OPEN -> 0
            WifiSecurity.WPA2 -> 1
            WifiSecurity.WPA3 -> 2
        }

        var flags = secBits and 0x03
        if (wifi.isHidden) flags = flags or 0x04
        if (hasPass) flags = flags or 0x08
        if (ssidIs7Bit) flags = flags or 0x10
        if (passIs7Bit) flags = flags or 0x20

        out.write(flags)

        // SSID
        if (ssidIs7Bit) {
            out.write(ssid.length)
            val packed = SevenBitPacker.packRaw(ssid)
            out.write(packed)
        } else {
            val raw = ssid.toByteArray(Charsets.UTF_8)
            out.write(raw.size)
            out.write(raw)
        }

        // Password
        if (hasPass && pass != null) {
            if (passIs7Bit) {
                out.write(pass.length)
                val packed = SevenBitPacker.packRaw(pass)
                out.write(packed)
            } else {
                val raw = pass.toByteArray(Charsets.UTF_8)
                out.write(raw.size)
                out.write(raw)
            }
        }

        return out.toByteArray()
    }

    /**
     * Expands compact binary Wi-Fi bytes.
     * Returns null if malformed.
     */
    public fun expand(bytes: ByteArray): Payload.WiFi? {
        if (bytes.isEmpty()) return null

        var offset = 0
        val flags = bytes[offset++].toInt() and 0xFF

        val security = when (flags and 0x03) {
            0 -> WifiSecurity.OPEN
            1 -> WifiSecurity.WPA2
            2 -> WifiSecurity.WPA3
            else -> return null
        }
        val isHidden = (flags and 0x04) != 0
        val hasPass = (flags and 0x08) != 0
        val ssidIs7Bit = (flags and 0x10) != 0
        val passIs7Bit = (flags and 0x20) != 0

        // SSID
        if (offset >= bytes.size) return null
        val ssidLen = bytes[offset++].toInt() and 0xFF
        val ssid: String = if (ssidIs7Bit) {
            val needed = (ssidLen * 7 + 7) / 8
            if (offset + needed > bytes.size) return null
            val s = SevenBitPacker.unpackRaw(bytes, offset, ssidLen) ?: return null
            offset += needed
            s
        } else {
            if (offset + ssidLen > bytes.size) return null
            val s = String(bytes, offset, ssidLen, Charsets.UTF_8)
            offset += ssidLen
            s
        }

        // Password
        var password: String? = null
        if (hasPass) {
            if (offset >= bytes.size) return null
            val passLen = bytes[offset++].toInt() and 0xFF
            password = if (passIs7Bit) {
                val needed = (passLen * 7 + 7) / 8
                if (offset + needed > bytes.size) return null
                val p = SevenBitPacker.unpackRaw(bytes, offset, passLen) ?: return null
                offset += needed
                p
            } else {
                if (offset + passLen > bytes.size) return null
                val p = String(bytes, offset, passLen, Charsets.UTF_8)
                offset += passLen
                p
            }
        }

        val wifi = Payload.WiFi(
            ssid = ssid,
            password = password,
            security = security,
            isHidden = isHidden
        )
        return if (wifi.validate() is PayloadValidationResult.Valid) wifi else null
    }
}
