package pleaseload;

import javax.swing.*;
import java.util.*;

public class SongMenu {
    private final Map<String, String> songMap; // Maps song titles to file paths
    private JComboBox<String> songDropdown;   // Dropdown menu for the songs

    public SongMenu() {
        // Initialize the song map and populate it with songs
        songMap = new HashMap<>();
        populateSongs();
        sortSongs();
        createDropdown();
    }

    // Populates the song map with initial values
    private void populateSongs() {
        songMap.put("Imperial March", "C:\\Users\\Michael Jr\\Music\\DEMO SONGS\\ImperialMarch60.wav");
        songMap.put("Star Wars", "C:\\Users\\Michael Jr\\Music\\DEMO SONGS\\StarWars60.wav");
        songMap.put("Baby Elephant Walk", "C:\\Users\\Michael Jr\\Music\\DEMO SONGS\\BabyElephantWalk60.wav");
        songMap.put("Pink Panther", "C:\\Users\\Michael Jr\\Music\\DEMO SONGS\\PinkPanther60.wav");
    }

    // Sorts the songs in alphabetical order by their titles
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
    

    // Creates the dropdown menu for songs
    private void createDropdown() {
        songDropdown = new JComboBox<>(songMap.keySet().toArray(new String[0]));
    }

    // Returns the dropdown menu for the GUI
    public JComboBox<String> getDropdown() {
        return songDropdown;
    }

    // Returns the file path of the selected song
    public String getSelectedSongPath() {
        String selectedSong = (String) songDropdown.getSelectedItem();
        return selectedSong != null ? songMap.get(selectedSong) : null;
    }

    // Allows the user to select a new song file, name it, and add it to the dropdown menu
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
    
    // Helper method to validate song names
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
