import { toggleLoading } from '../components/loader.js';
import { showToast } from '../components/toast.js';
import { validateLogin } from '../utils/validation.js';
import * as api from '../services/api.js';

export function initLogin() {
    const loginForm = document.getElementById('loginForm');
    
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const usernameInput = document.getElementById('username');
            const passwordInput = document.getElementById('password');
            const rememberInput = document.getElementById('rememberMe');
            
            const username = usernameInput.value.trim();
            const password = passwordInput.value;
            
            const errors = validateLogin(username, password);
            
            if (errors.length > 0) {
                showToast('error', 'Validation Error', errors[0].message);
                if (errors[0].field === 'username') usernameInput.focus();
                else passwordInput.focus();
                return;
            }
            
            try {
                toggleLoading(true, 'Authenticating...');
                
                await api.login({ username, password, remember: Boolean(rememberInput && rememberInput.checked) });
                showToast('success', 'Login Successful', 'Welcome back!');
                
                setTimeout(() => {
                    window.location.href = 'dashboard.html';
                }, 1000);
                
            } catch (error) {
                showToast('error', 'Login Failed', error.message || 'Invalid credentials. Please try again.');
            } finally {
                toggleLoading(false);
            }
        });
    }
}
