const token = localStorage.getItem("bluejoin_token");
const message = document.getElementById("message");

if (!token) {
    window.location.href = "/bluejoin/login.html";
}

async function api(path, method, body) {
    const res = await fetch(path, {
        method,
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`
        },
        body: body ? JSON.stringify(body) : undefined
    });
    return res.json();
}

function setMessage(text) {
    message.textContent = text || "";
}

function fill(user) {
    document.getElementById("username").textContent = user.username || "-";
    document.getElementById("email").textContent = user.email || "-";
    document.getElementById("webPlayerName").textContent = user.webPlayerName || "-";
    document.getElementById("skinUrl").value = user.skinUrl || "";
    document.getElementById("displayName").value = user.displayName || "";
}

async function loadMe() {
    const data = await api("/bluejoin/api/me", "GET");
    if (!data.ok) {
        localStorage.removeItem("bluejoin_token");
        window.location.href = "/bluejoin/login.html";
        return;
    }
    fill(data);
}

document.getElementById("profileForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    setMessage("");
    const skinUrl = document.getElementById("skinUrl").value.trim();
    const displayName = document.getElementById("displayName").value.trim();
    const data = await api("/bluejoin/api/profile", "POST", { skinUrl, displayName });
    if (!data.ok) {
        setMessage("Save failed: " + (data.error || "unknown"));
        return;
    }
    fill(data);
    setMessage("Saved.");
});

document.getElementById("logoutBtn").addEventListener("click", async () => {
    await api("/bluejoin/api/logout", "POST");
    localStorage.removeItem("bluejoin_token");
    window.location.href = "/bluejoin/login.html";
});

loadMe().catch(() => {
    localStorage.removeItem("bluejoin_token");
    window.location.href = "/bluejoin/login.html";
});
