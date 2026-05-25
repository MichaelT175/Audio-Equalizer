package com.audioeq;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OnlineSongSearchService {
    private static final int DEFAULT_LIMIT = 12;
    private final Gson gson = new Gson();

    public List<OnlineSongResult> search(String query, int limit) throws IOException, InterruptedException {
        int resolvedLimit = limit > 0 ? limit : DEFAULT_LIMIT;
        String script = buildSearchScript();
        String output = runPython(script, query, String.valueOf(resolvedLimit));
        Type listType = new TypeToken<List<OnlineSongResult>>() {}.getType();
        List<OnlineSongResult> results = gson.fromJson(output, listType);
        return results != null ? results : Collections.emptyList();
    }

    private String runPython(String script, String query, String limit) throws IOException, InterruptedException {
        List<List<String>> commands = Arrays.asList(
            Arrays.asList("python", "-c", script, query, limit),
            Arrays.asList("py", "-c", script, query, limit)
        );
        IOException lastError = null;
        for (List<String> command : commands) {
            try {
                return runCommand(command);
            } catch (IOException ex) {
                lastError = ex;
                if (!isExecutableMissing(ex)) {
                    throw ex;
                }
            }
        }
        throw lastError != null ? lastError : new IOException("Python executable not found.");
    }

    private String runCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("ytmusicapi search failed: " + output);
        }

        String json = output.toString().trim();
        if (json.isEmpty()) {
            throw new IOException("ytmusicapi returned no data.");
        }
        return json;
    }

    private boolean isExecutableMissing(IOException ex) {
        String message = ex.getMessage();
        return message != null && message.contains("CreateProcess error=2");
    }

    private String buildSearchScript() {
        return String.join("\n",
            "from ytmusicapi import YTMusic",
            "import json, sys",
            "query = sys.argv[1]",
            "limit = int(sys.argv[2]) if len(sys.argv) > 2 else 12",
            "yt = YTMusic()",
            "results = yt.search(query, filter='songs', limit=limit)",
            "payload = []",
            "for item in results:",
            "    title = item.get('title') or 'Unknown Title'",
            "    artists = item.get('artists') or []",
            "    artist = ''",
            "    if artists and isinstance(artists, list):",
            "        artist = artists[0].get('name', '') if artists[0] else ''",
            "    video_id = item.get('videoId')",
            "    thumbs = item.get('thumbnails') or []",
            "    thumb_url = ''",
            "    if thumbs:",
            "        thumb_url = thumbs[-1].get('url', '')",
            "    duration = item.get('duration') or ''",
            "    if video_id:",
            "        payload.append({",
            "            'title': title,",
            "            'artist': artist,",
            "            'videoId': video_id,",
            "            'thumbnailUrl': thumb_url,",
            "            'duration': duration",
            "        })",
            "print(json.dumps(payload))"
        );
    }
}
