/**
 * Taleplerim listesinde talep iptali — onay modali ile POST.
 */
(function () {
    document.querySelectorAll('.cancel-ticket-form').forEach(form => {
        form.addEventListener('submit', (e) => {
            e.preventDefault();
            const btn = form.querySelector('button[type="submit"]');
            const title = btn?.getAttribute('data-ticket-title') || 'Bu talep';

            window.ConfirmModal.open({
                title: 'Talebi İptal Et',
                message: `"${title}" talebini iptal etmek istediğinize emin misiniz?\n\nTalep "İptal Edildi" durumuna geçecektir.`,
                variant: 'danger',
                confirmText: 'Evet, İptal Et',
                onConfirm: () => {
                    window.ConfirmModal.close();
                    form.submit();
                }
            });
        });
    });
})();
