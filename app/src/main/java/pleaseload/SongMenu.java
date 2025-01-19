package pleaseload;

import javax.swing.*;
import java.util.*;

/**
 * The SongMenu class provides functionality for managing a list of songs.
 * It allows for selecting a song from a dropdown menu, adding new songs, 
 * and ensuring that song names are valid. It also ensures that the list of 
 * songs remains alphabetically sorted.
 */
public class SongMenu {
    
    //A map that stores the song titles and their corresponding file paths. */
    private final Map<String, String> songMap; // Maps song titles to file paths
    
    //The dropdown menu that allows users to select a song
    private JComboBox<String> songDropdown;   // Dropdown menu for the songs

    /**
     * Constructs a new SongMenu, initializing the song map, populating it with songs,
     * sorting the songs, and creating the dropdown menu.
     */
    public SongMenu() {
        // Initialize the song map and populate it with songs
        songMap = new HashMap<>();
        populateSongs();
        sortSongs();
        createDropdown();
    }

    /**
     * Populates the song map with initial songs and their corresponding file paths.
     */
    private void populateSongs() {
        songMap.put("Imperial March", "C:\\Users\\Michael Jr\\Music\\DEMO SONGS\\ImperialMarch60.wav");
        songMap.put("Star Wars", "C:\\Users\\Michael Jr\\Music\\DEMO SONGS\\StarWars60.wav");
        songMap.put("Baby Elephant Walk", "C:\\Users\\Michael Jr\\Music\\DEMO SONGS\\BabyElephantWalk60.wav");
        songMap.put("Pink Panther", "C:\\Users\\Michael Jr\\Music\\DEMO SONGS\\PinkPanther60.wav");
        songMap.put("Sin Sweep", "C:\\Users\\Michael Jr\\Music\\DEMO SONGS\\OnlineSound_net_Sweep_Tone.wav");
    }

    /**
     * Sorts the songs alphabetically by their titles using the selection sort algorithm.
     */
    private void sortSongs() {
        List<String> songTitles = new ArrayList<>(songMap.keySet());
    
        // Selection sort
        for (int i = 0; i < songTitles.size() - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j < songTitles.size(); j++) {
                if (songTitles.get(j).compareTo(songTitles.get(minIndex)) < 0) {
                    minIndex = j;
                }
            }
            // Swap the elements
            if (minIndex != i) {
                String temp = songTitles.get(i);
                songTitles.set(i, songTitles.get(minIndex));
                songTitles.set(minIndex, temp);
            }
        }
    
        // Create a new map preserving the sorted order
        Map<String, String> sortedMap = new LinkedHashMap<>();
        for (String title : songTitles) {
            sortedMap.put(title, songMap.get(title));
        }
    
        songMap.clear();
        songMap.putAll(sortedMap);
    }

    /**
     * Creates the dropdown menu with the sorted list of song titles.
     */
    private void createDropdown() {
        songDropdown = new JComboBox<>(songMap.keySet().toArray(new String[0]));
    }

    /**
     * Returns the dropdown menu that allows the user to select a song.
     * 
     * @return The JComboBox containing the list of song titles.
     */
    public JComboBox<String> getDropdown() {
        return songDropdown;
    }

    /**
     * Returns the file path of the selected song from the dropdown menu.
     * 
     * @return The file path of the selected song, or null if no song is selected.
     */
    public String getSelectedSongPath() {
        String selectedSong = (String) songDropdown.getSelectedItem();
        return selectedSong != null ? songMap.get(selectedSong) : null;
    }

    /**
     * Allows the user to select a new song file, name it, and add it to the list of songs.
     * Validates the name and ensures it adheres to the required constraints.
     * 
     * @return A message indicating the result of the song addition (e.g., success or cancellation).
     */
    public String addNewFile() {
        // Open a file chooser dialog to select the song file
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a Song File");
        int result = fileChooser.showOpenDialog(null);
    
        if (result == JFileChooser.APPROVE_OPTION) {
            // Get the selected file's path
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
    
            while (true) {
                // Ask the user for a name for the song
                String songName = JOptionPane.showInputDialog(null, 
                    "Enter a name for the song (No more than 5 words, no numbers/special characters, starts with a capital letter):", 
                    "Name Your Song", 
                    JOptionPane.PLAIN_MESSAGE);
    
                // Check if the input is valid
                if (songName == null) {
                    return "Song addition canceled.";
                }
    
                songName = songName.trim();
    
                // Validation for the song name
                if (isValidSongName(songName)) {
                    // Add the new song to the map
                    songMap.put(songName, filePath);
    
                    // Re-sort the songs to maintain alphabetical order
                    sortSongs();
    
                    // Update the dropdown menu
                    songDropdown.removeAllItems();
                    for (String title : songMap.keySet()) {
                        songDropdown.addItem(title);
                    }
    
                    return "Song added successfully!";
                } else {
                    JOptionPane.showMessageDialog(null, 
                        "Invalid song name. A valid name should:\n" +
                        "- Be no more than 5 words\n" +
                        "- Contain no numbers or special characters\n" +
                        "- Start with a capital letter", 
                        "Invalid Name", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    
        return "No file selected.";
    }

    /**
     * Validates a song name based on the following criteria:
     * - No more than 5 words
     * - Only letters and spaces (no numbers or special characters)
     * - Starts with a capital letter
     * 
     * @param songName The name of the song to validate.
     * @return true if the song name is valid, false otherwise.
     */
    private boolean isValidSongName(String songName) {
        if (songName.isEmpty()) {
            return false;
        }
    
        // Check for word count (no more than 5 words)
        String[] words = songName.split("\\s+");
        if (words.length > 5) {
            return false;
        }
    
        // Check for valid characters (letters and spaces only)
        if (!songName.matches("[A-Za-z ]+")) {
            return false;
        }
    
        // Check if the first letter of the song name is capitalized
        if (!Character.isUpperCase(songName.charAt(0))) {
            return false;
        }
    
        return true;
    }
}
