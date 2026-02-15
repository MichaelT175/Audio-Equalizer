package com.audioeq;

import java.io.*;
import java.util.*;

/**
 * The PresetManager class is responsible for managing audio presets for different tracks.
 * It provides functionality to load presets from a file, save new presets, and retrieve presets for a specific audio file.
 */
public class PresetManager {
    
    private static final String PRESETS_FILE = "presets.txt";
    
    private final Map<String, Map<String, float[]>> presets = new HashMap<>(); // song -> (presetName -> values)

    /**
     * Loads presets from the stored file into memory.
     * 
     * @return A map of presets, where the key is the song name and the value is a map of preset names to their associated values.
     */
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

    /**
     * Saves a new preset for a song to the presets.txt file.
     * This method updates the in-memory map and writes the entire set of presets back to the file.
     * 
     * @param song The name of the song for which the preset is being saved.
     * @param presetName The name of the preset.
     * @param bass The bass value for the preset.
     * @param mid The mid value for the preset.
     * @param treble The treble value for the preset.
     */
    public void savePreset(String song, String presetName, float bass, float mid, float treble) {
        presets.putIfAbsent(song, new HashMap<>());
        presets.get(song).put(presetName, new float[]{bass, mid, treble});

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PRESETS_FILE))) {
            // Write all presets to the file
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

    /**
     * Retrieves all presets for a specific song.
     * 
     * @param song The name of the song.
     * @return A map of preset names to their associated values for the specified song.
     */
    public Map<String, float[]> getPresetsForSong(String song) {
        return presets.getOrDefault(song, Collections.emptyMap());
    }
}