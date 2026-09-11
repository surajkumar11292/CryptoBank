/* CryptoBank — Admin console logic */
const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];

const escapeHtml = (v) => v == null ? "" : String(v).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
const money = (n) => Number(n || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const fmtAcct = (num) => String(num).replace(/(\d{4})(?=\d)/g, "$1 ");
const fmtDate = (iso) => { try { return new Date(iso).toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" }); } catch { return iso; } };

function toast(msg, type = "ok") {
  const t = document.createElement("div");
  t.className = "toast" + (type === "error" ? " toast--error" : "");
  t.innerHTML = `<span class="toast__ic">${type === "error" ? "!" : "\u2713"}</span>${escapeHtml(msg)}`;
  $("#toasts").appendChild(t);
  setTimeout(() => { t.style.opacity = "0"; t.style.transform = "translateX(16px)"; t.style.transition = "all .2s"; setTimeout(() => t.remove(), 200); }, 3000);
}

/** Same fetch wrapper convention as the customer app.js. */
async function api(path, options = {}) {
  const res = await fetch(path, { headers: { "Content-Type": "application/json" }, ...options });
  if (res.status === 401) { window.location.href = "/login.html"; throw { status: 401 }; }
  const text = await res.text();
  const body = text ? JSON.parse(text) : null;
  if (!res.ok) throw { status: res.status, body };
  return body;
}

let users = [];
let tickets = [];

function renderStats() {
  const totalAccounts = users.reduce((sum, u) => sum + u.accounts.length, 0);
  const frozenAccounts = users.reduce((sum, u) => sum + u.accounts.filter((a) => a.frozen).length, 0);
  const openTickets = tickets.filter((t) => t.status === "OPEN").length;
  $("#adminStats").innerHTML = [
    ["Users", users.length],
    ["Accounts", totalAccounts],
    ["Frozen accounts", frozenAccounts],
    ["Open tickets", openTickets],
  ].map(([label, value]) => `
    <div class="admin-stat">
      <div class="admin-stat__label">${label}</div>
      <div class="admin-stat__value">${value}</div>
    </div>`).join("");
}

function renderUsers() {
  $("#usersCount").textContent = `${users.length} user${users.length === 1 ? "" : "s"}`;
  if (!users.length) {
    $("#usersBody").innerHTML = `<tr><td colspan="5"><div class="state"><div class="state__title">No users yet</div></div></td></tr>`;
    return;
  }
  $("#usersBody").innerHTML = users.map((u) => `
    <tr>
      <td>${escapeHtml(u.fullName)}</td>
      <td>${escapeHtml(u.email)}</td>
      <td><span class="status-pill ${u.role === "ADMIN" ? "status-pill--open" : "status-pill--closed"}">${escapeHtml(u.role)}</span></td>
      <td>
        <div class="admin-accts">
          ${u.accounts.length ? u.accounts.map((a) => `
            <div class="admin-acct-row">
              <span>#${fmtAcct(a.accountNumber)}</span>
              <span style="color:var(--text-dim)">&#8377;${money(a.balance)}</span>
              <span class="status-pill ${a.frozen ? "status-pill--frozen" : "status-pill--active"}">${a.frozen ? "Frozen" : "Active"}</span>
              <button class="btn ${a.frozen ? "btn--soft" : "btn--danger"}" data-toggle-freeze="${a.accountNumber}" data-frozen="${a.frozen}">
                ${a.frozen ? "Unfreeze" : "Freeze"}
              </button>
            </div>`).join("") : `<span style="color:var(--text-faint)">No accounts</span>`}
        </div>
      </td>
      <td>${fmtDate(u.createdAt)}</td>
    </tr>`).join("");
}

function renderTickets() {
  $("#ticketsCount").textContent = `${tickets.length} ticket${tickets.length === 1 ? "" : "s"}`;
  const el = $("#ticketList");
  if (!tickets.length) { el.innerHTML = `<div class="state"><div class="state__title">No tickets</div></div>`; return; }
  el.innerHTML = tickets.map((t) => `
    <div class="ticket-admin-row">
      <div class="ticket-admin-row__body">
        <div style="display:flex;align-items:center;gap:8px;margin-bottom:4px;">
          <div class="ticket-admin-row__subject">${escapeHtml(t.subject)}</div>
          <span class="ai-spark-badge" style="font-size:10px;padding:2px 6px;">AI: ${escapeHtml(t.aiCategory || "GENERAL")}</span>
        </div>
        <div class="ticket-admin-row__msg">${escapeHtml(t.message)}</div>
        <div class="ticket-admin-row__meta">Ticket #${t.id} &middot; ${escapeHtml(t.userFullName || "Customer")} (${escapeHtml(t.userEmail || "")}) &middot; ${fmtDate(t.createdAt)}</div>
        ${t.aiDraftReply ? `
        <div class="ai-draft-box" style="margin-top:10px;padding:10px 12px;background:var(--surface-2);border-radius:8px;font-size:12px;border:1px solid var(--border);line-height:1.45;">
          <div style="font-weight:700;color:#818cf8;display:flex;align-items:center;gap:4px;margin-bottom:4px;">
            <span>✨ Gemini Auto-Drafted Resolution:</span>
          </div>
          <div style="white-space:pre-wrap;color:var(--text-dim);">${escapeHtml(t.aiDraftReply)}</div>
        </div>` : ""}
      </div>
      <div class="ticket-admin-row__side">
        <span class="status-pill ${t.status === "OPEN" ? "status-pill--open" : "status-pill--closed"}">${escapeHtml(t.status)}</span>
        ${t.status === "OPEN" ? `<button class="btn btn--ghost btn--sm" data-close-ticket="${t.id}">Mark resolved</button>` : ""}
      </div>
    </div>`).join("");
}

async function loadAll() {
  try {
    [users, tickets] = await Promise.all([api("/api/admin/users"), api("/api/admin/tickets")]);
  } catch (err) {
    if (err.status === 403) { $("#gate").hidden = false; $("#adminContent").hidden = true; return; }
    toast("Couldn't load admin data", "error");
    return;
  }
  renderStats();
  renderUsers();
  renderTickets();
}

async function init() {
  let me;
  try { me = await api("/api/auth/me"); } catch { return; }
  $("#adminName").textContent = me.fullName;

  if (me.role !== "ADMIN") {
    $("#gate").hidden = false;
    $("#adminContent").hidden = true;
    return;
  }
  $("#adminContent").hidden = false;
  await loadAll();

  $("#usersBody").addEventListener("click", async (e) => {
    const btn = e.target.closest("[data-toggle-freeze]");
    if (!btn) return;
    const accountNumber = btn.dataset.toggleFreeze;
    const currentlyFrozen = btn.dataset.frozen === "true";
    btn.disabled = true;
    try {
      await api(`/api/admin/accounts/${accountNumber}/${currentlyFrozen ? "unfreeze" : "freeze"}`, { method: "POST" });
      toast(`Account #${accountNumber} ${currentlyFrozen ? "unfrozen" : "frozen"}`);
      await loadAll();
    } catch {
      toast("Couldn't update that account", "error");
      btn.disabled = false;
    }
  });

  $("#ticketList").addEventListener("click", async (e) => {
    const btn = e.target.closest("[data-close-ticket]");
    if (!btn) return;
    btn.disabled = true;
    try {
      await api(`/api/admin/tickets/${btn.dataset.closeTicket}/close`, { method: "POST" });
      toast("Ticket marked resolved");
      await loadAll();
    } catch {
      toast("Couldn't update that ticket", "error");
      btn.disabled = false;
    }
  });

  $("#logoutBtn").addEventListener("click", async () => {
    try { await fetch("/api/auth/logout", { method: "POST" }); } catch {}
    window.location.href = "/login.html";
  });
}

init();
