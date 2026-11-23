package com.piotrek.groundworksasphalt.client.render;

import com.piotrek.groundworksasphalt.GroundworksAsphaltMod;
import com.piotrek.groundworksasphalt.client.input.GamepadController;
import com.piotrek.groundworksasphalt.entity.AsphaltPaverEntity;
import com.piotrek.groundworksasphalt.entity.AsphaltRollerEntity;
import com.piotrek.groundworksasphalt.entity.CatGraderEntity;
import com.piotrek.groundworksasphalt.entity.RoadRollerEntity;
import java.util.Locale;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;

/**
 * In-cab digital telemetry and instrument HUD displayed while operating Groundworks Asphalt machinery.
 * <p>
 * Displays real-time operational data including:
 * <ul>
 *   <li>Motor Grader: Blade height, angle from vertical axis (yaw), angle from horizontal axis (roll/cross-slope), autolevel status, blade mode</li>
 *   <li>Tandem Asphalt Roller: Compaction & smoothing status, water sprinklers, road slope & pitch, travel speed</li>
 *   <li>Soil Compactor: Subgrade compaction status, water spray, carried loose materials buffer, machine attitude</li>
 *   <li>Asphalt Paver: Paving screed status, hopper opening, roadbed slope, speed</li>
 * </ul>
 */
public class AsphaltHudOverlay implements HudElement {

    public static final Identifier ID = GroundworksAsphaltMod.id("asphalt_hud_overlay");

    private static final int PANEL_BG = 0xEE101418;
    private static final int PANEL_BORDER = 0xFFFFB000; // Industrial Safety Amber
    private static final int TEXT_AMBER = 0xFFFFC247;
    private static final int TEXT_WHITE = 0xFFFFFFFF;
    private static final int TEXT_GREEN = 0xFF72FF72;
    private static final int TEXT_CYAN = 0xFF4FD8EB;
    private static final int TEXT_MUTED = 0xFFA0A5AA;
    private static final int TEXT_RED = 0xFFFF5555;
    private static final int TEXT_YELLOW = 0xFFFFF050;
    private static final int BAR_BG = 0xFF242830;
    private static final int BAR_FILL = 0xFFFFB000;
    private static final int BAR_FULL = 0xFFFF5555;

    public static void register() {
        HudElementRegistry.addLast(ID, new AsphaltHudOverlay());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        Entity vehicle = client.player.getVehicle();
        if (vehicle == null) {
            return;
        }

        Font font = client.font;
        int x = 12;
        int y = 12;

        if (vehicle instanceof CatGraderEntity grader) {
            renderGraderHud(extractor, font, grader, x, y);
        } else if (vehicle instanceof AsphaltRollerEntity roller) {
            renderAsphaltRollerHud(extractor, font, roller, x, y);
        } else if (vehicle instanceof RoadRollerEntity compactor) {
            renderSoilCompactorHud(extractor, font, compactor, x, y);
        } else if (vehicle instanceof AsphaltPaverEntity paver) {
            renderPaverHud(extractor, font, paver, x, y);
        }
    }

    // ── 1. MOTOR GRADER HUD ───────────────────────────────────────────

