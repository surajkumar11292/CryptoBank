/* CryptoBank — login / register page logic */
const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];

const form = $("#authForm");
const mfaForm = $("#mfaForm");
let mode = "login";
let regStep = 1;
const REG_STEPS = 4;
const MIN_AGE_YEARS = 18;

const escapeHtml = (v) => v == null ? "" : String(v).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

function toast(msg, type = "ok") {
  const t = document.createElement("div");
  t.className = "toast" + (type === "error" ? " toast--error" : type === "info" ? " toast--info" : "");
  const icon = type === "error" ? "!" : type === "info" ? "\u23F3" : "\u2713";
  t.innerHTML = `<span class="toast__ic">${icon}</span>${escapeHtml(msg)}`;
  $("#toasts").appendChild(t);
  setTimeout(() => { t.style.opacity = "0"; t.style.transform = "translateX(16px)"; t.style.transition = "all .2s"; setTimeout(() => t.remove(), 200); }, 4200);
}

/* ---------- Form-level banners: for server messages that aren't tied to a single field ---------- */
function showBanner(scopeForm, msg) {
  const isMfa = scopeForm === mfaForm;
  const banner = isMfa ? $("#mfaFormError") : $("#authFormError");
  const text = isMfa ? $("#mfaFormErrorText") : $("#authFormErrorText");
  if (!banner || !text) return;
  text.textContent = msg;
  banner.hidden = false;
}
function hideBanner(scopeForm) {
  const isMfa = scopeForm === mfaForm;
  const banner = isMfa ? $("#mfaFormError") : $("#authFormError");
  if (banner) banner.hidden = true;
}

/* ---------- Button loading state ---------- */
function setLoading(btn, isLoading) {
  if (!btn) return;
  btn.disabled = isLoading;
  btn.classList.toggle("is-loading", isLoading);
}

/* ---------- Networking: one place that talks to the API and never fails silently ----------
   Returns { ok, status, body } always. Network-level failures (offline, DNS, CORS) resolve
   with status 0 instead of throwing, so callers have one consistent shape to check. */
async function apiCall(path, options = {}) {
  try {
    const res = await fetch(path, {
      headers: { "Content-Type": "application/json" },
      ...options,
    });
    const text = await res.text().catch(() => "");
    let body = null;
    if (text) { try { body = JSON.parse(text); } catch { body = null; } }
    return { ok: res.ok, status: res.status, body };
  } catch {
    return { ok: false, status: 0, body: null };
  }
}

/**
 * The universal error handler. Every failed request in this file funnels through here so no
 * server-sent error can ever get silently dropped again:
 *   - field-level errors (fieldErrors: {field: msg}) populate the inline field messages
 *   - a top-level `message` (business-rule rejections like the age check, 401s, 403s, 409s,
 *     500s) surfaces as a form banner AND a toast, so it's impossible to miss
 *   - a true network failure (status 0) gets its own clear, actionable message
 * Returns true if it displayed something (so callers know not to also show a generic message).
 */
function reportApiError(scopeForm, { status, body }, fallback = "Something went wrong — please try again") {
  const fieldErrors = body && typeof body === "object" ? body.fieldErrors : null;
  if (fieldErrors && Object.keys(fieldErrors).length) {
    showErrors(fieldErrors, scopeForm);
    return true;
  }
  if (status === 0) {
    showBanner(scopeForm, "Couldn't reach the server — check your connection and try again.");
    toast("Couldn't reach the server", "error");
    return true;
  }
  const msg = (body && body.message) || fallback;
  showBanner(scopeForm, msg);
  toast(msg, "error");
  return true;
}

