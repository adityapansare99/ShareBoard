/**
 * WebSocket client for real-time board updates.
 * Uses native WebSocket (no STOMP library needed).
 */
class WebSocketClient {
  constructor() {
    this.ws = null;
    this.listeners = {};
    this.reconnectTimer = null;
    this.boardCode = null;
    this.sessionId = null;
    this.connected = false;
  }

  connect(boardCode, sessionId) {
    this.boardCode = boardCode;
    this.sessionId = sessionId;

    // When VITE_BACKEND_URL is set (frontend & backend on different origins,
    // e.g. Netlify → Railway) derive the ws origin from it; otherwise stay
    // same-origin so the Vite dev proxy / nginx can forward /ws transparently.
    const backend = import.meta.env && import.meta.env.VITE_BACKEND_URL;
    let wsOrigin;
    if (backend) {
      const u = new URL(backend);
      wsOrigin = `${u.protocol === 'https:' ? 'wss:' : 'ws:'}//${u.host}`;
    } else {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      wsOrigin = `${protocol}//${window.location.host}`;
    }
    const url = `${wsOrigin}/ws`;

    this.ws = new WebSocket(url);

    this.ws.onopen = () => {
      this.connected = true;
      this._send({ type: 'SUBSCRIBE', boardCode, sessionId });
      this._emit('connected');
    };

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        this._emit(data.type, data);
      } catch (e) {
        console.error('WS parse error:', e);
      }
    };

    this.ws.onclose = () => {
      this.connected = false;
      this._emit('disconnected');
      this._scheduleReconnect();
    };

    this.ws.onerror = () => {
      this.ws.close();
    };
  }

  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.ws) {
      this._send({ type: 'UNSUBSCRIBE', boardCode: this.boardCode, sessionId: this.sessionId });
      this.ws.close();
      this.ws = null;
    }
    this.connected = false;
    this.boardCode = null;
  }

  on(event, callback) {
    if (!this.listeners[event]) {
      this.listeners[event] = [];
    }
    this.listeners[event].push(callback);
    return () => {
      this.listeners[event] = this.listeners[event].filter(cb => cb !== callback);
    };
  }

  _emit(event, data) {
    const callbacks = this.listeners[event] || [];
    callbacks.forEach(cb => cb(data));
  }

  _send(data) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(data));
    }
  }

  _scheduleReconnect() {
    if (this.reconnectTimer) return;
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      if (this.boardCode && this.sessionId) {
        this.connect(this.boardCode, this.sessionId);
      }
    }, 3000);
  }

  isConnected() {
    return this.connected;
  }
}

const wsClient = new WebSocketClient();
export default wsClient;
