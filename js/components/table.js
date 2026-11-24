import { showToast } from './toast.js';
import { openModal } from './modal.js';
import { toggleLoading } from './loader.js';
import { confirmAction, escapeHtml } from '../utils/helpers.js';
import * as api from '../services/api.js';

export function renderTable(patientTableBody, tableContainer, emptyState, currentPatients, onPatientsUpdated, patientForm) {
    if (!patientTableBody) return;
    
    patientTableBody.innerHTML = '';
    
    if (currentPatients.length === 0) {
        tableContainer.style.display = 'none';
        emptyState.style.display = 'block';
        return;
    }
    
    tableContainer.style.display = 'block';
    emptyState.style.display = 'none';

    currentPatients.forEach(patient => {
        const tr = document.createElement('tr');
        
        let statusBadgeClass = 'badge-info';
        if (patient.status === 'Admitted') statusBadgeClass = 'badge-danger';
        if (patient.status === 'Discharged') statusBadgeClass = 'badge-success';
        if (patient.status === 'Under Review') statusBadgeClass = 'badge-warning';

        tr.innerHTML = `
            <td><span class="font-medium text-main">${escapeHtml(patient.id)}</span></td>
            <td>${escapeHtml(patient.fullName)}</td>
            <td>${escapeHtml(patient.age)}</td>
            <td>${escapeHtml(patient.gender)}</td>
            <td>${escapeHtml(patient.phone)}</td>
            <td><span class="font-medium">${escapeHtml(patient.bloodGroup)}</span></td>
            <td><span class="badge ${statusBadgeClass}">${escapeHtml(patient.status)}</span></td>
            <td>
                <div class="flex gap-2">
                    <button class="btn btn-secondary btn-sm action-view" data-id="${escapeHtml(patient.id)}" title="View">👁️</button>
                    <button class="btn btn-secondary btn-sm action-edit" data-id="${escapeHtml(patient.id)}" title="Edit">✏️</button>
                    <button class="btn btn-danger-outline btn-sm action-delete" data-id="${escapeHtml(patient.id)}" title="Delete">🗑️</button>
                </div>
            </td>
        `;
        
        patientTableBody.appendChild(tr);
    });

    attachTableActions(currentPatients, onPatientsUpdated, patientForm);
}

function attachTableActions(currentPatients, onPatientsUpdated, patientForm) {
    document.querySelectorAll('.action-view').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            const id = e.currentTarget.getAttribute('data-id');
            try {
                toggleLoading(true, 'Loading Patient...');
                const p = await api.getPatient(id);
                const details = [
                    `${p.age} yrs`, p.gender, p.bloodGroup, p.phone, p.email || 'no email',
                    p.address || 'no address', `Emergency: ${p.emergencyContact || 'n/a'}`, p.status
                ].map(escapeHtml).join(' · ');
                showToast('info', `${escapeHtml(p.fullName)} (${escapeHtml(p.id)})`, details);
            } catch (error) {
                showToast('error', 'Error', error.message || 'Failed to load patient details.');
            } finally {
                toggleLoading(false);
            }
        });
    });

    document.querySelectorAll('.action-edit').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const id = e.currentTarget.getAttribute('data-id');
            
            document.getElementById('modalTitle').textContent = 'Edit Patient';
            let idInput = document.getElementById('patientId');
            if(!idInput) {
                idInput = document.createElement('input');
                idInput.type = 'hidden';
                idInput.id = 'patientId';
                patientForm.appendChild(idInput);
            }
            idInput.value = id;
            
            const patient = currentPatients.find(p => p.id === id);
            if (patient) {
                const formElements = patientForm.elements;
                if(formElements['fullName']) formElements['fullName'].value = patient.fullName ?? '';
                if(formElements['age']) formElements['age'].value = patient.age ?? '';
                if(formElements['gender']) formElements['gender'].value = patient.gender ?? '';
                if(formElements['phone']) formElements['phone'].value = patient.phone ?? '';
                if(formElements['email']) formElements['email'].value = patient.email ?? '';
                if(formElements['bloodGroup']) formElements['bloodGroup'].value = patient.bloodGroup ?? '';
                if(formElements['status']) formElements['status'].value = patient.status ?? 'Pending';
                if(formElements['address']) formElements['address'].value = patient.address ?? '';
                if(formElements['emergencyContact']) formElements['emergencyContact'].value = patient.emergencyContact ?? '';
            }
            
            openModal('patientModal');
        });
    });

    document.querySelectorAll('.action-delete').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const id = e.currentTarget.getAttribute('data-id');
            
            confirmAction('Are you sure you want to delete this patient record? This action cannot be undone.', async () => {
                try {
                    toggleLoading(true, 'Deleting...');
                    await api.deletePatient(id);
                    showToast('success', 'Deleted', `Patient record ${escapeHtml(id)} has been deleted.`);
                    if (typeof onPatientsUpdated === 'function') {
                        await onPatientsUpdated();
                    }
                } catch (error) {
                    showToast('error', 'Error', error.message || 'Failed to delete patient.');
                } finally {
                    toggleLoading(false);
                }
            });
        });
    });
}