/* ---------- Mode: sign in (single step) vs create account (wizard) ---------- */
function setMode(m) {
  mode = m;
  $$(".auth-tab").forEach((t) => t.setAttribute("aria-selected", String(t.dataset.mode === m)));
  const register = m === "register";

  $(`[data-mode-panel="login"]`).classList.toggle("is-active", !register);
  $("#regStepper").hidden = !register;
  $("#authDemo").hidden = register;

  if (register) {
    regStep = 1;
    renderRegStep();
  } else {
    $$("[data-reg-step]").forEach((p) => p.classList.remove("is-active"));
  }

  $("#authHeading").textContent = register ? "Create your account" : "Welcome back";
  $("#authSubtext").textContent = register ? "A few quick steps and you're set up." : "Sign in to manage your accounts.";
  clearErrors(form);
  hideBanner(form);
}
$$(".auth-tab").forEach((t) => t.addEventListener("click", () => setMode(t.dataset.mode)));

/* ---------- Signup wizard: step rendering ---------- */
function renderRegStep() {
  $$("[data-reg-step]").forEach((p) => p.classList.toggle("is-active", Number(p.dataset.regStep) === regStep));
  $$("[data-step-indicator]").forEach((el) => {
    const n = Number(el.dataset.stepIndicator);
    el.classList.toggle("is-active", n === regStep);
    el.classList.toggle("is-done", n < regStep);
  });
  $$("[data-step-label]").forEach((el) => el.classList.toggle("is-active", Number(el.dataset.stepLabel) === regStep));
  if (regStep === REG_STEPS) buildReview();
  const active = $(`[data-reg-step="${regStep}"]`);
  const firstInput = active && active.querySelector("input:not([type=checkbox])");
  if (firstInput) setTimeout(() => firstInput.focus(), 60);
}

const REG_STEP_FIELDS = {
  1: ["fullName", "regEmail"],
  2: ["regPassword", "confirmPassword"],
  3: ["phone", "dateOfBirth", "panNumber", "address"],
};

/** Mirrors the backend's 18+ rule so the person finds out instantly, not after a round trip. */
function calcAge(dobStr) {
  const dob = new Date(dobStr);
  if (Number.isNaN(dob.getTime())) return null;
  const today = new Date();
  let age = today.getFullYear() - dob.getFullYear();
  const monthDiff = today.getMonth() - dob.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < dob.getDate())) age--;
  return age;
}

function validateRegStep(step) {
  const errs = {};
  const v = (name) => (form.elements[name] ? form.elements[name].value.trim() : "");

  if (step === 1) {
    if (!v("fullName")) errs.fullName = "Enter your full name";
    const email = v("regEmail");
    if (!email) errs.regEmail = "Enter your email address";
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) errs.regEmail = "Enter a valid email address";
  }
  if (step === 2) {
    const pw = v("regPassword");
    const cpw = v("confirmPassword");
    if (!pw) errs.regPassword = "Create a password";
    else if (pw.length < 8) errs.regPassword = "Use at least 8 characters";
    if (!cpw) errs.confirmPassword = "Re-enter your password";
    else if (pw && cpw !== pw) errs.confirmPassword = "Passwords don't match";
  }
  if (step === 3) {
    if (!/^[6-9][0-9]{9}$/.test(v("phone"))) errs.phone = "Enter a valid 10-digit mobile number";
    const dob = v("dateOfBirth");
    if (!dob) errs.dateOfBirth = "Enter your date of birth";
    else {
      const dobDate = new Date(dob);
      if (Number.isNaN(dobDate.getTime()) || dobDate > new Date()) errs.dateOfBirth = "Enter a valid date of birth";
      else {
        const age = calcAge(dob);
        if (age !== null && age < MIN_AGE_YEARS) errs.dateOfBirth = `You must be at least ${MIN_AGE_YEARS} years old to open an account`;
      }
    }
    if (!/^[A-Z]{5}[0-9]{4}[A-Z]$/.test(v("panNumber").toUpperCase())) errs.panNumber = "PAN must look like ABCDE1234F";
    if (!v("address")) errs.address = "Enter your residential address";
  }
  return errs;
}

$$("[data-step-next]").forEach((btn) => btn.addEventListener("click", () => {
  const errs = validateRegStep(regStep);
  if (Object.keys(errs).length) { showErrors(errs, form); return; }
  clearErrors(form);
  hideBanner(form);
  regStep = Math.min(REG_STEPS, regStep + 1);
  renderRegStep();
}));
$$("[data-step-back]").forEach((btn) => btn.addEventListener("click", () => {
  clearErrors(form);
  hideBanner(form);
  regStep = Math.max(1, regStep - 1);
  renderRegStep();
}));

