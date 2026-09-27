const { WebSocket } = require('ws');

const URL = 'ws://localhost:8080';
const FAMILY_CODE = '884219';

console.log('🧪 Starting KinAssist End-to-End Signaling Test...');

const seniorWs = new WebSocket(URL);
const helperWs = new WebSocket(URL);

seniorWs.on('error', (err) => console.error('👵 Senior WS Error:', err.message));
helperWs.on('error', (err) => console.error('🧑‍💻 Helper WS Error:', err.message));

let testsPassed = 0;

seniorWs.on('open', () => {
  console.log('✅ Senior WS Connected');
  seniorWs.send(JSON.stringify({
    type: 'REGISTER',
    familyCode: FAMILY_CODE,
    role: 'SENIOR',
    deviceName: "Mom's Galaxy S23"
  }));
});

helperWs.on('open', () => {
  console.log('✅ Helper WS Connected');
  helperWs.send(JSON.stringify({
    type: 'REGISTER',
    familyCode: FAMILY_CODE,
    role: 'HELPER',
    deviceName: "Rahul's Pixel 9 Pro"
  }));
});

seniorWs.on('message', (data) => {
  const msg = JSON.parse(data.toString());
  console.log('👵 [Senior Received]:', msg.type);

  if (msg.type === 'REGISTERED') {
    testsPassed++;
  } else if (msg.type === 'CALL_ACCEPTED') {
    testsPassed++;
    console.log('✅ Call accepted by helper. Senior sending SDP Offer...');
    seniorWs.send(JSON.stringify({
      type: 'SDP_OFFER',
      familyCode: FAMILY_CODE,
      sdp: 'v=0\r\no=senior 12345 IN IP4 0.0.0.0\r\ns=KinAssist Screen Share...'
    }));
  } else if (msg.type === 'SDP_ANSWER') {
    testsPassed++;
    console.log('✅ Senior received SDP Answer. Sending ICE Candidate...');
    seniorWs.send(JSON.stringify({
      type: 'ICE_CANDIDATE',
      familyCode: FAMILY_CODE,
      candidate: { sdpMid: '0', sdpMLineIndex: 0, sdp: 'candidate:1 1 UDP 2130706431 192.168.1.50 50000 typ host' }
    }));
  } else if (msg.type === 'ICE_CANDIDATE') {
    testsPassed++;
    console.log('✅ Senior received Helper ICE Candidate! P2P Handshake Complete!');
    console.log(`🎉 ALL TESTS PASSED! (${testsPassed}/6)`);
    seniorWs.close();
    helperWs.close();
    process.exit(0);
  }
});

helperWs.on('message', (data) => {
  const msg = JSON.parse(data.toString());
  console.log('🧑‍💻 [Helper Received]:', msg.type);

  if (msg.type === 'REGISTERED') {
    testsPassed++;
    // Once both registered, Senior triggers SOS
    setTimeout(() => {
      console.log('🚨 Senior triggering SOS...');
      seniorWs.send(JSON.stringify({
        type: 'SOS_ALERT',
        familyCode: FAMILY_CODE,
        seniorName: 'Mom',
        batteryLevel: '82%'
      }));
    }, 500);
  } else if (msg.type === 'INCOMING_CALL') {
    testsPassed++;
    console.log('✅ Helper received Incoming Call chime! Accepting...');
    helperWs.send(JSON.stringify({
      type: 'CALL_ACCEPT',
      familyCode: FAMILY_CODE,
      deviceName: "Rahul's Pixel 9 Pro"
    }));
  } else if (msg.type === 'SDP_OFFER') {
    testsPassed++;
    console.log('✅ Helper received SDP Offer! Sending SDP Answer...');
    helperWs.send(JSON.stringify({
      type: 'SDP_ANSWER',
      familyCode: FAMILY_CODE,
      sdp: 'v=0\r\no=helper 67890 IN IP4 0.0.0.0\r\ns=KinAssist Helper Answer...'
    }));
    // Helper also sends ICE Candidate
    helperWs.send(JSON.stringify({
      type: 'ICE_CANDIDATE',
      familyCode: FAMILY_CODE,
      candidate: { sdpMid: '0', sdpMLineIndex: 0, sdp: 'candidate:2 1 UDP 2130706431 192.168.1.60 50001 typ host' }
    }));
  }
});
