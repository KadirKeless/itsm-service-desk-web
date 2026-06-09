/**
 * Onay modali — desktop JOptionPane benzeri.
 * Yalnizca Evet / Hayir. Hata olursa detay ayni modal icinde gosterilir.
 */
(function () {
    let overlay, titleEl, messageEl, errorEl, confirmBtn, cancelBtn;
    let onConfirmHandler = null;

    function ensureDom() {
        if (overlay) return;
        overlay = document.getElementById('confirmModal');
        if (!overlay) return;
        titleEl = document.getElementById('confirmModalTitle');
        messageEl = document.getElementById('confirmModalMessage');
        errorEl = document.getElementById('confirmModalError');
        confirmBtn = document.getElementById('confirmModalConfirm');
        cancelBtn = document.getElementById('confirmModalCancel');

        cancelBtn?.addEventListener('click', close);
        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) close();
        });
        confirmBtn?.addEventListener('click', async () => {
            if (!onConfirmHandler || confirmBtn.disabled) {
                return;
            }
            confirmBtn.disabled = true;
            try {
                await onConfirmHandler();
            } finally {
                if (!errorEl || errorEl.hidden) {
                    confirmBtn.disabled = false;
                }
            }
        });
    }

    function setVariant(variant) {
        if (!overlay) return;
        overlay.dataset.variant = variant || 'default';
    }

    function hideError() {
        if (!errorEl) return;
        errorEl.hidden = true;
        errorEl.textContent = '';
        if (confirmBtn) {
            confirmBtn.disabled = false;
            confirmBtn.hidden = false;
        }
        if (cancelBtn) {
            cancelBtn.hidden = false;
            cancelBtn.textContent = 'Hayır';
        }
    }

    function showError(message) {
        if (!errorEl) return;
        errorEl.textContent = message || 'İşlem gerçekleştirilemedi.';
        errorEl.hidden = false;
        if (confirmBtn) {
            confirmBtn.disabled = true;
        }
    }

    function open(options) {
        ensureDom();
        if (!overlay) return;

        hideError();
        setVariant(options.variant);

        titleEl.textContent = options.title || 'Onay';
        messageEl.textContent = options.message || '';
        confirmBtn.textContent = options.confirmText || 'Evet';
        cancelBtn.textContent = options.cancelText || 'Hayır';
        confirmBtn.hidden = options.hideConfirm === true;
        cancelBtn.hidden = options.hideCancel === true;

        onConfirmHandler = options.onConfirm || null;
        overlay.style.display = 'flex';
    }

    function close() {
        if (!overlay) return;
        overlay.style.display = 'none';
        hideError();
        onConfirmHandler = null;
    }

    /** Bilgi / uyari — yalnizca Hayir (Tamam) */
    function alertDialog(options) {
        open({
            title: options.title || 'Bilgi',
            message: options.message || '',
            variant: options.variant || 'default',
            hideConfirm: true,
            cancelText: 'Tamam',
            onConfirm: null
        });
        if (options.message) {
            messageEl.textContent = options.message;
        }
    }

    window.ConfirmModal = {
        open,
        close,
        showError,
        alert: alertDialog
    };
})();
