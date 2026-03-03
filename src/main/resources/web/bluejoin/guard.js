(function () {
    const path = window.location.pathname;
    const allowed = path.startsWith("/bluejoin/");
    if (!allowed) {
        const token = localStorage.getItem("bluejoin_token");
        if (!token) {
            window.location.replace("/bluejoin/register.html");
            return;
        }

        fetch("/bluejoin/api/me", {
            method: "GET",
            headers: { Authorization: `Bearer ${token}` }
        }).then(async (res) => {
            const data = await res.json().catch(() => ({ ok: false }));
            if (!res.ok || !data.ok) {
                localStorage.removeItem("bluejoin_token");
                window.location.replace("/bluejoin/login.html");
            }
        }).catch(() => {
            localStorage.removeItem("bluejoin_token");
            window.location.replace("/bluejoin/login.html");
        });
    }

    function removeSpectatorUi() {
        document.querySelectorAll("option, button, li, div, span").forEach((el) => {
            const text = (el.textContent || "").trim().toLowerCase();
            if (text === "spectator" || text.includes("spectator mode")) {
                const target = el.closest("li,button,div") || el;
                target.remove();
            }
        });
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", removeSpectatorUi);
    } else {
        removeSpectatorUi();
    }

    const observer = new MutationObserver(removeSpectatorUi);
    observer.observe(document.documentElement, { childList: true, subtree: true });
})();
