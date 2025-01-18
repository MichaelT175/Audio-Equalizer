package pleaseload;

import java.io.*;
import java.util.*;

public class PresetManager {
    private static final String PRESETS_FILE = "presets.txt";
    private final Map<String, Map<String, float[]>> presets = new HashMap<>(); // song -> (presetName -> values)

    // Load presets from the file
    public Map<String, Map<String, float[]>> loadPresets() {
        File file = new File(PRESETS_FILE);
        if (!file.exists()) {
            System.out.println("No presets file found. It will be created when saving.");
            return presets;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length == 5) {
                    String song = parts[0];
                    String presetName = parts[1];
                    float bass = Float.parseFloat(parts[2]);
                    float mid = Float.parseFloat(parts[3]);
                    float treble = Float.parseFloat(parts[4]);

                    presets.putIfAbsent(song, new HashMap<>());
                    presets.get(song).put(presetName, new float[]{bass, mid, treble});
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading presets: " + e.getMessage());
        }

        return presets;
    }

    // Save a preset
    public void savePreset(String song, String presetName, float bass, float mid, float treble) {
        presets.putIfAbsent(song, new HashMap<>());
        presets.get(song).put(presetName, new float[]{bass, mid, treble});

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PRESETS_FILE))) {
            for (String s : presets.keySet()) {
                for (Map.Entry<String, float[]> entry : presets.get(s).entrySet()) {
                    String name = entry.getKey();
                    float[] values = entry.getValue();
                    writer.write(String.format("%s;%s;%.2f;%.2f;%.2f%n", s, name, values[0], values[1], values[2]));
                }
            }
        } catch (IOException e) {
            System.err.println("Error saving presets: " + e.getMessage());
        }
    }

    // Get all presets for a specific song
    public Map<String, float[]> getPresetsForSong(String song) {
        return presets.getOrDefault(song, Collections.emptyMap());
    }
}