    private void renderGraderHud(GuiGraphicsExtractor extractor, Font font, CatGraderEntity grader, int x, int y) {
        int width = 360;
        int height = 120;

        extractor.fill(x - 5, y - 5, x + width, y + height, PANEL_BG);
        extractor.outline(x - 5, y - 5, width + 5, height + 5, PANEL_BORDER);

        // Header with title and gamepad status
        Component title = Component.literal("§6[RÓWNIARKA DROGOWA CAT 140M]");
        if (GamepadController.isConnected()) {
            title = Component.empty().append(title).append(Component.literal(" §b[🎮 " + GamepadController.getGamepadName() + "]§r"));
        }
        float speedKmh = Math.abs(grader.getForwardSpeed()) * 72.0F;
        Component speedComp = Component.literal(String.format(Locale.ROOT, "§fPrędkość: §e%.1f km/h", speedKmh));

        extractor.text(font, title, x, y, TEXT_AMBER, true);
        extractor.text(font, speedComp, x + 230, y, TEXT_WHITE, true);

        // 1. Blade Height (Wysokość lemiesza)
        float heightM = grader.getBladeHeight();
        String heightBadge;
        int heightColor;
        if (heightM > 0.05F) {
            heightBadge = "§a[UNIESIONY]";
            heightColor = TEXT_GREEN;
        } else if (heightM >= -0.15F) {
            heightBadge = "§e[SKRAWANIE / POZIOM]";
            heightColor = TEXT_AMBER;
        } else {
            heightBadge = "§c[GŁĘBOKIE SKRAWANIE]";
            heightColor = TEXT_RED;
        }

        Component heightLine = Component.literal(String.format(
                Locale.ROOT,
                "§fWysokość lemiesza: §6%+.2f m  %s",
                heightM, heightBadge
        ));
        extractor.text(font, heightLine, x, y + 14, heightColor, true);

        // 2. Blade Angle / Yaw (Kąt z osi pionowej)
        float angleRad = grader.getBladeAngle();
        float angleDeg = (float) Math.toDegrees(angleRad);
        String yawDirection;
        if (angleDeg > 1.0F) {
            yawDirection = "§e(W PRAWO)";
        } else if (angleDeg < -1.0F) {
            yawDirection = "§e(W LEWO)";
        } else {
            yawDirection = "§a(PROSTOPADLE 0°)";
        }

        Component yawLine = Component.literal(String.format(
                Locale.ROOT,
                "§fKąt z osi pionowej (Yaw): §b%+.1f°  %s",
                angleDeg, yawDirection
        ));
        extractor.text(font, yawLine, x, y + 27, TEXT_WHITE, true);

        // 3. Blade Roll / Cross-slope (Kąt z osi poziomej - poziomowanie)
        float rollRad = grader.getBladeRoll();
        float rollDeg = (float) Math.toDegrees(rollRad);
        float slopePct = (float) (Math.tan(rollRad) * 100.0);
        String rollStatus;
        if (Math.abs(rollDeg) < 0.2F) {
            rollStatus = "§a[POZIOM 0.0%]";
        } else if (rollDeg > 0.0F) {
            rollStatus = "§e[POCHYLENIE PRAWE]";
        } else {
            rollStatus = "§e[POCHYLENIE LEWE]";
        }

        Component rollLine = Component.literal(String.format(
                Locale.ROOT,
                "§fKąt z osi poziomej (Roll): §e%+.1f°  §fSpadek: §6%+.1f%%  %s",
                rollDeg, slopePct, rollStatus
        ));
        extractor.text(font, rollLine, x, y + 40, TEXT_WHITE, true);

        // 4. Autolevel status & active blade arrow control mode
        boolean autoLevel = grader.isAutoLevel();
        Component autoLevelLine = Component.literal(String.format(
                "§fAutopoziomowanie: %s",
                autoLevel ? "§aWŁĄCZONE (samopoziomowanie)§r" : "§7WYŁĄCZONE (ręczne)§r"
        ));
        extractor.text(font, autoLevelLine, x, y + 54, autoLevel ? TEXT_GREEN : TEXT_MUTED, true);

        boolean isRollMode = grader.isRollMode();
        Component modeLine = Component.literal(String.format(
                "§fSterowanie [←/→]: %s",
                isRollMode ? "§ePochylenie / Poziomowanie (Roll) [Z]" : "§aKąt obrotu lemiesza (Yaw) [Z]"
        ));
        extractor.text(font, modeLine, x + 160, y + 54, isRollMode ? TEXT_YELLOW : TEXT_GREEN, true);

        // 5. Work status (Grading active & Ripper)
        boolean isGrading = grader.isGrading();
        boolean ripperDown = grader.getRipperLowered() > 0.5F;
        boolean lightsOn = grader.isLightsOn();
        Component statusLine = Component.literal(String.format(
                "§fLemiesz: %s§r   §fZrywak: %s§r   §fŚwiatła: %s",
                isGrading ? "§aSKRAWANIE [Act1]" : "§7WSTRZYMANY [Act1]",
                ripperDown ? "§cOPUSZCZONY [Act2]" : "§7UNIESIONY [Act2]",
                lightsOn ? "§aWŁ [L]" : "§7WYŁ [L]"
        ));
        extractor.text(font, statusLine, x, y + 68, TEXT_WHITE, true);

        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget graderSurvey =
                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTarget(grader.level(), grader.getX(), grader.getZ());
        if (graderSurvey.inCorridor()) {
            Component surveyLine = Component.literal(String.format(
                    Locale.ROOT,
                    "§6[OPCJA B: NIWELETA TYCZEK] §fY: §b%.2fm §7(odchyłka: %+.2fm) §fOś: §e%.1fm",
                    graderSurvey.targetY(), grader.getY() - graderSurvey.targetY(), graderSurvey.distToCenterline()
            ));
            extractor.text(font, surveyLine, x, y + 82, TEXT_AMBER, true);
        } else {
            // Machine pitch / roll attitude
            Component attitudeLine = Component.literal(String.format(
                    Locale.ROOT,
                    "§7Podwozie: Pochylenie podłużne: %+.1f° | Przechył: %+.1f°",
                    grader.getVehiclePitch(), grader.getVehicleRoll()
            ));
            extractor.text(font, attitudeLine, x, y + 82, TEXT_MUTED, true);
        }

        // Control key hints
        Component hintsLine = Component.literal("§8Sterowanie: ↑/↓: Wysokość | ←/→: Kąt/Roll | Z: Tryb | L: Światła | C: Trąbka | Act1: Skrawanie");
        extractor.text(font, hintsLine, x, y + 96, TEXT_MUTED, true);
    }