function buildReview() {
  const v = (name) => (form.elements[name] ? form.elements[name].value.trim() : "");
  const rows = [
    ["Full name", v("fullName")],
    ["Email", v("regEmail")],
    ["Mobile number", v("phone")],
    ["Date of birth", v("dateOfBirth")],
    ["PAN number", v("panNumber").toUpperCase()],
    ["Address", v("address")],
  ];
  $("#reviewList").innerHTML = rows.map(([k, val]) =>
    `<div class="review-list__row"><span>${escapeHtml(k)}</span><span>${escapeHtml(val || "—")}</span></div>`
  ).join("");
}

/* Keep users from even picking a future or under-18 date of birth in the date picker itself. */
(function constrainDobInput() {
  const dobInput = form.elements["dateOfBirth"];
  if (!dobInput) return;
  const today = new Date();
  const maxDob = new Date(today.getFullYear() - MIN_AGE_YEARS, today.getMonth(), today.getDate());
  dobInput.max = maxDob.toISOString().slice(0, 10);
  dobInput.min = "1900-01-01";
})();

/* ---------- Password strength meter (step 2) ---------- */
const pwInput = form.elements["regPassword"];
function pwStrength(pw) {
  if (!pw) return 0;
  let score = 0;
  if (pw.length >= 8) score++;
  if (pw.length >= 12) score++;
  if (/[a-z]/.test(pw) && /[A-Z]/.test(pw)) score++;
  if (/[0-9]/.test(pw) && /[^A-Za-z0-9]/.test(pw)) score++;
  return Math.max(pw.length >= 8 ? 1 : 0, Math.min(4, score));
}
const STRENGTH_LABELS = ["Enter a password", "Weak — add more characters", "Fair — try mixing case & numbers", "Good — add a symbol for extra strength", "Strong password"];
if (pwInput) {
  pwInput.addEventListener("input", () => {
    const level = pwStrength(pwInput.value);
    $("#pwStrength").dataset.level = String(level);
    $("#pwStrengthLabel").textContent = STRENGTH_LABELS[level];
  });
}

function clearErrors(scopeForm) {
  $$(".field__error", scopeForm).forEach((e) => (e.textContent = ""));
  $$("input", scopeForm).forEach((e) => e.classList.remove("invalid"));
}
function showErrors(errs, scopeForm) {
  clearErrors(scopeForm);
  hideBanner(scopeForm);
  for (const [k, v] of Object.entries(errs)) {
    const box = $(`[data-error="${k}"]`, scopeForm);
    const input = scopeForm.elements[k];
    if (box) box.textContent = v;
    if (input) input.classList.add("invalid");
  }
  // Jump back to whichever register step actually owns the first error, if we're past it.
  if (scopeForm === form && mode === "register") {
    for (const [step, fields] of Object.entries(REG_STEP_FIELDS)) {
      if (fields.some((f) => errs[f]) && Number(step) < regStep) {
        regStep = Number(step);
        renderRegStep();
        setTimeout(() => showErrors(errs, scopeForm), 30);
        break;
      }
    }
  }
}

