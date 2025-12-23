package gg.itzkatze.thehypixelrecreationmod.features.region;


import net.minecraft.core.BlockPos;

public class RegionBox {
    public final String regionId;
    public final String regionType;
    public final BlockPos corner1, corner2;
    public final String serverType;
    private final int color;
    private boolean isComplete = true; // Always true for 1x1 blocks

    public RegionBox(String regionType, BlockPos pos1, BlockPos pos2, int color) {
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
        this.serverType = "HUB";
        this.regionId = generateRegionId(regionType, corner1);
        this.color = color;
        this.isComplete = true;
    }

    private String generateRegionId(String regionType, BlockPos pos) {
        return regionType.toLowerCase() + "_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
    }

    public boolean isComplete() {
        return isComplete;
    }

    public int getColor() {
        return color;
    }

    public BlockPos getLastCorner() {
        return corner2; // For rendering purposes
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() == corner1.getX() && pos.getX() == corner2.getX() &&
                pos.getY() >= corner1.getY() && pos.getY() <= corner2.getY() &&
                pos.getZ() == corner1.getZ() && pos.getZ() == corner2.getZ();
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
}