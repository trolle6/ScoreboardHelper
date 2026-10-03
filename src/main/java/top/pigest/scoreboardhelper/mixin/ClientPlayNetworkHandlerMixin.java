package top.pigest.scoreboardhelper.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.pigest.scoreboardhelper.util.Constants;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "handleAddObjective", at = @At(value = "HEAD"))
    private void injectedUpdate(ClientboundSetObjectivePacket packet, CallbackInfo ci) {
        Constants.PAGE = 0;
    }

    @Inject(method = "handleSetDisplayObjective", at = @At(value = "HEAD"))
    private void injectedDisplay(ClientboundSetDisplayObjectivePacket packet, CallbackInfo ci) {
        Constants.PAGE = 0;
    }
}
