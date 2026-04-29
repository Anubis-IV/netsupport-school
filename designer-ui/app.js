/* ══════════════════════════════════════════════════════════
   NetSupport School — Exam Designer
   Connects to Spring Boot ExamController REST API
   ══════════════════════════════════════════════════════════ */

'use strict';

// ── State ────────────────────────────────────────────────────
const state = {
    exams:         [],
    editingExamId: null,   // null = create mode, number = edit mode
    questions:     [],     // current form questions array
    deleteTarget:  null,   // exam id pending delete confirmation
};

// ── API helpers ──────────────────────────────────────────────

function apiBase() {
    return (document.getElementById('apiBase').value || 'http://localhost:8080').replace(/\/$/, '');
}

async function apiFetch(path, options = {}) {
    const url = apiBase() + path;
    const res  = await fetch(url, {
        headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
        ...options,
    });

    if (!res.ok) {
        let msg = `HTTP ${res.status}`;
        try { const j = await res.json(); msg = j.error || JSON.stringify(j.errors) || msg; } catch {}
        throw new Error(msg);
    }

    // 204 No Content
    if (res.status === 204) return null;
    return res.json();
}

// ── Server health check ──────────────────────────────────────

async function checkServer() {
    const dot  = document.getElementById('statusDot');
    const text = document.getElementById('statusText');
    try {
        await apiFetch('/api/exams');
        dot.className  = 'status-dot online';
        text.textContent = 'API Online';
    } catch {
        dot.className  = 'status-dot offline';
        text.textContent = 'API Offline';
    }
}

// ── Toast ────────────────────────────────────────────────────

let toastTimer;
function showToast(msg, type = 'success') {
    const el = document.getElementById('toast');
    document.getElementById('toastMsg').textContent = msg;
    el.className = `toast ${type}`;
    el.classList.remove('hidden');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.classList.add('hidden'), 3500);
}

// ── View routing ─────────────────────────────────────────────

function showView(id) {
    document.querySelectorAll('.view').forEach(v => v.classList.add('hidden'));
    document.getElementById('view-' + id).classList.remove('hidden');

    document.querySelectorAll('.nav-item').forEach(n => {
        n.classList.toggle('active', n.dataset.view === id);
    });
}

// ── Exam List ────────────────────────────────────────────────

async function loadExams() {
    showView('exams');
    const grid    = document.getElementById('examGrid');
    const empty   = document.getElementById('emptyState');
    const loading = document.getElementById('loadingState');

    grid.innerHTML    = '';
    empty.classList.add('hidden');
    loading.classList.remove('hidden');

    try {
        const data = await apiFetch('/api/exams');
        state.exams = data.exams || [];
    } catch (e) {
        showToast('Failed to load exams: ' + e.message, 'error');
        state.exams = [];
    }

    loading.classList.add('hidden');

    if (state.exams.length === 0) {
        empty.classList.remove('hidden');
        return;
    }

    state.exams.forEach(exam => grid.appendChild(buildExamCard(exam)));
}

function buildExamCard(exam) {
    const card = document.createElement('div');
    card.className = 'exam-card';
    card.dataset.id = exam.examId;

    const date = exam.createdAt
        ? new Date(exam.createdAt).toLocaleDateString('en-GB', { day:'2-digit', month:'short', year:'numeric' })
        : '—';

    card.innerHTML = `
    <div class="card-badge">Exam #${exam.examId}</div>
    <div class="card-name">${esc(exam.title)}</div>
    <div class="card-meta">
      <span class="meta-chip">
        <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
        ${exam.durationMinutes} min
      </span>
      <span class="meta-chip">
        <svg viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
        ${exam.questionCount} question${exam.questionCount !== 1 ? 's' : ''}
      </span>
    </div>
    <div class="card-footer">
      <span class="card-date">${date}</span>
      <div class="card-actions" onclick="event.stopPropagation()">
        <button class="icon-btn" title="Edit" onclick="startEdit(${exam.examId})">
          <svg viewBox="0 0 24 24"><path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
        </button>
        <button class="icon-btn danger" title="Delete" onclick="promptDelete(${exam.examId})">
          <svg viewBox="0 0 24 24"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4h6v2"/></svg>
        </button>
      </div>
    </div>
  `;

    // click card body → detail view
    card.addEventListener('click', () => loadDetail(exam.examId));
    return card;
}

// ── Detail View ──────────────────────────────────────────────

async function loadDetail(examId) {
    showView('detail');
    document.getElementById('detailContent').innerHTML = '<div class="loading-state"><div class="spinner"></div></div>';

    try {
        const exam = await apiFetch(`/api/exams/${examId}`);
        renderDetail(exam);
        state.editingExamId = examId; // so Edit/Delete buttons know the target
    } catch (e) {
        showToast('Could not load exam: ' + e.message, 'error');
        loadExams();
    }
}

