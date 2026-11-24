import { initLogin } from './pages/login.js';
import { initRegister } from './pages/register.js';
import { initDashboard } from './pages/dashboard.js';
import { initPatients } from './pages/patients.js';
import { getToken } from './services/api.js';

document.addEventListener('DOMContentLoaded', () => {
    const path = window.location.pathname;

    const isProtectedPage = path.includes('dashboard.html') || path.includes('patients.html');
    if (isProtectedPage && !getToken()) {
        window.location.replace('login.html');
        return;
    }

    if (path.includes('login.html') || path.endsWith('/')) {
        initLogin();
    } else if (path.includes('register.html')) {
        initRegister();
    } else if (path.includes('dashboard.html')) {
        initDashboard();
    } else if (path.includes('patients.html')) {
        initPatients();
    }
});
