# ShoutSend Field Test Plan (Acoustic Link Validation)

**Version:** 1.0  
**Target:** ShoutSend Acoustic Physical Layer Protocol v1  
**Execution Mode:** OWNER RUNS (Physical Handsets)  

---

## 1. Objectives & Testing Principles
Physical acoustic data communication depends heavily on speaker and microphone frequency responses, physical transducers, room reverberation, ambient noise spectra, and multipath reflections.

Because agents in simulation environments cannot hear or evaluate real-world room physics (Rule 23: *"Agents cannot hear. Never claim acoustic performance without channel-simulator numbers, and mark real-device behavior as OWNER RUNS"*), this field test plan specifies the exact physical test harness, procedures, and data capture matrices for physical Android hardware testing.

---

## 2. Test Hardware Matrix
Field testing requires a pair of physical Android devices running the `dev` or `user` flavor build:

| Device Role | Device Class | Representative Models | Speaker / Mic Characteristics |
| :--- | :--- | :--- | :--- |
| **Device A (Tx/Rx)** | Premium Flagship | Google Pixel 7/8/9, Samsung Galaxy S23/S24 | Wideband stereo transducers, flat response (100 Hz – 21 kHz), hardware AGC/AEC |
| **Device B (Tx/Rx)** | Budget / Mid-range | Motorola Moto G, Xiaomi Redmi, Samsung Galaxy A-series | Narrower bandwidth, roll-off above 16 kHz, plastic resonance, aggressive hardware noise gating |

---

## 3. Acoustic Environment Test Matrix

| Environment ID | Name | Ambient Noise Floor | Dominant Noise Characteristics |
| :--- | :--- | :--- | :--- |
| **ENV-1** | Anechoic / Quiet Room | < 35 dBA | Minimal background noise, gentle indoor air currents |
| **ENV-2** | Typical Living Room / Office | 45 – 55 dBA | HVAC hum, computer fans, distant voices, light wall reverberation |
| **ENV-3** | Busy Cafeteria / Coffee Shop | 65 – 75 dBA | Heavy speech babble (300 Hz – 3 kHz), clinking cutlery (high transient clicks), background music |
| **ENV-4** | Outdoor Street / Transit | 70 – 80 dBA | Low-frequency engine rumble (< 500 Hz), wind buffeting, sirens |

---

## 4. Test Configuration & Distance Matrix

For each environment (ENV-1 through ENV-4), perform bidirectional transfers (A -> B and B -> A) across:

| Distance | Orientation | Description |
| :--- | :--- | :--- |
| **0.1 m** | Desk Flat | Phones sitting side-by-side on a wooden desk |
| **0.5 m** | Direct Line of Sight | Held in hands at normal desk/conversation distance, speaker facing mic |
| **1.0 m** | Direct Line of Sight | Across a small coffee table |
| **2.0 m** | Angled (45 deg) | Across an office meeting desk, phones resting angled toward ceiling |
| **3.0 m** | Obstructed / Hand | Held naturally with palm partially cupping speaker or mic |

---

## 5. Modem Profiles Under Test

1. **Retro Normal (Universal Default)**:
   - Band: `Audible Retro` (1.5 kHz – 3.9 kHz, 16 tones)
   - Modulation: MFSK16, 30 ms symbol duration
   - Reed-Solomon: RS(60, 48), $t=6$ byte error correction
   - Target SNR: > 6.0 dB
2. **Retro Fast (High-Speed)**:
   - Band: `Audible Retro` (1.5 kHz – 3.9 kHz)
   - Modulation: MFSK16, 20 ms symbol duration
   - Reed-Solomon: RS(60, 54), $t=3$ byte error correction
   - Target SNR: > 10.0 dB
3. **Retro Robust (High-Noise)**:
   - Band: `Audible Retro` (1.5 kHz – 3.9 kHz)
   - Modulation: MFSK16, 45 ms symbol duration
   - Reed-Solomon: RS(60, 40), $t=10$ byte error correction
   - Target SNR: > 3.0 dB
4. **Stealth Normal (Near-Ultrasound)**:
   - Band: `Ultrasonic Stealth` (17.5 kHz – 19.9 kHz, 16 tones)
   - Modulation: MFSK16, 30 ms symbol duration
   - Reed-Solomon: RS(60, 48), $t=6$ byte error correction
   - Target SNR: > 8.0 dB
5. **Stealth Robust (Near-Ultrasound)**:
   - Band: `Ultrasonic Stealth` (17.5 kHz – 19.9 kHz)
   - Modulation: MFSK16, 45 ms symbol duration
   - Reed-Solomon: RS(60, 40), $t=10$ byte error correction
   - Target SNR: > 4.5 dB

---

## 6. Execution Procedure for Owner (Step-by-Step)

1. **Pre-Flight Device Preparation**:
   - Ensure media volume on Transmitter phone is set to 80% – 100%.
   - Ensure Receiver phone microphone is unmuted and no Bluetooth headset is connected.
   - Run the **Acoustic Calibration** tool (`Settings -> Diagnostics -> Calibration`) on both devices to verify transducer frequency response and establish baseline ambient noise floor.
2. **Execute Transmissions**:
   - Select payload (e.g. Text: *"Acoustic Link Test 2026"*, Wi-Fi credential, URL).
   - Press **Transmit** on Phone A and observe **Receive Waterfall** on Phone B.
   - Note down:
     - Synchronization lock acquisition latency.
     - Preamble SNR in dB displayed on the Signal Meter.
     - Instant action card appearance and RS repaired error count badge (*"✓ Noise detected · Auto-repaired"*).
3. **Data Recording**:
   - In Developer flavor (`devDebug`), open the Developer Panel -> **Field Test Log**.
   - Tap **Export Summary** to copy JSON/CSV test run summary to clipboard.
   - Compare results against pass criteria.

---

## 7. Pass / Acceptance Criteria
- **ENV-1 (Quiet Room, 0.1m - 1.0m)**: 100% packet success rate for all Retro and Stealth profiles.
- **ENV-2 (Office/Living Room, 0.5m - 2.0m)**: $\ge 98\%$ success rate for Retro Normal; $\ge 90\%$ for Stealth Normal.
- **ENV-3 (Busy Cafe, 0.5m - 1.5m)**: $\ge 95\%$ success rate for Retro Robust; $\ge 90\%$ for Retro Normal.
- **Transducer Latency**: Preamble sync acquisition $\le 450$ ms from audio start.
