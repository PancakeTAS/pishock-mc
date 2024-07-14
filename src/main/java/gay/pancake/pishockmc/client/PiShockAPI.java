package gay.pancake.pishockmc.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Interface to the PiShock API.
 */
@Environment(EnvType.CLIENT)
public class PiShockAPI {

    /**
     * The duration of the action. Can be either 100ms, 300ms or 1-10s.
     */
    public enum ActionDuration {

        MS_100(100), MS_300(300),
        S_1(1), S_2(2),  S_3(3),  S_4(4), S_5(5),
        S_6(6), S_7(7), S_8(8), S_9(9), S_10(10);

        /** API-encoded duration of the action. */
        public final int duration;

        ActionDuration(int duration) {
            this.duration = duration;
        }

    }

    /**
     * Type of action to perform.
     */
    public enum ActionType {

        SHOCK(0), VIBRATE(1), BEEP(2);

        /** API-encoded type of action. */
        public final int type;

        ActionType(int type) {
            this.type = type;
        }

    }

    /** API URL */
    private static final String API_URL = "https://do.pishock.com/api/apioperate/";

    /** HTTP client */
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    /** Request json */
    private static final String REQUEST_JSON = """
            {
                "Username": "%s",
                "Apikey": "%s",
                "Code": "%s",
                "Name": "pishock-ms",
                "Op": "%d",
                "Intensity": "%d",
                "Duration": "%d"
            }
            """;

    /**
     * Call the PiShock API.
     *
     * @param username PiShock username
     * @param apiKey PiShock API key
     * @param sharecode PiShock share code
     * @param op Type of action to perform
     * @param intensity Intensity of the action (ignored for BEEP)
     * @param duration Duration of the action
     * @return CompletableFuture of the HTTP response
     */
    public static CompletableFuture<HttpResponse<String>> call(String username, String apiKey, String sharecode, ActionType op, int intensity, ActionDuration duration) {
        // Log the action
        System.err.printf("Calling PiShock API: %s with %d%% for %d\n", op, intensity, duration);

        // Create the request
        var body = String.format(REQUEST_JSON, username, apiKey, sharecode, op.type, intensity, duration.duration);
        var request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

}