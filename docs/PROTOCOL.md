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

## Change Log

### 2026-10-22 - Step 6: Initial Protocol v1 Baseline
- Adopted Protocol v1 specification verbatim from Section 6 of `SHOUTSEND_ROADMAP.md`.
- Pure Kotlin JVM module `:modem` established.


### 2026-10-22 - Step 7: Acoustic Profiles & Physical Constants Formalized
- Specified exact sync word `[3, 11, 7, 14, 1, 9, 5, 12]` with proven zero side-lobes.
- Specified exact frame marker `[13, 2, 10, 4]`.
- Defined 8-bit mode block bitfield with orthogonal 2-bit parity checksum for 100% single-bit error detection.
- Documented full tone frequency tables for Stealth (17.6-20.0 kHz) and Retro (1.5-3.5 kHz) bands at 48 kHz and 44.1 kHz.