# Step 23 Verification Report: Benchmark and Preset Tuning in Simulation

## 1. Overview
Executed the comprehensive simulation benchmark sweep across 42 evaluation cells (7 modem profile configurations $\times$ 6 acoustic channel scenarios) using the deterministic channel impairment pipeline (`AcousticChannelSimulator` and `LoopbackHarness`). This step validates the physical layer protocol constants, measures empirical net throughput with 95% confidence intervals, and confirms the preset recommendations for the user interface.

## 2. Benchmark Summary
- Total cells evaluated: 42 (10 trials each with fixed deterministic pseudo-random seeds 5000L..5009L).
- Scenarios tested:
  - `CLEAN`: Ideal noiseless transmission.
  - `CLOSE_RANGE`: Line-of-sight desktop communication (<0.5m).
  - `LIVING_ROOM`: Realistic multi-path reverberation with domestic appliances.
  - `QUIET_ROOM`: Domestic room with low ambient noise and moderate reverberation.
  - `CAFE`: High ambient conversational babble and pink noise (target SNR 12 dB).
  - `WEAK_ULTRASOUND_PHONE`: Transceiver hardware exhibiting steep high-frequency roll-off above 16 kHz.

## 3. Results Highlights
| Profile | Scenarios with 100% Reliability | Net Throughput | Best Use Case |
|---|---|---|---|
| `RETRO / MFSK16 / NORMAL` | ALL 6 scenarios (100.0% in Clean, Close, Living, Quiet, Cafe, Weak Phone) | 2.8 B/s | **Universal Default** across all environments |
| `RETRO / MFSK16 / FAST` | Clean, Close Range, Living Room, Quiet Room, Weak Phone (80% Cafe) | 12.3 - 12.5 B/s | High-speed audible line-of-sight |
| `RETRO / DTFSK / FAST` | Clean, Close Range, Living Room, Quiet Room, Weak Phone (90% Cafe) | 9.9 - 10.0 B/s | High-speed multi-tone transfer |
| `RETRO / MFSK16 / ROBUST` | Clean, Close Range, Living Room, Quiet Room, Weak Phone (90% Cafe) | 1.4 B/s | Maximum resilience under extreme ambient noise |
| `STEALTH / MFSK16 / NORMAL` | Clean, Close Range, Weak Phone (attenuates in large reflective rooms) | 2.8 B/s | Silent close-proximity transfer (<0.5m) |

## 4. Architectural & Protocol Decisions Confirmed
1. **Retain MFSK16 as Universal Default**: 1-tone-per-symbol MFSK16 maintains flawless detection through severe multi-path room echoes (100% in living room and cafe).
2. **Retain DTFSK for High Speed**: DTFSK achieves up to 10.0 B/s net throughput, performing well in direct line-of-sight conditions.
3. **Symbol Timings & Ramps**: 30 ms Normal (4 ms ramp), 20 ms Fast (3 ms ramp), and 60 ms Robust (6 ms ramp) confirmed.
4. **Hardware Qualification Verification**: `WEAK_ULTRASOUND_PHONE` correctly achieves 100% in Retro band while failing ultrasound stealth benchmarks, justifying the Step 29 capability calibration recommendation hierarchy.

## 5. UI Implications
- The UI exposes three intuitive preset choices:
  - **Fast (20ms)**: ~3.5s for 44-byte benchmark payload (fits in 1 frame, 1 repeat).
  - **Normal (30ms)**: ~15.9s for 44-byte benchmark payload (splits into 2 frames, 2 repeats = 4 on-air frames). For payloads <= 42 bytes (1 frame, 2 repeats), duration is ~8.5s.
  - **Robust (60ms)**: ~30.8s for 44-byte benchmark payload (splits into 2 frames, 2 repeats).

## 6. Throughput Discrepancy Analysis (Normal 2.8 B/s vs Fast 12.5 B/s)
The measured benchmark throughput difference between Normal (2.8 B/s) and Fast (12.3-12.5 B/s) is explained by three multiplicative physical factors:
1. **Reed-Solomon Codeword Capacity Threshold**:
   - The benchmark payload is 44 bytes.
   - Fast uses `RsProfile.LIGHT` ($k=54$), offering $54 - 6 = 48$ bytes of frame payload capacity. The 44-byte payload fits into **1 single frame**.
   - Normal uses `RsProfile.STANDARD` ($k=48$), offering $48 - 6 = 42$ bytes of frame payload capacity. Because $44 > 42$, it splits into **2 frames**.
2. **Repetition Multiplier**:
   - Fast uses `repeats = 1` $\implies 1 \times 1 = 1$ physical frame transmitted on air.
   - Normal uses `repeats = 2` $\implies 2 \times 2 = 4$ physical frames transmitted on air ($4\times$ the frame volume of Fast).
3. **Symbol Duration**:
   - Fast uses 20 ms symbols ($124 \times 20\text{ ms} = 2.48\text{ s}$ per frame).
   - Normal uses 30 ms symbols ($124 \times 30\text{ ms} = 3.72\text{ s}$ per frame).
4. **Total On-Air Time & Net Throughput**:
   - Fast total on-air time: $1.04\text{ s (preamble/silences)} + 1 \times 2.48\text{ s} = 3.52\text{ s} \implies 44 / 3.52 = \mathbf{12.5\text{ B/s}}$.
   - Normal total on-air time: $1.04\text{ s (preamble/silences)} + 4 \times 3.72\text{ s} = 15.92\text{ s} \implies 44 / 15.92 = \mathbf{2.8\text{ B/s}}$.

