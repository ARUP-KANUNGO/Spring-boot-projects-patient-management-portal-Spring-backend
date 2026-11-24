import { initializeSidebar } from '../components/sidebar.js';
import { toggleLoading } from '../components/loader.js';
import { showToast } from '../components/toast.js';
import { timeAgo } from '../utils/helpers.js';
import * as api from '../services/api.js';

export function initDashboard() {
    initializeSidebar();
    
    const dateElement = document.getElementById('currentDate');
    if (dateElement) {
        const dateOptions = { weekday: 'short', year: 'numeric', month: 'short', day: 'numeric' };
        dateElement.textContent = new Date().toLocaleDateString('en-US', dateOptions);
    }

    loadDashboard();
}

async function loadDashboard() {
    try {
        toggleLoading(true, 'Loading Dashboard...');
        const stats = await api.getDashboardStats();

        setStat('statTotalPatients', stats.totalPatients);
        setStat('statTodaysAdmissions', stats.todaysAdmissions);
        setStat('statPendingReviews', stats.pendingReviews);
        setStat('statDischargedToday', stats.dischargedToday);
        renderActivity(stats.recentActivity || []);
    } catch (error) {
        showToast('error', 'Error', error.message || 'Failed to load dashboard statistics.');
    } finally {
        toggleLoading(false);
    }
}

function setStat(id, value) {
    const el = document.getElementById(id);
    if (el) el.textContent = Number(value ?? 0).toLocaleString('en-US');
}

function bulletColor(item) {
    if (item.event === 'registered') return 'var(--primary)';
    if (item.status === 'Discharged') return 'var(--success)';
    if (item.status === 'Admitted') return 'var(--danger)';
    return 'var(--warning)';
}

function describe(item) {
    if (item.event === 'registered') return ['New patient ', ' was registered.'];
    switch (item.status) {
        case 'Discharged': return ['', ' was discharged.'];
        case 'Admitted': return ['', ' was admitted.'];
        case 'Under Review': return ['', ' is under review.'];
        default: return ['Record for ', ' was updated.'];
    }
}

// Built with DOM APIs (textContent) so patient names can never inject markup.
function renderActivity(items) {
    const list = document.getElementById('activityList');
    if (!list) return;
    list.innerHTML = '';

    if (items.length === 0) {
        const empty = document.createElement('p');
        empty.className = 'text-sm text-muted';
        empty.textContent = 'No recent activity yet.';
        list.appendChild(empty);
        return;
    }

    items.forEach((item) => {
        const row = document.createElement('div');
        row.className = 'activity-item';

        const bullet = document.createElement('div');
        bullet.className = 'activity-bullet';
        bullet.style.backgroundColor = bulletColor(item);

        const content = document.createElement('div');
        content.className = 'activity-content';

        const [before, after] = describe(item);
        const p = document.createElement('p');
        const strong = document.createElement('strong');
        strong.textContent = item.patientName;
        p.append(before, strong, after);

        const when = document.createElement('span');
        when.textContent = timeAgo(item.timestamp);

        content.append(p, when);
        row.append(bullet, content);
        list.appendChild(row);
    });
}
