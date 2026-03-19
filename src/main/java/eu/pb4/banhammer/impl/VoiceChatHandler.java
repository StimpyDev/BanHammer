package eu.pb4.banhammer.impl;

import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import eu.pb4.placeholders.api.PlaceholderContext;
import net.minecraft.server.level.ServerPlayer;

public class VoiceChatHandler implements VoicechatPlugin {

    @Override
    public String getPluginId() {
        return "banhammer";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
    }

    private void onMicrophonePacket(MicrophonePacketEvent event) {
        if (event.getSenderConnection() == null) return;
        
        if (event.getSenderConnection().getPlayer().getEntity() instanceof ServerPlayer player) {
            var mute = BHUtils.getActiveMute(player);
            
            if (mute != null) {
                event.cancel();
                
                player.displayClientMessage(
                    mute.getDisconnectMessage(PlaceholderContext.of(player)), 
                    true
                );
            }
        }
    }
}
