# KinAssist WebRTC Signaling & Relay Server

A high-performance, low-latency WebSocket signaling and TURN/STUN relay server designed for **KinAssist (1-Tap Remote Elderly Assistance)**.

## Features
- **Family Room Routing:** Connects Senior and Caregiver via 6-digit family invite code.
- **Instant SOS Dispatch:** Dispatches incoming call events with battery level and senior identity.
- **SDP Offer/Answer Relay:** Seamless H.264/VP8 WebRTC session negotiation.
- **ICE Candidate Exchange:** Dynamic NAT traversal across mobile carriers.
- **Heartbeat & Zombie Pruning:** 30s automatic cleanup of stale connections.

---

## Quick Start (Local / Development)

### 1. Run with Node.js
```bash
cd server
npm install
npm start
```
Default port is `8080`. Healthcheck is available at:
`http://localhost:8080/health`

### 2. Run with Docker Compose (Signaling + Coturn TURN Server)
```bash
docker-compose up -d
```
This launches:
- **WebSocket Signaling Server:** `ws://<your-ip>:8080`
- **Coturn STUN/TURN Server:** Port `3478` (UDP/TCP) with user `kinuser` and pass `kinassist_secret_pass`

---

## Production Deployment Options
- **Railway / Render:** Connect this repo, set root directory to `server/`, build command `npm install`, start command `npm start`.
- **DigitalOcean / AWS EC2:** Run `docker-compose up -d` with ports `8080` and `3478` open in security groups.
