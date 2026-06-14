package org.yuemi.bingogacha.api.model;

public enum CardSize {
    THREE_BY_THREE(3, 9),
    FIVE_BY_FIVE(5, 25);

    private final int side;
    private final int totalSlots;

    CardSize(int side, int totalSlots) {
        this.side = side;
        this.totalSlots = totalSlots;
    }

    public int getSide() {
        return side;
    }

    public int getTotalSlots() {
        return totalSlots;
    }
}
