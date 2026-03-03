const message = document.getElementById("message");
const prefixInfo = document.getElementById("prefixInfo");

async function api(path, method, body) {
    const res = await fetch(path, {
        method,
        headers: {
            "Content-Type": "application/json"
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
    if (!data.ok) return;
    prefixInfo.textContent = `Web player prefix: ${data.webPlayerPrefix}`;
    if (!data.allowRegistration) {
        document.getElementById("registerForm").style.display = "none";
        setMessage("Registration is disabled.");
    }
}

document.getElementById("registerForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    setMessage("");
    const username = document.getElementById("registerUsername").value.trim();
    const email = document.getElementById("registerEmail").value.trim();
    const password = document.getElementById("registerPassword").value;
    const data = await api("/bluejoin/api/register", "POST", { username, email, password });
    if (!data.ok) {
        setMessage("Register failed: " + (data.error || "unknown"));
        return;
    }
    setMessage("Register completed. Move to login page.");
});

loadConfig().catch(() => setMessage("Failed to load config"));
