package gg.itzkatze.thehypixelrecreationmod.features.region;

import gg.itzkatze.thehypixelrecreationmod.utils.StringUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.*;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public class RegionTracker {
    public static final Map<String, List<RegionBox>> regions = new HashMap<>();
    private static final Map<String, Integer> regionColors = new HashMap<>();
    private static String currentRegion = "";
    private static RegionBox activeBox = null;
    private static BlockPos lastPlayerPos = null;
    private static final Map<BlockPos, String> positionRegionCache = new HashMap<>();
    private static final int CACHE_SIZE = 1000;
    private static int regionStableTicks = 0;
    private static final int REGION_STABILITY_THRESHOLD = 2;

    // Color palette for different regions
    private static final int[] COLOR_PALETTE = {
            0xFF0000, // Red
            0x00FF00, // Green
            0x0000FF, // Blue
            0xFFFF00, // Yellow
            0xFF00FF, // Magenta
            0x00FFFF, // Cyan
            0xFFA500, // Orange
            0x800080, // Purple
            0x008000, // Dark Green
            0x800000, // Maroon
    };

    public static void update() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        BlockPos currentPos = client.player.blockPosition();

        // Only process if player moved
        if (lastPlayerPos != null && lastPlayerPos.equals(currentPos)) return;
        lastPlayerPos = currentPos;

        // Get current region from scoreboard
        String newRegion = getCurrentRegionFromScoreboard();
        if (newRegion != null) newRegion = newRegion.toUpperCase();

        if (newRegion == null) {
            regionStableTicks = 0;
            return;
        }

        // Region stability tracking
        if (newRegion.equals(currentRegion)) {
            regionStableTicks++;
        } else {
            regionStableTicks = 1;

            // Region changed, finalize current active box
            if (activeBox != null) {
                finalizeActiveBox();
            }

            currentRegion = newRegion;
        }

        if (regionStableTicks < REGION_STABILITY_THRESHOLD) return;

        // Cache this position
        cacheRegion(currentPos, currentRegion);

        // If we have an active box for current region, try to expand it
        if (activeBox != null && activeBox.regionType.equals(currentRegion)) {
            expandBoxIfNeeded(currentPos);
            return;
        }

        // No active box or wrong region - check if we're in an existing box
        RegionBox existingBox = findBoxForRegion(currentPos, currentRegion);

        if (existingBox != null) {
            // We're in an existing finalized box - make it active again
            List<RegionBox> regionBoxes = regions.get(currentRegion);
            if (regionBoxes != null) {
                regionBoxes.remove(existingBox);
            }
            activeBox = existingBox;
            expandBoxIfNeeded(currentPos);
            return;
        }

        // Check if position is near any existing box of current region (within 5 blocks)
        RegionBox nearbyBox = findNearbyBoxForRegion(currentPos, currentRegion, 5);
        if (nearbyBox != null) {
            // Make it active and expand to include current position
            List<RegionBox> regionBoxes = regions.get(currentRegion);
            if (regionBoxes != null) {
                regionBoxes.remove(nearbyBox);
            }
            activeBox = nearbyBox;
            expandBoxIfNeeded(currentPos);
            return;
        }

        // Check if we're in a different region's territory
        RegionBox otherRegionBox = findBoxAtPosition(currentPos);
        if (otherRegionBox != null && !otherRegionBox.regionType.equals(currentRegion)) {
            // In another region's box - don't create anything here
            return;
        }

        // Create new box only if we're not near any existing boxes of this region
        createNewBox(currentPos);
    }

    private static void expandBoxIfNeeded(BlockPos currentPos) {
        if (activeBox == null) return;

        // Don't expand if position is already contained
        if (activeBox.contains(currentPos)) {
            activeBox.setLastCorner(currentPos);
            return;
        }

        // Calculate what the expanded box would look like
        BlockPos candidateMin = new BlockPos(
                Math.min(activeBox.getMin().getX(), currentPos.getX()),
                Math.min(activeBox.getMin().getY(), currentPos.getY()),
                Math.min(activeBox.getMin().getZ(), currentPos.getZ())
        );
        BlockPos candidateMax = new BlockPos(
                Math.max(activeBox.getMax().getX(), currentPos.getX()),
                Math.max(activeBox.getMax().getY(), currentPos.getY()),
                Math.max(activeBox.getMax().getZ(), currentPos.getZ())
        );

        // Check if any other region's boxes would be intersected by this expansion
        if (!canExpandToPosition(candidateMin, candidateMax, activeBox.regionType)) {
            // Can't expand - but don't create a new box, just stop expanding
            return;
        }

        // Try to merge with adjacent same-region boxes
        List<RegionBox> sameRegionBoxes = regions.getOrDefault(activeBox.regionType, new ArrayList<>());
        List<RegionBox> toMerge = new ArrayList<>();

        RegionBox testBox = new RegionBox(activeBox.regionType, candidateMin, candidateMax);

        for (RegionBox other : sameRegionBoxes) {
            if (RegionBox.boxesTouchOrOverlap(testBox, other)) {
                toMerge.add(other);
            }
        }

        // Merge boxes if found
        if (!toMerge.isEmpty()) {
            for (RegionBox mergeBox : toMerge) {
                candidateMin = new BlockPos(
                        Math.min(candidateMin.getX(), mergeBox.getMin().getX()),
                        Math.min(candidateMin.getY(), mergeBox.getMin().getY()),
                        Math.min(candidateMin.getZ(), mergeBox.getMin().getZ())
                );
                candidateMax = new BlockPos(
                        Math.max(candidateMax.getX(), mergeBox.getMax().getX()),
                        Math.max(candidateMax.getY(), mergeBox.getMax().getY()),
                        Math.max(candidateMax.getZ(), mergeBox.getMax().getZ())
                );
            }

            // Double-check safety after merge
            if (!canExpandToPosition(candidateMin, candidateMax, activeBox.regionType)) {
                return;
            }

            // Remove merged boxes
            sameRegionBoxes.removeAll(toMerge);
            invalidateCacheForRegion(activeBox.regionType);
        }

        // Apply expansion
        activeBox.corner1 = candidateMin;
        activeBox.corner2 = candidateMax;
        activeBox.setLastCorner(currentPos);
    }

    private static boolean canExpandToPosition(BlockPos min, BlockPos max, String regionType) {
        RegionBox testBox = new RegionBox(regionType, min, max);

        // Check against all finalized boxes of other regions
        for (Map.Entry<String, List<RegionBox>> entry : regions.entrySet()) {
            if (entry.getKey().equals(regionType)) continue;

            for (RegionBox other : entry.getValue()) {
                if (boxesIntersect(testBox, other)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static void cacheRegion(BlockPos pos, String region) {
        positionRegionCache.put(pos, region);

        if (positionRegionCache.size() > CACHE_SIZE) {
            Iterator<BlockPos> iterator = positionRegionCache.keySet().iterator();
            for (int i = 0; i < CACHE_SIZE / 10 && iterator.hasNext(); i++) {
                iterator.next();
                iterator.remove();
            }
        }
    }

    private static void invalidateCacheForRegion(String regionType) {
        positionRegionCache.entrySet().removeIf(entry ->
                entry.getValue().equals(regionType)
        );
    }

    private static RegionBox findBoxAtPosition(BlockPos pos) {
        // First check active box
        if (activeBox != null && activeBox.contains(pos)) {
            return activeBox;
        }

        // Check finalized boxes
        for (List<RegionBox> regionBoxes : regions.values()) {
            for (RegionBox box : regionBoxes) {
                if (box.contains(pos)) {
                    return box;
                }
            }
        }
        return null;
    }

    private static RegionBox findBoxForRegion(BlockPos pos, String regionType) {
        List<RegionBox> regionBoxes = regions.get(regionType);
        if (regionBoxes != null) {
            for (RegionBox box : regionBoxes) {
                if (box.contains(pos)) {
                    return box;
                }
            }
        }
        return null;
    }

    private static RegionBox findNearbyBoxForRegion(BlockPos pos, String regionType, int maxDistance) {
        List<RegionBox> regionBoxes = regions.get(regionType);
        if (regionBoxes == null) return null;

        RegionBox closest = null;
        double closestDist = Double.MAX_VALUE;

        for (RegionBox box : regionBoxes) {
            double dist = getDistanceToBox(pos, box);
            if (dist <= maxDistance && dist < closestDist) {
                closest = box;
                closestDist = dist;
            }
        }

        return closest;
    }

    private static double getDistanceToBox(BlockPos pos, RegionBox box) {
        BlockPos min = box.getMin();
        BlockPos max = box.getMax();

        // If inside box, distance is 0
        if (box.contains(pos)) return 0;

        // Calculate closest point on box to pos
        int closestX = Math.max(min.getX(), Math.min(pos.getX(), max.getX()));
        int closestY = Math.max(min.getY(), Math.min(pos.getY(), max.getY()));
        int closestZ = Math.max(min.getZ(), Math.min(pos.getZ(), max.getZ()));

        // Calculate distance
        int dx = pos.getX() - closestX;
        int dy = pos.getY() - closestY;
        int dz = pos.getZ() - closestZ;

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static void createNewBox(BlockPos pos) {
        // Create 1x1 box at player position
        activeBox = new RegionBox(currentRegion, pos, pos);

        // Initialize region color if needed
        regionColors.putIfAbsent(currentRegion,
                COLOR_PALETTE[regionColors.size() % COLOR_PALETTE.length]);
        activeBox.setColor(regionColors.get(currentRegion));

        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.displayClientMessage(
                    Component.literal("§aStarted new region box: §e" + currentRegion),
                    false
            );
        }
    }

    private static boolean boxesIntersect(RegionBox a, RegionBox b) {
        BlockPos aMin = a.getMin();
        BlockPos aMax = a.getMax();
        BlockPos bMin = b.getMin();
        BlockPos bMax = b.getMax();

        return aMin.getX() <= bMax.getX() && aMax.getX() >= bMin.getX()
                && aMin.getY() <= bMax.getY() && aMax.getY() >= bMin.getY()
                && aMin.getZ() <= bMax.getZ() && aMax.getZ() >= bMin.getZ();
    }

    private static void finalizeActiveBox() {
        if (activeBox == null) return;

        regions
                .computeIfAbsent(activeBox.regionType, k -> new ArrayList<>())
                .add(activeBox);

        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.displayClientMessage(
                    Component.literal("§7Finalized region box: §e" + activeBox.regionType),
                    true
            );
        }

        activeBox = null;
    }

    private static String getCurrentRegionFromScoreboard() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return null;

        Scoreboard scoreboard = client.level.getScoreboard();

        Objective sidebarObjective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebarObjective == null) return null;

        List<PlayerScoreEntry> entries = scoreboard
                .listPlayerScores(sidebarObjective)
                .stream()
                .sorted(java.util.Comparator.comparingInt(PlayerScoreEntry::value).reversed())
                .toList();

        for (PlayerScoreEntry entry : entries) {
            Component displayComponent = entry.display();
            String entryText = "";

            if (displayComponent != null) {
                entryText = displayComponent.getString();
            } else {
                entryText = entry.owner();
                PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
                if (team != null) {
                    String prefix = team.getPlayerPrefix().getString();
                    String suffix = team.getPlayerSuffix().getString();
                    entryText = prefix + entryText + suffix;
                }
            }

            if (entryText.isEmpty()) continue;

            if (entryText.contains("⏣")) {
                String regionName = StringUtility.stripColor(entryText.substring(entryText.indexOf("⏣") + 1).trim());

                if (!regionName.contains(":") && !regionName.matches(".*\\d+.*") && isValidRegionName(regionName)) {
                    return regionName;
                }
            }
        }

        return null;
    }

    private static boolean isValidRegionName(String name) {
        if (name.isEmpty()) return false;
        if (name.equalsIgnoreCase("coins")) return false;
        if (name.equalsIgnoreCase("purse")) return false;
        if (name.equalsIgnoreCase("bits")) return false;
        if (name.matches(".*\\d+.*")) return false;

        return name.length() > 1 && name.length() < 50;
    }

    public static List<RegionBox> getActiveBoxes() {
        List<RegionBox> allBoxes = new ArrayList<>();
        for (List<RegionBox> boxList : regions.values()) {
            allBoxes.addAll(boxList);
        }
        if (activeBox != null) {
            allBoxes.add(activeBox);
        }
        return allBoxes;
    }

    public static List<RegionBox> getIncompleteBoxes() {
        List<RegionBox> incomplete = new ArrayList<>();
        if (activeBox != null) {
            incomplete.add(activeBox);
        }
        return incomplete;
    }

    public static int getRegionColor(String regionName) {
        return regionColors.getOrDefault(regionName, 0xFFFFFF);
    }

    public static void initialize() {
        regions.clear();
        regionColors.clear();
        positionRegionCache.clear();
        currentRegion = "";
        activeBox = null;
        lastPlayerPos = null;
        regionStableTicks = 0;
    }
}