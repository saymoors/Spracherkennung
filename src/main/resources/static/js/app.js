const API_URL = window.location.origin;

let refreshInterval = null;
let currentPage = 0;
let hasMore = false;
let isLoading = false;

document.addEventListener('DOMContentLoaded', function() {
    const fileInput = document.getElementById('fileInput');
    const fileLabel = document.getElementById('fileLabel');

    if (fileInput && fileLabel) {
        fileInput.addEventListener('change', function(e) {
            const fileName = e.target.files[0]?.name;
            if (fileName) {
                const shortName = fileName.length > 30 ? fileName.substring(0, 27) + '...' : fileName;
                fileLabel.innerText = shortName;
                fileLabel.classList.add('has-file');
            } else {
                fileLabel.innerText = 'Выберите файл';
                fileLabel.classList.remove('has-file');
            }
        });
    }

    const loadMoreBtn = document.getElementById('loadMoreBtn');
    if (loadMoreBtn) {
        loadMoreBtn.addEventListener('click', loadMoreHistory);
    }

    document.getElementById('registerBtn').addEventListener('click', register);
    document.getElementById('loginBtn').addEventListener('click', login);
    document.getElementById('showLoginBtn').addEventListener('click', showLogin);
    document.getElementById('showRegisterBtn').addEventListener('click', showRegister);
    document.getElementById('logoutBtn').addEventListener('click', logout);
    document.getElementById('uploadBtn').addEventListener('click', uploadFile);
    document.getElementById('recognizeAgainBtn').addEventListener('click', recognizeAgainFile);
    document.getElementById('historyList').addEventListener('click', handleHistoryClick);
    document.getElementById('modalBody').addEventListener('click', handleModalClick);
    document.getElementById('detailsModal').addEventListener('click', handleModalOverlayClick);
});

function showRegister() {
    document.getElementById('registerForm').hidden = false;
    document.getElementById('loginForm').hidden = true;
    clearErrors();
}

function showLogin() {
    document.getElementById('registerForm').hidden = true;
    document.getElementById('loginForm').hidden = false;
    clearErrors();
}

function clearErrors() {
    clearMessage(document.getElementById('regError'));
    clearMessage(document.getElementById('loginError'));
    clearInvalidFields(
        document.getElementById('regLogin'),
        document.getElementById('regEmail'),
        document.getElementById('regPassword'),
        document.getElementById('loginLogin'),
        document.getElementById('loginPassword')
    );
}

function clearMessage(element) {
    element.innerText = '';
    element.classList.remove('message-error', 'message-success');
}

function showMessage(element, message, className) {
    element.innerText = message;
    element.classList.remove('message-error', 'message-success');
    element.classList.add(className);
}

function showError(element, message) {
    showMessage(element, message, 'message-error');
}

function showSuccess(element, message) {
    showMessage(element, message, 'message-success');
}

function markInvalid(element) {
    element.classList.add('is-invalid');
}

function clearInvalidFields(...elements) {
    elements.forEach(element => element?.classList.remove('is-invalid'));
}

async function register() {
    const loginInput = document.getElementById('regLogin');
    const emailInput = document.getElementById('regEmail');
    const passwordInput = document.getElementById('regPassword');
    const login = loginInput.value.trim();
    const email = emailInput.value.trim();
    const password = passwordInput.value;

    const regError = document.getElementById('regError');
    clearMessage(regError);
    clearInvalidFields(loginInput, emailInput, passwordInput);

    if (!login) {
        showError(regError, 'Введите логин');
        markInvalid(loginInput);
        return;
    }
    if (!email) {
        showError(regError, 'Введите email');
        markInvalid(emailInput);
        return;
    }
    if (!password) {
        showError(regError, 'Введите пароль');
        markInvalid(passwordInput);
        return;
    }

    try {
        const response = await fetch(`${API_URL}/api/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ login, email, password })
        });

        if (response.status === 201) {
            loginInput.value = '';
            emailInput.value = '';
            passwordInput.value = '';

            document.getElementById('registerForm').hidden = true;
            document.getElementById('loginForm').hidden = false;

            const loginErrorDiv = document.getElementById('loginError');
            showSuccess(loginErrorDiv, 'Регистрация успешна! Теперь войдите');

            setTimeout(() => {
                if (loginErrorDiv.innerText === 'Регистрация успешна! Теперь войдите') {
                    clearMessage(loginErrorDiv);
                }
            }, 5000);
        } else {
            const errorText = await response.text();
            showError(regError, errorText || 'Ошибка регистрации');
        }
    } catch (error) {
        showError(regError, 'Ошибка соединения с сервером');
    }
}

async function login() {
    const loginInput = document.getElementById('loginLogin');
    const passwordInput = document.getElementById('loginPassword');
    const login = loginInput.value.trim();
    const password = passwordInput.value;

    const loginError = document.getElementById('loginError');
    clearMessage(loginError);
    clearInvalidFields(loginInput, passwordInput);

    if (!login) {
        showError(loginError, 'Введите логин');
        markInvalid(loginInput);
        return;
    }
    if (!password) {
        showError(loginError, 'Введите пароль');
        markInvalid(passwordInput);
        return;
    }

    try {
        const response = await fetch(`${API_URL}/api/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ login: login, password: password })
        });

        if (response.status === 200) {
            const data = await response.json();
            localStorage.setItem('token', data.token);
            showMainApp();
            loadHistory(true);
            startAutoRefresh();
        } else {
            const errorText = await response.text();
            showError(loginError, errorText || 'Неверный логин или пароль');
            markInvalid(loginInput);
            markInvalid(passwordInput);
        }
    } catch (error) {
        showError(loginError, 'Ошибка соединения с сервером');
    }
}

