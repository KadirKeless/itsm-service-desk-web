(function () {
    function initSortableTable(table) {
        if (table.dataset.sortInitialized === 'true') return;
        table.dataset.sortInitialized = 'true';

        const headers = table.querySelectorAll('thead th[data-sort]');
        headers.forEach(header => {
            header.style.cursor = 'pointer';
            header.style.userSelect = 'none';
            let asc = true;

            header.addEventListener('click', () => {
                const colIdx = Array.from(header.parentElement.children).indexOf(header);
                const tbody = table.querySelector('tbody');
                if (!tbody) return;

                const rows = Array.from(tbody.querySelectorAll('tr'));
                const sortType = header.dataset.sort || 'string';

                rows.sort((a, b) => {
                    const cellA = a.children[colIdx];
                    const cellB = b.children[colIdx];
                    if (!cellA || !cellB) return 0;

                    let va = cellA.dataset.sortValue ?? cellA.innerText.trim();
                    let vb = cellB.dataset.sortValue ?? cellB.innerText.trim();

                    if (sortType === 'number' || sortType === 'priority' || sortType === 'status') {
                        va = parseFloat(va) || 0;
                        vb = parseFloat(vb) || 0;
                    } else if (sortType === 'date') {
                        va = va || '0000';
                        vb = vb || '0000';
                    } else {
                        va = String(va).toLocaleLowerCase('tr');
                        vb = String(vb).toLocaleLowerCase('tr');
                    }

                    if (va < vb) return asc ? -1 : 1;
                    if (va > vb) return asc ? 1 : -1;
                    return 0;
                });

                asc = !asc;
                rows.forEach(r => tbody.appendChild(r));

                table.querySelectorAll('thead th').forEach(h => h.classList.remove('sort-asc', 'sort-desc'));
                header.classList.add(asc ? 'sort-desc' : 'sort-asc');

                table.dispatchEvent(new CustomEvent('table-sorted', { bubbles: true }));
            });
        });
    }

    function boot() {
        document.querySelectorAll('table.sortable-table').forEach(initSortableTable);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', boot);
    } else {
        boot();
    }
})();
