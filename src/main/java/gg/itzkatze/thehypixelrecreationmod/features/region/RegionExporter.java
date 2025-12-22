package gg.itzkatze.thehypixelrecreationmod.features.region;

import gg.itzkatze.thehypixelrecreationmod.TheHypixelRecreationMod;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class RegionExporter {

    private static final Path EXPORT_BASE_DIR =
            FabricLoader.getInstance()
                    .getGameDir()
                    .resolve("exports")
                    .resolve("regions");

    public static void exportToFile(String filename) {
        try {
            // Ensure filename only (no directories)
            String safeFilename = filename.replaceAll("[/\\\\]", "");

            Path filePath = EXPORT_BASE_DIR.resolve(safeFilename);

            // Create base directory if needed
            Files.createDirectories(EXPORT_BASE_DIR);

            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                writer.write("_id\ttype\tx1\ty1\tz1\tx2\ty2\tz2\tserverType\n");

                int totalBlocks = 0;
                for (String region : RegionTracker.regionBlocks.keySet()) {
                    Set<BlockPos> blocks = RegionTracker.regionBlocks.get(region);
                    if (blocks != null) {
                        int color = RegionTracker.getRegionColor(region);
                        for (BlockPos pos : blocks) {
                            RegionBox box = new RegionBox(region, pos, pos, color);
                            writer.write(box.toCSVFormat() + "\n");
                            totalBlocks++;
                        }
                    }
                }

                TheHypixelRecreationMod.LOGGER.info(
                        "Exported {} blocks to {}",
                        totalBlocks,
                        filePath.toAbsolutePath()
                );
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to export regions", e);
        }
    }


    public static void exportWithTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String timestamp = dateFormat.format(new Date());
        exportToFile("blocks_export_" + timestamp + ".txt");
    }

    public static String getExportSummary() {
        int totalBlocks = RegionTracker.getTotalBlocks();
        Map<String, Integer> stats = RegionTracker.getRegionStatistics();

        StringBuilder summary = new StringBuilder();
        summary.append("Total blocks: ").append(totalBlocks).append("\n");
        summary.append("Regions: ").append(stats.size()).append("\n");

        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            summary.append("  - ").append(entry.getKey()).append(": ").append(entry.getValue()).append(" blocks\n");
        }

        return summary.toString();
    }
}