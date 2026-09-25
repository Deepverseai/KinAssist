const http = require('http');
const { WebSocketServer, WebSocket } = require('ws');

const PORT = process.env.PORT || 8080;

// HTTP Server for Healthchecks and Status
const server = http.createServer((req, res) => {
  if (req.url === '/health' || req.url === '/') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      status: 'ok',
      service: 'KinAssist Signaling Server',
      version: '1.0.0',
      activeRooms: rooms.size,
      uptimeSeconds: Math.floor(process.uptime()),
      timestamp: new Date().toISOString()
    }));
  } else {
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('Not Found');
  }
});

// WebSocket Server
const wss = new WebSocketServer({ server });

/**
 * Room structure:
 * familyCode -> {
 *   senior: { ws, deviceName },
 *   helpers: Map<ws, { deviceName }>,
 *   state: 'IDLE' | 'CALLING' | 'CONNECTED',
 *   activeHelper: ws | null
 * }
 */
const rooms = new Map();

function getOrCreateRoom(familyCode) {
  if (!rooms.has(familyCode)) {
    rooms.set(familyCode, {
      senior: null,
      helpers: new Map(),
      state: 'IDLE',
      activeHelper: null
    });
  }
  return rooms.get(familyCode);
}

function cleanUpClient(ws) {
  const meta = ws.clientMeta;
  if (!meta || !meta.familyCode) return;

  const room = rooms.get(meta.familyCode);
  if (!room) return;

  if (meta.role === 'SENIOR' && room.senior?.ws === ws) {
    console.log(`[DISCONNECT] Senior disconnected from room: ${meta.familyCode}`);
    room.senior = null;
    room.state = 'IDLE';
    room.activeHelper = null;
    // Notify all helpers
    broadcastToRoom(meta.familyCode, {
      type: 'PEER_DISCONNECTED',
      role: 'SENIOR',
      message: 'Senior device disconnected'
    });
  } else if (meta.role === 'HELPER') {
    console.log(`[DISCONNECT] Helper (${meta.deviceName || 'unknown'}) disconnected from room: ${meta.familyCode}`);
    room.helpers.delete(ws);
    if (room.activeHelper === ws) {
      room.activeHelper = null;
      room.state = 'IDLE';
      if (room.senior?.ws && room.senior.ws.readyState === WebSocket.OPEN) {
        room.senior.ws.send(JSON.stringify({
          type: 'PEER_DISCONNECTED',
          role: 'HELPER',
          message: 'Active helper disconnected'
        }));
      }
    }
  }

  // Remove empty room
  if (!room.senior && room.helpers.size === 0) {
    rooms.delete(meta.familyCode);
    console.log(`[ROOM REMOVED] Empty room cleared: ${meta.familyCode}`);
  }
}

function broadcastToRoom(familyCode, message, excludeWs = null) {
  const room = rooms.get(familyCode);
  if (!room) return;

  const payload = JSON.stringify(message);

  if (room.senior?.ws && room.senior.ws !== excludeWs && room.senior.ws.readyState === WebSocket.OPEN) {
    room.senior.ws.send(payload);
  }

  for (const [helperWs] of room.helpers.entries()) {
    if (helperWs !== excludeWs && helperWs.readyState === WebSocket.OPEN) {
      helperWs.send(payload);
    }
  }
}

wss.on('connection', (ws, req) => {
  const ip = req.headers['x-forwarded-for'] || req.socket.remoteAddress;
  console.log(`[NEW CONNECTION] Client connected from ${ip}`);

  ws.isAlive = true;
  ws.on('pong', () => {
    ws.isAlive = true;
  });

  ws.on('message', (data) => {
    try {
      const message = JSON.parse(data.toString());
      handleMessage(ws, message);
    } catch (err) {
      console.error('[ERROR] Malformed JSON received:', err.message);
      ws.send(JSON.stringify({ type: 'ERROR', message: 'Invalid JSON format' }));
    }
  });

  ws.on('close', () => {
    cleanUpClient(ws);
  });

  ws.on('error', (err) => {
    console.error('[WS ERROR]', err.message);
    cleanUpClient(ws);
  });
});

