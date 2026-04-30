/* ══════════════════════════════════════════════════════════
   NetSupport School — Tutor Dashboard  app.js
   Full WebSocket + REST integration
   ══════════════════════════════════════════════════════════ */

// ── State ─────────────────────────────────────────────────────────────────

const state = {
  ws: null,
  connected: false,
  students: {}, // studentId → { studentId, studentName, hostname, locked, score, totalQ, answeredQ, submitted }
  activeExam: null, // { examId, title }
  selectedStudents: new Set(),
  exams: [],
  currentResultExamId: null,
};

// ── DOM Refs ──────────────────────────────────────────────────────────────

const $ = (id) => document.getElementById(id);

const dom = {
  statusDot: $("statusDot"),
  statusText: $("statusText"),
  serverUrl: $("serverUrl"),
  connectBtn: $("connectBtn"),
  scanBtn: $("scanBtn"),
  testLoginBtn: $("testLoginBtn"),
  lockAllBtn: $("lockAllBtn"),
  unlockAllBtn: $("unlockAllBtn"),
  startExamBtn: $("startExamBtn"),
  stopExamBtn: $("stopExamBtn"),
  statTotal: $("statTotal"),
  statLocked: $("statLocked"),
  statSubmitted: $("statSubmitted"),
  activeExamChip: $("activeExamChip"),
  activeExamTitle: $("activeExamTitle"),
  stopExamChip: $("stopExamChip"),
  selectAll: $("selectAll"),
  selectionCount: $("selectionCount"),
  searchInput: $("searchInput"),
  studentGrid: $("studentGrid"),
  emptyState: $("emptyState"),
  // Modal
  startExamModal: $("startExamModal"),
  closeStartExamModal: $("closeStartExamModal"),
  modalExamSelect: $("modalExamSelect"),
  modalRequireName: $("modalRequireName"),
  modalStudentList: $("modalStudentList"),
  modalSelectAll: $("modalSelectAll"),
  modalDeselectAll: $("modalDeselectAll"),
  cancelStartExamBtn: $("cancelStartExamBtn"),
  confirmStartExamBtn: $("confirmStartExamBtn"),
  // Results
  examPickerList: $("examPickerList"),
  resultsEmpty: $("resultsEmpty"),
  resultsContent: $("resultsContent"),
  resultsExamTitle: $("resultsExamTitle"),
  resultsExamMeta: $("resultsExamMeta"),
  downloadReportBtn: $("downloadReportBtn"),
  clearResultsBtn: $("clearResultsBtn"),
  resultsSummary: $("resultsSummary"),
  resultsTableBody: $("resultsTableBody"),
  refreshResultsBtn: $("refreshResultsBtn"),
  // Toast
  toast: $("toast"),
  toastMsg: $("toastMsg"),
};

// ── Navigation ────────────────────────────────────────────────────────────

document.querySelectorAll(".nav-item").forEach((item) => {
  item.addEventListener("click", (e) => {
    e.preventDefault();
    const view = item.dataset.view;
    document
      .querySelectorAll(".nav-item")
      .forEach((n) => n.classList.remove("active"));
    item.classList.add("active");
    document
      .querySelectorAll(".view")
      .forEach((v) => v.classList.add("hidden"));
    $("view-" + view).classList.remove("hidden");
    if (view === "exam-results") loadExamsForResults();
  });
});

// ── WebSocket ────────────────────────────────────────────────────────────

dom.connectBtn.addEventListener("click", () => {
  if (state.connected) {
    disconnect();
  } else {
    connect();
  }
});

