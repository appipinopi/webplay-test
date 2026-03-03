package com.bluejoin.addon;

public final class BlueJoinConfig {

    private String webPlayerPrefix = "web.";
    private boolean allowRegistration = true;
    private int sessionHours = 24;

    public String getWebPlayerPrefix() {
        return webPlayerPrefix == null || webPlayerPrefix.isBlank() ? "web." : webPlayerPrefix;
    }

    public void setWebPlayerPrefix(String webPlayerPrefix) {
        this.webPlayerPrefix = webPlayerPrefix;
    }

    public boolean isAllowRegistration() {
        return allowRegistration;
    }

    public void setAllowRegistration(boolean allowRegistration) {
        this.allowRegistration = allowRegistration;
    }

    public int getSessionHours() {
        return sessionHours <= 0 ? 24 : sessionHours;
    }

    public void setSessionHours(int sessionHours) {
        this.sessionHours = sessionHours;
    }
}
