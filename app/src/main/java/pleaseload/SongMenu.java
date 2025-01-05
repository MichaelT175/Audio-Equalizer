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
        Collections.sort(songTitles); // Sort titles alphabetically

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
}
