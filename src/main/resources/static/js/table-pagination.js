/**
 * Tablo arama ve sayfalama (istemci tarafı).
 * Kullanim: <table class="paginated-table" id="myTable">
 * Sayfa basi varsayilan 10; secenekler 10 / 30 / 50.
 */
(function () {
    const DEFAULT_SIZE = 5;
    const SIZE_OPTIONS = [10, 30, 50];

    function normalizeText(value) {
        return String(value || '').toLocaleLowerCase('tr').trim();
    }

    function rowSearchText(row, skipSelector) {
        const cells = Array.from(row.children);
        return cells
            .filter(cell => !cell.matches(skipSelector))
            .map(cell => cell.innerText)
            .join(' ');
    }

    function initPaginatedTable(table) {
        if (table.dataset.paginationInitialized === 'true') return;
        table.dataset.paginationInitialized = 'true';

        const skipSelector = table.dataset.paginationSkip || '.col-check, .col-actions';
        const countTarget = table.dataset.paginationCount;
        const wrapper = table.closest('.data-table-wrapper') || table.parentElement;

        const controls = document.createElement('div');
        controls.className = 'table-controls';
        controls.innerHTML =
            '<div class="table-search">' +
            '  <label class="table-control-label" for="' + table.id + '-search">Ara</label>' +
            '  <input type="search" id="' + table.id + '-search" class="table-search-input"' +
            '         placeholder="Tabloda ara..." autocomplete="off" spellcheck="false">' +
            '</div>' +
            '<div class="table-page-size">' +
            '  <label class="table-control-label" for="' + table.id + '-size">Sayfa başına</label>' +
            '  <select id="' + table.id + '-size" class="table-page-size-select">' +
            SIZE_OPTIONS.map(size =>
                '<option value="' + size + '"' + (size === DEFAULT_SIZE ? ' selected' : '') + '>' + size + '</option>'
            ).join('') +
            '  </select>' +
            '</div>';

        const footer = document.createElement('div');
        footer.className = 'table-pagination';
        footer.innerHTML =
            '<p class="table-pagination-info" aria-live="polite"></p>' +
            '<div class="table-pagination-nav">' +
            '  <button type="button" class="btn-secondary btn-pagination" data-action="prev">Önceki</button>' +
            '  <span class="table-pagination-status"></span>' +
            '  <button type="button" class="btn-secondary btn-pagination" data-action="next">Sonraki</button>' +
            '</div>';

        const emptySearch = document.createElement('div');
        emptySearch.className = 'table-search-empty';
        emptySearch.hidden = true;
        emptySearch.textContent = 'Aramanızla eşleşen kayıt bulunamadı.';

        wrapper.insertBefore(controls, table);
        wrapper.appendChild(emptySearch);
        wrapper.appendChild(footer);

        const searchInput = controls.querySelector('.table-search-input');
        const sizeSelect = controls.querySelector('.table-page-size-select');
        const infoEl = footer.querySelector('.table-pagination-info');
        const statusEl = footer.querySelector('.table-pagination-status');
        const prevBtn = footer.querySelector('[data-action="prev"]');
        const nextBtn = footer.querySelector('[data-action="next"]');

        const state = {
            query: '',
            pageSize: DEFAULT_SIZE,
            currentPage: 1
        };

        function allRows() {
            const tbody = table.querySelector('tbody');
            return tbody ? Array.from(tbody.querySelectorAll('tr')) : [];
        }

        function matchedRows() {
            return allRows().filter(row => !row.classList.contains('paginate-no-match'));
        }

        function visibleRows() {
            return matchedRows().filter(row => !row.classList.contains('paginate-hidden'));
        }

        function totalPages(count) {
            return Math.max(1, Math.ceil(count / state.pageSize));
        }

        function updateCountBadge(total, filtered) {
            if (!countTarget) return;
            const el = document.getElementById(countTarget);
            if (!el) return;
            if (state.query && filtered !== total) {
                el.textContent = 'Toplam: ' + total + ' · Eşleşen: ' + filtered;
            } else {
                el.textContent = 'Toplam: ' + total;
            }
        }

        function render() {
            const rows = allRows();
            const query = normalizeText(state.query);

            rows.forEach(row => {
                const text = normalizeText(rowSearchText(row, skipSelector));
                const matches = !query || text.includes(query);
                row.classList.toggle('paginate-no-match', !matches);
            });

            const matched = matchedRows();
            const pages = totalPages(matched.length);

            if (state.currentPage > pages) {
                state.currentPage = pages;
            }
            if (state.currentPage < 1) {
                state.currentPage = 1;
            }

            const start = (state.currentPage - 1) * state.pageSize;
            const end = start + state.pageSize;

            matched.forEach((row, index) => {
                const onPage = index >= start && index < end;
                row.classList.toggle('paginate-hidden', !onPage);
            });

            const showing = matched.length;
            const visibleCount = showing === 0 ? 0 : Math.min(end, showing) - start;
            const from = showing === 0 ? 0 : start + 1;
            const to = showing === 0 ? 0 : start + visibleCount;

            infoEl.textContent = showing === 0
                ? (query ? 'Eşleşen kayıt yok' : 'Kayıt yok')
                : from + '–' + to + ' / ' + showing + ' kayıt gösteriliyor';

            statusEl.textContent = 'Sayfa ' + state.currentPage + ' / ' + pages;
            prevBtn.disabled = state.currentPage <= 1;
            nextBtn.disabled = state.currentPage >= pages || showing === 0;

            emptySearch.hidden = !(query && showing === 0);
            table.hidden = query && showing === 0;
            footer.hidden = rows.length === 0;
            controls.hidden = rows.length === 0;

            updateCountBadge(rows.length, matched.length);

            table.dispatchEvent(new CustomEvent('table-pagination-updated', {
                bubbles: true,
                detail: {
                    table,
                    matchedRows: matched,
                    visibleRows: visibleRows(),
                    currentPage: state.currentPage,
                    pageSize: state.pageSize,
                    query: state.query
                }
            }));
        }

        searchInput.addEventListener('input', () => {
            state.query = searchInput.value;
            state.currentPage = 1;
            render();
        });

        sizeSelect.addEventListener('change', () => {
            state.pageSize = parseInt(sizeSelect.value, 10) || DEFAULT_SIZE;
            state.currentPage = 1;
            render();
        });

        prevBtn.addEventListener('click', () => {
            if (state.currentPage > 1) {
                state.currentPage -= 1;
                render();
            }
        });

        nextBtn.addEventListener('click', () => {
            state.currentPage += 1;
            render();
        });

        table.addEventListener('table-sorted', () => render());

        render();

        return {
            table,
            getVisibleRows: visibleRows,
            getMatchedRows: matchedRows,
            refresh: render
        };
    }

    const instances = new Map();

    function boot() {
        document.querySelectorAll('table.paginated-table').forEach(table => {
            const instance = initPaginatedTable(table);
            if (table.id) {
                instances.set(table.id, instance);
            }
        });
    }

    window.TablePagination = {
        get(tableId) {
            return instances.get(tableId) || null;
        },
        refresh(tableId) {
            const instance = instances.get(tableId);
            if (instance) instance.refresh();
        }
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', boot);
    } else {
        boot();
    }
})();