function connect() {
  const raw = dom.serverUrl.value.trim() || "localhost:8080";
  const url = "ws://" + raw + "/websocket";
  setStatus("connecting", "Connecting…");

  try {
    state.ws = new WebSocket(url);
  } catch (e) {
    setStatus("offline", "Failed");
    showToast("Invalid URL: " + raw, "error");
    return;
  }

  state.ws.onopen = () => {
    state.connected = true;
    setStatus("online", "Connected");
    dom.connectBtn.textContent = "Disconnect";
    dom.connectBtn.classList.add("connected");
    enableButtons(true);
    sendWs({ type: "TUTOR_ONLINE" });
    showToast("Connected to server", "success");
  };

  state.ws.onmessage = (e) => {
    try {
      const msg = JSON.parse(e.data);
      handleMessage(msg);
    } catch (err) {
      console.error("WS parse error:", err);
    }
  };

  state.ws.onclose = () => {
    state.connected = false;
    setStatus("offline", "Disconnected");
    dom.connectBtn.textContent = "Connect";
    dom.connectBtn.classList.remove("connected");
    enableButtons(false);
    showToast("Disconnected from server", "error");
  };

  state.ws.onerror = () => {
    setStatus("offline", "Error");
    showToast("WebSocket error — check server URL", "error");
  };
}

function disconnect() {
  if (state.ws) state.ws.close();
}

function sendWs(obj) {
  if (state.ws && state.ws.readyState === WebSocket.OPEN) {
    state.ws.send(JSON.stringify(obj));
  }
}

// ── Message Handling ──────────────────────────────────────────────────────

function handleMessage(msg) {
  console.log("← WS:", msg);
  switch (msg.type) {
    case "STUDENT_ONLINE":
      onStudentOnline(msg);
      break;
    case "STUDENT_OFFLINE":
      onStudentOffline(msg);
      break;
    case "STUDENT_SUBMITTED":
      onStudentSubmitted(msg);
      break;
    default:
      // unknown — ignore silently
      break;
  }
}

function onStudentOnline(msg) {
  const existing = state.students[msg.studentId];
  state.students[msg.studentId] = {
    ...(existing || {}),
    studentId: msg.studentId,
    studentName: msg.studentName || existing?.studentName || "Student",
    hostname: existing?.hostname || "",
    locked: existing?.locked || false,
    score: existing?.score || 0,
    totalQ: existing?.totalQ || 0,
    answeredQ: existing?.answeredQ || 0,
    submitted: existing?.submitted || false,
  };
  renderStudentGrid();
  updateStats();
  showToast(`${state.students[msg.studentId].studentName} joined`, "success");
}

function onStudentOffline(msg) {
  if (state.students[msg.studentId]) {
    delete state.students[msg.studentId];
    state.selectedStudents.delete(msg.studentId);
    renderStudentGrid();
    updateStats();
  }
}

function onStudentSubmitted(msg) {
  const s = state.students[msg.studentId];
  if (!s) return;

  s.studentName = msg.studentName || s.studentName;
  s.score = msg.score;
  s.totalQ = msg.totalQuestions;
  s.answeredQ = msg.answeredQuestions;
  s.submitted = msg.trigger !== "ANSWER_CHANGE";

  renderStudentCard(msg.studentId);
  updateStats();
}

// ── Button Actions ────────────────────────────────────────────────────────

dom.scanBtn.addEventListener("click", () => {
  sendWs({ type: "SCAN_STUDENTS" });
  showToast("Scanning for students on LAN…");
  dom.scanBtn.disabled = true;
  setTimeout(() => {
    dom.scanBtn.disabled = !state.connected;
  }, 7000);
});

dom.testLoginBtn.addEventListener("click", () => {
  sendWs({ type: "TEST_LOGIN" });
  showToast("Requested name entry from all students");
});

dom.lockAllBtn.addEventListener("click", () => {
  sendWs({ type: "LOCK_ALL" });
  Object.values(state.students).forEach((s) => (s.locked = true));
  renderStudentGrid();
  updateStats();
  showToast("Locked all students", "error");
});

dom.unlockAllBtn.addEventListener("click", () => {
  sendWs({ type: "UNLOCK_ALL" });
  Object.values(state.students).forEach((s) => (s.locked = false));
  renderStudentGrid();
  updateStats();
  showToast("Unlocked all students", "success");
});

dom.startExamBtn.addEventListener("click", openStartExamModal);
dom.stopExamBtn.addEventListener("click", stopExam);

