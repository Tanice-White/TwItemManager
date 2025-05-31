package io.github.tanice.twItemManager.listener;

import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle;
import io.github.tanice.twItemManager.config.Config;

import static io.github.tanice.twItemManager.util.Logger.logInfo;

public class GenericParticleListener implements PacketListener {

    public GenericParticleListener() {
        logInfo("GenericParticleListener loaded");
    }

    public void onPacketSend(PacketSendEvent event){
        if(!Config.cancelGenericParticles) return;
        if(event.getPacketType()==PacketType.Play.Server.PARTICLE){
            WrapperPlayServerParticle packet = new WrapperPlayServerParticle(event);
            if (packet.getParticle().getType() == ParticleTypes.DAMAGE_INDICATOR) event.setCancelled(true);
        }else if (event.getPacketType() == PacketType.Play.Server.ENTITY_ANIMATION) {
            WrapperPlayServerEntityAnimation packet = new WrapperPlayServerEntityAnimation(event);
            if (packet.getType() == WrapperPlayServerEntityAnimation.EntityAnimationType.CRITICAL_HIT) event.setCancelled(true);
        }
    }
}
