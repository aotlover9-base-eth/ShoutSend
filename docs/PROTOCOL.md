# ShoutSend Acoustic Protocol Specification

## 6. Protocol v1 (the contract every DSP step follows)

**Audio:** mono, 16-bit PCM, 48 kHz preferred and 44.1 kHz fallback. Tone frequencies are computed from the actual sample rate. Goertzel coefficients are computed for exact frequencies, so tones do not need to sit on integer FFT bins.

**Tone plans**

| Mode | Data tones (16) | Sync tones |
| --- | --- | --- |
| Stealth | 17,600 Hz upward in 150 Hz steps, ending at 19,850 Hz | S0 = 17,450 Hz, S1 = 20,000 Hz |
| Retro | 1,500 Hz upward in 125 Hz steps, ending at 3,375 Hz | S0 = 1,375 Hz, S1 = 3,500 Hz |

**Schemes**

| Scheme | Bits per symbol | How |
| --- | --- | --- |
| MFSK16 | 4 | One tone out of 16 per symbol |
| DTFSK | 6 | Two tones at once: one of the lower 8 tones and one of the upper 8, each at half amplitude. The receiver decides each group separately |

**Speed presets (starting values, Step 23 tunes them in simulation)**

| Preset | Symbol length | Raised-cosine edge |
| --- | --- | --- |
| Robust | 60 ms | 6 ms each side |
| Normal | 30 ms | 4 ms each side |
| Fast | 20 ms | 3 ms each side |

**Raw and net data rate (before field testing)**

| Scheme and preset | Raw bytes per second | Approx. net after Reed-Solomon, header and CRC (about 70 percent) |
| --- | --- | --- |
| MFSK16 Robust | 8.3 | 5.8 |
| MFSK16 Normal | 16.7 | 11.7 |
| MFSK16 Fast | 25 | 17.5 |
| DTFSK Robust | 12.5 | 8.8 |
| DTFSK Normal | 25 | 17.5 |
| DTFSK Fast | 37.5 | 26 |

**Transmission layout**

1. **Preamble:** 16 symbols alternating S0 and S1 at full amplitude. It lets the receiver detect energy, estimate the noise floor and lock timing.
2. **Sync word:** 8 symbols of a fixed pseudo-random MFSK pattern, for exact symbol alignment.
3. **Mode block:** 2 symbols declaring scheme, speed preset and RS profile, repeated once for safety. Preamble, sync word and mode block always use MFSK16 at 30 ms, so any receiver can lock without negotiation.
4. **Frames:** each frame has a 4-symbol frame marker (fixed pattern at the active timing), then one interleaved Reed-Solomon codeword.
5. **Repeats:** the whole frame list is sent R times (default 2, setting 1 to 3). The receiver keeps every frame it decodes, ignores duplicates, and finishes the moment all fragments are present.

**Frame content (before error correction)**

| Byte | Content |
| --- | --- |
| 0 | version (2 bits, value 1), type (3 bits: 0 text, 1 URL, 2 Wi-Fi, 3 contact, 4 to 7 reserved), flags (3 bits: encrypted, compact, reserved) |
| 1 | message id (random byte, groups the fragments of one message) |
| 2 | fragment index (high 4 bits) and fragment count (low 4 bits) |
| 3 | length of the payload bytes in this frame |
| 4 to end | payload bytes, zero padded |
| last 2 | CRC-16/CCITT-FALSE over bytes 0 to the last payload byte |

**Error correction:** Reed-Solomon over GF(256), primitive polynomial 0x11D, shortened codeword. Default RS(60, 48): 48 data bytes, 12 parity bytes, corrects up to 6 wrong bytes, or up to 12 erasures, or any mix where 2 times the unknown errors plus the known erasures is at most 12. The demodulator marks low-confidence symbols as erasures. Before mapping to tones, codeword bytes are whitened with an LFSR sequence so long runs of one tone are avoided. Echo bursts are deliberately kept inside few adjacent bytes (byte-oriented Reed-Solomon handles that best), and the receiver turns low-confidence symbols into byte erasures.

**Limits:** at most 16 fragments, 42 payload bytes per fragment, 320 bytes on air per message, so v1 messages are short on purpose.

---


### Physical Layer Constants (Step 7)

#### Sync Word Pattern
- **Length**: 8 MFSK16 symbols
- **Pattern**: `[3, 11, 7, 14, 1, 9, 5, 12]`
- **Properties**: All 8 tone indices are distinct; aperiodic autocorrelation matches are 8 at lag 0 and 0 at all non-zero lags.

