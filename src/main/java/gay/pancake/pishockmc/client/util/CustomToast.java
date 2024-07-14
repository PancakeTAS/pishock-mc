package gay.pancake.pishockmc.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Toast implementation for displaying custom toasts
 */
@Environment(EnvType.CLIENT)
public class CustomToast implements Toast {

    /** Background sprite resource location */
    private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("toast/system");

    /** Toast text */
    private final Component text;

    /**
     * Create a new custom toast
     *
     * @param text Toast text
     */
    public CustomToast(Component text) {
        this.text = text;
    }

    @Override
    public @NotNull Visibility render(GuiGraphics gui, ToastComponent toast, long l) {
        gui.blitSprite(BACKGROUND_SPRITE, 0, 0, this.width(), this.height());
        gui.drawString(toast.getMinecraft().font, Component.translatable("text.toast.title"), 18, 7, 0xFFFF00, false);
        gui.drawString(toast.getMinecraft().font, this.text, 18, 18, 0xFFFFFF, false);

        return Visibility.SHOW;
    }

}
