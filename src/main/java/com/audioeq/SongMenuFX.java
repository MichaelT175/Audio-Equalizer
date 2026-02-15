package com.audioeq;


import javafx.scene.control.ComboBox;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;
import java.io.File;
import java.util.*;

/**
 * JavaFX version of SongMenu for managing songs in the audio equalizer.
 * Provides file management with modern JavaFX dialogs.
 */
public class SongMenuFX {
    
    private final Map<String, String> songMap;
    private ComboBox<String> songDropdown;
    
    /**
     * Constructs a new SongMenuFX.
     */
    public SongMenuFX() {
        songMap = new LinkedHashMap<>();
        populateSongs();
        sortSongs();
        createDropdown();
    }
    
    /**
     * Populates the song map with initial songs.
     */
    private void populateSongs() {
        songMap.put("Imperial March", "C:\\Users\\Michael Jr\\Music\\DEMO SONGS\\ImperialMarch60.wav");
        songMap.put("Star Wars", "C:\\Users\\Michael Jr\\Music\\DEMO SONGS\\StarWars60.wav");
        songMap.put("Baby Elephant Walk", "C:\\Users\\Michael Jr\\Music\\DEMO SONGS\\BabyElephantWalk60.wav");
        songMap.put("Pink Panther", "C:\\Users\\Michael Jr\\Music\\DEMO SONGS\\PinkPanther60.wav");
        songMap.put("Sin Sweep", "C:\\Users\\Michael Jr\\Music\\DEMO SONGS\\OnlineSound_net_Sweep_Tone.wav");
    }
    
    /**
     * Sorts songs alphabetically.
     */
    private void sortSongs() {
        List<String> songTitles = new ArrayList<>(songMap.keySet());
        Collections.sort(songTitles);
        
        Map<String, String> sortedMap = new LinkedHashMap<>();
        for (String title : songTitles) {
            sortedMap.put(title, songMap.get(title));
        }
        
        songMap.clear();
        songMap.putAll(sortedMap);
    }
    
    /**
     * Creates the dropdown ComboBox.
     */
    private void createDropdown() {
        songDropdown = new ComboBox<>();
        updateDropdown();
    }
    
    /**
     * Returns the ComboBox dropdown.
     */
    public ComboBox<String> getDropdown() {
        return songDropdown;
    }
    
    /**
     * Gets the file path of the selected song.
     */
    public String getSelectedSongPath() {
        String selectedSong = songDropdown.getValue();
        return selectedSong != null ? songMap.get(selectedSong) : null;
    }
    
    /**
     * Adds a new song file to the library.
     */
    public void addNewFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a Song File");
        
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Audio Files", 
                "*.wav", "*.mp3", "*.flac", "*.aiff"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        File selectedFile = fileChooser.showOpenDialog(null);
        
        if (selectedFile != null) {
            if (!selectedFile.exists()) {
                showError("File Error", "Selected file does not exist.");
                return;
            }
            
            String filePath = selectedFile.getAbsolutePath();
            
            while (true) {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Name Your Song");
                dialog.setHeaderText("Enter a name for the song:");
                dialog.setContentText("Song name:");
                
                Optional<String> result = dialog.showAndWait();
                
                if (!result.isPresent()) {
                    return; // User cancelled
                }
                
                String songName = result.get().trim();
                
                if (songMap.containsKey(songName)) {
                    showError("Duplicate Name", 
                        "A song with this name already exists. Please choose a different name.");
                    continue;
                }
                
                if (isValidSongName(songName)) {
                    songMap.put(songName, filePath);
                    sortSongs();
                    updateDropdown();
                    
                    showInfo("Success", 
                        "Song \"" + songName + "\" added successfully!");
                    return;
                } else {
                    showError("Invalid Name", 
                        "Invalid song name. A valid name should:\n" +
                        "- Be no more than 5 words\n" +
                        "- Contain no numbers or special characters\n" +
                        "- Start with a capital letter");
                }
            }
        }
    }
    
    /**
     * Removes the selected song from the library.
     */
    public void removeSelectedFile() {
        String selectedSong = songDropdown.getValue();
        
        if (selectedSong == null || songMap.isEmpty()) {
            showWarning("Remove Song", "No song selected to remove.");
            return;
        }
        
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirm Removal");
        confirmDialog.setHeaderText("Remove \"" + selectedSong + "\"?");
        confirmDialog.setContentText(
            "Are you sure you want to remove this song from the library?\n" +
            "(This will not delete the actual file)"
        );
        
        Optional<javafx.scene.control.ButtonType> result = confirmDialog.showAndWait();
        
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            songMap.remove(selectedSong);
            updateDropdown();
            showInfo("Success", "Song \"" + selectedSong + "\" removed successfully!");
        }
    }
    
    /**
     * Updates the dropdown with current songs.
     */
    private void updateDropdown() {
        String currentSelection = songDropdown.getValue();
        songDropdown.getItems().clear();
        songDropdown.getItems().addAll(songMap.keySet());
        
        if (songMap.containsKey(currentSelection)) {
            songDropdown.setValue(currentSelection);
        } else if (!songMap.isEmpty()) {
            songDropdown.setValue(songMap.keySet().iterator().next());
        }
    }
    
    /**
     * Validates a song name.
     */
    private boolean isValidSongName(String songName) {
        if (songName.isEmpty()) {
            return false;
        }
        
        String[] words = songName.split("\\s+");
        if (words.length > 5) {
            return false;
        }
        
        if (!songName.matches("[A-Za-z ]+")) {
            return false;
        }
        
        if (!Character.isUpperCase(songName.charAt(0))) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Shows an error dialog.
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Shows a warning dialog.
     */
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Shows an info dialog.
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