/* ---------- Animated password show/hide ---------- */
$$("[data-toggle-pw]").forEach((btn) => {
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

/* ---------- Enter key advances the wizard instead of submitting mid-flow ---------- */
form.addEventListener("keydown", (e) => {
  if (e.key !== "Enter" || e.target.tagName !== "INPUT") return;
  if (mode !== "register" || regStep === REG_STEPS) return; // last step: let Enter submit normally
  e.preventDefault();
  const nextBtn = $(`[data-reg-step="${regStep}"] [data-step-next]`);
  if (nextBtn) nextBtn.click();
});

/* ---------- login / register submit ---------- */
form.addEventListener("submit", async (e) => {
  e.preventDefault();
  if (mode === "register") {
    const errs = validateRegStep(3);
    Object.assign(errs, validateRegStep(2), validateRegStep(1));
    if (Object.keys(errs).length) { showErrors(errs, form); return; }
    if (!$("#consentCheck").checked) { toast("Please accept the Terms & Conditions to continue", "error"); return; }
  }
  clearErrors(form);
  hideBanner(form);
  const data = Object.fromEntries(new FormData(form).entries());
  const submitBtn = mode === "register" ? $("#authSubmit-register") : $("#authSubmit");
  setLoading(submitBtn, true);

  // If the free-tier server is cold-starting, a request can take 30–60s. Tell the person what's
  // going on instead of leaving them staring at a button that looks frozen.
  const coldStartTimer = setTimeout(() => {
    toast("Still working — the server may be waking up, this can take up to a minute", "info");
  }, 6000);

  try {
    const loginEmail = mode === "register" ? data.regEmail : data.email;
    const loginPassword = mode === "register" ? data.regPassword : data.password;

    if (mode === "register") {
      const res = await apiCall("/api/auth/register", {
        method: "POST",
        body: JSON.stringify({
          fullName: data.fullName, email: data.regEmail, password: data.regPassword,
          phone: data.phone, dateOfBirth: data.dateOfBirth,
          panNumber: (data.panNumber || "").toUpperCase(), address: data.address,
        }),
      });
      if (res.status === 409) { showBanner(form, "An account already exists for that email"); toast("An account already exists for that email", "error"); regStep = 1; renderRegStep(); return; }
      if (!res.ok) { reportApiError(form, res, "Couldn't create your account — please try again"); return; }
    }
    // login (also runs right after a successful register)
    const login = await apiCall("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email: loginEmail, password: loginPassword }),
    });
    if (login.status === 401) { showBanner(form, "Invalid email or password"); toast("Invalid email or password", "error"); return; }
    if (!login.ok) { reportApiError(form, login, "Couldn't sign you in — please try again"); return; }

    const body = login.body || {};
    if (body.mfaRequired) {
      showMfaStep(body.fullName);
      return;
    }
    window.location.href = "/";
  } finally {
    clearTimeout(coldStartTimer);
    setLoading(submitBtn, false);
  }
});

/* ---------- 2FA step ---------- */
function showMfaStep(fullName) {
  form.hidden = true;
  $("#authDemo").hidden = true;
  $$(".auth-tabs")[0].hidden = true;
  $("#regStepper").hidden = true;
  mfaForm.hidden = false;
  $("#authHeading").textContent = "Verify it's you";
  $("#authSubtext").textContent = fullName ? `Welcome back, ${fullName.split(" ")[0]}.` : "Enter your authentication code.";
  mfaForm.reset();
  clearErrors(mfaForm);
  hideBanner(mfaForm);
  setTimeout(() => mfaForm.elements.code.focus(), 60);
}

function backToLogin() {
  mfaForm.hidden = true;
  form.hidden = false;
  $$(".auth-tabs")[0].hidden = false;
  setMode("login");
}
$("#mfaBack").addEventListener("click", backToLogin);

mfaForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  clearErrors(mfaForm);
  hideBanner(mfaForm);
  const code = mfaForm.elements.code.value.trim();
  const btn = $("#mfaSubmit");
  setLoading(btn, true);
  try {
    const res = await apiCall("/api/auth/login/2fa", {
      method: "POST",
      body: JSON.stringify({ code }),
    });
    if (res.status === 403) {
      const msg = res.body?.message || "Incorrect code";
      showErrors({ code: msg }, mfaForm);
      if (msg.toLowerCase().includes("sign in again")) { toast(msg, "error"); backToLogin(); }
      return;
    }
    if (!res.ok) { reportApiError(mfaForm, res, "Couldn't verify that code — please try again"); return; }
    window.location.href = "/";
  } finally {
    setLoading(btn, false);
  }
});

// If already signed in, skip the login page.
apiCall("/api/auth/me").then((r) => { if (r.ok) window.location.href = "/"; });
