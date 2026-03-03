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
    }
}

document.getElementById("loginForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    setMessage("");
    const email = document.getElementById("loginEmail").value.trim();
    const password = document.getElementById("loginPassword").value;
    const data = await api("/bluejoin/api/login", "POST", { email, password });
    if (!data.ok) {
        setMessage("Login failed: " + (data.error || "unknown"));
        return;
    }
    localStorage.setItem("bluejoin_token", data.token);
    window.location.href = "/bluejoin/mypage.html";
});

loadConfig().catch(() => setMessage("Failed to load config"));