// ── Student Grid ──────────────────────────────────────────────────────────

function renderStudentGrid() {
  const query = dom.searchInput.value.toLowerCase();
  const ids = Object.keys(state.students).filter((id) => {
    const s = state.students[id];
    return (
      !query ||
      s.studentName.toLowerCase().includes(query) ||
      (s.hostname || "").toLowerCase().includes(query)
    );
  });

  dom.studentGrid.innerHTML = "";
  ids.forEach((id) => dom.studentGrid.appendChild(buildStudentCard(id)));

  const hasStudents = Object.keys(state.students).length > 0;
  dom.emptyState.classList.toggle("visible", !hasStudents);

  // Sync select-all checkbox
  const allSelected =
    ids.length > 0 && ids.every((id) => state.selectedStudents.has(id));
  dom.selectAll.checked = allSelected;
  dom.selectAll.indeterminate =
    !allSelected && ids.some((id) => state.selectedStudents.has(id));

  updateSelectionCount();
}

function renderStudentCard(id) {
  const existing = dom.studentGrid.querySelector(`[data-student-id="${id}"]`);
  const newCard = buildStudentCard(id);
  if (existing) {
    dom.studentGrid.replaceChild(newCard, existing);
  }
}

function buildStudentCard(id) {
  const s = state.students[id];
  if (!s) return document.createElement("div");

  const card = document.createElement("div");
  card.className =
    "student-card" +
    (s.locked ? " locked" : "") +
    (s.submitted ? " submitted" : "");
  card.dataset.studentId = id;

  const pct = s.totalQ > 0 ? Math.round((s.score / s.totalQ) * 100) : 0;
  const answeredPct =
    s.totalQ > 0 ? Math.round((s.answeredQ / s.totalQ) * 100) : 0;

  card.innerHTML = `
        <div class="student-card-header">
            <input type="checkbox" class="student-card-check" data-id="${id}"
                   ${state.selectedStudents.has(id) ? "checked" : ""} />
            <span class="student-name" title="${esc(s.studentName)}">${esc(s.studentName)}</span>
            <span class="student-badge ${s.locked ? "locked" : "online"}">${s.locked ? "🔒" : "●"}</span>
        </div>
        <div class="student-meta">
            <span title="${esc(id)}">${esc(shortId(id))}</span>
            ${s.hostname ? `<span>${esc(s.hostname)}</span>` : ""}
        </div>
        <div class="student-score ${s.totalQ > 0 ? "visible" : ""}">
            <div class="score-row">
                <span class="score-text">${s.score} / ${s.totalQ} (${pct}%)</span>
                ${s.submitted ? '<span style="font-family:var(--font-mono);font-size:10px;color:var(--green)">✔ submitted</span>' : ""}
            </div>
            <div class="score-progress"><div class="score-bar" style="width:${pct}%"></div></div>
            <div class="score-answered">${s.answeredQ} / ${s.totalQ} answered (${answeredPct}%)</div>
        </div>
        <div class="student-card-actions">
            ${
              s.locked
                ? `<button class="card-btn unlock-btn" data-action="unlock" data-id="${id}">Unlock</button>`
                : `<button class="card-btn lock-btn"   data-action="lock"   data-id="${id}">Lock</button>`
            }
        </div>`;

  // Checkbox toggle
  card.querySelector(".student-card-check").addEventListener("change", (e) => {
    if (e.target.checked) state.selectedStudents.add(id);
    else state.selectedStudents.delete(id);
    updateSelectionCount();
    syncSelectAllCheckbox();
  });

  // Lock / Unlock per card
  card.querySelector("[data-action]").addEventListener("click", (e) => {
    const action = e.currentTarget.dataset.action;
    const sid = e.currentTarget.dataset.id;
    if (action === "lock") {
      sendWs({ type: "LOCK_STUDENT", studentId: sid });
      state.students[sid].locked = true;
    } else {
      sendWs({ type: "UNLOCK_STUDENT", studentId: sid });
      state.students[sid].locked = false;
    }
    renderStudentCard(sid);
    updateStats();
  });

  return card;
}