    // ── 2. TANDEM ASPHALT ROLLER HUD ──────────────────────────────────

    private void renderAsphaltRollerHud(GuiGraphicsExtractor extractor, Font font, AsphaltRollerEntity roller, int x, int y) {
        int width = 340;
        int height = 96;

        extractor.fill(x - 5, y - 5, x + width, y + height, PANEL_BG);
        extractor.outline(x - 5, y - 5, width + 5, height + 5, PANEL_BORDER);

        Component title = Component.literal("§6[WALEC DO ASFALTU 14T - TANDEM]");
        if (GamepadController.isConnected()) {
            title = Component.empty().append(title).append(Component.literal(" §b[🎮 " + GamepadController.getGamepadName() + "]§r"));
        }
        float speedKmh = Math.abs(roller.getForwardSpeed()) * 72.0F;
        Component speedComp = Component.literal(String.format(Locale.ROOT, "§fPrędkość: §e%.1f km/h", speedKmh));

        extractor.text(font, title, x, y, TEXT_AMBER, true);
        extractor.text(font, speedComp, x + 215, y, TEXT_WHITE, true);

        // Compaction status
        boolean compacting = roller.isCompacting();
        Component compLine = Component.literal(String.format(
                "§fTryb roboczy: %s",
                compacting ? "§aZAGĘSZCZANIE I SMOOTHOWANIE ASFALTU" : "§7JAZDA TRANSPORTOWA"
        ));
        extractor.text(font, compLine, x, y + 15, compacting ? TEXT_GREEN : TEXT_MUTED, true);

        // Water spraying sprinklers & Lights
        boolean spraying = roller.isWaterSpraying();
        boolean rollerLights = roller.isLightsOn();
        Component sprayLine = Component.literal(String.format(
                "§fZraszanie: %s§r   §fŚwiatła: %s",
                spraying ? "§bWŁ [Act2]" : "§7WYŁ [Act2]",
                rollerLights ? "§aWŁ [L]" : "§7WYŁ [L]"
        ));
        extractor.text(font, sprayLine, x, y + 29, spraying ? TEXT_CYAN : TEXT_WHITE, true);

        // Road surface attitude (Pitch & Roll conforming)
        Component attLine = Component.literal(String.format(
                Locale.ROOT,
                "§fPochylenie podłużne: §6%+.1f°§r   §fPrzechył (Camber): §6%+.1f°§r",
                roller.getVehiclePitch(), roller.getVehicleRoll()
        ));
        extractor.text(font, attLine, x, y + 43, TEXT_WHITE, true);

        // Steering articulation
        float steerDeg = (float) Math.toDegrees(roller.getSteerAngle());
        Component steerLine = Component.literal(String.format(
                Locale.ROOT,
                "§7Skręt przegubu ramy: %+.1f°  (Dwa bębny stalowe 2.2m)",
                steerDeg
        ));
        extractor.text(font, steerLine, x, y + 57, TEXT_MUTED, true);

        // Control key hints
        Component hintsLine = Component.literal("§8Sterowanie: Act1: Gładzenie | Act2: Zraszanie | L: Światła | C: Trąbka");
        extractor.text(font, hintsLine, x, y + 74, TEXT_MUTED, true);
    }

