package com.commonsware.cwac.cam2;

public enum ImageQuality {
    LOW(0), HIGH(1);

    private final int value;

    private ImageQuality(int value) {
        this.value = value;
    }

    int getValue() {
        return (value);
    }
}
