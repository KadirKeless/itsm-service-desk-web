/**
 * Admin kullanici sayfasi — AJAX islemler + onay modali.
 */
(function () {
    const API = '/api/admin/users';

    function csrfHeaders() {
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
        return token ? { [header]: token } : {};
    }

    function formatApiMessage(data) {
        if (!data || typeof data !== 'object') {
            return 'Sunucu yanıtı işlenemedi.';
        }

        // Spring Boot varsayilan hata govdesi (404, 403 vb.)
        if (typeof data.status === 'number' && data.status >= 400) {
            if (data.status === 404) {
                return 'İşlem endpoint\'i bulunamadı. Lütfen uygulamayı yeniden başlatın.';
            }
            if (data.status === 403) {
                return 'Bu işlem için yetkiniz yok.';
            }
            if (data.message && typeof data.message === 'string' && data.message !== 'No message available') {
                return data.message;
            }
            if (data.error) {
                return String(data.error);
            }
        }

        let message = data.message || data.detail || '';
        if (typeof message !== 'string') {
            message = String(message || '');
        }

        if (data.fieldErrors && typeof data.fieldErrors === 'object' && data.success === false) {
            const details = Object.values(data.fieldErrors)
                .filter(Boolean)
                .join('\n');
            if (details) {
                message = message ? message + '\n\n' + details : details;
            }
        }

        return message.trim() || 'İşlem gerçekleştirilemedi.';
    }

    function parseApiResponse(data, res) {
        if (data && typeof data.success === 'boolean') {
            return {
                success: data.success,
                message: data.success ? (data.message || '') : formatApiMessage(data),
                fieldErrors: data.fieldErrors || null
            };
        }

        if (res && !res.ok) {
            return {
                success: false,
                message: formatApiMessage(data),
                fieldErrors: data?.fieldErrors || null
            };
        }

        return {
            success: false,
            message: formatApiMessage(data),
            fieldErrors: null
        };
    }

    async function postJson(url, params) {
        const body = new URLSearchParams();
        Object.entries(params || {}).forEach(([key, value]) => {
            if (Array.isArray(value)) {
                value.forEach(v => body.append(key, v));
            } else if (value != null && value !== '') {
                body.append(key, value);
            }
        });
        try {
            const res = await fetch(url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    Accept: 'application/json',
                    ...csrfHeaders()
                },
                body
            });
            const data = await res.json();
            return parseApiResponse(data, res);
        } catch (e) {
            return { success: false, message: 'Sunucuya bağlanılamadı veya yanıt okunamadı.', fieldErrors: null };
        }
    }

    async function postFormData(url, formData) {
        try {
            const res = await fetch(url, {
                method: 'POST',
                headers: {
                    Accept: 'application/json',
                    ...csrfHeaders()
                },
                body: formData
            });
            const data = await res.json();
            return parseApiResponse(data, res);
        } catch (e) {
            return { success: false, message: 'Sunucuya bağlanılamadı veya yanıt okunamadı.', fieldErrors: null };
        }
    }

    function reloadOnSuccess(data) {
        if (data.success && window.Toast) {
            window.Toast.success(data.message);
        }
        window.setTimeout(() => window.location.reload(), data.success ? 400 : 0);
    }

    const STATUS_LABELS = {
        pending: 'Onay Bekliyor',
        active: 'Aktif',
        frozen: 'Dondurulmuş'
    };

    function selectLabel(select) {
        if (!select || !select.value) return '—';
        return select.options[select.selectedIndex]?.text?.trim() || '—';
    }

    function getRowDetails(row) {
        if (!row) {
            return { id: '', name: '—', email: '—', role: '—', department: '—', status: '' };
        }
        const cells = row.querySelectorAll('td');
        return {
            id: row.dataset.userId || '',
            name: row.dataset.name || cells[2]?.innerText?.trim() || '—',
            email: row.dataset.email || cells[3]?.innerText?.trim() || '—',
            role: row.dataset.roleName || cells[4]?.innerText?.trim() || '—',
            department: row.dataset.deptName || cells[5]?.innerText?.trim() || '—',
            status: row.dataset.status || ''
        };
    }

    function formatUserBlock(details, overrides) {
        const role = overrides?.role ?? details.role;
        const department = overrides?.department ?? details.department;
        const lines = [
            'Ad Soyad: ' + details.name,
            'E-posta: ' + details.email,
            'Rol: ' + (role || '—'),
            'Departman: ' + (department || '—')
        ];
        if (details.status) {
            lines.push('Durum: ' + (STATUS_LABELS[details.status] || details.status));
        }
        return lines.join('\n');
    }

    function resolveAssignValues(row, bulkRole, bulkDept) {
        const details = getRowDetails(row);
        if (details.status === 'pending') {
            return {
                role: selectLabel(bulkRole),
                department: selectLabel(bulkDept)
            };
        }
        return {
            role: details.role,
            department: details.department
        };
    }

    function buildActivateMessage(rows, bulkRole, bulkDept) {
        if (rows.length === 1) {
            const assign = resolveAssignValues(rows[0], bulkRole, bulkDept);
            return 'Aşağıdaki kullanıcı aktifleştirilecek:\n\n'
                + formatUserBlock(getRowDetails(rows[0]), assign)
                + '\n\nDevam etmek istiyor musunuz?';
        }

        let message = 'Seçili ' + rows.length + ' kullanıcı aktifleştirilecek:\n\n';
        rows.forEach((row, index) => {
            const assign = resolveAssignValues(row, bulkRole, bulkDept);
            message += (index + 1) + ')\n' + formatUserBlock(getRowDetails(row), assign) + '\n\n';
        });
        return message.trim() + '\n\nDevam etmek istiyor musunuz?';
    }

    function buildFreezeMessage(rows) {
        if (rows.length === 1) {
            return 'Aşağıdaki kullanıcının hesabı dondurulacak.\nBu kullanıcı sisteme giriş yapamayacaktır.\n\n'
                + formatUserBlock(getRowDetails(rows[0]))
                + '\n\nDevam etmek istiyor musunuz?';
        }

        let message = 'Seçili ' + rows.length + ' kullanıcının hesabı dondurulacak.\nKullanıcılar sisteme giriş yapamayacaktır.\n\n';
        rows.forEach((row, index) => {
            message += (index + 1) + ')\n' + formatUserBlock(getRowDetails(row)) + '\n\n';
        });
        return message.trim() + '\n\nDevam etmek istiyor musunuz?';
    }

    function buildDeleteMessage(rows) {
        if (rows.length === 1) {
            return 'Aşağıdaki kullanıcı sistemden tamamen silinecek!\nBu işlem GERİ ALINAMAZ.\n\n'
                + formatUserBlock(getRowDetails(rows[0]))
                + '\n\nDevam etmek istiyor musunuz?';
        }

        let message = 'Seçili ' + rows.length + ' kullanıcı sistemden tamamen silinecek!\nBu işlem GERİ ALINAMAZ.\n\n';
        rows.forEach((row, index) => {
            message += (index + 1) + ')\n' + formatUserBlock(getRowDetails(row)) + '\n\n';
        });
        return message.trim() + '\n\nDevam etmek istiyor musunuz?';
    }

    function runConfirmed(options) {
        ConfirmModal.open({
            title: options.title,
            message: options.message,
            variant: options.variant || 'warning',
            confirmText: options.confirmText || 'Evet',
            cancelText: 'Hayır',
            onConfirm: async () => {
                const data = await options.action();
                if (!data.success) {
                    ConfirmModal.showError(data.message);
                    return;
                }
                ConfirmModal.close();
                reloadOnSuccess(data);
            }
        });
    }

    function allRows() {
        return Array.from(document.querySelectorAll('#usersTable tbody tr'));
    }

    function selectedRows() {
        return allRows().filter(r => r.querySelector('.row-select')?.checked);
    }

    function selectedIds() {
        return selectedRows().map(r => r.dataset.userId);
    }

    function isAdminRow(row) {
        return row?.dataset?.isAdmin === 'true';
    }

    function selectionHasAdmin(rows) {
        return rows.some(isAdminRow);
    }

    function bindRowActions() {
        document.querySelectorAll('[data-user-action]').forEach(btn => {
            btn.addEventListener('click', () => {
                const action = btn.dataset.userAction;
                const id = btn.dataset.userId;
                const row = btn.closest('tr');

                if (action === 'activate') {
                    runConfirmed({
                        title: 'Kullanıcı Aktifleştir',
                        message: buildActivateMessage([row], null, null),
                        variant: 'default',
                        action: () => postJson(API + '/' + id + '/activate', {})
                    });
                    return;
                }

                if (action === 'freeze') {
                    runConfirmed({
                        title: 'Kullanıcı Dondur',
                        message: buildFreezeMessage([row]),
                        variant: 'warning',
                        action: () => postJson(API + '/' + id + '/freeze', {})
                    });
                    return;
                }

                if (action === 'delete') {
                    runConfirmed({
                        title: 'Kullanıcı Sil',
                        message: buildDeleteMessage([row]),
                        variant: 'danger',
                        action: () => postJson(API + '/' + id + '/delete', {})
                    });
                }
            });
        });
    }

    function bindBulkActions() {
        const bulkRole = document.getElementById('bulkRole');
        const bulkDept = document.getElementById('bulkDept');

        document.getElementById('btnBulkActivate')?.addEventListener('click', () => {
            const selected = selectedRows();
            if (selected.length === 0) return;

            if (selected.some(r => r.dataset.status === 'active')) {
                ConfirmModal.alert({
                    title: 'Hatalı Seçim',
                    message: selected.length === 1
                        ? selected[0].dataset.name + ' zaten Aktif durumda.'
                        : 'Seçili kullanıcılar arasında zaten Aktif olanlar bulunuyor.',
                    variant: 'warning'
                });
                return;
            }

            const needsAssign = selected.some(r => r.dataset.status === 'pending');
            if (needsAssign && (!bulkRole?.value || !bulkDept?.value)) {
                ConfirmModal.alert({
                    title: 'Eksik Bilgi',
                    message: 'Onay bekleyen kullanıcılar için rol ve departman seçmelisiniz!',
                    variant: 'warning'
                });
                return;
            }

            runConfirmed({
                title: selected.length === 1 ? 'Kullanıcı Aktifleştir' : 'Toplu Aktifleştir',
                message: buildActivateMessage(selected, bulkRole, bulkDept),
                action: () => postJson(API + '/bulk/activate', {
                    userIds: selectedIds(),
                    roleId: bulkRole?.value,
                    departmentId: bulkDept?.value
                })
            });
        });

        document.getElementById('btnBulkFreeze')?.addEventListener('click', () => {
            const selected = selectedRows();
            if (selected.length === 0) return;

            if (selected.some(r => r.dataset.status !== 'active')) {
                ConfirmModal.alert({
                    title: 'Hatalı Seçim',
                    message: 'Toplu dondurma için lütfen SADECE Aktif kullanıcıları seçiniz.',
                    variant: 'warning'
                });
                return;
            }
            if (selectionHasAdmin(selected)) {
                ConfirmModal.alert({
                    title: 'Kritik Uyarı',
                    message: 'Seçili kullanıcılar arasında Admin hesabı bulunuyor!',
                    variant: 'danger'
                });
                return;
            }

            runConfirmed({
                title: selected.length === 1 ? 'Kullanıcı Dondur' : 'Toplu Dondur',
                message: buildFreezeMessage(selected),
                variant: 'warning',
                action: () => postJson(API + '/bulk/freeze', { userIds: selectedIds() })
            });
        });

        document.getElementById('btnBulkDelete')?.addEventListener('click', () => {
            const selected = selectedRows();
            if (selected.length === 0) return;

            if (selectionHasAdmin(selected)) {
                ConfirmModal.alert({
                    title: 'Kritik Hata',
                    message: 'Seçilen kullanıcılar arasında Admin hesabı bulunuyor! Admin hesapları silinemez.',
                    variant: 'danger'
                });
                return;
            }

            runConfirmed({
                title: selected.length === 1 ? 'Kullanıcı Sil' : 'Toplu Sil',
                message: buildDeleteMessage(selected),
                variant: 'danger',
                action: () => postJson(API + '/bulk/delete', { userIds: selectedIds() })
            });
        });
    }

    function clearModalFieldErrors(form) {
        form.querySelectorAll('.field-error').forEach(el => {
            el.textContent = '';
            el.hidden = true;
        });
    }

    function showModalFieldErrors(form, fieldErrors) {
        if (!fieldErrors) return;
        Object.entries(fieldErrors).forEach(([field, message]) => {
            const input = form.querySelector('[name="' + field + '"]');
            const group = input?.closest('.form-group');
            let err = group?.querySelector('.field-error');
            if (!err && group) {
                err = document.createElement('span');
                err.className = 'field-error';
                group.appendChild(err);
            }
            if (err) {
                err.textContent = message;
                err.hidden = false;
            }
        });
    }

    function showModalBanner(bannerEl, message) {
        if (!bannerEl) return;
        if (message) {
            bannerEl.textContent = message;
            bannerEl.hidden = false;
        } else {
            bannerEl.hidden = true;
            bannerEl.textContent = '';
        }
    }

    function bindCreateForm() {
        const form = document.getElementById('createUserForm');
        const banner = document.getElementById('createModalError');
        if (!form) return;

        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            clearModalFieldErrors(form);
            showModalBanner(banner, null);

            const submitBtn = form.querySelector('[type="submit"]');
            if (submitBtn) submitBtn.disabled = true;

            try {
                const data = await postFormData(API, new FormData(form));
                if (!data.success) {
                    showModalBanner(banner, data.message);
                    showModalFieldErrors(form, data.fieldErrors);
                    return;
                }
                closeCreateModal();
                if (window.Toast) window.Toast.success(data.message);
                window.setTimeout(() => window.location.reload(), 400);
            } finally {
                if (submitBtn) submitBtn.disabled = false;
            }
        });
    }

    function bindEditForm() {
        const form = document.getElementById('editUserForm');
        const banner = document.getElementById('editModalError');
        if (!form) return;

        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            clearModalFieldErrors(form);
            showModalBanner(banner, null);

            const userId = document.getElementById('editUserIdHidden')?.value;
            if (!userId) return;

            const submitBtn = form.querySelector('[type="submit"]');
            if (submitBtn) submitBtn.disabled = true;

            try {
                const data = await postFormData(API + '/' + userId + '/edit', new FormData(form));
                if (!data.success) {
                    showModalBanner(banner, data.message);
                    showModalFieldErrors(form, data.fieldErrors);
                    return;
                }
                closeEditModal();
                if (window.Toast) window.Toast.success(data.message);
                window.setTimeout(() => window.location.reload(), 400);
            } finally {
                if (submitBtn) submitBtn.disabled = false;
            }
        });
    }

    function openResetPasswordModal(userId, userName) {
        const form = document.getElementById('resetPasswordForm');
        const modal = document.getElementById('resetPasswordModal');
        const banner = document.getElementById('resetPasswordModalError');
        if (!form || !modal) return;

        const label = document.getElementById('resetPasswordUserLabel');
        if (label) {
            label.textContent = (userName || 'Kullanıcı') + ' için yeni şifre belirleyin.';
        }
        document.getElementById('resetPasswordUserId').value = userId || '';
        const newPass = form.querySelector('[name="newPassword"]');
        const confirmPass = form.querySelector('[name="confirmPassword"]');
        if (newPass) newPass.value = '';
        if (confirmPass) confirmPass.value = '';
        clearModalFieldErrors(form);
        showModalBanner(banner, null);
        modal.style.display = 'flex';
        newPass?.focus();
    }

    function bindResetPasswordForm() {
        const form = document.getElementById('resetPasswordForm');
        const modal = document.getElementById('resetPasswordModal');
        const banner = document.getElementById('resetPasswordModalError');
        if (!form || !modal) return;

        window.openResetPasswordModal = openResetPasswordModal;

        document.querySelectorAll('.reset-password-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                openResetPasswordModal(btn.dataset.userId, btn.dataset.userName);
            });
        });

        document.getElementById('resetPasswordCancel')?.addEventListener('click', () => {
            modal.style.display = 'none';
        });

        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            clearModalFieldErrors(form);
            showModalBanner(banner, null);

            const userId = document.getElementById('resetPasswordUserId')?.value;
            if (!userId) return;

            const submitBtn = form.querySelector('[type="submit"]');
            if (submitBtn) submitBtn.disabled = true;

            try {
                const data = await postFormData(API + '/' + userId + '/password', new FormData(form));
                if (!data.success) {
                    showModalBanner(banner, data.message);
                    showModalFieldErrors(form, data.fieldErrors);
                    return;
                }
                modal.style.display = 'none';
                if (window.Toast) window.Toast.success(data.message);
                window.setTimeout(() => window.location.reload(), 400);
            } finally {
                if (submitBtn) submitBtn.disabled = false;
            }
        });
    }

    function boot() {
        bindRowActions();
        bindBulkActions();
        bindCreateForm();
        bindEditForm();
        bindResetPasswordForm();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', boot);
    } else {
        boot();
    }
})();