// ── Select All ────────────────────────────────────────────────────────────

dom.selectAll.addEventListener("change", () => {
  const all = Object.keys(state.students);
  if (dom.selectAll.checked)
    all.forEach((id) => state.selectedStudents.add(id));
  else state.selectedStudents.clear();
  renderStudentGrid();
});

dom.searchInput.addEventListener("input", () => renderStudentGrid());

function updateSelectionCount() {
  const n = state.selectedStudents.size;
  dom.selectionCount.textContent = n + " selected";
  dom.selectionCount.classList.toggle("hidden", n === 0);
}

function syncSelectAllCheckbox() {
  const ids = Object.keys(state.students);
  const allSelected =
    ids.length > 0 && ids.every((id) => state.selectedStudents.has(id));
  dom.selectAll.checked = allSelected;
  dom.selectAll.indeterminate =
    !allSelected && ids.some((id) => state.selectedStudents.has(id));
}

// ── Stats ─────────────────────────────────────────────────────────────────

function updateStats() {
  const all = Object.values(state.students);
  dom.statTotal.textContent = all.length;
  dom.statLocked.textContent = all.filter((s) => s.locked).length;
  dom.statSubmitted.textContent = all.filter((s) => s.submitted).length;
}

// ── Start Exam Modal ──────────────────────────────────────────────────────

async function openStartExamModal() {
  // Load exams list
  try {
    const apiBase = "http://" + dom.serverUrl.value.trim().replace(/\/$/, "");
    const res = await fetch(apiBase + "/api/exams");
    const data = await res.json();
    state.exams = data.exams || [];
  } catch (e) {
    state.exams = [];
    showToast("Could not load exams", "error");
  }

  // Populate exam select
  dom.modalExamSelect.innerHTML =
    '<option value="">— Choose an exam —</option>';
  state.exams.forEach((ex) => {
    const opt = document.createElement("option");
    opt.value = ex.examId;
    opt.textContent =
      ex.title +
      "  (" +
      ex.questionCount +
      " Q, " +
      ex.durationMinutes +
      " min)";
    dom.modalExamSelect.appendChild(opt);
  });

  // Populate student checkboxes
  dom.modalStudentList.innerHTML = "";
  const students = Object.values(state.students);
  if (students.length === 0) {
    dom.modalStudentList.innerHTML =
      '<div style="color:var(--text-muted);font-size:13px;padding:8px 0;">No students connected.</div>';
  } else {
    students.forEach((s) => {
      const item = document.createElement("label");
      item.className = "modal-student-item";
      item.innerHTML = `
                <input type="checkbox" value="${esc(s.studentId)}" checked />
                <span class="ms-name">${esc(s.studentName)}</span>
                <span class="ms-id">${esc(shortId(s.studentId))}</span>`;
      item.querySelector("input").addEventListener("change", (e) => {
        item.classList.toggle("selected", e.target.checked);
      });
      item.classList.add("selected");
      dom.modalStudentList.appendChild(item);
    });
  }

  dom.startExamModal.classList.remove("hidden");
}

dom.closeStartExamModal.addEventListener("click", closeStartExamModal);
dom.cancelStartExamBtn.addEventListener("click", closeStartExamModal);

dom.modalSelectAll.addEventListener("click", () => {
  dom.modalStudentList
    .querySelectorAll('input[type="checkbox"]')
    .forEach((cb) => {
      cb.checked = true;
      cb.closest(".modal-student-item")?.classList.add("selected");
    });
});

dom.modalDeselectAll.addEventListener("click", () => {
  dom.modalStudentList
    .querySelectorAll('input[type="checkbox"]')
    .forEach((cb) => {
      cb.checked = false;
      cb.closest(".modal-student-item")?.classList.remove("selected");
    });
});

function closeStartExamModal() {
  dom.startExamModal.classList.add("hidden");
}

