/**
 * Toast bildirimleri (toastr benzeri).
 * window.Toast.success|error|info|warning(message)
 */
(function () {
    const DEFAULT_DURATION = {
        success: 4500,
        error: 7000,
        info: 5000,
        warning: 6000
    };

    const ICONS = {
        success: 'bi-check-circle-fill',
        error: 'bi-x-circle-fill',
        info: 'bi-info-circle-fill',
        warning: 'bi-exclamation-triangle-fill'
    };

    let container;

    function ensureContainer() {
        if (container) return container;
        container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'toast-container';
        container.setAttribute('aria-live', 'polite');
        container.setAttribute('aria-atomic', 'true');
        document.body.appendChild(container);
        return container;
    }

    function removeToast(toast) {
        if (!toast || toast.dataset.removing === 'true') return;
        toast.dataset.removing = 'true';
        toast.classList.add('toast-hide');
        window.setTimeout(() => toast.remove(), 220);
    }

    function show(message, type) {
        const text = String(message || '').trim();
        if (!text) return null;

        const toastType = type || 'info';
        const box = ensureContainer();
        const toast = document.createElement('div');
        toast.className = 'toast toast-' + toastType;
        toast.setAttribute('role', toastType === 'error' ? 'alert' : 'status');

        const icon = document.createElement('i');
        icon.className = 'bi ' + (ICONS[toastType] || ICONS.info);
        icon.setAttribute('aria-hidden', 'true');

        const body = document.createElement('span');
        body.className = 'toast-message';
        body.textContent = text;

        const closeBtn = document.createElement('button');
        closeBtn.type = 'button';
        closeBtn.className = 'toast-close';
        closeBtn.setAttribute('aria-label', 'Kapat');
        closeBtn.innerHTML = '&times;';
        closeBtn.addEventListener('click', () => removeToast(toast));

        toast.appendChild(icon);
        toast.appendChild(body);
        toast.appendChild(closeBtn);
        box.appendChild(toast);

        requestAnimationFrame(() => toast.classList.add('toast-show'));

        const duration = DEFAULT_DURATION[toastType] || DEFAULT_DURATION.info;
        let timer = window.setTimeout(() => removeToast(toast), duration);
        toast.addEventListener('mouseenter', () => window.clearTimeout(timer));
        toast.addEventListener('mouseleave', () => {
            timer = window.setTimeout(() => removeToast(toast), 1800);
        });

        return toast;
    }

    function restoreScrollIfNeeded() {
        const raw = document.documentElement.getAttribute('data-restore-scroll');
        if (raw == null) return;

        document.documentElement.removeAttribute('data-restore-scroll');
        const top = parseInt(raw, 10);

        const reveal = () => document.documentElement.classList.remove('scroll-pending');

        if (Number.isNaN(top)) {
            reveal();
            return;
        }

        window.scrollTo(0, top);
        requestAnimationFrame(() => {
            window.scrollTo(0, top);
            requestAnimationFrame(reveal);
        });
    }

    function bootFlashMessages() {
        const data = document.getElementById('flash-messages-data');
        if (!data) return;

        if (data.dataset.success) show(data.dataset.success, 'success');
        if (data.dataset.error) show(data.dataset.error, 'error');
        if (data.dataset.info) show(data.dataset.info, 'info');
        if (data.dataset.warning) show(data.dataset.warning, 'warning');

        data.remove();
    }

    function boot() {
        restoreScrollIfNeeded();
        bootFlashMessages();
    }

    window.Toast = {
        show,
        success: (message) => show(message, 'success'),
        error: (message) => show(message, 'error'),
        info: (message) => show(message, 'info'),
        warning: (message) => show(message, 'warning')
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', boot);
    } else {
        boot();
    }
})();
