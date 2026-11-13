# Battle Royale: Cross-Platform 100-Player Architecture Specification

## 1. High-Level Architecture
This repository implements a modular cross-platform 100-player Battle Royale architecture built for Unity 6 LTS, standalone Linux Dedicated Servers, and a containerized Node.js/PostgreSQL authoritative backend.

### Component Layers:
1. **Client Core (PC / Android / iOS):**
   - Universal Input System (Raw KBM on PC, dynamic touch on mobile).
   - Client-side prediction with local interpolation buffers.
   - Addressables for progressive map and asset streaming.
2. **Dedicated Game Server (Headless Linux):**
   - Authoritative 60 Hz physics and tick simulation.
   - Lag compensation with historical rewind buffers.
   - Safe zone shrink state machine and airdrop scheduler.
3. **Multiplayer Services & Backend:**
   - Ticket-based matchmaking with ping routing.
   - PostgreSQL authoritative database transactions for match settlement, XP, and inventory.
   - Admin moderation telemetry.

---

## 2. Directory Layout & Module Responsibilities

* `Assets/Scripts/Core/`: Game lifecycle, scene bootstrapping, and persistent state.
* `Assets/Scripts/Player/`: Kinematic character controller, camera, and animations.
* `Assets/Scripts/Multiplayer/`: Netcode, lag compensation, and tick synchronization.
* `Assets/Scripts/Matchmaking/`: Matchmaking queue, region selection, and room codes.
* `Assets/Scripts/Inventory/`: Backpack weight, weapon slots, and consumable usage.
* `Assets/Scripts/Vehicles/`: 4-wheel raycast vehicle physics, passenger seat management.
* `Assets/Scripts/UI/`: Cross-platform tactical HUD, virtual joystick, and map overlay.
* `Assets/Scripts/Backend/`: REST / gRPC client for player profile, auth, and store transactions.
* `Server/`: Dedicated server entrypoint, CLI argument parser, and headless loop.
* `Backend/`: Microservices for auth, inventory, matchmaking tickets, and database scripts.
* `Tests/`: Automated network load testing and 100-client stress test harness.
