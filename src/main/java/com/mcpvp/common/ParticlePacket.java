package com.mcpvp.common;

import com.mcpvp.common.util.nms.PacketUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Represents a single particle packet that can be sent to the client.
 *
 * @author NomNuggetNom
 * @author redslime
 */
@Getter
@SuppressWarnings("UnusedReturnValue")
public class ParticlePacket {

    private EnumParticle particle;
    @Setter
    private int[] dataArray = null;
    private float x;
    private float y;
    private float z;
    private float offX = 0;
    private float offY = 0;
    private float offZ = 0;
    private float data = 0;
    private int count = 1;
    private boolean showFar = true;
    private World world;

    public ParticlePacket() {

    }

    public ParticlePacket(EnumParticle particle) {
        this.particle = particle;
    }

    public ParticlePacket(EnumParticle particle, Location location) {
        this(particle);
        this.at(location);
    }

    public static ParticlePacket of(EnumParticle particle) {
        return new ParticlePacket(particle);
    }

    @SuppressWarnings("deprecation")
    public static ParticlePacket blockDust(Material material) {
        ParticlePacket packet = of(EnumParticle.BLOCK_CRACK);
        packet.setDataArray(new int[]{material.getId(), 0});
        return packet;
    }

    /**
     * Factory method shortcut to {@link #color(org.bukkit.Color)}.
     */
    public static ParticlePacket colored(org.bukkit.Color color) {
        return of(EnumParticle.REDSTONE).color(color);
    }

    /**
     * @param particle The EnumParticle type to use.
     */
    public ParticlePacket type(EnumParticle particle) {
        this.particle = particle;
        return this;
    }

    /**
     * @param location The location to spawn the particle(s) at.
     */
    public ParticlePacket at(Location location) {
        this.x = (float) location.getX();
        this.y = (float) location.getY();
        this.z = (float) location.getZ();
        this.world = location.getWorld();
        return this;
    }

    /**
     * @param world The world to show the Particle in.
     */
    public ParticlePacket in(World world) {
        this.world = world;
        return this;
    }

    /**
     * The client randomizes the location of the particle(s) between the base
     * coordinates and the offset. Essentially this gives it more room to
     * spread.
     *
     * @param x The X location to spawn at.
     * @param y The Y location to spawn at.
     * @param z The Z location to spawn at.
     */
    public ParticlePacket offset(float x, float y, float z) {
        this.setOffX(x);
        this.setOffY(y);
        this.setOffZ(z);
        return this;
    }

    /**
     * The client randomizes the location of the particle(s) between the base X
     * and the offset of the X. Essentially this gives it more room to spread.
     *
     * @param offX The distance the particle can be offset in the X direction.
     */
    public ParticlePacket setOffX(float offX) {
        this.offX = offX;
        return this;
    }

    /**
     * The client randomizes the location of the particle(s) between the base Y
     * and the offset of the Y. Essentially this gives it more room to spread.
     *
     * @param offY The distance the particle can be offset in the Y direction.
     */
    public ParticlePacket setOffY(float offY) {
        this.offY = offY;
        return this;
    }

    /**
     * The client randomizes the location of the particle(s) between the base Z
     * and the offset of the Z. Essentially this gives it more room to spread.
     *
     * @param offZ The distance the particle can be offset in the Z direction.
     */
    public ParticlePacket setOffZ(float offZ) {
        this.offZ = offZ;
        return this;
    }

    /**
     * The client randomizes the location of the particle(s) between the base
     * coordinates and the offset. Essentially this gives it more room to
     * spread.
     *
     * @param spread The distance in each direction to spread.
     */
    public ParticlePacket spread(Number spread) {
        this.offset(spread.floatValue(), spread.floatValue(), spread.floatValue());
        return this;
    }

    /**
     * Usually causes the particle to spread out more. For "reddust" it sets the
     * color.
     */
    public ParticlePacket setData(float data) {
        this.data = data;
        return this;
    }

    /**
     * Usually causes the particle to spread out more. For "reddust" it sets the
     * color.
     */
    public ParticlePacket data(float data) {
        return this.setData(data);
    }

    public ParticlePacket setShowFar(boolean showFar) {
        this.showFar = showFar;
        return this;
    }

    /**
     * Enables the particle to be shown extremely far away.
     */
    public ParticlePacket showFar() {
        return this.setShowFar(true);
    }

    /**
     * @param count The number of particles to spawn.
     */
    public ParticlePacket count(int count) {
        this.count = count;
        return this;
    }

    /**
     * Sets the color of this Particle. This is only for use with
     * {@link EnumParticle#REDSTONE}. This has a few limitations, like only
     * being able to send one particle.
     *
     * @param color The color to make the particle.
     */
    public ParticlePacket color(org.bukkit.Color color) {
        Validate.isTrue((this.particle == EnumParticle.REDSTONE || this.particle == EnumParticle.SPELL_MOB || this.particle == EnumParticle.NOTE), "Only redstone particles can be colored");
        this.count(0);
        this.setShowFar(false);
        this.setOffX(-1 + (color.getRed() * 1f / 255));
        this.setOffY(color.getGreen() * 1f / 255);
        this.setOffZ(color.getBlue() * 1f / 255);
        this.data(1);
        return this;
    }

    /**
     * Uses {@link #color(org.bukkit.Color)}.
     */
    public ParticlePacket color(java.awt.Color color) {
        return this.color(org.bukkit.Color.fromRGB(color.getRGB()));
    }

    public String getParticleName() {
        return this.particle.name();
    }

    public void send() {
        Validate.notNull(this.particle);
        Validate.notNull(this.world);
        Bukkit.getOnlinePlayers().stream().filter(p -> p.getWorld().equals(this.world)).forEach(this::send);
    }

    public void send(Player player) {
        Validate.notNull(this.particle);
        Validate.notNull(this.world);
        PacketUtil.sendPacket(player, this.create());
    }

    public PacketPlayOutWorldParticles create() {
        return new PacketPlayOutWorldParticles(EnumParticle.valueOf(EnumParticle.class, this.getParticleName()),
            this.showFar,
            this.x, this.y, this.z,
            this.offX, this.offY, this.offZ,
            this.data,
            this.count,
            this.dataArray
        );
    }

}