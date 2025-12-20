package gg.itzkatze.thehypixelrecreationmod.features.region;


import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import javax.swing.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class RegionBox {
    public final String regionId;
    public final String regionType;
    public BlockPos corner1;
    public BlockPos corner2;
    public final String serverType;
    private BlockPos lastCorner = null;
    private int color = 0xFFFFFF;

    public RegionBox(String regionType, BlockPos pos1, BlockPos pos2) {
        this.regionType = regionType.toUpperCase();
        this.corner1 = new BlockPos(
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ())
        );
        this.corner2 = new BlockPos(
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ())
        );
        this.serverType = detectServerType();
        this.regionId = generateRegionId(regionType);
        this.color = RegionTracker.getRegionColor(regionType);
        this.lastCorner = corner2;
    }

    // Constructor for incomplete boxes
    public RegionBox(String regionType, BlockPos firstCorner) {
        this.regionType = regionType.toUpperCase();
        this.corner1 = firstCorner;
        this.corner2 = firstCorner;
        this.serverType = "HUB";
        this.regionId = generateRegionId(regionType) + "_incomplete";
        this.color = RegionTracker.getRegionColor(regionType);
        this.lastCorner = firstCorner;
    }

    private String generateRegionId(String regionType) {
        int count = 1;
        // Count existing boxes of this type
        if (RegionTracker.regions.containsKey(regionType)) {
            count = RegionTracker.regions.get(regionType).size() + 1;
        }
        return regionType.toLowerCase() + "_" + count;
    }

    private String detectServerType() {
        // In Hypixel Skyblock, you can detect server type from player position or world data
        // For now, default to HUB
        return "HUB";
    }

    public BlockPos getLastCorner() {
        return lastCorner;
    }

    public void setLastCorner(BlockPos lastCorner) {
        this.lastCorner = lastCorner;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String toCSVFormat() {
        return String.format("%s\t%s\t%d\t%d\t%d\t%d\t%d\t%d\t%s",
                regionId,
                regionType,
                corner1.getX(),
                corner1.getY(),
                corner1.getZ(),
                corner2.getX(),
                corner2.getY(),
                corner2.getZ(),
                serverType
        );
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= corner1.getX() && pos.getX() <= corner2.getX() &&
                pos.getY() >= corner1.getY() && pos.getY() <= corner2.getY() &&
                pos.getZ() >= corner1.getZ() && pos.getZ() <= corner2.getZ();
    }

    public boolean contains(Vec3 pos) {
        BlockPos min = getMin();
        BlockPos max = getMax();
        return pos.x >= min.getX() && pos.x <= max.getX() + 1 &&
                pos.y >= min.getY() && pos.y <= max.getY() + 1 &&
                pos.z >= min.getZ() && pos.z <= max.getZ() + 1;
    }

    public boolean expandToInclude(BlockPos pos) {
        BlockPos min = getMin();
        BlockPos max = getMax();

        int minX = Math.min(min.getX(), pos.getX());
        int minY = Math.min(min.getY(), pos.getY());
        int minZ = Math.min(min.getZ(), pos.getZ());

        int maxX = Math.max(max.getX(), pos.getX());
        int maxY = Math.max(max.getY(), pos.getY());
        int maxZ = Math.max(max.getZ(), pos.getZ());

        if (
                minX == min.getX() &&
                        minY == min.getY() &&
                        minZ == min.getZ() &&
                        maxX == max.getX() &&
                        maxY == max.getY() &&
                        maxZ == max.getZ()
        ) {
            return false;
        }

        this.corner1 = new BlockPos(minX, minY, minZ);
        this.corner2 = new BlockPos(maxX, maxY, maxZ);
        return true;
    }

    public static boolean boxesTouchOrOverlap(RegionBox a, RegionBox b) {
        BlockPos aMin = a.getMin();
        BlockPos aMax = a.getMax();
        BlockPos bMin = b.getMin();
        BlockPos bMax = b.getMax();

        return aMin.getX() <= bMax.getX() + 1 && aMax.getX() + 1 >= bMin.getX()
                && aMin.getZ() <= bMax.getZ() + 1 && aMax.getZ() + 1 >= bMin.getZ();
    }

    public BlockPos getMin() {
        return new BlockPos(
                Math.min(corner1.getX(), corner2.getX()),
                Math.min(corner1.getY(), corner2.getY()),
                Math.min(corner1.getZ(), corner2.getZ())
        );
    }

    public BlockPos getMax() {
        return new BlockPos(
                Math.max(corner1.getX(), corner2.getX()),
                Math.max(corner1.getY(), corner2.getY()),
                Math.max(corner1.getZ(), corner2.getZ())
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RegionBox regionBox = (RegionBox) obj;
        return corner1.equals(regionBox.corner1) &&
                corner2.equals(regionBox.corner2) &&
                regionType.equals(regionBox.regionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(corner1, corner2, regionType);
    }
}