    // ── 3. SOIL COMPACTOR HUD ─────────────────────────────────────────

    private void renderSoilCompactorHud(GuiGraphicsExtractor extractor, Font font, RoadRollerEntity compactor, int x, int y) {
        int width = 340;
        int height = 96;

        extractor.fill(x - 5, y - 5, x + width, y + height, PANEL_BG);
        extractor.outline(x - 5, y - 5, width + 5, height + 5, PANEL_BORDER);

        Component title = Component.literal("§6[ZAGĘSZCZARKA DO GRUNTU 16T]");
        if (GamepadController.isConnected()) {
            title = Component.empty().append(title).append(Component.literal(" §b[🎮 " + GamepadController.getGamepadName() + "]§r"));
        }
        float speedKmh = Math.abs(compactor.getForwardSpeed()) * 72.0F;
        Component speedComp = Component.literal(String.format(Locale.ROOT, "§fPrędkość: §e%.1f km/h", speedKmh));

        extractor.text(font, title, x, y, TEXT_AMBER, true);
        extractor.text(font, speedComp, x + 215, y, TEXT_WHITE, true);

        boolean compacting = compactor.isCompacting();
        Component compLine = Component.literal(String.format(
                "§fTryb roboczy: %s",
                compacting ? "§aZAGĘSZCZANIE PODBUDOWY (MATERIAŁY SYPKIE)" : "§7JAZDA TRANSPORTOWA"
        ));
        extractor.text(font, compLine, x, y + 15, compacting ? TEXT_GREEN : TEXT_MUTED, true);

        boolean spraying = compactor.isWaterSpraying();
        boolean compactorLights = compactor.isLightsOn();
        Component sprayLine = Component.literal(String.format(
                "§fZraszanie: %s§r   §fŚwiatła: %s",
                spraying ? "§bWŁ [Act2]" : "§7WYŁ [Act2]",
                compactorLights ? "§aWŁ [L]" : "§7WYŁ [L]"
        ));
        extractor.text(font, sprayLine, x, y + 29, spraying ? TEXT_CYAN : TEXT_WHITE, true);

        Component attLine = Component.literal(String.format(
                Locale.ROOT,
                "§fPochylenie podłużne: §6%+.1f°§r   §fPrzechył: §6%+.1f°§r",
                compactor.getVehiclePitch(), compactor.getVehicleRoll()
        ));
        extractor.text(font, attLine, x, y + 43, TEXT_WHITE, true);

        com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.SurveyTarget compactorSurvey =
                com.piotrek.groundworksasphalt.terrain.RoadSurveyCorridor.sampleTarget(compactor.level(), compactor.getX(), compactor.getZ());
        if (compactorSurvey.inCorridor()) {
            Component subgradeNote = Component.literal(String.format(
                    Locale.ROOT,
                    "§6[OPCJA B: NIWELETA TYCZEK] §fY: §b%.2fm §7(odchyłka: %+.2fm) §fOś: §e%.1fm",
                    compactorSurvey.targetY(), compactor.getY() - compactorSurvey.targetY(), compactorSurvey.distToCenterline()
            ));
            extractor.text(font, subgradeNote, x, y + 57, TEXT_AMBER, true);
        } else {
            Component subgradeNote = Component.literal("§7Sonda: Pomiar materiałów sypkich 2-3 bloki przed walcem (Gładzenie podbudowy)");
            extractor.text(font, subgradeNote, x, y + 57, TEXT_MUTED, true);
        }

        Component hintsLine = Component.literal("§8Sterowanie: Act1: Zagęszczanie | Act2: Zraszanie | L: Światła | C: Trąbka");
        extractor.text(font, hintsLine, x, y + 74, TEXT_MUTED, true);
    }

