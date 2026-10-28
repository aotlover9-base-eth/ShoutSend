# ShoutSend Roadmap & Step Status

| Step | Phase | Title | UI Gate | Status |
| :--- | :--- | :--- | :--- | :--- |
| 0 | A Foundation | Discovery | Done | **Done** |
| 1 | A Foundation | Project setup, flavors, docs | No | **Done** |
| 2 | A Foundation | Design system core | No | **Done** |
| 3 | A Foundation | Token state and persistence | No | **Done** |
| 4 | A Foundation | Developer panel (dev flavor) | Light | **Done** |
| 5 | A Foundation | Style guard and User APK purity check | No | **Done** |
| 6 | A Foundation | `:modem` module scaffold and test harness | No | **Done** |
| 7 | B Protocol | Acoustic profiles and tone plan | No | **Done** |
| 8 | B Protocol | Bit utilities, CRC-16, whitening | No | **Done** |
| 9 | B Protocol | Reed-Solomon field and encoder | No | **Done** |
| 10 | B Protocol | Reed-Solomon decoder | No | **Done** |
| 11 | B Protocol | Codeword symbolizer and erasure mapping | No | **Done** |
| 12 | B Protocol | Packet format, fragmentation, reassembly | No | **Done** |
| 13 | B Protocol | Payload model and codecs | No | **Done** |
| 14 | B Protocol | Payload compaction | No | **Done** |
| 15 | B Protocol | Secure mode crypto | No | **Done** |
| 16 | C Modem | Tone synthesizer and windowing | No | **Done** |
| 17 | C Modem | Modulator | No | **Done** |
| 18 | C Modem | Goertzel detector | No | **Done** |
| 19 | C Modem | Noise floor tracker and SNR | No | **Done** |
| 20 | C Modem | Preamble detection and symbol sync | No | **Done** |
| 21 | C Modem | Demodulator and receive state machine | No | **Done** |
| 22 | C Modem | Channel simulator and loopback harness | No | **Done** |
| 23 | C Modem | Benchmark and preset tuning in simulation | No | **Done** |
| 24 | C Modem | DSP performance and allocation audit | No | **Done** |
| 25 | C Modem | Spectrum analyzer (FFT) for the waterfall | No | **Done** |
| 26 | D Android layer | Manifest, permissions logic, audio config | No | **Done** |
| 27 | D Android layer | AudioTrack transmitter | No | **Done** |
| 28 | D Android layer | AudioRecord receiver | No | **Done** |
| 29 | D Android layer | Capability probe and calibration engine | No | **Done** |
| 30 | D Android layer | Send and receive sessions, audio focus, routes | No | **Done** |
| 31 | D Android layer | Vault storage (Room plus Keystore) | No | **Done** |
| 32 | D Android layer | Settings store | No | **Done** |
| 33 | D Android layer | Modem config provider (dev override seam) | No | **Done** |
| 34 | E UI | ShoutSend components and extra tokens | Yes | **Done** |
| 35 | E UI | App shell and navigation | Yes | **Done** |
| 36 | E UI | Onboarding and permission flow | Yes | **Done** |
| 37 | E UI | Send: compose screen (text and URL) | Yes | **Done** |
| 38 | E UI | Send: Wi-Fi and contact forms | Yes | **Done** |
| 39 | E UI | Transmit screen: soundwave rings | Yes | **Done** |
| 40 | E UI | Receive screen: spectrogram waterfall | Yes | **Done** |
| 41 | E UI | Receive progress and success states | Yes | **Done** |
| 42 | E UI | Instant action cards | Yes | **Done** |
| 43 | E UI | Transmission Vault screen | Yes | **Done** |
| 44 | E UI | Settings screen | Yes | **Done** |
| 45 | E UI | Calibration and link test screen | Yes | **Done** |
| 46 | E UI | Secure mode UI | Yes | **Done** |
| 47 | E UI | Haptics and audio feedback | Light | **Done** |
| 48 | E UI | Motion, transitions, predictive back | Yes | **Done** |
| 49 | E UI | Accessibility pass | Light | **Done** |
| 50 | E UI | Developer panel: DSP Lab | Light | **Done** |
| 51 | F Hardening | Robustness and edge cases | No | **Done** |
| 52 | F Hardening | Security and privacy review | No | **Done** |
| 53 | F Hardening | Test consolidation | No | **Done** |
| 54 | F Hardening | Performance and battery profile | No | **Done** |
| 55 | F Hardening | Field test plan and tooling (OWNER RUNS) | No | **Done** |
| 56 | F Hardening | Preset tuning from field data | No | **Done** |
| 57 | F Hardening | Lint, R8, size cleanup | No | **Done** |
| 58 | F Hardening | Release build and docs | No | **Done** |
| 59 | F Hardening | Final audit and handoff | No | **Done** |

---

## v2: Material You Redesign + Real File Sharing

| Step | Area | Title | Touches UI | Status |
|---|---|---|---|---|
| R0 | Foundation | Baseline audit and v2 docs (no code changes) | No | **Done** |
| R1 | Foundation | Foundation: dependency upgrade, proper ViewModels, permission and network guards | No | Todo |
| R2 | Design | New Material 3 Expressive design system and dev panel update | Components only | Todo |
| R3 | UI | App shell and Home hub | Yes | Todo |
| R4 | UI | Composer sheets: Text, Link, Wi-Fi, Contact | Yes | Todo |
| R5 | UI | Sending and receiving moments, result cards | Yes | Todo |
| R6 | UI | History, Settings, Onboarding, Calibration redesign | Yes | Todo |
| R7 | Modem | Protocol additions: FILE and HANDOFF payloads (`:modem`) | No | Todo |
| R8 | Transfer | Wi-Fi Direct transfer engine (`core/transfer`) | No | Todo |
| R9 | UI | File sharing UI and share-sheet intake | Yes | Todo |
| R10 | Polish | Polish: motion, haptics, accessibility, large screens | Yes | Todo |
| R11 | Release | Release v2.0.0, README, owner test plan | No | Todo |
