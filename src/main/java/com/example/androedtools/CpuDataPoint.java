package com.example.androedtools;

public class CpuDataPoint {
    private final long timestamp;
    private final double usage;

    public CpuDataPoint(long timestamp, double usage) {
        this.timestamp = timestamp;
        this.usage = usage;
    }

    public long getTimestamp() { return timestamp; }
    public double getUsage() { return usage; }
}