function handleMessage(ws, msg) {
  const { type, familyCode, role, deviceName } = msg;

  switch (type) {
    case 'REGISTER': {
      if (!familyCode || !role) {
        return ws.send(JSON.stringify({ type: 'ERROR', message: 'Missing familyCode or role' }));
      }
      ws.clientMeta = { familyCode, role, deviceName: deviceName || role };
      const room = getOrCreateRoom(familyCode);

      if (role === 'SENIOR') {
        room.senior = { ws, deviceName: ws.clientMeta.deviceName };
        console.log(`[REGISTERED] Senior in room ${familyCode}`);
      } else {
        room.helpers.set(ws, { deviceName: ws.clientMeta.deviceName });
        console.log(`[REGISTERED] Helper in room ${familyCode} (Total Helpers: ${room.helpers.size})`);
      }

      ws.send(JSON.stringify({
        type: 'REGISTERED',
        familyCode,
        role,
        roomState: room.state,
        hasSenior: !!room.senior,
        helpersCount: room.helpers.size
      }));
      break;
    }

    case 'SOS_ALERT': {
      const room = rooms.get(familyCode);
      if (!room) {
        return ws.send(JSON.stringify({ type: 'ERROR', message: 'Room not found' }));
      }
      room.state = 'CALLING';
      console.log(`[SOS_ALERT] Senior triggered SOS in room: ${familyCode}`);

      // Broadcast high-priority incoming alert to all paired helpers
      for (const [helperWs] of room.helpers.entries()) {
        if (helperWs.readyState === WebSocket.OPEN) {
          helperWs.send(JSON.stringify({
            type: 'INCOMING_CALL',
            familyCode,
            seniorName: msg.seniorName || 'Mom / Dad',
            batteryLevel: msg.batteryLevel || 'Unknown',
            timestamp: Date.now()
          }));
        }
      }
      break;
    }

    case 'CALL_ACCEPT': {
      const room = rooms.get(familyCode);
      if (!room) return;

      room.state = 'CONNECTED';
      room.activeHelper = ws;
      console.log(`[CALL_ACCEPT] Helper accepted call in room: ${familyCode}`);

      // Notify senior that helper has accepted
      if (room.senior?.ws && room.senior.ws.readyState === WebSocket.OPEN) {
        room.senior.ws.send(JSON.stringify({
          type: 'CALL_ACCEPTED',
          familyCode,
          helperName: ws.clientMeta?.deviceName || 'Caregiver'
        }));
      }

      // Notify other helpers that session is answered
      for (const [helperWs] of room.helpers.entries()) {
        if (helperWs !== ws && helperWs.readyState === WebSocket.OPEN) {
          helperWs.send(JSON.stringify({
            type: 'CALL_TAKEN',
            familyCode,
            handledBy: ws.clientMeta?.deviceName || 'Another helper'
          }));
        }
      }
      break;
    }

    case 'CALL_DECLINE': {
      const room = rooms.get(familyCode);
      if (!room) return;

      if (room.senior?.ws && room.senior.ws.readyState === WebSocket.OPEN) {
        room.senior.ws.send(JSON.stringify({
          type: 'CALL_DECLINED',
          familyCode,
          reason: msg.reason || 'Caregiver is busy'
        }));
      }
      break;
    }

    case 'SDP_OFFER': {
      const room = rooms.get(familyCode);
      if (!room) return;

      console.log(`[SDP_OFFER] Relaying SDP Offer in room: ${familyCode}`);
      // Send offer to the active helper (or first connected helper)
      const targetHelper = room.activeHelper || (room.helpers.size > 0 ? Array.from(room.helpers.keys())[0] : null);
      if (targetHelper && targetHelper.readyState === WebSocket.OPEN) {
        targetHelper.send(JSON.stringify({
          type: 'SDP_OFFER',
          familyCode,
          sdp: msg.sdp
        }));
      }
      break;
    }

    case 'SDP_ANSWER': {
      const room = rooms.get(familyCode);
      if (!room) return;

      console.log(`[SDP_ANSWER] Relaying SDP Answer in room: ${familyCode}`);
      if (room.senior?.ws && room.senior.ws.readyState === WebSocket.OPEN) {
        room.senior.ws.send(JSON.stringify({
          type: 'SDP_ANSWER',
          familyCode,
          sdp: msg.sdp
        }));
      }
      break;
    }

    case 'ICE_CANDIDATE': {
      const room = rooms.get(familyCode);
      if (!room) return;

      const meta = ws.clientMeta;
      if (meta?.role === 'SENIOR') {
        // Forward ICE candidate to active helper
        if (room.activeHelper && room.activeHelper.readyState === WebSocket.OPEN) {
          room.activeHelper.send(JSON.stringify({
            type: 'ICE_CANDIDATE',
            candidate: msg.candidate
          }));
        }
      } else if (meta?.role === 'HELPER') {
        // Forward ICE candidate to senior
        if (room.senior?.ws && room.senior.ws.readyState === WebSocket.OPEN) {
          room.senior.ws.send(JSON.stringify({
            type: 'ICE_CANDIDATE',
            candidate: msg.candidate
          }));
        }
      }
      break;
    }

    case 'CALL_END': {
      const room = rooms.get(familyCode);
      if (!room) return;

      console.log(`[CALL_END] Session ended in room: ${familyCode}`);
      room.state = 'IDLE';
      room.activeHelper = null;

      broadcastToRoom(familyCode, {
        type: 'CALL_ENDED',
        familyCode,
        endedBy: ws.clientMeta?.role || 'USER'
      });
      break;
    }

    case 'PING': {
      ws.send(JSON.stringify({ type: 'PONG', timestamp: Date.now() }));
      break;
    }

    default:
      console.warn(`[WARN] Unknown message type: ${type}`);
  }
}

// 30-second Heartbeat Interval to clean dead sockets
const heartbeatInterval = setInterval(() => {
  wss.clients.forEach((ws) => {
    if (!ws.isAlive) {
      console.log('[HEARTBEAT] Terminating inactive WebSocket client');
      cleanUpClient(ws);
      return ws.terminate();
    }
    ws.isAlive = false;
    ws.ping();
  });
}, 30000);

wss.on('close', () => {
  clearInterval(heartbeatInterval);
});

server.listen(PORT, () => {
  console.log(`====================================================`);
  console.log(`🚀 KinAssist WebRTC Signaling Server Running`);
  console.log(`📡 Port: ${PORT}`);
  console.log(`🩺 Healthcheck: http://localhost:${PORT}/health`);
  console.log(`🔗 WebSocket Endpoint: ws://localhost:${PORT}`);
  console.log(`====================================================`);
});
