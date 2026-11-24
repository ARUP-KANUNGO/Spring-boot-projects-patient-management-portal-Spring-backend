import { API_BASE_URL, TOKEN_KEY, USER_KEY } from '../utils/constants.js';

/* ---------- session handling ----------
 * "Remember me" -> localStorage (survives browser restarts); otherwise sessionStorage (cleared with the tab).
 * Both survive a page refresh, so authenticated navigation keeps working.
 */
export function getToken() {
    return sessionStorage.getItem(TOKEN_KEY) || localStorage.getItem(TOKEN_KEY);
}

export function getStoredUser() {
    const raw = sessionStorage.getItem(USER_KEY) || localStorage.getItem(USER_KEY);
    try {
        return raw ? JSON.parse(raw) : null;
    } catch {
        return null;
    }
}

export function clearSession() {
    [sessionStorage, localStorage].forEach((store) => {
        store.removeItem(TOKEN_KEY);
        store.removeItem(USER_KEY);
    });
}

function saveSession(token, user, remember) {
    clearSession();
    const store = remember ? localStorage : sessionStorage;
    store.setItem(TOKEN_KEY, token);
    store.setItem(USER_KEY, JSON.stringify(user));
}

/* ---------- HTTP helper ---------- */
async function request(path, { method = 'GET', body, auth = true } = {}) {
    const headers = { Accept: 'application/json' };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (auth) {
        const token = getToken();
        if (token) headers.Authorization = `Bearer ${token}`;
    }

    let response;
    try {
        response = await fetch(`${API_BASE_URL}${path}`, {
            method,
            headers,
            body: body !== undefined ? JSON.stringify(body) : undefined
        });
    } catch {
        throw new Error('Unable to reach the server. Please make sure the backend is running.');
    }

    // Expired / invalid token on a protected call -> back to the login page.
    if (response.status === 401 && auth) {
        clearSession();
        window.location.href = 'login.html';
        throw new Error('Your session has expired. Please sign in again.');
    }

    const text = await response.text();
    let data = null;
    if (text) {
        try {
            data = JSON.parse(text);
        } catch {
            data = null;
        }
    }

    if (!response.ok) {
        throw new Error((data && data.message) || `Request failed (HTTP ${response.status})`);
    }
    return data;
}

/* ---------- authentication ---------- */
export async function login(credentials) {
    const { username, password, remember = false } = credentials;
    const data = await request('/auth/login', { method: 'POST', body: { username, password }, auth: false });
    saveSession(data.token, data.user, remember);
    return data;
}

export async function logout() {
    // Stateless JWT: logging out means discarding the token on the client.
    clearSession();
}

export async function register(user) {
    const { fullName, email, username, password } = user;
    return request('/auth/register', { method: 'POST', body: { fullName, email, username, password }, auth: false });
}

export async function getCurrentUser() {
    return request('/auth/me');
}

/* ---------- patients ---------- */
function toPatientPayload(patientData) {
    const payload = { ...patientData };
    if (payload.age !== undefined && payload.age !== '') payload.age = Number(payload.age);
    return payload;
}

export async function getPatients() {
    return request('/patients');
}

export async function getPatient(id) {
    return request(`/patients/${encodeURIComponent(id)}`);
}

export async function createPatient(patientData) {
    return request('/patients', { method: 'POST', body: toPatientPayload(patientData) });
}

export async function updatePatient(id, patientData) {
    return request(`/patients/${encodeURIComponent(id)}`, { method: 'PUT', body: toPatientPayload(patientData) });
}

export async function deletePatient(id) {
    return request(`/patients/${encodeURIComponent(id)}`, { method: 'DELETE' });
}

export async function searchPatients(query) {
    return request(`/patients/search?query=${encodeURIComponent(query)}`);
}

/* ---------- dashboard ---------- */
export async function getDashboardStats() {
    return request('/dashboard/stats');
}
