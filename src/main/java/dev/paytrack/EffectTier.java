package dev.paytrack;

public enum EffectTier {
    TOAST("Toast", 1_000_000L, 0xFF55FFFF),
    SMALL_FIREWORKS("Small Fireworks", 5_000_000L, 0xFFFFD700),
    BIG_FIREWORKS("Big Fireworks", 10_000_000L, 0xFFFF55FF),
    JACKPOT("Jackpot", 100_000_000L, 0xFFFFAA00),
    BILLION("Billion", 1_000_000_000L, 0xFFFFFFFF);

    public final String label;
    public final long minAmount;
    public final int color;

    EffectTier(String label, long minAmount, int color) {
        this.label = label;
        this.minAmount = minAmount;
        this.color = color;
    }

    /** Highest tier the amount reaches, or null if below 1m. */
    public static EffectTier forAmount(long amount) {
        EffectTier best = null;
        for (EffectTier tier : values()) {
            if (amount >= tier.minAmount) {
                best = tier;
            }
        }
        return best;
    }
}