function logout() {
    localStorage.removeItem('token');
    if (refreshInterval) {
        clearInterval(refreshInterval);
        refreshInterval = null;
    }
    showLogin();
    document.getElementById('mainApp').hidden = true;
}

function startAutoRefresh() {
    if (refreshInterval) {
        clearInterval(refreshInterval);
    }
    refreshInterval = setInterval(() => {
        if (localStorage.getItem('token')) {
            refreshHistory();
        }
    }, 5000);
}

async function refreshHistory() {
    if (isLoading) return;

    const token = getToken();
    if (!token) return;

    const lastVisiblePage = currentPage;
    const refreshedItems = [];
    isLoading = true;

    try {
        let loadedLastPage = 0;
        for (let page = 0; page <= lastVisiblePage; page++) {
            const response = await fetch(`${API_URL}/api/transcriptions?page=${page}&size=5`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });

            if (response.status === 200) {
                const data = await response.json();
                hasMore = Boolean(data.hasMore);
                refreshedItems.push(...(data.content || []));
                loadedLastPage = page;

                if (!hasMore) {
                    break;
                }
            } else if (response.status === 401) {
                logout();
                return;
            } else {
                const errorText = await response.text();
                console.error('Ошибка обновления истории:', errorText);
                return;
            }
        }

        currentPage = loadedLastPage;
        renderHistory(refreshedItems, true);
        updateLoadMoreVisibility();
    } catch (error) {
        console.error('Ошибка обновления истории:', error);
    } finally {
        isLoading = false;
    }
}

function showMainApp() {
    if (!getToken()) {
        showLogin();
        return;
    }

    document.getElementById('registerForm').hidden = true;
    document.getElementById('loginForm').hidden = true;
    document.getElementById('mainApp').hidden = false;

    const token = getToken();
    if (token) {
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            const login = payload.sub || 'Пользователь';
            document.getElementById('userLogin').innerText = login;
        } catch(e) {
            document.getElementById('userLogin').innerText = 'Пользователь';
        }
    }
}

function getToken() {
    return localStorage.getItem('token');
}

async function loadHistory(reset = true) {
    const token = getToken();
    if (!token) {
        logout();
        return;
    }

    if (reset) {
        currentPage = 0;
        const container = document.getElementById('historyList');
        if (container) {
            container.innerHTML = '<div class="loading">Загрузка...</div>';
        }
    }

    if (isLoading) return;
    isLoading = true;

    try {
        const response = await fetch(`${API_URL}/api/transcriptions?page=${currentPage}&size=5`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.status === 200) {
            const data = await response.json();
            hasMore = Boolean(data.hasMore);
            renderHistory(data.content || [], reset);

            updateLoadMoreVisibility();
        } else if (response.status === 401) {
            logout();
        } else {
            const errorText = await response.text();
            console.error('Ошибка загрузки истории:', errorText);
        }
    } catch (error) {
        console.error('Ошибка загрузки истории:', error);
    } finally {
        isLoading = false;
    }
}

async function loadMoreHistory() {
    if (isLoading) return;
    if (!hasMore) {
        document.getElementById('loadMoreContainer').hidden = true;
        return;
    }
    currentPage++;
    await loadHistory(false);
}

