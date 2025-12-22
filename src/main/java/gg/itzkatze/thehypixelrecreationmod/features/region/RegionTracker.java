package gg.itzkatze.thehypixelrecreationmod.features.region;

import gg.itzkatze.thehypixelrecreationmod.utils.StringUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.*;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import java.util.*;

public class RegionTracker {
    public static final Map<String, Set<BlockPos>> regionBlocks = new HashMap<>();
    private static final Map<String, Integer> regionColors = new HashMap<>();
    private static String currentRegion = "";
    private static BlockPos lastCheckedPos = null;
    private static final int REGION_STABILITY_THRESHOLD = 2;
    private static int regionStableTicks = 0;
    private static boolean debugMode = true;
    private static boolean enabled = true;

    // Extended color palette with more distinct colors
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
            0x008080, // Teal
            0xFF6347, // Tomato
            0x9370DB, // Medium Purple
            0x32CD32, // Lime Green
            0xFF4500, // Orange Red
            0x2E8B57, // Sea Green
            0x8A2BE2, // Blue Violet
            0xDC143C, // Crimson
            0x00CED1, // Dark Turquoise
            0xFF1493, // Deep Pink
            0x1E90FF, // Dodger Blue
            0xB22222, // Fire Brick
            0x228B22, // Forest Green
            0xDAA520, // Golden Rod
            0xADFF2F, // Green Yellow
            0xFF69B4, // Hot Pink
            0xCD5C5C, // Indian Red
            0x4B0082, // Indigo
            0xF0E68C, // Khaki
            0x7CFC00, // Lawn Green
    };

    // Track which regions are adjacent to each other
    private static final Map<String, Set<String>> regionAdjacency = new HashMap<>();

    private static void debug(String message) {
        if (!debugMode) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.displayClientMessage(
                    Component.literal("§7[DEBUG] " + message),
                    false
            );
        }
        System.out.println("[RegionTracker DEBUG] " + message);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean state) {
        enabled = state;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            String status = state ? "§aENABLED" : "§cDISABLED";
            client.player.displayClientMessage(
                    Component.literal("§7Region tracking " + status),
                    false
            );
        }
        debug("Region tracking " + (state ? "enabled" : "disabled"));
    }

    public static void toggle() {
        setEnabled(!enabled);
    }

    public static void update() {
        if (!enabled) return;

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        BlockPos currentPos = client.player.blockPosition();

        // Only process if player moved to a new block
        if (lastCheckedPos != null && lastCheckedPos.equals(currentPos)) {
            return;
        }
        lastCheckedPos = currentPos;

        // Get current region from scoreboard
        String newRegion = getCurrentRegionFromScoreboard();
        if (newRegion != null) newRegion = newRegion.toUpperCase();

        if (newRegion == null) {
            regionStableTicks = 0;
            return;
        }

        // Check region stability
        if (newRegion.equals(currentRegion)) {
            regionStableTicks++;
        } else {
            debug("§eRegion changed: §c" + currentRegion + " §7-> §a" + newRegion);
            regionStableTicks = 1;
            currentRegion = newRegion;
        }

        // Only process if region is stable
        if (regionStableTicks < REGION_STABILITY_THRESHOLD) {
            return;
        }

        // Check if this position is already registered with the correct region
        String existingRegion = getRegionAtPosition(currentPos);

        if (existingRegion != null) {
            if (existingRegion.equals(currentRegion)) {
                debug("§aBlock " + posToString(currentPos) + " already belongs to " + currentRegion);
                return; // Already correct, nothing to do
            } else {
                // Wrong region - remove from old region and add to correct one
                debug("§cCorrecting block " + posToString(currentPos) +
                        " from " + existingRegion + " to " + currentRegion);
                removeBlockFromRegion(currentPos, existingRegion);
                // Update adjacency after removal
                updateAdjacencyForPosition(currentPos, existingRegion, currentRegion);
            }
        } else {
            // Update adjacency for new block
            updateAdjacencyForPosition(currentPos, null, currentRegion);
        }

        // Add block to current region
        addBlockToRegion(currentPos, currentRegion);

        // Initialize region color if needed, ensuring no adjacent regions share colors
        if (!regionColors.containsKey(currentRegion)) {
            assignDistinctColorToRegion(currentRegion);
        }
    }

    private static void updateAdjacencyForPosition(BlockPos pos, String oldRegion, String newRegion) {
        // Check all 6 adjacent positions (up, down, north, south, east, west)
        BlockPos[] adjacentPositions = {
                pos.above(), pos.below(),
                pos.north(), pos.south(),
                pos.east(), pos.west()
        };

        // Remove old adjacency relationships
        if (oldRegion != null) {
            for (BlockPos adjacentPos : adjacentPositions) {
                String adjacentRegion = getRegionAtPosition(adjacentPos);
                if (adjacentRegion != null && !adjacentRegion.equals(oldRegion)) {
                    // Remove bidirectional adjacency
                    regionAdjacency.computeIfAbsent(oldRegion, k -> new HashSet<>()).remove(adjacentRegion);
                    regionAdjacency.computeIfAbsent(adjacentRegion, k -> new HashSet<>()).remove(oldRegion);
                }
            }
        }

        // Add new adjacency relationships
        if (newRegion != null) {
            for (BlockPos adjacentPos : adjacentPositions) {
                String adjacentRegion = getRegionAtPosition(adjacentPos);
                if (adjacentRegion != null && !adjacentRegion.equals(newRegion)) {
                    // Add bidirectional adjacency
                    regionAdjacency.computeIfAbsent(newRegion, k -> new HashSet<>()).add(adjacentRegion);
                    regionAdjacency.computeIfAbsent(adjacentRegion, k -> new HashSet<>()).add(newRegion);

                    debug("§dAdjacency: " + newRegion + " ↔ " + adjacentRegion);

                    // Check if colors need to be reassigned
                    if (regionColors.containsKey(newRegion) && regionColors.containsKey(adjacentRegion)) {
                        if (regionColors.get(newRegion).equals(regionColors.get(adjacentRegion))) {
                            debug("§cColor conflict detected between " + newRegion + " and " + adjacentRegion);
                            // Reassign color to the new region
                            assignDistinctColorToRegion(newRegion);
                        }
                    }
                }
            }
        }
    }

    private static void assignDistinctColorToRegion(String region) {
        Set<String> adjacentRegions = regionAdjacency.getOrDefault(region, new HashSet<>());
        Set<Integer> forbiddenColors = new HashSet<>();

        // Collect colors of adjacent regions
        for (String adjacentRegion : adjacentRegions) {
            if (regionColors.containsKey(adjacentRegion)) {
                forbiddenColors.add(regionColors.get(adjacentRegion));
            }
        }

        // Also check for regions that are 2 blocks away to avoid similar colors
        Set<String> nearbyRegions = new HashSet<>(adjacentRegions);
        for (String adjacentRegion : adjacentRegions) {
            Set<String> secondLevelAdjacent = regionAdjacency.getOrDefault(adjacentRegion, new HashSet<>());
            for (String secondRegion : secondLevelAdjacent) {
                if (!secondRegion.equals(region)) {
                    nearbyRegions.add(secondRegion);
                    if (regionColors.containsKey(secondRegion)) {
                        forbiddenColors.add(regionColors.get(secondRegion));
                    }
                }
            }
        }

        // Find a suitable color
        int assignedColor = 0xFFFFFF; // Default white

        // Try to find a color that's not forbidden
        for (int color : COLOR_PALETTE) {
            if (!forbiddenColors.contains(color)) {
                assignedColor = color;
                break;
            }
        }

        // If all colors in palette are forbidden, find one that's least similar
        if (forbiddenColors.contains(assignedColor)) {
            assignedColor = findMostDifferentColor(forbiddenColors);
        }

        regionColors.put(region, assignedColor);
        debug("§aAssigned color #" + Integer.toHexString(assignedColor).toUpperCase() +
                " to region " + region + " (avoiding " + forbiddenColors.size() + " forbidden colors)");

        // Log adjacency info
        if (!adjacentRegions.isEmpty()) {
            StringBuilder adjList = new StringBuilder();
            for (String adj : adjacentRegions) {
                adjList.append(adj).append(", ");
            }
            debug("§7Adjacent to: " + adjList.toString());
        }
    }

    private static int findMostDifferentColor(Set<Integer> forbiddenColors) {
        // If we have to reuse a color, try to pick one that's far from most forbidden colors
        Map<Integer, Double> colorScores = new HashMap<>();

        for (int candidateColor : COLOR_PALETTE) {
            double totalDistance = 0;
            int r1 = (candidateColor >> 16) & 0xFF;
            int g1 = (candidateColor >> 8) & 0xFF;
            int b1 = candidateColor & 0xFF;

            for (int forbiddenColor : forbiddenColors) {
                int r2 = (forbiddenColor >> 16) & 0xFF;
                int g2 = (forbiddenColor >> 8) & 0xFF;
                int b2 = forbiddenColor & 0xFF;

                // Calculate color distance (Euclidean in RGB space)
                double distance = Math.sqrt(
                        Math.pow(r1 - r2, 2) +
                                Math.pow(g1 - g2, 2) +
                                Math.pow(b1 - b2, 2)
                );
                totalDistance += distance;
            }

            colorScores.put(candidateColor, totalDistance / forbiddenColors.size());
        }

        // Return color with highest average distance
        return colorScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0xFFFFFF);
    }

    private static void addBlockToRegion(BlockPos pos, String region) {
        regionBlocks.computeIfAbsent(region, k -> new HashSet<>()).add(pos.immutable());
        debug("§aAdded block " + posToString(pos) + " to region " + region);

        // Show feedback every 10th block added
        Set<BlockPos> blocks = regionBlocks.get(region);
        if (blocks != null && blocks.size() % 10 == 0) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.displayClientMessage(
                        Component.literal("§7Mapped " + blocks.size() + " blocks for §e" + region),
                        true
                );
            }
        }
    }

    private static void removeBlockFromRegion(BlockPos pos, String region) {
        Set<BlockPos> blocks = regionBlocks.get(region);
        if (blocks != null) {
            blocks.remove(pos);
            debug("§cRemoved block " + posToString(pos) + " from region " + region);

            // Clean up empty regions
            if (blocks.isEmpty()) {
                regionBlocks.remove(region);
                regionColors.remove(region);
                regionAdjacency.remove(region);

                // Also remove this region from other regions' adjacency lists
                for (Set<String> adjSet : regionAdjacency.values()) {
                    adjSet.remove(region);
                }

                debug("§cRegion " + region + " has no blocks remaining - removed");
            }
        }
    }

    private static String getRegionAtPosition(BlockPos pos) {
        for (Map.Entry<String, Set<BlockPos>> entry : regionBlocks.entrySet()) {
            if (entry.getValue().contains(pos)) {
                return entry.getKey();
            }
        }
        return null;
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

    private static String posToString(BlockPos pos) {
        if (pos == null) return "§c(null)";
        return "§7(" + pos.getX() + "§7, §b" + pos.getY() + "§7, §a" + pos.getZ() + "§7)";
    }

    public static List<RegionBox> getActiveBoxes() {
        if (!enabled) return new ArrayList<>();

        List<RegionBox> boxes = new ArrayList<>();
        for (String region : regionBlocks.keySet()) {
            boxes.addAll(getMergedBoxesForRegion(region));
        }
        return boxes;
    }

    public static List<RegionBox> getIncompleteBoxes() {
        // In this system, all boxes are "complete" (1x1 blocks)
        return getActiveBoxes();
    }

    public static int getRegionColor(String regionName) {
        return regionColors.getOrDefault(regionName, 0xFFFFFF);
    }

    public static void initialize() {
        regionBlocks.clear();
        regionColors.clear();
        regionAdjacency.clear();
        currentRegion = "";
        lastCheckedPos = null;
        regionStableTicks = 0;
        debug("§a1x1 Block RegionTracker initialized");
    }

    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
        debug("Debug mode " + (enabled ? "enabled" : "disabled"));
    }

    public static int getTotalBlocks() {
        return regionBlocks.values().stream().mapToInt(Set::size).sum();
    }

    public static int getRegionBlockCount(String region) {
        Set<BlockPos> blocks = regionBlocks.get(region);
        return blocks != null ? blocks.size() : 0;
    }

    public static Map<String, Integer> getRegionStatistics() {
        Map<String, Integer> stats = new HashMap<>();
        for (Map.Entry<String, Set<BlockPos>> entry : regionBlocks.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().size());
        }
        return stats;
    }

    public static Map<String, Set<String>> getRegionAdjacency() {
        return new HashMap<>(regionAdjacency);
    }

    public static void recolorAllRegions() {
        debug("§6Recoloring all regions to ensure distinct adjacent colors...");

        // Store old colors for comparison
        Map<String, Integer> oldColors = new HashMap<>(regionColors);
        regionColors.clear();

        // Assign new colors to all regions
        for (String region : regionBlocks.keySet()) {
            assignDistinctColorToRegion(region);
        }

        // Count changed colors
        int changed = 0;
        for (String region : regionColors.keySet()) {
            if (oldColors.containsKey(region) &&
                    !oldColors.get(region).equals(regionColors.get(region))) {
                changed++;
            }
        }

        debug("§aRecoloring complete: " + changed + " regions changed color");
    }

    private static BlockPos pickOutermost(Set<BlockPos> blocks) {
        return blocks.stream()
                .min(Comparator
                        .comparingInt((BlockPos p) -> p.getY())
                        .thenComparingInt((BlockPos p) -> p.getX())
                        .thenComparingInt((BlockPos p) -> p.getZ()))
                .orElseThrow();
    }



    private static RegionBox extractMaxBox(
            String region,
            Set<BlockPos> remaining,
            int color
    ) {
        BlockPos start = pickOutermost(remaining);

        int minX = start.getX();
        int minY = start.getY();
        int minZ = start.getZ();

        int maxX = minX;
        int maxY = minY;
        int maxZ = minZ;

        boolean expanded;

        do {
            expanded = false;

            // Try +X
            if (canExpandX(remaining, minX, minY, minZ, maxX + 1, maxY, maxZ)) {
                maxX++;
                expanded = true;
            }

            // Try +Z
            if (canExpandZ(remaining, minX, minY, minZ, maxX, maxY, maxZ + 1)) {
                maxZ++;
                expanded = true;
            }

            // Try +Y
            if (canExpandY(remaining, minX, minY, minZ, maxX, maxY + 1, maxZ)) {
                maxY++;
                expanded = true;
            }

        } while (expanded);

        // Remove covered blocks
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    remaining.remove(new BlockPos(x, y, z));
                }
            }
        }

        return new RegionBox(
                region,
                new BlockPos(minX, minY, minZ),
                new BlockPos(maxX, maxY, maxZ),
                color
        );
    }

    private static boolean canExpandX(Set<BlockPos> blocks,
                                      int minX, int minY, int minZ,
                                      int newX, int maxY, int maxZ) {
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!blocks.contains(new BlockPos(newX, y, z))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean canExpandZ(Set<BlockPos> blocks,
                                      int minX, int minY, int minZ,
                                      int maxX, int maxY, int newZ) {
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (!blocks.contains(new BlockPos(x, y, newZ))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean canExpandY(Set<BlockPos> blocks,
                                      int minX, int minY, int minZ,
                                      int maxX, int newY, int maxZ) {
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (!blocks.contains(new BlockPos(x, newY, z))) {
                    return false;
                }
            }
        }
        return true;
    }

    public static List<RegionBox> getMergedBoxesForRegion(String region) {
        Set<BlockPos> blocks = regionBlocks.get(region);
        if (blocks == null || blocks.isEmpty()) return List.of();

        Set<BlockPos> remaining = new HashSet<>(blocks);
        List<RegionBox> result = new ArrayList<>();
        int color = getRegionColor(region);

        while (!remaining.isEmpty()) {
            result.add(extractMaxBox(region, remaining, color));
        }

        return result;
    }
}