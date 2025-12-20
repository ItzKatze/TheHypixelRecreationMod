package gg.itzkatze.thehypixelrecreationmod.features.region;

import gg.itzkatze.thehypixelrecreationmod.TheHypixelRecreationMod;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class RegionExporter {
    public static void exportToFile(String filename) {
        try {
            Path filePath = Paths.get(filename);

            // Create directory if it doesn't exist
            Files.createDirectories(filePath.getParent());

            try (FileWriter writer = new FileWriter(filename)) {
                // Write header
                writer.write("_id\ttype\tx1\ty1\tz1\tx2\ty2\tz2\tserverType\n");

                // Write all regions
                for (List<RegionBox> regionList : RegionTracker.regions.values()) {
                    for (RegionBox box : regionList) {
                        writer.write(box.toCSVFormat() + "\n");
                    }
                }

                TheHypixelRecreationMod.LOGGER.info("Exported {} region boxes to {}",
                        RegionTracker.regions.values().stream().mapToInt(List::size).sum(),
                        filename);
            }
        } catch (IOException e) {
            TheHypixelRecreationMod.LOGGER.error("Failed to export regions: {}", e.getMessage());
            throw new RuntimeException("Failed to export regions", e);
        }
    }

    public static void exportWithTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String timestamp = dateFormat.format(new Date());
        String filename = "regions/regions_export_" + timestamp + ".txt";
        exportToFile(filename);
    }

    public static String getExportSummary() {
        int totalBoxes = RegionTracker.regions.values().stream().mapToInt(List::size).sum();
        int completeBoxes = (int) RegionTracker.regions.values().stream()
                .mapToLong(List::size)
                .sum();

        return String.format("Total regions: %d, Complete boxes: %d, Incomplete: %d",
                RegionTracker.regions.size(), completeBoxes, totalBoxes - completeBoxes);
    }
}