    // ── 4. ASPHALT PAVER HUD ──────────────────────────────────────────

    private void renderPaverHud(GuiGraphicsExtractor extractor, Font font, AsphaltPaverEntity paver, int x, int y) {
        int width = 340;
        int height = 100;

        extractor.fill(x - 5, y - 5, x + width, y + height, PANEL_BG);
        extractor.outline(x - 5, y - 5, width + 5, height + 5, PANEL_BORDER);

        Component title = Component.literal("§6[ROZŚCIEŁACZ ASFALTU]");
        if (GamepadController.isConnected()) {
            title = Component.empty().append(title).append(Component.literal(" §b[🎮 " + GamepadController.getGamepadName() + "]§r"));
        }
        float speedKmh = ((Math.abs(paver.getTrackLeftSpeed()) + Math.abs(paver.getTrackRightSpeed())) * 0.5F) * 72.0F;
        Component speedComp = Component.literal(String.format(Locale.ROOT, "§fPrędkość: §e%.1f km/h", speedKmh));

        extractor.text(font, title, x, y, TEXT_AMBER, true);
        extractor.text(font, speedComp, x + 215, y, TEXT_WHITE, true);

        boolean paving = paver.isPaving();
        Component paveLine = Component.literal(String.format(
                "§fUkładanie asfaltu: %s",
                paving ? "§aROZKŁADANIE MASY WŁĄCZONE [Act1]" : "§7WSTRZYMANE [Act1]"
        ));
        extractor.text(font, paveLine, x, y + 15, paving ? TEXT_GREEN : TEXT_MUTED, true);

        float fillRatio = paver.getAsphaltFillRatio();
        Component fillLine = Component.literal(String.format(
                Locale.ROOT,
                "§fZasobnik masy: §e%.0f%%§r   §fKosz: §b%.0f%% otwarty [Act2]",
                fillRatio * 100.0F, (1.0F - paver.getHopperFold()) * 100.0F
        ));
        extractor.text(font, fillLine, x, y + 29, TEXT_WHITE, true);

        // Screed settings & Lights
        boolean paverLights = paver.isWorkLightsOn();
        Component screedLine = Component.literal(String.format(
                Locale.ROOT,
                "§fStół: Wysokość §6%+.2f m§r [↑/↓]   §fŚwiatła: %s",
                paver.getScreedHeight(),
                paverLights ? "§aWŁ [L]" : "§7WYŁ [L]"
        ));
        extractor.text(font, screedLine, x, y + 43, TEXT_WHITE, true);

        Component attLine = Component.literal(String.format(
                Locale.ROOT,
                "§7Pochylenie drogi: %+.1f° | Przechył: %+.1f°",
                paver.getVehiclePitch(), paver.getVehicleRoll()
        ));
        extractor.text(font, attLine, x, y + 57, TEXT_MUTED, true);

        Component hintsLine = Component.literal("§8Sterowanie: Act1: Układanie | Act2: Kosz | ↑/↓: Stół | L: Światła | C: Trąbka");
        extractor.text(font, hintsLine, x, y + 74, TEXT_MUTED, true);
    }
}
