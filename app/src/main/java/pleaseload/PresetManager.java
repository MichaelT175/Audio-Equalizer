package pleaseload;

import java.io.*;
import java.util.*;

public class PresetManager {
    private static final String PRESET_FILE = "presets.txt";

    // Saves a preset for a specific song
    public void savePreset(String songTitle, float bass, float mid, float treble) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PRESET_FILE, true))) {
            writer.write(songTitle + "," + bass + "," + mid + "," + treble);
            writer.newLine();
            System.out.println("Preset saved for song: " + songTitle);
        } catch (IOException e) {
            System.err.println("Error saving preset: " + e.getMessage());
        }
    }

    // Loads presets from the file
    public Map<String, float[]> loadPresets() {
        Map<String, float[]> presets = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(PRESET_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    String songTitle = parts[0];
                    float bass = Float.parseFloat(parts[1]);
                    float mid = Float.parseFloat(parts[2]);
                    float treble = Float.parseFloat(parts[3]);
                    presets.put(songTitle, new float[]{bass, mid, treble});
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("No presets file found. It will be created when saving.");
        } catch (IOException e) {
            System.err.println("Error loading presets: " + e.getMessage());
        }
        return presets;
    }

    // Deletes all presets for cleanup purposes (optional)
    public void clearPresets() {
        try {
            new PrintWriter(PRESET_FILE).close();
            System.out.println("All presets cleared.");
        } catch (IOException e) {
            System.err.println("Error clearing presets: " + e.getMessage());
        }
    }
}