dom.confirmStartExamBtn.addEventListener("click", () => {
  const examId = parseInt(dom.modalExamSelect.value, 10);
  if (!examId) {
    showToast("Please select an exam", "error");
    return;
  }

  const studentIds = [
    ...dom.modalStudentList.querySelectorAll('input[type="checkbox"]:checked'),
  ].map((cb) => cb.value);

  if (studentIds.length === 0) {
    showToast("Select at least one student", "error");
    return;
  }

  const requireNameEntry = dom.modalRequireName.checked;

  sendWs({ type: "TUTOR_START_EXAM", examId, studentIds, requireNameEntry });

  const exam = state.exams.find((e) => e.examId === examId);
  state.activeExam = { examId, title: exam?.title || "Exam #" + examId };

  // Reset submission state for targeted students
  studentIds.forEach((id) => {
    if (state.students[id]) {
      state.students[id].submitted = false;
      state.students[id].score = 0;
      state.students[id].totalQ = 0;
      state.students[id].answeredQ = 0;
    }
  });

  dom.activeExamChip.classList.remove("hidden");
  dom.activeExamTitle.textContent = state.activeExam.title;
  dom.stopExamChip.classList.remove("hidden");
  dom.statSubmitted.textContent = "0";
  updateStats();
  renderStudentGrid();

  closeStartExamModal();
  showToast("Exam launched: " + state.activeExam.title, "success");
});

// ── Stop Exam ─────────────────────────────────────────────────────────────

function stopExam() {
  if (!state.activeExam) return;
  sendWs({ type: "STOP_EXAM", examId: state.activeExam.examId });
  state.activeExam = null;
  dom.activeExamChip.classList.add("hidden");
  dom.stopExamChip.classList.add("hidden");
  showToast("Exam stopped", "error");
}

// ── Results View ──────────────────────────────────────────────────────────

async function loadExamsForResults() {
  dom.examPickerList.innerHTML =
    '<div class="loading-small">Loading exams…</div>';
  try {
    const apiBase = "http://" + dom.serverUrl.value.trim().replace(/\/$/, "");
    const res = await fetch(apiBase + "/api/exams");
    const data = await res.json();
    state.exams = data.exams || [];
    renderExamPicker();
  } catch (e) {
    dom.examPickerList.innerHTML =
      '<div class="loading-small" style="color:var(--red)">Failed to load. Check connection.</div>';
  }
}

function renderExamPicker() {
  dom.examPickerList.innerHTML = "";
  if (state.exams.length === 0) {
    dom.examPickerList.innerHTML =
      '<div class="loading-small">No exams found.</div>';
    return;
  }
  state.exams.forEach((ex) => {
    const item = document.createElement("div");
    item.className =
      "exam-picker-item" +
      (state.currentResultExamId === ex.examId ? " active" : "");
    item.innerHTML = `
            <div class="ep-name">${esc(ex.title)}</div>
            <div class="ep-meta">${ex.questionCount} Q · ${ex.durationMinutes} min</div>`;
    item.addEventListener("click", () => selectResultExam(ex.examId));
    dom.examPickerList.appendChild(item);
  });
}

async function selectResultExam(examId) {
  state.currentResultExamId = examId;
  renderExamPicker();

  dom.resultsEmpty.classList.add("hidden");
  dom.resultsContent.classList.add("hidden");

  try {
    const apiBase = "http://" + dom.serverUrl.value.trim().replace(/\/$/, "");
    const res = await fetch(apiBase + "/api/results/exam/" + examId);
    const data = await res.json();
    renderResults(data);
    dom.downloadReportBtn.href =
      apiBase + "/api/results/exam/" + examId + "/report";
  } catch (e) {
    showToast("Failed to load results", "error");
    dom.resultsEmpty.classList.remove("hidden");
  }
}