#### Frame Marker Pattern
- **Length**: 4 MFSK16 symbols
- **Pattern**: `[13, 2, 10, 4]`
- **Properties**: Zero tone collision with the sync word.

#### Mode Block Bit Layout
- **Encoding**: 8-bit mode byte encoded into 2 MFSK16 symbols (high nibble, low nibble), repeated once (4 symbols total), sent at MFSK16 Normal (30ms) timing.
- **Bit Fields**:
  - `Bits 7..6`: Modulation Scheme (`00` = MFSK16, `01` = DTFSK, `10..11` reserved)
  - `Bits 5..4`: Speed Preset (`00` = Robust, `01` = Normal, `10` = Fast, `11` reserved)
  - `Bits 3..2`: Reed-Solomon Profile (`00` = RS(60,48), `01` = RS(40,28), `10` = RS(24,16), `11` reserved)
  - `Bits 1..0`: 2-bit Orthogonal Parity Checksum:
    - `Bit 1` ($P_1$): $b_7 \oplus b_5 \oplus b_3$
    - `Bit 0` ($P_0$): $b_6 \oplus b_4 \oplus b_2$
    - Guarantees 100% detection of all 8 single-bit error patterns.


### Bit Manipulation, CRC, and Whitening (Step 8)

#### CRC-16 Specification
- **Algorithm**: CRC-16/CCITT-FALSE
- **Polynomial**: `0x1021` ($x^{16} + x^{12} + x^5 + 1$)
- **Initial Value**: `0xFFFF`
- **Reflection**: None (`refIn = false`, `refOut = false`)
- **Final XOR**: `0x0000`
- **Known Answer**: `"123456789"` -> `0x29B1`

#### PN9 Whitening Specification
- **Polynomial**: $x^9 + x^5 + 1$ (LFSR mask `0x1FF`)
- **Seed Mapping**: State $S_0 = ((	ext{seed} \ \& \ 0x0F) \ll 5) \oplus 0x1FF$
- **Feedback**: $	ext{bit}_8 \oplus 	ext{bit}_4$
- **Operation**: Involutory bitwise XOR. Guaranteed max run of identical symbols $\le 3$ (strictly below 6) on all-zero payloads.


### Reed-Solomon Error Correction Codec (Step 9)

#### Galois Field Specification
- **Field**: $GF(2^8)$
- **Primitive Polynomial**: `0x11D` ($x^8 + x^4 + x^3 + x^2 + 1$)
- **Primitive Element**: $lpha = 2$ ($0x02$)
- **Lookup Tables**: 512-entry exponent table, 256-entry logarithm table.

#### Generator Polynomials & Profiles
- **Generator Form**: $g(x) = \prod_{i=0}^{nsym-1} (x - lpha^i) = \prod_{i=0}^{nsym-1} (x + lpha^i)$ (first root $lpha^0 = 1$)
- **Profiles**:
  - `LIGHT` (ID 0): $RS(60, 54)$ with $nsym = 6$ ($t = 3$ errors)
  - `STANDARD` (ID 1): $RS(60, 48)$ with $nsym = 12$ ($t = 6$ errors) [Default]
  - `STRONG` (ID 2): $RS(60, 40)$ with $nsym = 20$ ($t = 10$ errors)
- **Shortening**: Leading zeros are omitted from transmission; mathematically identical parity and syndromes.


### Reed-Solomon Decoder Specification (Step 10)

#### Algorithm & Capacity
- **Variant**: Berlekamp-Massey initialized from erasure locator $\Lambda_{erasures}(x)$ on syndromes $S_i$, Chien root search, and Forney algorithm with $X_l$ scaling for generator roots starting at $lpha^0$.
- **Capacity**: Corrects any error pattern satisfying:
  $$2 \cdot v + \mu \le nsym$$
  where $v$ is the count of unknown byte errors and $\mu$ is the count of known byte erasures.
- **Integrity**: 
  - Degree match validation between Chien roots and locator polynomial degree.
  - Syndrome re-evaluation over corrected codeword before release.
  - Measured miscorrection rate on beyond-capacity codewords: **0.0000%** (0 false corrections out of 10,000 trials).


### Codeword Symbolizer & Erasure Mapping (Step 11)

