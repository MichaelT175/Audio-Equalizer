package com.audioeq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class OnlineAudioDownloader {
    private final Path cacheDir;

    public OnlineAudioDownloader() {
        cacheDir = Paths.get(System.getProperty("user.home"), ".audioeq", "online-cache");
    }

    public Path getOrDownload(OnlineSongResult song) throws IOException, InterruptedException {
        Files.createDirectories(cacheDir);
        String baseName = sanitize(song.getTitle()) + "-" + sanitize(song.getArtist()) + "-" + song.getVideoId();
        Path expectedFile = cacheDir.resolve(baseName + ".wav");
        if (Files.exists(expectedFile)) {
            return expectedFile;
        }

        String outputTemplate = cacheDir.resolve(baseName + ".%(ext)s").toString();
        String url = "https://music.youtube.com/watch?v=" + song.getVideoId();

        runDownloader(outputTemplate, url);

        if (!Files.exists(expectedFile)) {
            throw new IOException("Download completed but WAV file was not found.");
        }
        return expectedFile;
    }

    private void runDownloader(String outputTemplate, String url) throws IOException, InterruptedException {
        List<List<String>> commands = Arrays.asList(
            buildYtDlpCommand("yt-dlp", outputTemplate, url),
            buildYtDlpCommand("python", outputTemplate, url, true),
            buildYtDlpCommand("py", outputTemplate, url, true)
        );

        IOException lastError = null;
        for (List<String> command : commands) {
            try {
                runCommand(command);
                return;
            } catch (IOException ex) {
                lastError = ex;
                if (isExecutableMissing(ex)) {
                    continue;
                }
                throw toFriendlyError(ex);
            }
        }

        if (lastError != null && isExecutableMissing(lastError)) {
            throw new IOException("yt-dlp not found. Install it with: pip install yt-dlp", lastError);
        }
        throw lastError != null ? lastError : new IOException("yt-dlp failed to run.");
    }

    private List<String> buildYtDlpCommand(String executable, String outputTemplate, String url) {
        return buildYtDlpCommand(executable, outputTemplate, url, false);
    }

    private List<String> buildYtDlpCommand(String executable, String outputTemplate, String url, boolean asModule) {
        if (asModule) {
            return Arrays.asList(
                executable, "-m", "yt_dlp",
                "-x", "--audio-format", "wav",
                "--no-playlist",
                "-o", outputTemplate,
                url
            );
        }
        return Arrays.asList(
            executable,
            "-x", "--audio-format", "wav",
            "--no-playlist",
            "-o", outputTemplate,
            url
        );
    }

    private void runCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("yt-dlp failed: " + output.toString().trim());
        }
    }

    private boolean isExecutableMissing(IOException ex) {
        String message = ex.getMessage();
        return message != null && message.contains("CreateProcess error=2");
    }

    private IOException toFriendlyError(IOException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "";
        String lower = message.toLowerCase();
        if (message.contains("No module named yt_dlp")) {
            return new IOException("yt-dlp is not installed. Install it with: pip install yt-dlp", ex);
        }
        if (lower.contains("ffmpeg") && (lower.contains("not found") || lower.contains("ffprobe"))) {
            return new IOException("ffmpeg was not found. Install ffmpeg and add it to PATH.", ex);
        }
        return ex;
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "track";
        }
        return value.replaceAll("[^A-Za-z0-9._-]+", "_");
    }
}
