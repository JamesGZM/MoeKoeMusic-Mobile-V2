const screens = [...document.querySelectorAll("[data-screen]")];
const methodButtons = [...document.querySelectorAll("[data-method]")];
const methodPanels = [...document.querySelectorAll("[data-panel]")];
const historyStack = [];
let currentScreen = "login";
let currentMethod = "sms";
let qrState = "waiting";
let toastTimer;

function showToast(message) {
  const toast = document.querySelector(".toast");
  toast.textContent = message;
  toast.classList.add("show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove("show"), 1800);
}

function showScreen(name, push = true) {
  if (name === currentScreen) return;
  if (push) historyStack.push(currentScreen);
  screens.forEach((screen) => screen.classList.toggle("active", screen.dataset.screen === name));
  currentScreen = name;
  updateConsole(name);
}

function goBack() {
  const previous = historyStack.pop() || "login";
  screens.forEach((screen) => screen.classList.toggle("active", screen.dataset.screen === previous));
  currentScreen = previous;
  updateConsole(previous);
}

function selectMethod(method) {
  currentMethod = method;
  methodButtons.forEach((button) => button.setAttribute("aria-selected", String(button.dataset.method === method)));
  methodPanels.forEach((panel) => {
    const selected = panel.dataset.panel === method;
    panel.hidden = !selected;
    panel.classList.toggle("active", selected);
  });
  if (currentScreen !== "login") showScreen("login");
  updateConsole(method);
}

function setQrState(state) {
  qrState = state;
  selectMethod("qr");
  const box = document.querySelector(".qr-box");
  const title = document.querySelector(".qr-title");
  const description = document.querySelector(".qr-description");
  const steps = document.querySelector(".qr-steps");
  const actions = document.querySelector(".qr-actions");
  box.dataset.qrState = state;
  steps.hidden = state !== "waiting";
  actions.hidden = !["expired", "failure"].includes(state);
  const copy = {
    generating: ["正在生成二维码", "请稍候，不会复用上一轮登录码"],
    waiting: ["请使用酷狗音乐扫码", "二维码将在 01:46 后过期"],
    scanned: ["已扫码，请在手机上确认", "确认后会自动返回刚才的页面"],
    expired: ["二维码已过期", "为保护账号，本轮登录会话已经结束"],
    failure: ["暂时无法获取二维码", "检查网络后重试，或改用其他登录方式"],
  };
  [title.textContent, description.textContent] = copy[state];
  updateConsole(`qr-${state}`);
}

function updateConsole(key) {
  document.querySelectorAll("[data-jump]").forEach((button) => button.classList.toggle("active", button.dataset.jump === key));
}

function bindFormState(form, inputs) {
  const submit = form.querySelector(".primary-action");
  const update = () => { submit.disabled = inputs.some((input) => !input.value.trim()); };
  inputs.forEach((input) => input.addEventListener("input", update));
  update();
}

methodButtons.forEach((button) => button.addEventListener("click", () => selectMethod(button.dataset.method)));
document.querySelectorAll("[data-back]").forEach((button) => button.addEventListener("click", goBack));

const phoneInput = document.querySelector("#phone");
const smsCodeInput = document.querySelector("#sms-code");
const accountInput = document.querySelector("#account");
const passwordInput = document.querySelector("#password");
bindFormState(document.querySelector("#sms-form"), [phoneInput, smsCodeInput]);
bindFormState(document.querySelector("#password-form"), [accountInput, passwordInput]);

document.querySelector("#send-code").addEventListener("click", (event) => {
  if (phoneInput.value.trim().length < 11) {
    showToast("请输入完整手机号");
    phoneInput.focus();
    return;
  }
  event.currentTarget.textContent = "56s 后重发";
  event.currentTarget.disabled = true;
  showToast("验证码已发送（原型）");
});

document.querySelector("#toggle-password").addEventListener("click", (event) => {
  const visible = passwordInput.type === "text";
  passwordInput.type = visible ? "password" : "text";
  event.currentTarget.setAttribute("aria-label", visible ? "显示密码" : "隐藏密码");
});

document.querySelector("#sms-form").addEventListener("submit", (event) => {
  event.preventDefault();
  const submit = event.currentTarget.querySelector(".primary-action");
  submit.classList.add("loading");
  submit.disabled = true;
  setTimeout(() => {
    submit.classList.remove("loading");
    showScreen("accounts");
  }, 650);
});

document.querySelector("#password-form").addEventListener("submit", (event) => {
  event.preventDefault();
  const submit = event.currentTarget.querySelector(".primary-action");
  submit.classList.add("loading");
  submit.disabled = true;
  document.querySelector("#password-message").textContent = "";
  setTimeout(() => {
    submit.classList.remove("loading");
    submit.disabled = false;
    showScreen("risk-gate");
  }, 650);
});

document.querySelector("[data-open-sms-risk]").addEventListener("click", () => showScreen("risk-sms"));
document.querySelector("[data-open-captcha]").addEventListener("click", () => showScreen("captcha"));

const riskCode = document.querySelector("#risk-code");
const verifyRisk = document.querySelector("#verify-risk");
riskCode.addEventListener("input", () => { verifyRisk.disabled = riskCode.value.trim().length !== 6; });
verifyRisk.addEventListener("click", () => {
  verifyRisk.classList.add("loading");
  verifyRisk.disabled = true;
  setTimeout(() => {
    verifyRisk.classList.remove("loading");
    showScreen("success");
  }, 650);
});

document.querySelector("#captcha-slider").addEventListener("click", (event) => {
  event.currentTarget.classList.add("solved");
  setTimeout(() => {
    document.querySelector("#success-copy").textContent = "验证完成，正在继续原密码登录；本次最多重试一次。";
    showScreen("success");
  }, 700);
});

document.querySelectorAll(".account-row").forEach((row) => row.addEventListener("click", () => {
  document.querySelectorAll(".account-row").forEach((item) => item.setAttribute("aria-checked", String(item === row)));
  document.querySelector("#choose-account").disabled = false;
  document.querySelector("#account-message").textContent = "";
}));

document.querySelector("#choose-account").addEventListener("click", () => {
  const selected = document.querySelector('.account-row[aria-checked="true"]');
  const button = document.querySelector("#choose-account");
  button.classList.add("loading");
  button.disabled = true;
  setTimeout(() => {
    button.classList.remove("loading");
    document.querySelector("#success-copy").textContent = `正在恢复“${selected.dataset.account}”的账号状态并返回来源页面。`;
    showScreen("success");
  }, 650);
});

document.querySelector("[data-qr-retry]").addEventListener("click", () => {
  setQrState("generating");
  setTimeout(() => setQrState("waiting"), 700);
});
document.querySelector("[data-switch-sms]").addEventListener("click", () => selectMethod("sms"));

document.querySelectorAll("[data-reset]").forEach((button) => button.addEventListener("click", () => {
  historyStack.length = 0;
  currentScreen = "success";
  selectMethod("sms");
  showToast("原型已重置");
}));

document.querySelectorAll("[data-jump]").forEach((button) => button.addEventListener("click", () => {
  const target = button.dataset.jump;
  if (["sms", "password"].includes(target)) return selectMethod(target);
  if (target === "password-error") {
    selectMethod("password");
    accountInput.value = "demo.account";
    passwordInput.value = "not-a-real-password";
    const message = document.querySelector("#password-message");
    message.textContent = "账号或密码不正确，请重新输入";
    passwordInput.closest(".field").classList.add("error");
    document.querySelector("#password-form .primary-action").disabled = false;
    return updateConsole(target);
  }
  if (target.startsWith("qr-")) return setQrState(target.slice(3));
  if (target === "accounts") {
    document.querySelectorAll(".account-row").forEach((row) => row.setAttribute("aria-checked", "false"));
    document.querySelector("#choose-account").disabled = true;
  }
  showScreen(target);
}));

selectMethod("sms");