#### Symbol Mapping Architecture
- **Whitening**: Bytes are XOR-whitened with the PN9 LFSR sequence initialized by `frameSeed` prior to symbol mapping, avoiding repetitive acoustic tone sequences.
- **MFSK16**: 4 bits per symbol. Each byte maps to exactly 2 symbols ($s_0 = \text{high nibble}, s_1 = \text{low nibble}$). 60-byte codeword -> 120 symbols.
- **DTFSK**: 6 bits per symbol (3 bits low group tone 0..7, 3 bits high group tone 8..15). 4 symbols cover exactly 3 bytes (24 bits). 60-byte codeword -> 80 symbols.
- **Symbol-to-Byte Erasure Mapping**:
  - Symbol $s$ spans bit range $[6s, 6s + 5]$.
  - Affected byte range: $[\lfloor 6s / 8 \rfloor, \lfloor (6s + 5) / 8 \rfloor]$.
  - When demodulator symbol confidence is below threshold ($0.5$), all bytes spanned by that symbol are flagged as Reed-Solomon erasures.

#### Intra-Codeword Interleaving Rejection
- Interleaving symbols within an individual Reed-Solomon codeword is strictly rejected.
- Acoustic noise bursts and echo corrupt consecutive symbols. With contiguous mapping, an $L$-symbol burst damages at most $\lceil L \cdot \text{bitsPerSymbol} / 8 \rceil + 1$ bytes.
- Interleaving would disperse these errors across many non-adjacent bytes, vastly increasing the number of corrupted bytes and causing decoding failure. Burst robustness is instead achieved through short frames, PN9 whitening, multi-frame repeat transmissions, and symbol-level erasure marking.

### Transport Framing & Packet Protocol (Step 12)

#### Frame Content Layout (Pre-FEC)
For an RS profile with data parameter $k$ ($k=48$ for STANDARD, $k=54$ for LIGHT, $k=40$ for STRONG):
- **Byte 0**:
  - `Bits 7..6`: Version (fixed value `1` = `0b01`)
  - `Bits 5..3`: Message Type (0: TEXT, 1: URL, 2: WIFI, 3: CONTACT; 4..7 reserved)
  - `Bits 2..0`: Message Flags (Bit 2: encrypted, Bit 1: compact, Bit 0: reserved)
- **Byte 1**: Message ID ($0 \dots 255$, randomly generated per message)
- **Byte 2**:
  - `Bits 7..4`: Fragment Index ($0 \dots 15$)
  - `Bits 3..0`: Fragment Count minus 1 ($0 \dots 15$, representing count $1 \dots 16$)
- **Byte 3**: Payload Length $L \in [0, k - 6]$
- **Bytes 4 to $4 + L - 1$**: Payload slice
- **Bytes $4 + L$ to $k - 3$**: Zero padding
- **Bytes $k - 2$ to $k - 1$**: CRC-16/CCITT-FALSE (big-endian) computed over bytes $0 \dots 4 + L - 1$ (header + payload)

#### Frame Decoding Validation Rules & Errors
Decoded $k$-byte blocks are validated in sequence:
1. `VersionMismatch`: Byte 0 bits 7..6 != 1.
2. `UnknownMessageType`: Byte 0 bits 5..3 not in 0..3.
3. `BadLength`: Byte 3 payload length $> k - 6$.
4. `BadCrc`: Suffix CRC-16 != computed CRC over bytes $0 \dots 4 + L - 1$.
5. `BadFragmentField`: Fragment count not in $1 \dots 16$, or fragment index $\ge$ fragment count.

#### Air-Time Duration Formula
$$T_{\text{air}} = T_{\text{header}} + (\text{frameCount} \cdot \text{repeats} \cdot T_{\text{frame}})$$
where:
- $T_{\text{header}} = (16 + 8 + 4) \cdot 30\text{ ms} = 840\text{ ms} = 0.84\text{ s}$
- $T_{\text{frame}} = (4 + \text{codewordSymbols}) \cdot T_{\text{symbol}}$
- $\text{codewordSymbols} = \frac{n \cdot 8}{\text{bitsPerSymbol}}$ (120 for MFSK16, 80 for DTFSK on $n=60$)
- $T_{\text{symbol}}$: 60 ms (Robust), 30 ms (Normal), 20 ms (Fast)

### Application Payload Codecs (Step 13)

#### Wire Encodings by MessageType
1. **TEXT (Type 0)**:
   - Raw UTF-8 bytes (1..280 bytes).
