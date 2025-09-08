package com.mcpvp.common;

import com.mcpvp.common.nms.PacketUtil;
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
        at(location);
    }

    public static ParticlePacket of(EnumParticle particle) {
        return new ParticlePacket(particle);
    }

    public static ParticlePacket blockDust(Material material) {
        ParticlePacket packet = of(EnumParticle.BLOCK_CRACK);
        packet.setDataArray(new int[]{material.getId(), 0});
        return packet;
    }

//    public WrapperParticlePacket create() {
//        return new WrapperParticlePacket(particle.name(), showFar, new Location(world, x, y, z), new Vector(offX, offY, offZ), data, count, dataArray);
//    }

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
        x = (float) location.getX();
        y = (float) location.getY();
        z = (float) location.getZ();
        world = location.getWorld();
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
        setOffX(x);
        setOffY(y);
        setOffZ(z);
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
        offset(spread.floatValue(), spread.floatValue(), spread.floatValue());
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
        return setData(data);
    }

    public ParticlePacket setShowFar(boolean showFar) {
        this.showFar = showFar;
        return this;
    }

    /**
     * Enables the particle to be shown extremely far away.
     */
    public ParticlePacket showFar() {
        return setShowFar(true);
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
        Validate.isTrue((particle == EnumParticle.REDSTONE || particle == EnumParticle.SPELL_MOB || particle == EnumParticle.NOTE), "Only redstone particles can be colored");
        count(0);
        setShowFar(false);
        setOffX(-1 + (color.getRed() * 1f / 255));
        setOffY(color.getGreen() * 1f / 255);
        setOffZ(color.getBlue() * 1f / 255);
        data(1);
        return this;
    }

    /**
     * Uses {@link #color(org.bukkit.Color)}.
     */
    public ParticlePacket color(java.awt.Color color) {
        return color(org.bukkit.Color.fromRGB(color.getRGB()));
    }

    public String getParticleName() {
        return particle.name();
    }

    public void send() {
        Validate.notNull(particle);
        Validate.notNull(world);
        Bukkit.getOnlinePlayers().stream().filter(p -> p.getWorld().equals(world)).forEach(this::send);
    }

    public void send(Player player) {
        Validate.notNull(particle);
        Validate.notNull(world);
        PacketUtil.sendPacket(player, create());
    }

    public PacketPlayOutWorldParticles create() {
        return new PacketPlayOutWorldParticles(EnumParticle.valueOf(EnumParticle.class, getParticleName()),
            showFar,
            x, y, z,
            offX, offY, offZ,
            data,
            count,
            dataArray
        );
    }

}