function updateLoadMoreVisibility() {
    const loadMoreContainer = document.getElementById('loadMoreContainer');
    if (!loadMoreContainer) return;

    loadMoreContainer.hidden = !hasMore;
}

function renderHistory(items, reset = true) {
    const container = document.getElementById('historyList');

    if (!items || items.length === 0) {
        if (reset) {
            container.innerHTML = '<div class="empty">Нет транскрипций. Загрузите аудиофайл</div>';
        }
        return;
    }

    if (reset) {
        container.innerHTML = '';
    }

    items.forEach(item => {
        const status = escapeHtml(item.status || '');
        const itemHtml = `
            <div class="history-item" data-transcription-id="${item.id}">
                <div class="item-name" data-label="Название" title="${escapeHtml(item.fileName)}">${escapeHtml(item.fileName)}</div>
                <div class="item-format" data-label="Формат">${escapeHtml(item.format || 'MP3')}</div>
                <div class="item-language" data-label="Язык">${escapeHtml(item.language || 'ru-RU')}</div>
                <div class="item-size" data-label="Размер">${formatBytes(item.sizeBytes)}</div>
                <div class="item-date" data-label="Дата">${formatDate(item.uploadedAt)}</div>
                <div class="item-status" data-label="Статус"><span class="status-badge status-${status}">${status}</span></div>
            </div>
        `;
        container.insertAdjacentHTML('beforeend', itemHtml);
    });
}

function handleHistoryClick(event) {
    const item = event.target.closest('[data-transcription-id]');
    if (!item) return;

    showDetails(item.dataset.transcriptionId);
}

async function uploadFile() {
    await submitAudioFile({
        endpoint: '/api/transcriptions/recognize',
        loadingText: 'Распознавание...',
        successText: 'Распознавание начато',
        fallbackError: 'Ошибка распознавания'
    });
}

async function recognizeAgainFile() {
    await submitAudioFile({
        endpoint: '/api/transcriptions/recognize-again',
        loadingText: 'Запуск повторного распознавания...',
        successText: 'Повторное распознавание начато',
        fallbackError: 'Ошибка повторного распознавания'
    });
}

async function submitAudioFile({ endpoint, loadingText, successText, fallbackError }) {
    const fileInput = document.getElementById('fileInput');
    const file = fileInput.files[0];
    const languageSelect = document.getElementById('languageSelect');
    const uploadBtn = document.getElementById('uploadBtn');
    const recognizeAgainBtn = document.getElementById('recognizeAgainBtn');
    const uploadStatus = document.getElementById('uploadStatus');

    const formData = new FormData();
    formData.append('file', file);
    formData.append('language', languageSelect.value);

    uploadBtn.disabled = true;
    if (recognizeAgainBtn) {
        recognizeAgainBtn.disabled = true;
    }
    clearMessage(uploadStatus);
    uploadStatus.innerText = loadingText;

    try {
        const token = getToken();
        const response = await fetch(`${API_URL}${endpoint}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${token}` },
            body: formData
        });

        if (response.status === 202) {
            showSuccess(uploadStatus, successText);
            fileInput.value = '';
            const fileLabel = document.getElementById('fileLabel');
            if (fileLabel) {
                fileLabel.innerText = 'Выберите файл';
                fileLabel.classList.remove('has-file');
            }
            refreshHistory();
            setTimeout(() => {
                if (uploadStatus.innerText === successText) {
                    clearMessage(uploadStatus);
                }
            }, 3000);
        } else if (response.status === 401) {
            logout();
        } else {
            const errorText = await response.text();
            showError(uploadStatus, errorText || fallbackError);
        }
    } catch (error) {
        showError(uploadStatus, 'Ошибка соединения с сервером');
    } finally {
        uploadBtn.disabled = false;
        if (recognizeAgainBtn) {
            recognizeAgainBtn.disabled = false;
        }
    }
}

async function showDetails(id) {
    const token = getToken();
    if (!token) return;

    try {
        const response = await fetch(`${API_URL}/api/transcriptions/${id}`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.status === 200) {
            const data = await response.json();
            renderModal(data, id);
        } else if (response.status === 401) {
            logout();
        } else {
            const errorText = await response.text();
            console.error('Ошибка загрузки деталей:', errorText);
        }
    } catch (error) {
        console.error('Ошибка загрузки деталей:', error);
    }
}

