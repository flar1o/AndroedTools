package com.example.androedtools;

public class RamDataPoint {
    private final long timestamp;
    private final double used;
    private final double total;

    public RamDataPoint(long timestamp, double used, double total) {
        this.timestamp = timestamp;
        this.used = used;
        this.total = total;
    }

    // Getters
    public long getTimestamp() { return timestamp; }
    public double getUsed() { return used; }
    public double getTotal() { return total; }
}
