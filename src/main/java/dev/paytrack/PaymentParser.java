package dev.paytrack;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PaymentParser {

    public record Payment(String player, long amount) {}

    // Matches the Donut SMP payment line exactly: "<username> Paid you $<AMOUNT>"
    // e.g. "Steve Paid you $5M", "Steve Paid you $1,000,000", "Steve Paid you $1.5B".
    // Anchored to the whole line so ordinary chat containing "paid you" is ignored.
    private static final Pattern[] PATTERNS = {
            Pattern.compile("^(?<player>[A-Za-z0-9_]{3,16}) [Pp]aid you \\$(?<amount>[0-9][0-9.,]*)(?<suffix>[kKmMbBtT])?\\s*$"),
    };

    private PaymentParser() {}

    public static Optional<Payment> parse(String raw) {
        String text = raw.replaceAll("§.", "").trim();
        for (Pattern pattern : PATTERNS) {
            Matcher m = pattern.matcher(text);
            if (m.find()) {
                try {
                    long amount = toAmount(m.group("amount"), m.group("suffix"));
                    if (amount > 0) {
                        return Optional.of(new Payment(m.group("player"), amount));
                    }
                } catch (NumberFormatException ignored) {
                    // Not a real number, try the next pattern.
                }
            }
        }
        return Optional.empty();
    }

    static long toAmount(String number, String suffix) {
        double base = Double.parseDouble(number.replace(",", ""));
        long multiplier = 1L;
        if (suffix != null) {
            multiplier = switch (suffix.toLowerCase(Locale.ROOT)) {
                case "k" -> 1_000L;
                case "m" -> 1_000_000L;
                case "b" -> 1_000_000_000L;
                case "t" -> 1_000_000_000_000L;
                default -> 1L;
            };
        }
        return Math.round(base * multiplier);
    }
}
