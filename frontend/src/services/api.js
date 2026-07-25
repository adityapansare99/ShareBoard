import axios from 'axios';

const api = axios.create({
  baseURL: (import.meta.env && import.meta.env.VITE_BACKEND_URL) || '',
  withCredentials: true,
});

// A 429 means the rate-limit filter rejected the request BEFORE it reached the
// controller (it short-circuits the chain), so the mutation never ran — retrying
// is safe for GET/POST/PATCH/DELETE alike. Honor the server's retryAfterMs and
// retry exactly once; the __retried flag prevents any loop.
api.interceptors.response.use(
  response => response,
  async error => {
    const { config, response } = error;
    if (response && response.status === 429 && config && !config.__retried) {
      config.__retried = true;
      const retryAfterMs = response.data?.retryAfterMs || 1000;
      await new Promise(resolve => setTimeout(resolve, retryAfterMs));
      return api(config);
    }
    return Promise.reject(error);
  }
);

export function fetchAuthStatus() {
  return api.get('/api/auth/me').then(r => r.data);
}

export function login(email, password) {
  return api.post('/api/auth/login', { email, password }).then(r => r.data);
}

export function sendOtp(email) {
  return api.post('/api/auth/send-otp', { email }).then(r => r.data);
}

export function register(email, name, password, otp = '') {
  return api.post('/api/auth/register', { email, name, password, otp }).then(r => r.data);
}

export function logout() {
  return api.post('/api/auth/logout');
}

export function fetchBoards() {
  return api.get('/api/boards').then(r => r.data);
}

export function createBoard(name, description) {
  return api.post('/api/boards', { name, description }).then(r => r.data);
}

export function fetchBoard(code, view = 'kanban') {
  return api.get(`/api/boards/${code}?view=${view}`).then(r => r.data);
}

export function deleteBoard(code) {
  return api.delete(`/api/boards/${code}`);
}

export function joinBoard(code) {
  return api.post('/api/boards/join', { code }).then(r => r.data);
}

export function createCard(boardCode, columnId, title, sessionId = 'default') {
  return api.post(`/api/boards/${boardCode}/cards`, { columnId, title, sessionId }).then(r => r.data);
}

export function editCard(boardCode, cardId, data) {
  return api.patch(`/api/boards/${boardCode}/cards/${cardId}`, data).then(r => r.data);
}

export function deleteCard(boardCode, cardId, sessionId) {
  return api.delete(`/api/boards/${boardCode}/cards/${cardId}?sessionId=${sessionId}`);
}

export function moveCard(boardCode, request) {
  return api.post(`/api/boards/${boardCode}/cards/move`, request).then(r => r.data);
}

export function changeCardState(boardCode, cardId, state, sessionId = 'default') {
  return api.post(`/api/boards/${boardCode}/cards/${cardId}/state`, { state, sessionId }).then(r => r.data);
}

export function undo(boardCode, sessionId) {
  return api.post(`/api/boards/${boardCode}/undo?sessionId=${sessionId}`).then(r => r.data);
}

export function redo(boardCode, sessionId) {
  return api.post(`/api/boards/${boardCode}/redo?sessionId=${sessionId}`).then(r => r.data);
}

export function getHistory(boardCode, sessionId) {
  return api.get(`/api/boards/${boardCode}/history?sessionId=${sessionId}`).then(r => r.data);
}

export function fetchBoardActivity(boardCode) {
  return api.get(`/api/boards/${boardCode}/activity`).then(r => r.data);
}

export default api;
