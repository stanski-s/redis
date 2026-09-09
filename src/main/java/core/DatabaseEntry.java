package core;

public record DatabaseEntry(Object value, Long expiresAt) {
    public boolean isExpired() {
        return expiresAt != null && System.currentTimeMillis() > expiresAt;
    }
}