async function downloadExport(id) {
    const token = getToken();
    if (!token) return;

    try {
        const response = await fetch(`${API_URL}/api/transcriptions/${id}/export`, {
            headers: { 'Authorization': `Bearer ${token}` }
        });

        if (response.status === 200) {
            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `transcript_${id}.pdf`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
        } else if (response.status === 401) {
            logout();
        } else {
            const errorText = await response.text();
            alert('Ошибка экспорта: ' + errorText);
        }
    } catch (error) {
        alert('Ошибка соединения: ' + error.message);
    }
}

function handleModalClick(event) {
    const closeButton = event.target.closest('[data-action="close-modal"]');
    if (closeButton) {
        closeModal();
        return;
    }

    const exportButton = event.target.closest('[data-action="download-export"]');
    if (exportButton) {
        downloadExport(exportButton.dataset.transcriptionId);
    }
}

function handleModalOverlayClick(event) {
    if (event.target.id === 'detailsModal') {
        closeModal();
    }
}

function renderModal(data, id) {
    const modal = document.getElementById('detailsModal');
    const modalContent = modal.querySelector('.modal-content');
    const modalBody = document.getElementById('modalBody');

    if (data.errorMessage) {
        modalContent.classList.add('error-modal');

        modalBody.innerHTML = `
            <div class="modal-header">
                <strong class="modal-error-title">Ошибка обработки</strong>
                <button type="button" class="modal-close" data-action="close-modal" aria-label="Закрыть">&times;</button>
            </div>
            <div class="modal-error-text">${escapeHtml(data.errorMessage)}</div>
        `;

        modal.classList.add('is-open');
        return;
    }

    modalContent.classList.remove('error-modal');

    let statsHtml = '';
    if (data.durationSeconds || data.characterCount || data.sentenceCount) {
        statsHtml = `
            <div class="modal-stats">
                <h4>Статистика</h4>
                <div class="stats-grid">
                    ${data.durationSeconds ? `
                        <div class="stat-item">
                            <span class="stat-label">Длительность</span>
                            <span class="stat-value">${formatDuration(data.durationSeconds)}</span>
                        </div>
                    ` : ''}
                    ${data.characterCount ? `
                        <div class="stat-item">
                            <span class="stat-label">Символов без пробелов</span>
                            <span class="stat-value">${data.characterCount.toLocaleString()}</span>
                        </div>
                    ` : ''}
                    ${data.sentenceCount ? `
                        <div class="stat-item">
                            <span class="stat-label">Предложений</span>
                            <span class="stat-value">${data.sentenceCount.toLocaleString()}</span>
                        </div>
                    ` : ''}
                </div>
            </div>
        `;
    }

    let blocksHtml = '';
    if (data.semanticBlocks && data.semanticBlocks.length > 0) {
        blocksHtml = data.semanticBlocks.map(block =>
            `<p>${escapeHtml(block.textContent)}</p>`
        ).join('');
    } else if (!data.errorMessage && (!data.semanticBlocks || data.semanticBlocks.length === 0)) {
        blocksHtml = '<p class="empty">Текст еще не распознан</p>';
    }

    const exportButtons = (!data.errorMessage && data.semanticBlocks && data.semanticBlocks.length > 0)
        ? `<div class="modal-export-buttons">
            <button type="button" data-action="download-export" data-transcription-id="${id}">Скачать PDF</button>
        </div>`
        : '';

    modalBody.innerHTML = `
        <div class="modal-header compact">
            <button type="button" class="modal-close" data-action="close-modal" aria-label="Закрыть">&times;</button>
        </div>
        ${statsHtml}
        <div class="modal-text">${blocksHtml}</div>
        ${exportButtons}
    `;

    modal.classList.add('is-open');
}

function closeModal() {
    const modal = document.getElementById('detailsModal');
    const modalContent = modal.querySelector('.modal-content');
    modalContent.classList.remove('error-modal');
    modal.classList.remove('is-open');
}

function formatBytes(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function formatDate(dateStr) {
    const date = new Date(dateStr);
    return date.toLocaleDateString('ru-RU') + ' ' + date.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
}

function formatDuration(seconds) {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = Math.floor(seconds % 60);
    if (hours > 0) return `${hours} ч ${minutes} мин`;
    if (minutes > 0) return `${minutes} мин ${secs} сек`;
    return `${secs} сек`;
}

function escapeHtml(str) {
    if (!str) return '';
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

if (localStorage.getItem('token')) {
    showMainApp();
    loadHistory(true);
    startAutoRefresh();
} else {
    showLogin();
}
