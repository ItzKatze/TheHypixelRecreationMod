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
    private static boolean enabled = false;
    private static boolean autoFillEnabled = true; // New: Enable/disable auto-filling

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

    public static void setAutoFillEnabled(boolean state) {
        autoFillEnabled = state;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            String status = state ? "§aENABLED" : "§cDISABLED";
            client.player.displayClientMessage(
                    Component.literal("§7Auto-fill " + status),
                    false
            );
        }
        debug("Auto-fill " + (state ? "enabled" : "disabled"));
    }

    public static void toggleAutoFill() {
        setAutoFillEnabled(!autoFillEnabled);
    }

    public static boolean isAutoFillEnabled() {
        return autoFillEnabled;
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

                // Even if block already exists, check for auto-fill opportunities
                if (autoFillEnabled) {
                    checkForEnclosedArea(currentRegion, currentPos);
                }
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

        // Check for auto-fill opportunities after adding new block
        if (autoFillEnabled) {
            checkForEnclosedArea(currentRegion, currentPos);
        }
    }

    /* ==================== COMPLETELY REVISED Auto-Fill System ==================== */
    private static void checkForEnclosedArea(String region, BlockPos addedPos) {
        Set<BlockPos> regionBlocksSet = regionBlocks.get(region);
        if (regionBlocksSet == null || regionBlocksSet.size() < 12) {
            debug("§7Auto-fill: Not enough blocks (" + (regionBlocksSet != null ? regionBlocksSet.size() : 0) + ") for region " + region);
            return;
        }

        debug("§6Auto-fill: Checking region " + region + " with " + regionBlocksSet.size() + " blocks");

        // Instead of looking for tiny enclosed areas, fill the entire bounding box
        // that's defined by the outermost region blocks
        fillRegionBoundary(region, regionBlocksSet);
    }

    /* ==================== Fill Region Boundary ==================== */
    private static void fillRegionBoundary(String region, Set<BlockPos> blocks) {
        // Find the complete bounding box of all blocks
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;

        for (BlockPos pos : blocks) {
            minX = Math.min(minX, pos.getX());
            maxX = Math.max(maxX, pos.getX());
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        debug("§7Region bounds: X[" + minX + " to " + maxX + "] Z[" + minZ + " to " + maxZ + "] Y[" + minY + " to " + maxY + "]");

        // For each Y level, fill the area within the boundary
        Map<Integer, Set<BlockPos>> blocksByY = new HashMap<>();
        for (BlockPos pos : blocks) {
            blocksByY.computeIfAbsent(pos.getY(), k -> new HashSet<>()).add(pos);
        }

        int totalFilled = 0;

        for (Map.Entry<Integer, Set<BlockPos>> entry : blocksByY.entrySet()) {
            int yLevel = entry.getKey();
            Set<BlockPos> levelBlocks = entry.getValue();

            if (levelBlocks.size() < 8) {
                debug("§7Skipping Y=" + yLevel + " (only " + levelBlocks.size() + " blocks)");
                continue;
            }

            // Find the boundary at this Y level
            int levelFilled = fillLevelBoundary(region, yLevel, levelBlocks, minX, maxX, minZ, maxZ, blocks);
            totalFilled += levelFilled;

            if (levelFilled > 0) {
                debug("§7  Y=" + yLevel + ": filled " + levelFilled + " blocks");
            }
        }

        if (totalFilled > 0) {
            debug("§aAuto-fill completed: " + totalFilled + " total blocks filled in " + region);
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.displayClientMessage(
                        Component.literal("§aAuto-fill: +" + totalFilled + " blocks in " + region),
                        true
                );
            }
        } else {
            debug("§7No blocks needed filling");
        }
    }

    /* ==================== Fill Level Boundary ==================== */
    private static int fillLevelBoundary(String region, int yLevel, Set<BlockPos> levelBlocks,
                                         int minX, int maxX, int minZ, int maxZ, Set<BlockPos> allBlocks) {
        // Create a grid for this level
        int width = maxX - minX + 1;
        int depth = maxZ - minZ + 1;
        boolean[][] grid = new boolean[width][depth];

        // Mark existing region blocks
        for (BlockPos pos : levelBlocks) {
            int gridX = pos.getX() - minX;
            int gridZ = pos.getZ() - minZ;
            if (gridX >= 0 && gridX < width && gridZ >= 0 && gridZ < depth) {
                grid[gridX][gridZ] = true;
            }
        }

        // Find the boundary of region blocks at this level
        Set<BlockPos> boundary = new HashSet<>();

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                if (grid[x][z]) {
                    // Check if this block is on the edge of the grid or has empty neighbors
                    boolean isBoundary = false;

                    if (x == 0 || x == width - 1 || z == 0 || z == depth - 1) {
                        isBoundary = true;
                    } else if (!grid[x-1][z] || !grid[x+1][z] || !grid[x][z-1] || !grid[x][z+1]) {
                        isBoundary = true;
                    }

                    if (isBoundary) {
                        boundary.add(new BlockPos(x + minX, yLevel, z + minZ));
                    }
                }
            }
        }

        if (boundary.size() < 4) {
            debug("§7  Y=" + yLevel + ": insufficient boundary blocks (" + boundary.size() + ")");
            return 0;
        }

        // Now fill everything inside the boundary
        return fillInsideBoundary2D(region, yLevel, boundary, minX, maxX, minZ, maxZ, allBlocks);
    }

    /* ==================== Fill Inside Boundary (2D) ==================== */
    private static int fillInsideBoundary2D(String region, int yLevel, Set<BlockPos> boundary,
                                            int minX, int maxX, int minZ, int maxZ, Set<BlockPos> allBlocks) {
        int filled = 0;

        // Convert boundary to a polygon for point-in-polygon test
        List<BlockPos> polygon = getConvexHull(boundary);

        if (polygon.size() < 3) {
            // Fallback: fill area where region blocks form a continuous shape
            return fillContinuousArea(region, yLevel, minX, maxX, minZ, maxZ, allBlocks);
        }

        // For each position in the bounding box, check if it's inside the polygon
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                BlockPos pos = new BlockPos(x, yLevel, z);

                // Skip if already in region
                if (allBlocks.contains(pos)) {
                    continue;
                }

                // Check if position is inside the boundary polygon
                if (isPointInPolygon(x, z, new HashSet<>(polygon))) {
                    // Check if this block belongs to another region
                    String existingRegion = getRegionAtPosition(pos);
                    if (existingRegion != null && !existingRegion.equals(region)) {
                        continue;
                    }

                    // Add to region
                    addBlockToRegion(pos, region);
                    filled++;
                }
            }
        }

        return filled;
    }

    /* ==================== Get Convex Hull ==================== */
    private static List<BlockPos> getConvexHull(Set<BlockPos> points) {
        if (points.size() < 3) {
            return new ArrayList<>(points);
        }

        List<BlockPos> pointList = new ArrayList<>(points);

        // Find the point with the lowest Y (and lowest X if tie)
        BlockPos lowest = pointList.get(0);
        for (BlockPos p : pointList) {
            if (p.getZ() < lowest.getZ() || (p.getZ() == lowest.getZ() && p.getX() < lowest.getX())) {
                lowest = p;
            }
        }

        // Sort by polar angle relative to lowest point
        final BlockPos finalLowest = lowest;
        pointList.sort((a, b) -> {
            if (a.equals(finalLowest)) return -1;
            if (b.equals(finalLowest)) return 1;

            double angleA = Math.atan2(a.getZ() - finalLowest.getZ(), a.getX() - finalLowest.getX());
            double angleB = Math.atan2(b.getZ() - finalLowest.getZ(), b.getX() - finalLowest.getX());

            if (angleA < angleB) return -1;
            if (angleA > angleB) return 1;

            // If same angle, keep the farthest one
            double distA = Math.pow(a.getX() - finalLowest.getX(), 2) + Math.pow(a.getZ() - finalLowest.getZ(), 2);
            double distB = Math.pow(b.getX() - finalLowest.getX(), 2) + Math.pow(b.getZ() - finalLowest.getZ(), 2);
            return Double.compare(distB, distA);
        });

        // Build convex hull using Graham scan
        List<BlockPos> hull = new ArrayList<>();
        for (BlockPos p : pointList) {
            while (hull.size() >= 2 && cross(hull.get(hull.size() - 2), hull.get(hull.size() - 1), p) <= 0) {
                hull.remove(hull.size() - 1);
            }
            hull.add(p);
        }

        return hull;
    }

    private static double cross(BlockPos o, BlockPos a, BlockPos b) {
        return (a.getX() - o.getX()) * (b.getZ() - o.getZ()) - (a.getZ() - o.getZ()) * (b.getX() - o.getX());
    }

    /* ==================== Fill Continuous Area ==================== */
    private static int fillContinuousArea(String region, int yLevel,
                                          int minX, int maxX, int minZ, int maxZ,
                                          Set<BlockPos> allBlocks) {
        int filled = 0;

        // Create grid for flood fill
        int width = maxX - minX + 1;
        int depth = maxZ - minZ + 1;
        boolean[][] grid = new boolean[width][depth];
        boolean[][] visited = new boolean[width][depth];

        // Mark existing region blocks
        for (BlockPos pos : allBlocks) {
            if (pos.getY() == yLevel) {
                int gridX = pos.getX() - minX;
                int gridZ = pos.getZ() - minZ;
                if (gridX >= 0 && gridX < width && gridZ >= 0 && gridZ < depth) {
                    grid[gridX][gridZ] = true;
                }
            }
        }

        // Find the largest connected component
        Set<BlockPos> largestComponent = findLargestComponent(grid, minX, minZ, width, depth);

        if (largestComponent.isEmpty()) {
            return 0;
        }

        // Find bounds of the largest component
        int compMinX = Integer.MAX_VALUE, compMaxX = Integer.MIN_VALUE;
        int compMinZ = Integer.MAX_VALUE, compMaxZ = Integer.MIN_VALUE;

        for (BlockPos pos : largestComponent) {
            compMinX = Math.min(compMinX, pos.getX());
            compMaxX = Math.max(compMaxX, pos.getX());
            compMinZ = Math.min(compMinZ, pos.getZ());
            compMaxZ = Math.max(compMaxZ, pos.getZ());
        }

        // Fill the area inside these bounds
        for (int x = compMinX; x <= compMaxX; x++) {
            for (int z = compMinZ; z <= compMaxZ; z++) {
                BlockPos pos = new BlockPos(x, yLevel, z);

                // Skip if already in region
                if (allBlocks.contains(pos)) {
                    continue;
                }

                // Check if position belongs to another region
                String existingRegion = getRegionAtPosition(pos);
                if (existingRegion != null && !existingRegion.equals(region)) {
                    continue;
                }

                // Add to region
                addBlockToRegion(pos, region);
                filled++;
            }
        }

        return filled;
    }

    /* ==================== Find Largest Component ==================== */
    private static Set<BlockPos> findLargestComponent(boolean[][] grid, int offsetX, int offsetZ, int width, int depth) {
        boolean[][] visited = new boolean[width][depth];
        Set<BlockPos> largestComponent = new HashSet<>();

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                if (grid[x][z] && !visited[x][z]) {
                    Set<BlockPos> component = new HashSet<>();
                    floodFillComponent(x, z, grid, visited, component, offsetX, offsetZ, width, depth);

                    if (component.size() > largestComponent.size()) {
                        largestComponent = component;
                    }
                }
            }
        }

        return largestComponent;
    }

    private static void floodFillComponent(int startX, int startZ, boolean[][] grid, boolean[][] visited,
                                           Set<BlockPos> component, int offsetX, int offsetZ, int width, int depth) {
        Stack<int[]> stack = new Stack<>();
        stack.push(new int[]{startX, startZ});

        while (!stack.isEmpty()) {
            int[] current = stack.pop();
            int x = current[0];
            int z = current[1];

            if (x < 0 || x >= width || z < 0 || z >= depth || !grid[x][z] || visited[x][z]) {
                continue;
            }

            visited[x][z] = true;
            component.add(new BlockPos(x + offsetX, 70, z + offsetZ));

            stack.push(new int[]{x + 1, z});
            stack.push(new int[]{x - 1, z});
            stack.push(new int[]{x, z + 1});
            stack.push(new int[]{x, z - 1});
        }
    }

    /* ==================== Point in Polygon Test ==================== */
    private static boolean isPointInPolygon(int testX, int testZ, Set<BlockPos> polygon) {
        // Simple ray casting algorithm
        boolean inside = false;

        // Convert polygon to list for ordered traversal
        List<BlockPos> polygonList = new ArrayList<>(polygon);
        if (polygonList.size() < 3) return false;

        for (int i = 0, j = polygonList.size() - 1; i < polygonList.size(); j = i++) {
            int xi = polygonList.get(i).getX();
            int zi = polygonList.get(i).getZ();
            int xj = polygonList.get(j).getX();
            int zj = polygonList.get(j).getZ();

            boolean intersect = ((zi > testZ) != (zj > testZ))
                    && (testX < (xj - xi) * (testZ - zi) / (zj - zi) + xi);

            if (intersect) {
                inside = !inside;
            }
        }

        return inside;
    }

    /* ==================== Existing Methods (with minor updates) ==================== */
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

    public static String getRegionAtPosition(BlockPos pos) {
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
                // Extract region name more carefully
                String regionName = entryText.substring(entryText.indexOf("⏣") + 1).trim();

                // Remove ALL color codes and formatting
                regionName = StringUtility.stripColor(regionName);

                // Remove any non-alphanumeric characters except spaces and underscores
                regionName = regionName.replaceAll("[^a-zA-Z0-9 _-]", "").trim();

                // Debug output to see what we're getting
                debug("§dRaw region text: '" + entryText + "' -> Cleaned: '" + regionName + "'");

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
        autoFillEnabled = true;
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

        Map<String, Integer> oldColors = new HashMap<>(regionColors);
        regionColors.clear();

        for (String region : regionBlocks.keySet()) {
            assignDistinctColorToRegion(region);
        }

        int changed = 0;
        for (String region : regionColors.keySet()) {
            if (oldColors.containsKey(region) &&
                    !oldColors.get(region).equals(regionColors.get(region))) {
                changed++;
            }
        }

        debug("§aRecoloring complete: " + changed + " regions changed color");
    }

    /* ==================== Manual Fill Command ==================== */
    public static void fillEnclosedAreaManually(String region) {
        if (!regionBlocks.containsKey(region)) {
            debug("§cRegion " + region + " not found");
            return;
        }

        debug("§6Manually filling enclosed areas for " + region);

        // Trigger fill check for this region
        Set<BlockPos> blocks = regionBlocks.get(region);
        if (!blocks.isEmpty()) {
            BlockPos samplePos = blocks.iterator().next();
            checkForEnclosedArea(region, samplePos);
        }
    }

    private static BlockPos pickOutermost(Set<BlockPos> blocks) {
        return blocks.stream()
                .min(Comparator
                        .comparingInt((BlockPos p) -> p.getY())
                        .thenComparingInt((BlockPos p) -> p.getX())
                        .thenComparingInt((BlockPos p) -> p.getZ()))
                .orElseThrow();
    }

    private static RegionBox extractMaxBox(String region, Set<BlockPos> remaining, int color) {
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

            if (canExpandX(remaining, minX, minY, minZ, maxX + 1, maxY, maxZ)) {
                maxX++;
                expanded = true;
            }

            if (canExpandZ(remaining, minX, minY, minZ, maxX, maxY, maxZ + 1)) {
                maxZ++;
                expanded = true;
            }

            if (canExpandY(remaining, minX, minY, minZ, maxX, maxY + 1, maxZ)) {
                maxY++;
                expanded = true;
            }

        } while (expanded);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    remaining.remove(new BlockPos(x, y, z));
                }
            }
        }

        return new RegionBox(region, new BlockPos(minX, minY, minZ),
                new BlockPos(maxX, maxY, maxZ), color);
    }

    private static boolean canExpandX(Set<BlockPos> blocks, int minX, int minY, int minZ,
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

    private static boolean canExpandZ(Set<BlockPos> blocks, int minX, int minY, int minZ,
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

    private static boolean canExpandY(Set<BlockPos> blocks, int minX, int minY, int minZ,
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