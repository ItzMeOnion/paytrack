# PayTrack

Fabric client mod for Minecraft Java 1.21.8. It watches chat for payments to you, adds them up per player, and plays celebration effects on your own screen. Everything is client-side, so other players only see the effects if they are looking at your screen (for example while streaming).

## Effect tiers

| Payment | Effect |
|---|---|
| 1m+ | Top-right toast: `"name" paid 1m` |
| 5m+ | Toast plus three firework bursts and a sound |
| 10m+ | Toast, gold/magenta firework shower, screen flash, WOW banner |
| 100m+ | Toast, JACKPOT banner, gold flash and a longer shower |
| 1b+ | Toast, rainbow border, BILLION banner, white flash and a long shower |

Payments under 1m are still counted in the leaderboard, but show no effect.

## Controls

- **P** opens the PayTrack menu (rebindable in Controls, category "PayTrack").
- **Previews** tab: press any tier to play it using your own name.
- **Leaderboard** tab: top 10 players by total paid, with payment counts. **Clear totals** resets everything.

Totals are saved to `config/paytrack.json` in your Minecraft folder.

## Setup and build

You need JDK 21 and Gradle (or an IDE such as IntelliJ with Gradle support).

1. Open the `paytrack` folder as a Gradle project.
2. Run `gradle wrapper` once if the wrapper files are missing.
3. Run `./gradlew build` (or `gradlew.bat build` on Windows).
4. The jar is in `build/libs/`. Put it, along with Fabric API for 1.21.8, into your `mods` folder.

Check `gradle.properties` against https://fabricmc.net/develop before building. The Minecraft, Yarn, Loader and Fabric API versions there are the ones to update if the build fails.

## Important: chat format

The mod expects the payment line to read exactly `<username> Paid you $<AMOUNT>`, for example `Steve Paid you $5M`. Lines in any other wording are ignored. If Donut SMP's wording changes, edit `PaymentParser.PATTERNS`.

Amounts with `k`, `m`, `b`, `t` suffixes are supported, with or without commas or decimals (`$1,000,000`, `$1.5B`).

## Project layout

```
src/main/java/dev/paytrack/
  PayTrackMod.java      entry point, chat listener, keybind
  PaymentParser.java    turns a chat line into (player, amount)
  PaymentTracker.java   totals, leaderboard, saving to JSON
  EffectTier.java       tier thresholds and colours
  EffectRenderer.java   HUD toasts, fireworks, flashes, banners
  PayTrackScreen.java   preview and leaderboard menu
src/main/resources/
  fabric.mod.json
  assets/paytrack/lang/en_us.json
```
