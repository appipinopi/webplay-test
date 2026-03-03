const message = document.getElementById("message");
const prefixInfo = document.getElementById("prefixInfo");

async function api(path, method, body, token) {
    const res = await fetch(path, {
        method,
        headers: {
            "Content-Type": "application/json",
            ...(token ? { Authorization: `Bearer ${token}` } : {})
        },
        body: body ? JSON.stringify(body) : undefined
    });
    return res.json();
}

function setMessage(text) {
    message.textContent = text || "";
}

async function loadConfig() {
    const data = await api("/bluejoin/api/config", "GET");
    if (data.ok) {
        prefixInfo.textContent = `Web player prefix: ${data.webPlayerPrefix}`;
        const registerForm = document.getElementById("registerForm");
        if (!data.allowRegistration) {
            registerForm.style.display = "none";
        }
    }
}

document.getElementById("loginForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    setMessage("");
    const username = document.getElementById("loginUsername").value.trim();
    const password = document.getElementById("loginPassword").value;
    const data = await api("/bluejoin/api/login", "POST", { username, password });
    if (!data.ok) {
        setMessage("Login failed: " + (data.error || "unknown"));
        return;
    }
    localStorage.setItem("bluejoin_token", data.token);
    window.location.href = "/bluejoin/mypage.html";
});

document.getElementById("registerForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    setMessage("");
    const username = document.getElementById("registerUsername").value.trim();
    const password = document.getElementById("registerPassword").value;
    const data = await api("/bluejoin/api/register", "POST", { username, password });
    if (!data.ok) {
        setMessage("Register failed: " + (data.error || "unknown"));
        return;
    }
    setMessage("Register completed. You can now login.");
});

loadConfig().catch(() => setMessage("Failed to load config"));
