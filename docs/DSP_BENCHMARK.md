# ShoutSend DSP Channel Benchmark Report

> Note: These results are deterministic channel simulations modeling acoustic room responses, noise, and hardware frequency shaping.

| Profile (Band / Scheme / Speed / RS) | Scenario | Trials | Success Rate | Mean Duration | P95 Duration | Net Throughput | Mean RS Corr |
|---|---|---|---|---|---|---|---|
| RETRO / MFSK16 / NORMAL / RS1 | CLEAN | 10 | 100.0% | 8.48s | 8.48s | 5.2 B/s | 0.0 |
| RETRO / MFSK16 / NORMAL / RS1 | QUIET_ROOM | 10 | 100.0% | 8.53s | 8.53s | 5.2 B/s | 0.0 |
| RETRO / MFSK16 / NORMAL / RS1 | LIVING_ROOM | 10 | 100.0% | 8.53s | 8.53s | 5.2 B/s | 0.0 |
| RETRO / MFSK16 / NORMAL / RS1 | CLOSE_RANGE | 10 | 100.0% | 8.48s | 8.48s | 5.2 B/s | 0.0 |
| RETRO / MFSK16 / NORMAL / RS1 | CAFE | 10 | 90.0% | 8.60s | 8.60s | 4.6 B/s | 0.7 |
| STEALTH / DTFSK / FAST / RS1 | CLEAN | 10 | 100.0% | 4.40s | 4.40s | 10.0 B/s | 0.0 |
| STEALTH / DTFSK / FAST / RS1 | QUIET_ROOM | 10 | 0.0% | 4.45s | 4.45s | 0.0 B/s | 0.0 |
| STEALTH / DTFSK / FAST / RS1 | LIVING_ROOM | 10 | 20.0% | 4.45s | 4.45s | 2.0 B/s | 0.0 |
| STEALTH / DTFSK / FAST / RS1 | CLOSE_RANGE | 10 | 100.0% | 4.40s | 4.40s | 10.0 B/s | 0.0 |
| STEALTH / DTFSK / FAST / RS1 | CAFE | 10 | 90.0% | 4.52s | 4.52s | 8.8 B/s | 2.1 |
| RETRO / DTFSK / ROBUST / RS2 | CLEAN | 10 | 100.0% | 21.21s | 21.21s | 2.1 B/s | 0.0 |
| RETRO / DTFSK / ROBUST / RS2 | QUIET_ROOM | 10 | 100.0% | 21.25s | 21.25s | 2.1 B/s | 0.0 |
| RETRO / DTFSK / ROBUST / RS2 | LIVING_ROOM | 10 | 100.0% | 21.25s | 21.25s | 2.1 B/s | 0.0 |
| RETRO / DTFSK / ROBUST / RS2 | CLOSE_RANGE | 10 | 100.0% | 21.21s | 21.21s | 2.1 B/s | 0.0 |
| RETRO / DTFSK / ROBUST / RS2 | CAFE | 10 | 100.0% | 21.33s | 21.33s | 2.1 B/s | 0.0 |
