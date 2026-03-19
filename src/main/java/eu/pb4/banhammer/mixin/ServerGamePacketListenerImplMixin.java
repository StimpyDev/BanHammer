package eu.pb4.banhammer.mixin;

import eu.pb4.banhammer.impl.BHUtils;
import eu.pb4.placeholders.api.PlaceholderContext;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import eu.pb4.banhammer.impl.config.ConfigManager;

import java.util.Optional;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin extends ServerCommonPacketListenerImpl {
    @Shadow public ServerPlayer player;

    public ServerGamePacketListenerImplMixin(MinecraftServer server, Connection connection, CommonListenerCookie clientData) {
        super(server, connection, clientData);
    }

    @Shadow protected abstract PlayerChatMessage getSignedMessage(ServerboundChatPacket packet, LastSeenMessages lastSeenMessages) throws SignedMessageChain.DecodeException;

    @Shadow protected abstract void handleMessageDecodeFailure(SignedMessageChain.DecodeException exception);

    @Shadow protected abstract Optional<LastSeenMessages> unpackAndApplyLastSeen(LastSeenMessages.Update acknowledgment);

    @Inject(method = "handleChat", at = @At("HEAD"), cancellable = true)
    private void banHammer_checkIfMuted(ServerboundChatPacket packet, CallbackInfo ci) {
        var mute = BHUtils.getActiveMute(this.player);

        if (mute != null) {
            this.player.displayClientMessage(mute.getDisconnectMessage(PlaceholderContext.of(this.player)), false);
            ci.cancel();

            Optional<LastSeenMessages> optional = this.unpackAndApplyLastSeen(packet.lastSeenMessages());
            optional.ifPresent(lastSeenMessageList -> this.server.submit(() -> {
                try {
                    this.getSignedMessage(packet, (LastSeenMessages) lastSeenMessageList);
                } catch (SignedMessageChain.DecodeException var6) {
                    this.handleMessageDecodeFailure(var6);
                }
            }));
        }
    }

    @Inject(method = "handleChatCommand", at = @At("HEAD"), cancellable = true)
    private void banHammer_checkIfMutedCommand(ServerboundChatCommandPacket packet, CallbackInfo ci) {
        if (checkIfMutedCommand(packet.command())) {
            ci.cancel();
        }
    }

    @Inject(method = "performSignedChatCommand", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/CommandSigningContext$SignedArguments;<init>(Ljava/util/Map;)V"), cancellable = true)
    private void banHammer_checkIfMutedCommand(ServerboundChatCommandSignedPacket packet, LastSeenMessages lastSeenMessages, CallbackInfo ci) {
        if (checkIfMutedCommand(packet.command())) {
            ci.cancel();
        }
    }

    @Unique
    private boolean checkIfMutedCommand(String string) {
        int x = string.indexOf(" ");
        String rawCommand = string.substring(0, x != -1 ? x : string.length());
        
        for (String command : ConfigManager.getConfig().mutedCommands) {
            if (rawCommand.equals(command)) {
                var mute = BHUtils.getActiveMute(this.player);
                if (mute != null) {
                    this.player.displayClientMessage(mute.getDisconnectMessage(PlaceholderContext.of(this.player)), false);
                    return true;
                }
                return false;
            }
        }
        return false;
    }
}