2. **URL (Type 1)**:
   - Raw UTF-8 string (1..280 bytes).
   - Scheme restricted strictly to `http` and `https`. Control characters forbidden; valid hostname required.
3. **WIFI (Type 2)**:
   - Standard Wi-Fi QR string: `WIFI:T:<WPA|nopass>;S:<ssid>;P:<password>;H:<true|false>;;`
   - Special characters `\`, `;`, `,`, `:`, `"` escaped with `\`.
   - SSID: 1..32 UTF-8 bytes.
   - WPA passphrase: 8..63 printable ASCII characters or exactly 64 hex characters.
   - Open networks require null/empty password.
4. **CONTACT (Type 3)**:
   - Binary Tag-Length-Value (TLV) encoding:
     - Tag `0x01`: Display Name (1..60 bytes)
     - Tag `0x02`: Phone Number (up to 2 entries, 1..20 chars each from `[0-9+\-() ]`)
     - Tag `0x03`: Email Address (optional, 1..80 bytes)

### Payload Compaction Specifications (Step 14)

#### URL Dictionary Tables
- **Prefix Table (16 entries, 1 byte index)**:
  `0: https://www.`, `1: https://`, `2: http://www.`, `3: http://`, `4: https://maps.google.com/?q=`, `5: https://maps.app.goo.gl/`, `6: https://github.com/`, `7: https://t.me/`, `8: https://wa.me/`, `9: https://x.com/`, `10: https://instagram.com/`, `11: https://youtube.com/watch?v=`, `12: https://youtu.be/`, `13: https://linkedin.com/in/`, `14: https://spotify.link/`, `15: ""` (empty/raw).
- **Suffix Table (16 entries, 1 byte index)**:
  `0: ""`, `1: /`, `2: .com`, `3: .com/`, `4: .org`, `5: .org/`, `6: .net`, `7: .net/`, `8: .io`, `9: .io/`, `10: .in`, `11: .in/`, `12: .co`, `13: .co/`, `14: .html`, `15: .php`.
- **Wire Format**: `[PrefixByte (1B)] [SuffixByte (1B)] [MiddleUTF-8 (N bytes)]`

#### Text 7-Bit ASCII Packing
- Used only when all characters are ASCII (0..127).
- Format: `[CharCount (2B big-endian)] [Dense 7-bit packed bitstream]`

#### Compact Wi-Fi Binary Format
- Format: `[Flags (1B)] [SSID Length (1B)] [SSID Data] [Pass Length (1B)]? [Pass Data]?`
- Flags bitfield:
  - `Bits 1..0`: Security (0: OPEN, 1: WPA2, 2: WPA3)
  - `Bit 2`: isHidden
  - `Bit 3`: hasPassword
  - `Bit 4`: ssidIs7BitAscii
  - `Bit 5`: passIs7BitAscii

#### Contact Compact TLV
- Replaces raw UTF-8 phone numbers with 4-bit BCD (Tag `0x22`) for digit-only phones, or 7-bit packed ASCII (Tag `0x12`).

### Secure Mode Cryptography (Step 15)

#### Cryptographic Suite & Parameters
- **Key Derivation (KDF)**:
  - Algorithm: `PBKDF2WithHmacSHA256`
  - Salt: 4 bytes cryptographically random entropy
  - Iterations: 60,000 rounds
  - Derived Key Length: 256 bits (32 bytes)
- **Authenticated Encryption (AEAD)**:
  - Cipher: `AES-256-GCM` (`AES/GCM/NoPadding`)
  - Nonce (IV): 12 bytes random entropy
  - Tag Length: 96 bits (12 bytes)
  - Associated Authenticated Data (AAD): `[Version Byte (0x01), MessageType ID Byte]`
- **On-Air Wire Container Layout**:
  `[Salt (4B)] [Nonce (12B)] [Ciphertext (N bytes)] [GCM Auth Tag (12B)]`
  - Fixed Overhead: exactly 28 bytes.
  - Frame Header Flag: Bit 2 (`isEncrypted`) set to 1.
  - Compaction precedes encryption (`Payload -> Compact -> Seal`).

## Change Log

### 2026-10-22 - Step 6: Initial Protocol v1 Baseline
- Adopted Protocol v1 specification verbatim from Section 6 of `SHOUTSEND_ROADMAP.md`.
- Pure Kotlin JVM module `:modem` established.


