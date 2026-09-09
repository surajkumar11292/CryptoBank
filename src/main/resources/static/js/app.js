/* ============================================================
   CryptoBank — dashboard logic
   Session-authenticated; talks to /api/accounts, /api/auth,
   /api/beneficiaries, /api/billpay, /api/cards, /api/support —
   every feature is backed by the real database, not localStorage.
   ============================================================ */

const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];

let accounts = [];
let activeNumber = null;
let ledgerPage = 0;
const PAGE_SIZE = 8;
const HIGH_VALUE_THRESHOLD = 50000;
let ledgerMeta = { totalPages: 1, totalElements: 0, first: true, last: true };
let currentUser = null;

/* ---------- helpers ---------- */
const escapeHtml = (v) => v == null ? "" : String(v).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

const money = (n) => Number(n || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const money0 = (n) => Math.round(Number(n || 0)).toLocaleString("en-IN");

function fmtAcct(num) {
  const s = String(num);
  return s.replace(/(\d{4})(?=\d)/g, "$1 ");
}

function fmtDate(iso) {
  try {
    return new Date(iso).toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" });
  } catch { return iso; }
}

function toast(msg, type = "ok") {
  const t = document.createElement("div");
  t.className = "toast" + (type === "error" ? " toast--error" : "");
  t.innerHTML = `<span class="toast__ic">${type === "error" ? "!" : "\u2713"}</span>${escapeHtml(msg)}`;
  $("#toasts").appendChild(t);
  setTimeout(() => { t.style.opacity = "0"; t.style.transform = "translateX(16px)"; t.style.transition = "all .2s"; setTimeout(() => t.remove(), 200); }, 3000);
}

/** fetch wrapper: bounces to login on 401, returns parsed JSON or throws {status, body}. */
async function api(path, options = {}) {
  const res = await fetch(path, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (res.status === 401) { window.location.href = "/login.html"; throw { status: 401 }; }
  const text = await res.text();
  const body = text ? JSON.parse(text) : null;
  if (!res.ok) throw { status: res.status, body };
  return body;
}

/* ---------- server-backed state: beneficiaries, bill history, cards, tickets ---------- */
let beneficiaries = [];
let billHistory = [];
let cards = [];

async function refreshBeneficiaries() {
  try { beneficiaries = await api("/api/beneficiaries"); } catch { /* keep previous cache */ }
  return beneficiaries;
}
async function refreshCards() {
  try { cards = await api("/api/cards"); } catch { /* keep previous cache */ }
  return cards;
}

/* ---------- view routing ---------- */
const VIEW_TITLES = {
  dashboard: "Dashboard", accounts: "Accounts", transfers: "Transfers",
  deposits: "Deposits & Loans", cards: "Cards", billpay: "Bill Pay & Recharge",
  beneficiaries: "Beneficiaries", profile: "Profile & KYC", support: "Support",
};

function goToView(name) {
  $$(".view").forEach((v) => v.classList.toggle("is-active", v.dataset.view === name));
  $$(".nav__item[data-view]").forEach((b) => b.classList.toggle("is-active", b.dataset.view === name));
  $("#viewTitle").textContent = VIEW_TITLES[name] || "CryptoBank";
  window.scrollTo({ top: 0, behavior: "instant" in window ? "instant" : "auto" });
  if (name === "cards") renderCardsView();
  if (name === "beneficiaries") loadAndRenderBeneficiaries();
  if (name === "transfers") loadAndRenderTransferBenefList();
  if (name === "billpay") { populateBillAccounts(); loadAndRenderBillHistory(); }
  if (name === "profile") renderProfile();
  if (name === "support") loadAndRenderTickets();
  document.getElementById("sidebar").classList.remove("is-open");
}

/* ---------- accounts ---------- */
function accountCardHtml(a) {
  return `
    <div class="bank-card ${a.accountNumber === activeNumber ? "is-active" : ""}" data-acct="${a.accountNumber}" role="button" tabindex="0">
      <div class="bank-card__top">
        <span class="bank-card__label"><img src="/assets/logo.svg" alt="" />CryptoBank</span>
        ${a.frozen ? `<span class="status-pill status-pill--frozen" title="Frozen by the bank — contact support">Frozen</span>` : `<span class="bank-card__chip" aria-hidden="true"></span>`}
      </div>
      <div class="bank-card__balance"><span class="cur">₹</span>${money(a.balance)}</div>
      <div>
        <div class="bank-card__holder">${escapeHtml(a.holderName)}</div>
        <div class="bank-card__number">${fmtAcct(a.accountNumber)}</div>
      </div>
    </div>`;
}

function renderCards() {
  const cards = accounts.map(accountCardHtml).join("");
  const newCard = `<div class="bank-card bank-card--new" id="openCard" role="button" tabindex="0">
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></svg>
      Open a new account
    </div>`;
  $("#cardsRail").innerHTML = cards + newCard;
  if ($("#dashCardsRail")) $("#dashCardsRail").innerHTML = cards;

  $("#accountsSub").textContent = accounts.length
    ? "Select an account to view its statement."
    : "You have no accounts yet — open your first one to get started.";
}

function renderDashboardStats() {
  const total = accounts.reduce((s, a) => s + Number(a.balance || 0), 0);
  $("#statTotal").innerHTML = `<span class="cur">₹</span>${money(total)}`;
  $("#statAccountsCount").textContent = `${accounts.length} account${accounts.length === 1 ? "" : "s"}`;
  $("#statBenef").textContent = beneficiaries.length;
  const name = currentUser?.fullName ? `, ${currentUser.fullName.split(" ")[0]}` : "";
  $("#dashName").textContent = name;
}

async function loadRecentActivity() {
  const tbody = $("#recentBody");
  if (!accounts.length) {
    tbody.innerHTML = `<tr><td colspan="3"><div class="state"><div class="state__title">No activity yet</div><div class="state__hint">Open an account to get started.</div></div></td></tr>`;
    $("#statIn").innerHTML = `<span class="cur">₹</span>0.00`;
    $("#statOut").innerHTML = `<span class="cur">₹</span>0.00`;
    renderNotifications([]);
    return;
  }
  try {
    const batches = await Promise.all(accounts.map((a) => api(`/api/accounts/${a.accountNumber}/history?page=0&size=5`).catch(() => ({ content: [] }))));
    const all = batches.flatMap((b) => b.content || []).sort((x, y) => new Date(y.createdAt) - new Date(x.createdAt));
    const top = all.slice(0, 8);
    if (!top.length) {
      tbody.innerHTML = `<tr><td colspan="3"><div class="state"><div class="state__title">No transactions yet</div><div class="state__hint">Deposit, withdraw, or transfer to see activity here.</div></div></td></tr>`;
    } else {
      tbody.innerHTML = top.map((e) => {
        const credit = e.type === "CREDIT";
        return `<tr class="${credit ? "txn-credit" : "txn-debit"}">
          <td><div class="txn-desc"><span class="txn-icon">${credit ? "↑" : "↓"}</span>${escapeHtml(e.description)}</div></td>
          <td class="txn-date">${fmtDate(e.createdAt)}</td>
          <td class="num amt ${credit ? "amt--credit" : "amt--debit"}">${credit ? "+" : "−"}₹${money(e.amount)}</td>
        </tr>`;
      }).join("");
    }
    const inSum = all.filter((e) => e.type === "CREDIT").reduce((s, e) => s + Number(e.amount), 0);
    const outSum = all.filter((e) => e.type === "DEBIT").reduce((s, e) => s + Number(e.amount), 0);
    $("#statIn").innerHTML = `<span class="cur">₹</span>${money(inSum)}`;
    $("#statOut").innerHTML = `<span class="cur">₹</span>${money(outSum)}`;
    renderNotifications(all.slice(0, 6));
  } catch {
    tbody.innerHTML = `<tr><td colspan="3"><div class="state"><div class="state__title">Couldn't load activity</div></div></td></tr>`;
  }
}

function renderNotifications(entries) {
  const list = $("#notifList");
  const dot = $("#notifDot");
  if (!entries.length) {
    list.innerHTML = `<div class="dropdown__empty">You're all caught up.</div>`;
    dot.hidden = true;
    return;
  }
  dot.hidden = false;
  list.innerHTML = entries.map((e) => {
    const credit = e.type === "CREDIT";
    return `<div class="dropdown__item">
      <span class="dropdown__item-ic">${credit ? "↑" : "↓"}</span>
      <div>
        <div class="dropdown__item-title">${escapeHtml(e.description)}</div>
        <div class="dropdown__item-meta">${credit ? "+" : "−"}₹${money(e.amount)} · ${fmtDate(e.createdAt)}</div>
      </div>
    </div>`;
  }).join("");
}

async function loadAccounts(selectNumber = null) {
  try {
    accounts = await api("/api/accounts");
    if (accounts.length) {
      activeNumber = selectNumber && accounts.some((a) => a.accountNumber === selectNumber)
        ? selectNumber
        : (accounts.some((a) => a.accountNumber === activeNumber) ? activeNumber : accounts[0].accountNumber);
    } else {
      activeNumber = null;
    }
    await refreshBeneficiaries();
    renderCards();
    renderDashboardStats();
    loadRecentActivity();
    if (document.querySelector('.view[data-view="billpay"]')) populateBillAccounts();
    if (activeNumber) { $("#detailSection").hidden = false; loadLedger(); }
    else { $("#detailSection").hidden = true; }
  } catch (e) {
    if (e.status !== 401) toast("Couldn't load your accounts", "error");
  }
}

/* ---------- ledger / statement ---------- */
function skeletonLedger() {
  $("#ledgerBody").innerHTML = Array.from({ length: 4 }).map(() =>
    `<tr>${'<td><div class="skeleton"></div></td>'.repeat(4)}</tr>`).join("");
}

async function loadLedger() {
  const acct = accounts.find((a) => a.accountNumber === activeNumber);
  if (!acct) return;
  $("#statementAcct").textContent = `#${fmtAcct(acct.accountNumber)} · ${acct.holderName}`;
  skeletonLedger();
  try {
    const data = await api(`/api/accounts/${activeNumber}/history?page=${ledgerPage}&size=${PAGE_SIZE}`);
    ledgerMeta = data;
    renderLedger(data);
  } catch (e) {
    if (e.status !== 401) $("#ledgerBody").innerHTML = emptyRow("Couldn't load statement", "Please try again.");
  }
}

function emptyRow(title, hint) {
  return `<tr><td colspan="4"><div class="state">
    <div class="state__label">Statement</div>
    <div class="state__title">${escapeHtml(title)}</div>
    <div class="state__hint">${escapeHtml(hint)}</div>
  </div></td></tr>`;
}

let lastLedgerRows = [];
function renderLedger(data) {
  const rows = data.content;
  lastLedgerRows = rows;
  if (!rows.length) {
    $("#ledgerBody").innerHTML = emptyRow("No transactions yet", "Deposit, withdraw, or transfer to see activity here.");
    $("#pagination").hidden = true;
    return;
  }
  $("#ledgerBody").innerHTML = rows.map((e) => {
    const credit = e.type === "CREDIT";
    const icon = credit
      ? `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 19V5M5 12l7-7 7 7"/></svg>`
      : `<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 5v14M5 12l7 7 7-7"/></svg>`;
    return `<tr class="${credit ? "txn-credit" : "txn-debit"}">
      <td><div class="txn-desc"><span class="txn-icon">${icon}</span>
        <span class="txn-desc__meta"><span>${escapeHtml(e.description)}</span><span class="txn-desc__ref">${escapeHtml(e.reference)}</span></span>
      </div></td>
      <td class="txn-date">${fmtDate(e.createdAt)}</td>
      <td class="num amt ${credit ? "amt--credit" : "amt--debit"}">${credit ? "+" : "−"}₹${money(e.amount)}</td>
      <td class="num amt amt--balance">₹${money(e.balanceAfter)}</td>
    </tr>`;
  }).join("");

  const start = data.page * data.size + 1;
  const end = Math.min(data.totalElements, (data.page + 1) * data.size);
  $("#pageInfo").textContent = `${start}–${end} of ${data.totalElements}`;
  $("#pageIndicator").textContent = `${data.page + 1} / ${Math.max(data.totalPages, 1)}`;
  $("#prevPage").disabled = data.first;
  $("#nextPage").disabled = data.last;
  $("#pagination").hidden = data.totalElements <= data.size;
}

function downloadStatementCsv() {
  if (!activeNumber) { toast("Open an account first", "error"); return; }
  window.location.href = `/api/accounts/${activeNumber}/statement.csv`;
}

/* ---------- modals ---------- */
function openOverlay(id) { $(id).classList.add("open"); }
function closeOverlay(id) { $(id).classList.remove("open"); }

function clearFormErrors(form) {
  $$(".field__error", form).forEach((e) => (e.textContent = ""));
  $$("input", form).forEach((e) => e.classList.remove("invalid"));
}
function showFormErrors(form, errs) {
  clearFormErrors(form);
  for (const [k, v] of Object.entries(errs)) {
    const box = $(`[data-error="${k}"]`, form);
    const input = form.elements[k];
    if (box) box.textContent = v;
    if (input) input.classList.add("invalid");
  }
}

function setupPasswordToggles() {
  $$("[data-toggle-pw]").forEach((btn) => {
    if (btn.dataset.wired) return;
    btn.dataset.wired = "1";
    btn.addEventListener("click", () => {
      const input = btn.closest(".field--password").querySelector("input");
      const showing = input.type === "text";
      btn.classList.add("is-blinking");
      setTimeout(() => {
        input.type = showing ? "password" : "text";
        btn.setAttribute("aria-pressed", String(!showing));
        btn.setAttribute("aria-label", showing ? "Show password" : "Hide password");
      }, 170);
      setTimeout(() => btn.classList.remove("is-blinking"), 380);
    });
  });
}

let moneyMode = "credit";
function openMoney(mode) {
  moneyMode = mode;
  $("#moneyTitle").textContent = mode === "credit" ? "Deposit" : "Withdraw";
  $("#moneySubmit").textContent = mode === "credit" ? "Deposit" : "Withdraw";
  $("#moneyForm").reset();
  clearFormErrors($("#moneyForm"));
  openOverlay("#moneyOverlay");
  setTimeout(() => $("#moneyForm").elements.amount.focus(), 60);
}

async function submitMoney(e) {
  e.preventDefault();
  const form = $("#moneyForm");
  const d = Object.fromEntries(new FormData(form).entries());
  const btn = $("#moneySubmit");
  btn.disabled = true;
  try {
    await api(`/api/accounts/${moneyMode}`, {
      method: "POST",
      body: JSON.stringify({ accountNumber: activeNumber, amount: d.amount, pin: d.pin }),
    });
    closeOverlay("#moneyOverlay");
    toast(moneyMode === "credit" ? "Deposit complete" : "Withdrawal complete");
    ledgerPage = 0;
    loadAccounts(activeNumber);
  } catch (err) { handleFormError(form, err); }
  finally { btn.disabled = false; }
}

let transferPrefill = null;

function checkTransferTotpVisibility() {
  const form = $("#transferForm");
  const amount = Number(form.elements.amount.value || 0);
  const needsTotp = currentUser?.totpEnabled && amount >= HIGH_VALUE_THRESHOLD;
  $("#transferTotpField").hidden = !needsTotp;
}

async function submitTransfer(e) {
  e.preventDefault();
  const form = $("#transferForm");
  const d = Object.fromEntries(new FormData(form).entries());
  const btn = $("#transferSubmit");
  btn.disabled = true;
  try {
    await api("/api/accounts/transfer", {
      method: "POST",
      body: JSON.stringify({
        fromAccountNumber: activeNumber,
        toAccountNumber: Number(d.toAccountNumber),
        amount: d.amount, pin: d.pin, totpCode: d.totpCode || null,
      }),
    });
    closeOverlay("#transferOverlay");
    toast("Transfer sent");
    ledgerPage = 0;
    loadAccounts(activeNumber);
  } catch (err) {
    if (err.body?.code === "TOTP_REQUIRED") {
      $("#transferTotpField").hidden = false;
      showFormErrors(form, {});
      $('[data-error="totpCode"]', form).textContent = err.body.message || "Enter your 2FA code";
      setTimeout(() => form.elements.totpCode.focus(), 30);
      return;
    }
    if (err.body?.code === "TOTP_INVALID") {
      showFormErrors(form, { totpCode: err.body.message || "Incorrect code" });
      return;
    }
    handleFormError(form, err);
  } finally { btn.disabled = false; }
}

async function submitOpen(e) {
  e.preventDefault();
  const form = $("#openForm");
  const d = Object.fromEntries(new FormData(form).entries());
  const btn = $("#openSubmit");
  btn.disabled = true;
  try {
    const created = await api("/api/accounts", {
      method: "POST",
      body: JSON.stringify({ holderName: d.holderName, openingBalance: d.openingBalance, pin: d.pin }),
    });
    closeOverlay("#openOverlay");
    toast("Account opened");
    ledgerPage = 0;
    loadAccounts(created.accountNumber);
  } catch (err) { handleFormError(form, err); }
  finally { btn.disabled = false; }
}

function handleFormError(form, err) {
  if (err.status === 400 && err.body?.fieldErrors) { showFormErrors(form, err.body.fieldErrors); return; }
  if (err.status === 401) return;
  const msg = err.body?.message || "Something went wrong";
  toast(msg, "error");
}

/* ---------- Beneficiaries ---------- */
async function loadAndRenderBeneficiaries() {
  await refreshBeneficiaries();
  renderBeneficiaryList();
}
async function loadAndRenderTransferBenefList() {
  await refreshBeneficiaries();
  renderTransferBenefList();
}

function renderBeneficiaryList() {
  const el = $("#benefList");
  if (!beneficiaries.length) { el.innerHTML = `<div class="empty-hint">No beneficiaries saved yet. Click "Add beneficiary" to save one.</div>`; return; }
  el.innerHTML = beneficiaries.map((b) => `
    <div class="list-row">
      <div class="list-row__avatar">${escapeHtml(b.name[0] || "?").toUpperCase()}</div>
      <div>
        <div class="list-row__name">${escapeHtml(b.name)}${b.nickname ? ` <span style="color:var(--text-faint);font-weight:400">(${escapeHtml(b.nickname)})</span>` : ""}</div>
        <div class="list-row__meta">#${fmtAcct(b.accountNumber)}</div>
      </div>
      <div class="list-row__actions">
        <button class="btn btn--soft btn--sm" data-send="${b.id}">Send</button>
        <button class="btn btn--danger btn--sm" data-del="${b.id}">Remove</button>
      </div>
    </div>`).join("");
}

function renderTransferBenefList() {
  const el = $("#transferBenefList");
  if (!beneficiaries.length) { el.innerHTML = `<div class="empty-hint">No saved beneficiaries yet. Add one from the Beneficiaries tab.</div>`; return; }
  el.innerHTML = beneficiaries.map((b) => `
    <div class="list-row">
      <div class="list-row__avatar">${escapeHtml(b.name[0] || "?").toUpperCase()}</div>
      <div>
        <div class="list-row__name">${escapeHtml(b.name)}</div>
        <div class="list-row__meta">#${fmtAcct(b.accountNumber)}</div>
      </div>
      <div class="list-row__actions">
        <button class="btn btn--primary btn--sm" data-send="${b.id}">Send money</button>
      </div>
    </div>`).join("");
}

function openTransferTo(accountNumber) {
  if (!activeNumber) { toast("Open an account first", "error"); return; }
  $("#transferForm").reset();
  clearFormErrors($("#transferForm"));
  openOverlay("#transferOverlay");
  setTimeout(() => {
    $("#transferForm").elements.toAccountNumber.value = accountNumber;
    $("#transferForm").elements.amount.focus();
  }, 60);
}

/* ---------- Cards view ---------- */
async function renderCardsView() {
  const body = $("#cardsViewBody");
  if (!accounts.length) {
    body.innerHTML = `<div class="empty-hint">Open an account first to get a debit card.</div>`;
    return;
  }
  body.innerHTML = `<div class="empty-hint">Loading your cards…</div>`;
  await refreshCards();
  body.innerHTML = cards.map((c) => {
    const masked = "•••• •••• •••• " + String(c.accountNumber).slice(-4).padStart(4, "0");
    const exp = String(c.expiryMonth).padStart(2, "0") + "/" + String(c.expiryYear).slice(-2);
    const requested = !!c.replacementRequestedAt;
    return `
    <div class="cards-view-grid" style="margin-bottom:26px">
      <div class="debit-card ${c.frozen ? "is-frozen" : ""}" id="dcard-${c.accountNumber}">
        <div class="debit-card__top">
          <div class="debit-card__brand"><img src="/assets/logo.svg" alt="" />CryptoBank</div>
          <div class="debit-card__chip"></div>
        </div>
        <div class="debit-card__number">${masked}</div>
        <div class="debit-card__bottom">
          <div class="debit-card__holder">${escapeHtml(c.holderName)}</div>
          <div class="debit-card__exp"><span>VALID THRU</span>${exp}</div>
          <div class="debit-card__network">${escapeHtml(c.network)}</div>
        </div>
      </div>
      <div class="card-controls">
        <div class="card-controls__row">
          <span>Freeze card</span>
          <label class="switch">
            <input type="checkbox" data-freeze="${c.accountNumber}" ${c.frozen ? "checked" : ""} />
            <span class="switch__track"></span>
          </label>
        </div>
        <div class="card-controls__row"><span>Contactless payments</span>
          <label class="switch"><input type="checkbox" data-contactless="${c.accountNumber}" ${c.contactlessEnabled ? "checked" : ""} /><span class="switch__track"></span></label>
        </div>
        <div class="card-controls__row"><span>Online transactions</span>
          <label class="switch"><input type="checkbox" data-online="${c.accountNumber}" ${c.onlineEnabled ? "checked" : ""} /><span class="switch__track"></span></label>
        </div>
        <div class="card-controls__row"><span>Daily ATM limit</span><span class="mono" style="font-family:var(--font-mono);font-size:12.5px">₹${money0(c.dailyLimit)}</span></div>
        <button class="btn btn--ghost btn--sm" data-request-card="${c.accountNumber}" ${requested ? "disabled" : ""}>
          ${requested ? "Replacement requested" : "Request replacement card"}
        </button>
      </div>
    </div>`;
  }).join("");

  $$("[data-freeze]", body).forEach((cb) => cb.addEventListener("change", () => updateCard(cb, "frozen", `#dcard-${cb.dataset.freeze}`)));
  $$("[data-contactless]", body).forEach((cb) => cb.addEventListener("change", () => updateCard(cb, "contactlessEnabled")));
  $$("[data-online]", body).forEach((cb) => cb.addEventListener("change", () => updateCard(cb, "onlineEnabled")));
  $$("[data-request-card]", body).forEach((btn) => btn.addEventListener("click", async () => {
    const num = Number(btn.dataset.requestCard);
    btn.disabled = true;
    try {
      await api(`/api/cards/${num}/request-replacement`, { method: "POST" });
      btn.textContent = "Replacement requested";
      toast("Replacement card requested — it will arrive in 5–7 business days");
    } catch (err) { btn.disabled = false; if (err.status !== 401) toast(err.body?.message || "Couldn't request a replacement", "error"); }
  }));
}

async function updateCard(checkbox, field, cardEl) {
  const num = Number(checkbox.dataset.freeze || checkbox.dataset.contactless || checkbox.dataset.online);
  const prev = !checkbox.checked;
  checkbox.disabled = true;
  try {
    await api(`/api/cards/${num}`, { method: "PATCH", body: JSON.stringify({ [field]: checkbox.checked }) });
    if (cardEl && field === "frozen") $(cardEl).classList.toggle("is-frozen", checkbox.checked);
    if (field === "frozen") toast(checkbox.checked ? "Card frozen" : "Card unfrozen");
  } catch (err) {
    checkbox.checked = prev;
    if (err.status !== 401) toast(err.body?.message || "Couldn't update card", "error");
  } finally { checkbox.disabled = false; }
}

/* ---------- Bill Pay ---------- */
function populateBillAccounts() {
  const sel = $("#billAccount");
  if (!sel) return;
  sel.innerHTML = accounts.map((a) => `<option value="${a.accountNumber}">#${fmtAcct(a.accountNumber)} · ₹${money(a.balance)}</option>`).join("")
    || `<option value="">No accounts</option>`;
}

async function loadAndRenderBillHistory() {
  const el = $("#billHistory");
  el.innerHTML = `<div class="empty-hint">Loading…</div>`;
  try {
    const data = await api("/api/billpay/history?page=0&size=8");
    billHistory = data.content;
    renderBillHistory();
  } catch (err) {
    if (err.status !== 401) el.innerHTML = `<div class="empty-hint">Couldn't load bill history.</div>`;
  }
}

function renderBillHistory() {
  const el = $("#billHistory");
  if (!billHistory.length) { el.innerHTML = `<div class="empty-hint">No bill payments yet.</div>`; return; }
  el.innerHTML = billHistory.map((b) => `
    <div class="list-row">
      <div class="list-row__avatar"><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 3h9l3 3v15H6z"/></svg></div>
      <div>
        <div class="list-row__name">${escapeHtml(b.category)}</div>
        <div class="list-row__meta">${escapeHtml(b.consumer)} · ${fmtDate(b.createdAt)}</div>
      </div>
      <div class="list-row__actions"><span class="amt amt--debit">−₹${money(b.amount)}</span></div>
    </div>`).join("");
}

async function submitBill(e) {
  e.preventDefault();
  const form = $("#billForm");
  const d = Object.fromEntries(new FormData(form).entries());
  const btn = $("#billSubmit");
  if (!d.account) { toast("Open an account first", "error"); return; }
  btn.disabled = true;
  try {
    await api("/api/billpay", {
      method: "POST",
      body: JSON.stringify({ accountNumber: Number(d.account), category: d.category, consumer: d.consumer, amount: d.amount, pin: d.pin }),
    });
    form.reset();
    toast(`${d.category} bill paid`);
    loadAndRenderBillHistory();
    loadAccounts(activeNumber);
  } catch (err) { handleFormError(form, err); }
  finally { btn.disabled = false; }
}

/* ---------- Deposits & Loans calculators ---------- */
function calcFd() {
  const principal = Number($("#fdAmount").value) || 0;
  const months = Number($("#fdTenure").value);
  const rate = Number($("#fdRate").value);
  const isRd = $('#fdType button[data-type="rd"]').classList.contains("is-active");
  $("#fdTenureVal").textContent = months;
  $("#fdRateVal").textContent = rate.toFixed(2);

  let maturity, invested;
  const r = rate / 100;
  if (isRd) {
    // Recurring deposit: monthly deposits, quarterly compounding approximation
    invested = principal * months;
    const n = 4; // quarterly compounding
    let total = 0;
    for (let m = 1; m <= months; m++) {
      const tYears = (months - m + 1) / 12;
      total += principal * Math.pow(1 + r / n, n * tYears);
    }
    maturity = total;
  } else {
    invested = principal;
    const n = 4; // quarterly compounding, typical for FDs
    const years = months / 12;
    maturity = principal * Math.pow(1 + r / n, n * years);
  }
  const interest = maturity - invested;
  $("#fdInvested").textContent = "₹" + money0(invested);
  $("#fdInterest").textContent = "₹" + money0(interest);
  $("#fdMaturity").textContent = "₹" + money0(maturity);
}

function calcLoan() {
  const principal = Number($("#loanAmount").value) || 0;
  const months = Number($("#loanTenure").value);
  const rate = Number($("#loanRate").value);
  $("#loanTenureVal").textContent = months;
  $("#loanRateVal").textContent = rate.toFixed(2);

  const r = rate / 1200; // monthly rate
  let emi;
  if (r === 0) emi = principal / months;
  else emi = (principal * r * Math.pow(1 + r, months)) / (Math.pow(1 + r, months) - 1);
  const total = emi * months;
  const interest = total - principal;

  $("#loanEmi").textContent = "₹" + money0(emi);
  $("#loanInterest").textContent = "₹" + money0(interest);
  $("#loanTotal").textContent = "₹" + money0(total);
}

function setupCalculators() {
  ["fdAmount", "fdTenure", "fdRate"].forEach((id) => $("#" + id).addEventListener("input", calcFd));
  $$('#fdType button').forEach((b) => b.addEventListener("click", () => {
    $$('#fdType button').forEach((x) => x.classList.remove("is-active"));
    b.classList.add("is-active");
    $("#fdAmountLabel").innerHTML = b.dataset.type === "rd"
      ? 'Monthly deposit <span class="req">*</span>' : 'Deposit amount <span class="req">*</span>';
    calcFd();
  }));
  ["loanAmount", "loanTenure", "loanRate"].forEach((id) => $("#" + id).addEventListener("input", calcLoan));
  $$('#loanType button').forEach((b) => b.addEventListener("click", () => {
    $$('#loanType button').forEach((x) => x.classList.remove("is-active"));
    b.classList.add("is-active");
    $("#loanRate").value = b.dataset.rate;
    calcLoan();
  }));
  calcFd(); calcLoan();
}

/* ---------- Profile ---------- */
function maskPan(pan) {
  if (!pan || pan.length < 4) return pan || "—";
  return pan.slice(0, 2) + "XXXXX" + pan.slice(-2);
}
function maskPhone(phone) {
  if (!phone || phone.length < 4) return phone || "—";
  return "+91 " + phone.slice(0, 2) + "••••••" + phone.slice(-2);
}

function renderProfile() {
  if (!currentUser) return;
  $("#profName").textContent = currentUser.fullName || "—";
  $("#profEmail").textContent = currentUser.email || "—";
  $("#profCustId").textContent = "IT" + String(currentUser.id ?? 100000).toString().padStart(6, "0");
  $("#profPhone").textContent = maskPhone(currentUser.phone);
  $("#profDob").textContent = currentUser.dateOfBirth ? fmtDate(currentUser.dateOfBirth) : "—";
  $("#profPan").textContent = maskPan(currentUser.panNumber);
  $("#profAddress").textContent = currentUser.address || "—";
  renderTwofaStatus();
}

function renderTwofaStatus() {
  const on = !!currentUser?.totpEnabled;
  $("#twofaOff").hidden = on;
  $("#twofaOn").hidden = !on;
  $("#twofaSetup").hidden = true;
}

async function startTwofaSetup() {
  try {
    const res = await api("/api/2fa/setup", { method: "POST" });
    $("#twofaSecret").value = res.secret;
    $("#twofaOff").hidden = true;
    $("#twofaSetup").hidden = false;
    $("#twofaEnableForm").reset();
    clearFormErrors($("#twofaEnableForm"));
  } catch (err) {
    toast(err.body?.message || "Couldn't start 2FA setup", "error");
  }
}

async function submitTwofaEnable(e) {
  e.preventDefault();
  const form = $("#twofaEnableForm");
  const code = form.elements.code.value.trim();
  const btn = $("#twofaEnableSubmit");
  btn.disabled = true;
  try {
    await api("/api/2fa/enable", { method: "POST", body: JSON.stringify({ code }) });
    toast("2FA enabled");
    currentUser = await api("/api/auth/me");
    renderTwofaStatus();
  } catch (err) {
    if (err.body?.fieldErrors) { showFormErrors(form, err.body.fieldErrors); }
    else { showFormErrors(form, { code: err.body?.message || "Incorrect code" }); }
  } finally { btn.disabled = false; }
}

async function submitTwofaDisable(e) {
  e.preventDefault();
  const form = $("#twofaDisableForm");
  const d = Object.fromEntries(new FormData(form).entries());
  const btn = $("#twofaDisableSubmit");
  btn.disabled = true;
  try {
    await api("/api/2fa/disable", { method: "POST", body: JSON.stringify({ password: d.password, code: d.code }) });
    toast("2FA disabled");
    form.reset();
    currentUser = await api("/api/auth/me");
    renderTwofaStatus();
  } catch (err) {
    if (err.body?.fieldErrors) { showFormErrors(form, err.body.fieldErrors); }
    else if (err.status === 403 && (err.body?.message || "").toLowerCase().includes("password")) {
      showFormErrors(form, { password: err.body.message });
    } else {
      showFormErrors(form, { code: err.body?.message || "Couldn't disable 2FA" });
    }
  } finally { btn.disabled = false; }
}

async function submitPasswordChange(e) {
  e.preventDefault();
  const form = $("#passwordForm");
  const d = Object.fromEntries(new FormData(form).entries());
  const btn = $("#passwordSubmit");
  clearFormErrors(form);
  if (d.newPassword !== d.confirmPassword) {
    showFormErrors(form, { confirmPassword: "Passwords don't match" });
    return;
  }
  btn.disabled = true;
  try {
    await api("/api/auth/change-password", {
      method: "POST",
      body: JSON.stringify({ currentPassword: d.currentPassword, newPassword: d.newPassword }),
    });
    form.reset();
    toast("Password updated");
  } catch (err) {
    if (err.status === 403) { showFormErrors(form, { currentPassword: err.body?.message || "Incorrect password" }); }
    else handleFormError(form, err);
  } finally { btn.disabled = false; }
}

/* ---------- Support / FAQ ---------- */
async function loadAndRenderTickets() {
  const el = $("#ticketList");
  el.innerHTML = `<div class="empty-hint">Loading…</div>`;
  try {
    const tickets = await api("/api/support/tickets");
    if (!tickets.length) { el.innerHTML = `<div class="empty-hint">No messages sent yet.</div>`; return; }
    el.innerHTML = tickets.map((t) => `
      <div class="list-row">
        <div class="list-row__avatar"><svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg></div>
        <div>
          <div class="list-row__name">${escapeHtml(t.subject)}</div>
          <div class="list-row__meta">${escapeHtml(t.ticketNumber)} · ${fmtDate(t.createdAt)}</div>
        </div>
        <div class="list-row__actions"><span class="amt" style="color:var(--text-dim)">${escapeHtml(t.status)}</span></div>
      </div>`).join("");
  } catch (err) {
    if (err.status !== 401) el.innerHTML = `<div class="empty-hint">Couldn't load your tickets.</div>`;
  }
}
const FAQS = [
  { q: "How do I open a new account?", a: "Go to Accounts and click \"Open a new account\". Set an opening balance and a security PIN, and it's ready instantly." },
  { q: "Is my security PIN the same as my login password?", a: "No. Your password signs you in; your PIN is required separately to authorize deposits, withdrawals, and transfers." },
  { q: "How long do transfers take?", a: "Transfers between accounts in this app complete instantly and appear immediately in both statements." },
  { q: "Can I freeze my debit card?", a: "Yes — go to Cards and toggle \"Freeze card\" any time. Unfreeze it the same way." },
  { q: "Where can I download my statement?", a: "Open Accounts, select an account, and use \"Download statement (CSV)\" — it exports your complete transaction history, not just the page you're viewing." },
  { q: "Are the deposit and loan rates real offers?", a: "No — the Deposits & Loans calculators use illustrative default rates for demonstration only." },
];
function renderFaq() {
  $("#faqList").innerHTML = FAQS.map((f, i) => `
    <div class="faq-item" data-i="${i}">
      <button type="button" class="faq-q">${escapeHtml(f.q)}<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9l6 6 6-6"/></svg></button>
      <div class="faq-a">${escapeHtml(f.a)}</div>
    </div>`).join("");
  $$(".faq-q").forEach((btn) => btn.addEventListener("click", () => btn.closest(".faq-item").classList.toggle("open")));
}

/* ---------- theme ---------- */
const THEME_KEY = "bank-theme";
const themeMq = window.matchMedia("(prefers-color-scheme: dark)");
const getThemeChoice = () => { try { return localStorage.getItem(THEME_KEY) || "light"; } catch { return "light"; } };
function applyTheme(choice) {
  const dark = choice === "dark" || (choice === "system" && themeMq.matches);
  document.documentElement.dataset.theme = dark ? "dark" : "light";
  $$("[data-theme-set]").forEach((b) => b.setAttribute("aria-pressed", String(b.dataset.themeSet === choice)));
}
function setupTheme() {
  applyTheme(getThemeChoice());
  $$("[data-theme-set]").forEach((b) => b.addEventListener("click", () => {
    try { localStorage.setItem(THEME_KEY, b.dataset.themeSet); } catch {}
    applyTheme(b.dataset.themeSet);
  }));
  themeMq.addEventListener("change", () => { if (getThemeChoice() === "system") applyTheme("system"); });
}

/* ---------- init ---------- */
async function init() {
  setupTheme();
  renderFaq();
  setupCalculators();

  // Gate: must be authenticated.
  try { currentUser = await api("/api/auth/me"); }
  catch { return; } // api() already redirected on 401
  $("#userName").textContent = currentUser.fullName;
  if (currentUser.role === "ADMIN") $("#adminNavLink").hidden = false;
  setupPasswordToggles();

  // sidebar nav
  $$(".nav__item[data-view]").forEach((b) => b.addEventListener("click", () => goToView(b.dataset.view)));
  $$("[data-goto]").forEach((b) => b.addEventListener("click", () => goToView(b.dataset.goto)));
  $$("[data-quick]").forEach((b) => b.addEventListener("click", () => {
    const a = b.dataset.quick;
    if (a === "transfer") { if (!activeNumber) { toast("Open an account first", "error"); return; } $("#transferForm").reset(); clearFormErrors($("#transferForm")); openOverlay("#transferOverlay"); }
    else if (a === "credit") { if (!activeNumber) { toast("Open an account first", "error"); return; } openMoney("credit"); }
    else if (a === "debit") { if (!activeNumber) { toast("Open an account first", "error"); return; } openMoney("debit"); }
  }));

  // mobile menu toggle
  const menuToggle = $("#menuToggle");
  function checkMobile() { menuToggle.style.display = window.innerWidth <= 780 ? "inline-flex" : "none"; }
  checkMobile();
  window.addEventListener("resize", checkMobile);
  menuToggle.addEventListener("click", () => $("#sidebar").classList.toggle("is-open"));

  // notifications dropdown
  $("#notifBtn").addEventListener("click", (e) => { e.stopPropagation(); $("#notifPanel").classList.toggle("open"); });
  document.addEventListener("click", (e) => { if (!e.target.closest(".dropdown")) $("#notifPanel").classList.remove("open"); });

  // card selection (event delegation) — both accounts view and dashboard rail
  ["#cardsRail", "#dashCardsRail"].forEach((sel) => {
    const el = $(sel);
    if (!el) return;
    el.addEventListener("click", (e) => {
      if (e.target.closest("#openCard")) { $("#openForm").reset(); clearFormErrors($("#openForm")); openOverlay("#openOverlay"); return; }
      const card = e.target.closest("[data-acct]");
      if (card) {
        activeNumber = Number(card.dataset.acct);
        ledgerPage = 0;
        renderCards();
        if (sel === "#dashCardsRail") { goToView("accounts"); }
        $("#detailSection").hidden = false;
        loadLedger();
      }
    });
    el.addEventListener("keydown", (e) => {
      if (e.key === "Enter" || e.key === " ") { const t = e.target.closest("[data-acct],#openCard"); if (t) { e.preventDefault(); t.click(); } }
    });
  });

  // actions
  $$("[data-action]").forEach((b) => b.addEventListener("click", () => {
    const a = b.dataset.action;
    if (a === "credit") openMoney("credit");
    else if (a === "debit") openMoney("debit");
    else if (a === "transfer") { $("#transferForm").reset(); clearFormErrors($("#transferForm")); openOverlay("#transferOverlay"); setTimeout(() => $("#transferForm").elements.toAccountNumber.focus(), 60); }
  }));
  $("#newTransferBtn").addEventListener("click", () => {
    if (!activeNumber) { toast("Open an account first", "error"); return; }
    $("#transferForm").reset(); clearFormErrors($("#transferForm")); openOverlay("#transferOverlay");
  });
  $("#downloadStatementBtn").addEventListener("click", downloadStatementCsv);

  // forms
  $("#moneyForm").addEventListener("submit", submitMoney);
  $("#transferForm").addEventListener("submit", submitTransfer);
  $("#transferForm").elements.amount.addEventListener("input", checkTransferTotpVisibility);
  $("#openForm").addEventListener("submit", submitOpen);
  $("#billForm").addEventListener("submit", submitBill);

  // beneficiaries
  $("#addBenefBtn").addEventListener("click", () => { $("#benefForm").reset(); clearFormErrors($("#benefForm")); openOverlay("#benefOverlay"); });
  $("#benefForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const form = e.target;
    const d = Object.fromEntries(new FormData(form).entries());
    if (!d.name || !d.accountNumber) return;
    try {
      await api("/api/beneficiaries", {
        method: "POST",
        body: JSON.stringify({ name: d.name, accountNumber: Number(d.accountNumber), nickname: d.nickname || "" }),
      });
      closeOverlay("#benefOverlay");
      toast("Beneficiary saved");
      await refreshBeneficiaries();
      renderBeneficiaryList();
      renderTransferBenefList();
      renderDashboardStats();
    } catch (err) { handleFormError(form, err); }
  });
  $("#benefClose").addEventListener("click", () => closeOverlay("#benefOverlay"));
  ["#benefList", "#transferBenefList"].forEach((sel) => {
    $(sel).addEventListener("click", async (e) => {
      const sendBtn = e.target.closest("[data-send]");
      const delBtn = e.target.closest("[data-del]");
      if (sendBtn) { const b = beneficiaries.find((x) => x.id === Number(sendBtn.dataset.send)); if (b) openTransferTo(b.accountNumber); }
      if (delBtn) {
        try {
          await api(`/api/beneficiaries/${delBtn.dataset.del}`, { method: "DELETE" });
          await refreshBeneficiaries();
          renderBeneficiaryList(); renderTransferBenefList(); renderDashboardStats();
          toast("Beneficiary removed");
        } catch (err) { if (err.status !== 401) toast(err.body?.message || "Couldn't remove beneficiary", "error"); }
      }
    });
  });

  // support form
  $("#supportForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const form = e.target;
    const d = Object.fromEntries(new FormData(form).entries());
    const btn = $("#supportSubmit");
    btn.disabled = true;
    try {
      const ticket = await api("/api/support/tickets", {
        method: "POST",
        body: JSON.stringify({ subject: d.subject, message: d.message }),
      });
      form.reset();
      toast("Message sent — ticket " + ticket.ticketNumber);
      loadAndRenderTickets();
    } catch (err) { handleFormError(form, err); }
    finally { btn.disabled = false; }
  });

  // password change
  $("#passwordForm").addEventListener("submit", submitPasswordChange);

  // two-factor authentication
  $("#twofaStartBtn").addEventListener("click", startTwofaSetup);
  $("#twofaCancelBtn").addEventListener("click", () => { $("#twofaSetup").hidden = true; $("#twofaOff").hidden = false; });
  $("#twofaCopyBtn").addEventListener("click", async () => {
    try { await navigator.clipboard.writeText($("#twofaSecret").value); toast("Key copied"); }
    catch { $("#twofaSecret").select(); }
  });
  $("#twofaEnableForm").addEventListener("submit", submitTwofaEnable);
  $("#twofaDisableForm").addEventListener("submit", submitTwofaDisable);

  // modal close
  $("#moneyClose").addEventListener("click", () => closeOverlay("#moneyOverlay"));
  $("#transferClose").addEventListener("click", () => closeOverlay("#transferOverlay"));
  $("#openClose").addEventListener("click", () => closeOverlay("#openOverlay"));
  $$(".overlay").forEach((o) => o.addEventListener("click", (e) => { if (e.target === o) o.classList.remove("open"); }));
  document.addEventListener("keydown", (e) => { if (e.key === "Escape") $$(".overlay.open").forEach((o) => o.classList.remove("open")); });

  // pagination
  $("#prevPage").addEventListener("click", () => { if (ledgerPage > 0) { ledgerPage--; loadLedger(); } });
  $("#nextPage").addEventListener("click", () => { if (!ledgerMeta.last) { ledgerPage++; loadLedger(); } });

  // logout
  $("#logoutBtn").addEventListener("click", async () => {
    try { await fetch("/api/auth/logout", { method: "POST" }); } catch {}
    window.location.href = "/login.html";
  });

  loadAccounts();
}

document.addEventListener("DOMContentLoaded", init);
