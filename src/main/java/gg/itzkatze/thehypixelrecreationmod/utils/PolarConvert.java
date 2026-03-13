package gg.itzkatze.thehypixelrecreationmod.utils;

import net.hollowcube.polar.AnvilPolar;
import net.hollowcube.polar.ChunkSelector;
import net.hollowcube.polar.PolarWriter;
import net.hollowcube.polar.PolarWorld;
import net.minestom.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PolarConvert {
    private PolarConvert() {
    }

    public static boolean convertWorldFolderToPolar(Path anvilPath, Path outputPath) throws IOException {
        MinecraftServer.init();
        try {
            PolarWorld polarWorld = AnvilPolar.anvilToPolar(anvilPath, ChunkSelector.all());
            Files.createDirectories(outputPath.getParent());
            Files.write(outputPath, PolarWriter.write(polarWorld));
            return true;
        } finally {
            MinecraftServer.stopCleanly();
        }
    }
}
