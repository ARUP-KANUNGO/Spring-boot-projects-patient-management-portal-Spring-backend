import { toggleLoading } from '../components/loader.js';
import { showToast } from '../components/toast.js';
import { openModal, closeModal } from '../components/modal.js';
import { renderTable } from '../components/table.js';
import { initializeSidebar } from '../components/sidebar.js';
import * as api from '../services/api.js';

const SEARCH_DEBOUNCE_MS = 300;
let latestRequest = 0;

export function initPatients() {
    initializeSidebar();
    
    const refreshBtn = document.getElementById('refreshBtn');
    const searchInput = document.getElementById('searchPatient');
    const addPatientBtn = document.getElementById('addPatientBtn');
    const patientForm = document.getElementById('patientForm');
    const cancelModalBtn = document.getElementById('cancelModalBtn');

    loadPatients();
    
    if (refreshBtn) {
        refreshBtn.addEventListener('click', () => loadPatients());
    }

    if (searchInput) {
        let debounceTimer;
        searchInput.addEventListener('input', () => {
            clearTimeout(debounceTimer);
            // Silent: no full-screen overlay while the user is typing.
            debounceTimer = setTimeout(() => loadPatients({ silent: true }), SEARCH_DEBOUNCE_MS);
        });
    }

    if (addPatientBtn) {
        addPatientBtn.addEventListener('click', () => {
            if (patientForm) patientForm.reset();
            const modalTitle = document.getElementById('modalTitle');
            if (modalTitle) modalTitle.textContent = 'Register New Patient';
            const patientIdInput = document.getElementById('patientId');
            if(patientIdInput) patientIdInput.value = '';
            openModal('patientModal');
        });
    }

    const closeBtn = document.querySelector('#patientModal .close-btn');
    if (closeBtn) {
        closeBtn.addEventListener('click', () => closeModal('patientModal'));
    }

    if (cancelModalBtn) {
        cancelModalBtn.addEventListener('click', () => {
            closeModal('patientModal');
        });
    }

    if (patientForm) {
        patientForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const formData = new FormData(patientForm);
            const patientData = Object.fromEntries(formData.entries());
            
            try {
                toggleLoading(true, 'Saving Patient...');
                const patientIdInput = document.getElementById('patientId');
                const patientId = patientIdInput ? patientIdInput.value : '';
                
                if (patientId) {
                    await api.updatePatient(patientId, patientData);
                    showToast('success', 'Success', 'Patient updated successfully');
                } else {
                    await api.createPatient(patientData);
                    showToast('success', 'Success', 'Patient registered successfully');
                }
                
                closeModal('patientModal');
                await loadPatients();
            } catch (error) {
                showToast('error', 'Error', error.message || 'Failed to save patient. Please try again.');
            } finally {
                toggleLoading(false);
            }
        });
    }
}

async function loadPatients({ silent = false } = {}) {
    const patientTableBody = document.getElementById('patientTableBody');
    const tableContainer = document.getElementById('tableContainer');
    const emptyState = document.getElementById('emptyState');
    const patientForm = document.getElementById('patientForm');
    const searchInput = document.getElementById('searchPatient');
    const query = searchInput ? searchInput.value.trim() : '';
    const requestId = ++latestRequest;
    
    try {
        if (!silent) toggleLoading(true, query ? 'Searching Patients...' : 'Loading Patients...');
        const patients = query ? await api.searchPatients(query) : await api.getPatients();
        if (requestId !== latestRequest) return; // a newer search superseded this one
        renderTable(patientTableBody, tableContainer, emptyState, patients, loadPatients, patientForm);
    } catch (error) {
        if (requestId !== latestRequest) return;
        tableContainer.style.display = 'none';
        emptyState.style.display = 'block';
        showToast('error', 'Error', error.message || 'Failed to load patients.');
    } finally {
        if (!silent) toggleLoading(false);
    }
}
