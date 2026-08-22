package me.johnadept;

import me.johnadept.config.AutoAttackConfig;
import me.johnadept.config.MessageDisplayMode;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public class ModEventHandlers {
    // Bewegings- en rotatie-detectie variabelen
    private static Vec3 lastPlayerPosition = null;
    private static float lastPlayerYaw = 0.0f;
    private static float lastPlayerPitch = 0.0f;

    public static void onClientTick() {
        AutoAttackConfig config = AutoAttackConfig.get();
        if (!config.enableMod) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (ModKeyBindings.toggleAttack.consumeClick()) {
            AutoAttackClient.autoAttackEnabled = !AutoAttackClient.autoAttackEnabled;
            mc.player.displayClientMessage(Component.translatable("gui.auto_attack.autoAttackPrefix", AutoAttackClient.autoAttackEnabled ? Component.translatable("gui.auto_attack.enabled").withStyle(ChatFormatting.GREEN) : Component.translatable("gui.auto_attack.disabled").withStyle(ChatFormatting.RED)), config.displayMode == MessageDisplayMode.ACTION_BAR);
        }
        if (ModKeyBindings.toggleRotation.consumeClick()) {
            Rotater.handleKeyPress(mc);
        }
        Rotater.tick(mc);

        // Detecteer beweging en rotatie - zet autoattack UIT
        if (AutoAttackClient.autoAttackEnabled && hasPlayerMovedOrRotated(mc)) {
            AutoAttackClient.autoAttackEnabled = false;
            mc.player.displayClientMessage(Component.translatable("gui.auto_attack.autoAttackPrefix", Component.literal("Disabled due to movement").withStyle(ChatFormatting.YELLOW)), config.displayMode == MessageDisplayMode.ACTION_BAR);
        }

        // Update vorige positie en rotatie
        updatePlayerPositionAndRotation(mc);

        if (mc.player.isSpectator() && AutoAttackClient.autoAttackEnabled) {
            mc.player.displayClientMessage(Component.translatable("gui.auto_attack.autoAttackPrefix", Component.translatable("gui.auto_attack.disabledDueSpectator").withStyle(ChatFormatting.RED)), config.displayMode == MessageDisplayMode.ACTION_BAR);
            AutoAttackClient.autoAttackEnabled = false;
        }

        if (AutoAttackClient.autoAttackEnabled && mc.player.getAttackStrengthScale(0) == 1.0f) {
            Attacker.tryAttack(mc);
        }
    }

    /**
     * Controleer of de speler is verplaatst of gedraaid
     */
    private static boolean hasPlayerMovedOrRotated(Minecraft mc) {
        if (mc.player == null) return false;

        Vec3 currentPos = mc.player.position();
        float currentYaw = mc.player.getYRot();
        float currentPitch = mc.player.getXRot();

        // Eerste keer: initialiseer de vorige positie/rotatie
        if (lastPlayerPosition == null) {
            lastPlayerPosition = currentPos;
            lastPlayerYaw = currentYaw;
            lastPlayerPitch = currentPitch;
            return false;
        }

        // Controleer beweging (met kleine tolerantie om jittering te vermijden)
        double distanceSquared = currentPos.distanceToSqr(lastPlayerPosition);
        if (distanceSquared > 0.01) { // 0.1 blokken beweging
            return true;
        }

        // Controleer rotatie (yaw en pitch met tolerantie)
        float yawDiff = Math.abs(currentYaw - lastPlayerYaw);
        float pitchDiff = Math.abs(currentPitch - lastPlayerPitch);

        // Zorg voor correcte wraparound bij yaw (-180 tot 180)
        if (yawDiff > 180) {
            yawDiff = 360 - yawDiff;
        }

        // 1 graad tolerantie
        if (yawDiff > 1.0f || pitchDiff > 1.0f) {
            return true;
        }

        return false;
    }

    /**
     * Update de vorige positie en rotatie van de speler
     */
    private static void updatePlayerPositionAndRotation(Minecraft mc) {
        if (mc.player == null) return;
        lastPlayerPosition = mc.player.position();
        lastPlayerYaw = mc.player.getYRot();
        lastPlayerPitch = mc.player.getXRot();
    }
}
