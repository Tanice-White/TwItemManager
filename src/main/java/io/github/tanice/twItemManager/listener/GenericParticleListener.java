package io.github.tanice.twItemManager.listener;

import com.github.retrooper.packetevents.event.*;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle;
import io.github.tanice.twItemManager.config.Config;
import org.bukkit.plugin.java.JavaPlugin;

import static io.github.tanice.twItemManager.util.Logger.logInfo;

public class GenericParticleListener implements PacketListener {
    private final JavaPlugin plugin;

    private boolean cancelGenericParticles;

    public GenericParticleListener(JavaPlugin plugin) {
        this.plugin = plugin;
        cancelGenericParticles = Config.cancelGenericParticles;
        logInfo("GenericParticleListener loaded");
    }

    public void onReload() {
        cancelGenericParticles = Config.cancelGenericParticles;
        plugin.getLogger().info("GenericParticleListener reloaded");
    }

    public void onPacketSend(PacketSendEvent event){
        if(!cancelGenericParticles) return;
        if(event.getPacketType()==PacketType.Play.Server.PARTICLE){
            WrapperPlayServerParticle packet = new WrapperPlayServerParticle(event);
            if (packet.getParticle().getType() == ParticleTypes.DAMAGE_INDICATOR) event.setCancelled(true);
        }else if (event.getPacketType() == PacketType.Play.Server.ENTITY_ANIMATION) {
            WrapperPlayServerEntityAnimation packet = new WrapperPlayServerEntityAnimation(event);
            if (packet.getType() == WrapperPlayServerEntityAnimation.EntityAnimationType.CRITICAL_HIT) event.setCancelled(true);
        }
    }
}