function renderDetail(exam) {
    document.getElementById('detailTitle').textContent = exam.title;
    document.getElementById('detailSub').textContent   = `${exam.durationMinutes} min · ${exam.questions.length} questions`;

    const content = document.getElementById('detailContent');
    content.innerHTML = `
    <div class="detail-meta-bar">
      <div class="detail-chip"><strong>#${exam.examId}</strong>Exam ID</div>
      <div class="detail-chip"><strong>${exam.durationMinutes}</strong>Minutes</div>
      <div class="detail-chip"><strong>${exam.questions.length}</strong>Questions</div>
    </div>
    <h2 class="card-title" style="margin-bottom:16px">Questions</h2>
    <div class="detail-questions">
      ${exam.questions.map((q, i) => buildDetailQuestion(q, i)).join('')}
    </div>
  `;
}

function buildDetailQuestion(q, index) {
    const choiceLetters = ['A','B','C','D'];
    const choices = q.choices.map(c => `
    <div class="dq-choice ${c.choiceId === q.correctChoiceId ? 'correct' : ''}">
      <span class="dq-badge">${choiceLetters[c.choiceId]}</span>
      ${esc(c.text)}
    </div>
  `).join('');

    return `
    <div class="detail-question-card">
      <div class="dq-header">
        <div class="dq-num">Q${index + 1}</div>
        <div class="dq-text">${esc(q.text)}</div>
      </div>
      <div class="dq-choices">${choices}</div>
    </div>
  `;
}

// ── Create / Edit Form ───────────────────────────────────────

function openCreateForm() {
    state.editingExamId = null;
    state.questions     = [];

    document.getElementById('formTitle').textContent = 'New Exam';
    document.getElementById('formSub').textContent   = 'Fill in the exam details and add questions below.';
    document.getElementById('examTitle').value    = '';
    document.getElementById('examDuration').value = '';
    clearFormErrors();

    renderQuestions();
    showView('create');
}

async function startEdit(examId) {
    try {
        const exam = await apiFetch(`/api/exams/${examId}`);
        state.editingExamId = examId;

        document.getElementById('formTitle').textContent = 'Edit Exam';
        document.getElementById('formSub').textContent   = `Editing: ${exam.title}`;
        document.getElementById('examTitle').value    = exam.title;
        document.getElementById('examDuration').value = exam.durationMinutes;
        clearFormErrors();

        // Populate questions from API response
        state.questions = exam.questions.map(q => ({
            text:            q.text,
            choices:         q.choices.map(c => c.text),
            correctChoiceId: q.correctChoiceId,
        }));

        renderQuestions();
        showView('create');
    } catch (e) {
        showToast('Could not load exam for editing: ' + e.message, 'error');
    }
}

// ── Questions Builder ────────────────────────────────────────

function addQuestion() {
    state.questions.push({ text: '', choices: ['', '', '', ''], correctChoiceId: 0 });
    renderQuestions();
    // Scroll to the new question
    setTimeout(() => {
        const list = document.getElementById('questionsList');
        list.lastElementChild?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }, 50);
}

function removeQuestion(index) {
    state.questions.splice(index, 1);
    renderQuestions();
}

function renderQuestions() {
    const list = document.getElementById('questionsList');
    const hint = document.getElementById('noQuestionsHint');
    const cnt  = document.getElementById('qCount');

    cnt.textContent = state.questions.length;
    hint.classList.toggle('hidden', state.questions.length > 0);
    list.innerHTML  = '';

    state.questions.forEach((q, i) => {
        list.appendChild(buildQuestionBlock(q, i));
    });
}

function buildQuestionBlock(q, index) {
    const letters = ['A','B','C','D'];
    const block   = document.createElement('div');
    block.className = 'question-block';
    block.dataset.index = index;

    block.innerHTML = `
    <div class="question-block-header">
      <span class="q-number">Question ${index + 1}</span>
      <button class="remove-q-btn" title="Remove question" onclick="removeQuestion(${index})">×</button>
    </div>

    <textarea class="question-text-input"
      placeholder="Enter your question here…"
      oninput="updateQuestion(${index},'text',this.value)"
    >${esc(q.text)}</textarea>

    <div class="choices-grid">
      ${q.choices.map((c, ci) => `
        <div class="choice-row">
          <span class="choice-label">${letters[ci]}</span>
          <input type="text" class="choice-input"
            placeholder="Choice ${letters[ci]}"
            value="${esc(c)}"
            oninput="updateChoice(${index},${ci},this.value)"
          />
        </div>
      `).join('')}
    </div>

    <div class="correct-row">
      <label>Correct Answer:</label>
      <select class="correct-select" onchange="updateQuestion(${index},'correctChoiceId',+this.value)">
        ${letters.map((l, li) => `
          <option value="${li}" ${q.correctChoiceId === li ? 'selected' : ''}>${l}</option>
        `).join('')}
      </select>
    </div>
  `;

    return block;
}

function updateQuestion(index, field, value) {
    state.questions[index][field] = value;
}

function updateChoice(qIndex, choiceIndex, value) {
    state.questions[qIndex].choices[choiceIndex] = value;
}

// ── Validation ───────────────────────────────────────────────

