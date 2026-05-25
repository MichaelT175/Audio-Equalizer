package com.audioeq;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

public class OnlineSearchService {
    private static final String ITUNES_SEARCH_URL = "https://itunes.apple.com/search?media=music&entity=song&limit=15&term=";

    private final HttpClient client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .build();

    private final Gson gson = new Gson();

    public List<OnlineTrack> search(String query) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        URI uri = URI.create(ITUNES_SEARCH_URL + encoded);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(uri)
            .timeout(Duration.ofSeconds(10))
            .GET()
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Search request failed (" + response.statusCode() + ")");
        }

        ItunesResponse payload = gson.fromJson(response.body(), ItunesResponse.class);
        List<OnlineTrack> results = new ArrayList<>();
        if (payload != null && payload.results != null) {
            for (ItunesResult result : payload.results) {
                if (result.previewUrl == null || result.trackName == null) {
                    continue;
                }
                results.add(new OnlineTrack(
                    result.trackName,
                    result.artistName != null ? result.artistName : "Unknown Artist",
                    result.collectionName != null ? result.collectionName : "Unknown Album",
                    result.previewUrl,
                    result.artworkUrl100
                ));
            }
        }
        return results;
    }

    private static class ItunesResponse {
        List<ItunesResult> results;
    }

    private static class ItunesResult {
        String trackName;
        String artistName;
        String collectionName;
        String previewUrl;
        String artworkUrl100;
    }
}