function renderResults(data) {
  dom.resultsExamTitle.textContent = data.examTitle || "—";
  dom.resultsExamMeta.textContent = "Exam ID: " + data.examId;

  const results = data.results || [];
  const total = results.length;
  const avg =
    total === 0
      ? 0
      : results.reduce((sum, r) => {
          return (
            sum +
            (r.totalQuestions > 0 ? (r.score / r.totalQuestions) * 100 : 0)
          );
        }, 0) / total;
  const topScore =
    total === 0
      ? 0
      : Math.max(
          ...results.map((r) =>
            r.totalQuestions > 0 ? (r.score / r.totalQuestions) * 100 : 0,
          ),
        );

  dom.resultsSummary.innerHTML = `
        <div class="summary-chip"><strong>${total}</strong><span>Students</span></div>
        <div class="summary-chip"><strong>${avg.toFixed(1)}%</strong><span>Avg Score</span></div>
        <div class="summary-chip"><strong>${topScore.toFixed(0)}%</strong><span>Top Score</span></div>`;

  dom.resultsTableBody.innerHTML = "";
  if (results.length === 0) {
    dom.resultsTableBody.innerHTML = `<tr><td colspan="6" style="text-align:center;color:var(--text-muted);padding:24px">No submissions yet.</td></tr>`;
  } else {
    results.forEach((r) => {
      const pct =
        r.totalQuestions > 0
          ? Math.round((r.score / r.totalQuestions) * 100)
          : 0;
      const scoreClass = pct >= 80 ? "high" : pct >= 50 ? "mid" : "low";
      const submitted = r.submittedAt
        ? new Date(r.submittedAt).toLocaleString()
        : "—";
      const tr = document.createElement("tr");
      tr.innerHTML = `
                <td>${esc(r.studentName)}</td>
                <td style="font-family:var(--font-mono);font-size:12px">${esc(shortId(r.studentId))}</td>
                <td style="font-family:var(--font-mono);font-size:12px">${esc(r.hostname || "—")}</td>
                <td class="score-cell ${scoreClass}">${r.score} / ${r.totalQuestions} (${pct}%)</td>
                <td style="font-family:var(--font-mono);font-size:12px">${r.answeredQuestions} / ${r.totalQuestions}</td>
                <td style="font-family:var(--font-mono);font-size:11px;color:var(--text-muted)">${submitted}</td>`;
      dom.resultsTableBody.appendChild(tr);
    });
  }

  dom.resultsContent.classList.remove("hidden");
}

dom.refreshResultsBtn.addEventListener("click", () => {
  if (state.currentResultExamId) selectResultExam(state.currentResultExamId);
  else loadExamsForResults();
});

dom.clearResultsBtn.addEventListener("click", async () => {
  if (!confirm("Clear all results? This cannot be undone.")) return;
  try {
    const apiBase = "http://" + dom.serverUrl.value.trim().replace(/\/$/, "");
    await fetch(apiBase + "/api/results/clear", { method: "DELETE" });
    showToast("Results cleared", "success");
    if (state.currentResultExamId) selectResultExam(state.currentResultExamId);
  } catch (e) {
    showToast("Failed to clear results", "error");
  }
});

// ── UI Helpers ────────────────────────────────────────────────────────────

function setStatus(cls, text) {
  dom.statusDot.className =
    "status-dot " +
    (cls === "online"
      ? "online"
      : cls === "connecting"
        ? "connecting"
        : "offline");
  dom.statusText.textContent = text;
}

function enableButtons(on) {
  [
    dom.scanBtn,
    dom.testLoginBtn,
    dom.lockAllBtn,
    dom.unlockAllBtn,
    dom.startExamBtn,
  ].forEach((btn) => {
    btn.disabled = !on;
  });
}

let toastTimer = null;
function showToast(msg, type = "") {
  dom.toastMsg.textContent = msg;
  dom.toast.className = "toast" + (type ? " " + type : "");
  dom.toast.classList.remove("hidden");
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(() => dom.toast.classList.add("hidden"), 3000);
}

function esc(str) {
  if (!str) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function shortId(id) {
  if (!id) return "";
  // Show last 8 chars for readability
  return id.length > 12 ? "…" + id.slice(-10) : id;
}

// ── Init ──────────────────────────────────────────────────────────────────

enableButtons(false);
renderStudentGrid();
updateStats();