function clearFormErrors() {
    document.getElementById('errTitle').classList.add('hidden');
    document.getElementById('errDuration').classList.add('hidden');
    document.getElementById('examTitle').classList.remove('error');
    document.getElementById('examDuration').classList.remove('error');
}

function validateForm() {
    clearFormErrors();
    let ok = true;

    const title = document.getElementById('examTitle').value.trim();
    const dur   = parseInt(document.getElementById('examDuration').value, 10);

    if (!title) {
        document.getElementById('errTitle').classList.remove('hidden');
        document.getElementById('examTitle').classList.add('error');
        ok = false;
    }
    if (!dur || dur < 1) {
        document.getElementById('errDuration').classList.remove('hidden');
        document.getElementById('examDuration').classList.add('error');
        ok = false;
    }
    if (state.questions.length === 0) {
        showToast('Add at least one question.', 'error');
        ok = false;
    }

    // validate each question
    for (let i = 0; i < state.questions.length; i++) {
        const q = state.questions[i];
        if (!q.text.trim()) {
            showToast(`Question ${i + 1} is missing its text.`, 'error');
            return false;
        }
        for (let ci = 0; ci < 4; ci++) {
            if (!q.choices[ci].trim()) {
                showToast(`Question ${i + 1}: choice ${['A','B','C','D'][ci]} is empty.`, 'error');
                return false;
            }
        }
    }

    return ok;
}

// ── Save Exam ────────────────────────────────────────────────

async function saveExam() {
    if (!validateForm()) return;

    const btn = document.getElementById('saveExamBtn');
    btn.textContent = 'Saving…';
    btn.disabled    = true;

    const payload = {
        title:           document.getElementById('examTitle').value.trim(),
        durationMinutes: parseInt(document.getElementById('examDuration').value, 10),
        questions:       state.questions.map(q => ({
            text:            q.text.trim(),
            choices:         q.choices.map(c => c.trim()),
            correctChoiceId: q.correctChoiceId,
        })),
    };

    try {
        if (state.editingExamId) {
            await apiFetch(`/api/exams/${state.editingExamId}`, {
                method: 'PUT',
                body:   JSON.stringify(payload),
            });
            showToast('Exam updated successfully ✓', 'success');
        } else {
            await apiFetch('/api/exams', {
                method: 'POST',
                body:   JSON.stringify(payload),
            });
            showToast('Exam created successfully ✓', 'success');
        }

        await loadExams();
    } catch (e) {
        showToast('Save failed: ' + e.message, 'error');


    }finally {
        btn.disabled    = false;
        btn.textContent = 'Save Exam';
    }
}

// ── Delete ───────────────────────────────────────────────────

function promptDelete(examId) {
    state.deleteTarget = examId;
    document.getElementById('deleteModal').classList.remove('hidden');
}

function closeDeleteModal() {
    state.deleteTarget = null;
    document.getElementById('deleteModal').classList.add('hidden');
}

async function confirmDelete() {
    if (!state.deleteTarget) return;
    const id = state.deleteTarget;
    closeDeleteModal();

    try {
        await apiFetch(`/api/exams/${id}`, { method: 'DELETE' });
        showToast('Exam deleted.', 'success');
        await loadExams();
    } catch (e) {
        showToast('Delete failed: ' + e.message, 'error');
    }
}

// ── Utility ──────────────────────────────────────────────────

function esc(str) {
    if (!str) return '';
    return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

// ── Event Wiring ─────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {

    // Nav
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', e => {
            e.preventDefault();
            if (item.dataset.view === 'create') openCreateForm();
            else loadExams();
        });
    });

    // Create button
    document.getElementById('goCreateBtn').addEventListener('click', openCreateForm);
    document.getElementById('emptyCreateBtn').addEventListener('click', openCreateForm);

    // Cancel
    ['cancelBtn','cancelBtn2'].forEach(id => {
        document.getElementById(id).addEventListener('click', loadExams);
    });

    // Back from detail
    document.getElementById('backBtn').addEventListener('click', loadExams);

    // Edit from detail
    document.getElementById('editBtn').addEventListener('click', () => {
        if (state.editingExamId) startEdit(state.editingExamId);
    });

    // Delete from detail
    document.getElementById('deleteBtn').addEventListener('click', () => {
        if (state.editingExamId) promptDelete(state.editingExamId);
    });

    // Add question
    document.getElementById('addQuestionBtn').addEventListener('click', addQuestion);

    // Save exam
    document.getElementById('saveExamBtn').addEventListener('click', saveExam);

    // Delete modal
    document.getElementById('cancelDeleteBtn').addEventListener('click', closeDeleteModal);
    document.getElementById('confirmDeleteBtn').addEventListener('click', confirmDelete);
    document.getElementById('deleteModal').addEventListener('click', e => {
        if (e.target === e.currentTarget) closeDeleteModal();
    });

    // API base change → recheck server
    document.getElementById('apiBase').addEventListener('change', () => {
        checkServer();
        loadExams();
    });

    // Init
    checkServer();
    loadExams();
    setInterval(checkServer, 30_000);
});