### 2026-10-22 - Step 7: Acoustic Profiles & Physical Constants Formalized
- Specified exact sync word `[3, 11, 7, 14, 1, 9, 5, 12]` with proven zero side-lobes.
- Specified exact frame marker `[13, 2, 10, 4]`.
- Defined 8-bit mode block bitfield with orthogonal 2-bit parity checksum for 100% single-bit error detection.
- Documented full tone frequency tables for Stealth (17.6-20.0 kHz) and Retro (1.5-3.5 kHz) bands at 48 kHz and 44.1 kHz.

### 2026-10-22 - Step 8: Bit Serialization, CRC-16, and Whitening Formalized
- Standardized table-driven CRC-16/CCITT-FALSE with verified 0x29B1 test vector.
- Formally specified PN9 LFSR ($x^9 + x^5 + 1$) whitening sequence with confirmed statistical uniformity.

### 2026-10-22 - Step 9: Reed-Solomon GF(256) and Systematic Encoder Formalized
- Specified primitive polynomial 0x11D and generator polynomial roots alpha^0..alpha^(nsym-1).
- Defined three RS profiles (LIGHT, STANDARD, STRONG) with designated mode block IDs 0, 1, and 2.
- Implemented zero-allocation systematic encoder verified with 100% zero-syndrome tests.

### 2026-10-22 - Step 10: Errors-and-Erasures Decoder Formalized
- Standardized Berlekamp-Massey + Forney errors-and-erasures decoding pipeline.
- Verified 100% recovery up to capacity limit and 0.0000% miscorrection rate in 10,000 stress trials.

### 2026-10-22 - Step 11: Codeword Symbolizer & Erasure Mapping Formalized
- Implemented `CodewordSymbolizer` with PN9 whitening and DTFSK/MFSK16 bit packing.
- Formalized and proved symbol-to-byte erasure mapping bounds ($\le \lceil L \cdot \text{bitsPerSymbol} / 8 \rceil + 1$).
- Formally documented the rejection rationale for intra-codeword interleaving.

### 2026-10-22 - Step 12: Packet Format, Fragmentation, and Reassembly Formalized
- Standardized 4-byte frame header, payload slicing, CRC-16 check, and typed decoder errors.
- Implemented `Fragmenter` with `MAX_AIR_BYTES = 320` and `MAX_FRAGMENTS = 16`.
- Implemented `MessageAssembler` with duplicate suppression, stream conflict auto-reset, and injected clock timeout.
- Implemented `AirTime` estimator with proven hand-calculated test vectors.

### 2026-10-23 - Step 13: Payload Codecs and Wire Formats Formalized
- Specified canonical payload wire representations for Text, Url, Wi-Fi (QR-format), and Contact (binary TLV).
- Formalized URL safety analysis heuristics.

### 2026-10-23 - Step 14: Payload Compaction Formats and Dictionaries Formalized
- Formalized 16-entry URL prefix and suffix dictionary tables.
- Standardized 7-bit ASCII bit packing and 4-bit BCD contact phone encoding.
- Formally specified strict shortest-representation chooser logic.

### 2026-10-23 - Step 15: Secure Mode Cryptography Formalized
- Formalized PBKDF2WithHmacSHA256 (60,000 iterations) and AES-256-GCM AEAD encryption pipeline.
- Bound Version and MessageType to GCM AAD and verified exactly 28-byte on-air wire overhead.

### 2026-10-23 - Step 16: Tone Synthesis and Windowing Formalized
- **Absolute-Phase Continuous Oscillators**: Every tone sample is derived from absolute index $n$: $s(n) = \sin(2\pi \cdot f \cdot n / F_s)$. Guarantees continuous phase across repeated tones and makes audio rendering chunk-independent.
- **Hann Overlap Ramps**: Adjacent symbols overlap by $R$ samples. Outgoing symbol decays via $w_{\text{down}}(i) = \frac{1 + \cos(\pi (i + 0.5) / R)}{2}$; incoming symbol attacks via $w_{\text{up}}(i) = \frac{1 - \cos(\pi (i + 0.5) / R)}{2}$.
- **Envelope Invariant**: $w_{\text{up}}(i) + w_{\text{down}}(i) \equiv 1.0f$ for all $i \in [0, R - 1]$, ensuring $0.0$ dB amplitude dip across identical tone boundaries and eliminating transient clicks.
- **Chunk-Independence Invariant**: Audio rendering in chunks of 1, 7, 480, or 10,000 samples yields bit-identical outputs. Zero allocations on rendering hot path.





