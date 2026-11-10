package dev.paytrack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PaymentTracker {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("paytrack.json");

    private static final Map<String, Long> totals = new HashMap<>();
    private static final Map<String, Integer> counts = new HashMap<>();

    // Shape of the saved JSON file.
    static final class Data {
        Map<String, Long> totals = new HashMap<>();
        Map<String, Integer> counts = new HashMap<>();
    }

    private PaymentTracker() {}

    public static synchronized void load() {
        if (!Files.exists(FILE)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(FILE)) {
            Data data = GSON.fromJson(reader, Data.class);
            if (data != null) {
                if (data.totals != null) totals.putAll(data.totals);
                if (data.counts != null) counts.putAll(data.counts);
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("[PayTrack] Could not read " + FILE + ": " + e);
        }
    }

    public static synchronized void record(String player, long amount) {
        totals.merge(player, amount, Long::sum);
        counts.merge(player, 1, Integer::sum);
        save();
    }

    public static synchronized void clear() {
        totals.clear();
        counts.clear();
        save();
    }

    public static synchronized int countFor(String player) {
        return counts.getOrDefault(player, 0);
    }

    /** Players sorted by total paid, highest first. */
    public static synchronized List<Map.Entry<String, Long>> leaderboard() {
        List<Map.Entry<String, Long>> list = new ArrayList<>(totals.entrySet());
        list.sort(Map.Entry.<String, Long>comparingByValue().reversed());
        return list;
    }

    private static void save() {
        Data data = new Data();
        data.totals = totals;
        data.counts = counts;
        try (Writer writer = Files.newBufferedWriter(FILE)) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("[PayTrack] Could not save " + FILE + ": " + e);
        }
    }

    /** 5000000 -> "5m", 1500000000 -> "1.5b", 100000000 -> "100m". */
    public static String shortAmount(long value) {
        if (value >= 1_000_000_000L) return trim(value / 1e9) + "b";
        if (value >= 1_000_000L) return trim(value / 1e6) + "m";
        if (value >= 1_000L) return trim(value / 1e3) + "k";
        return Long.toString(value);
    }

    private static String trim(double d) {
        return String.format(Locale.ROOT, "%.2f", d).replaceAll("\\.?0+$", "");
    }
}
