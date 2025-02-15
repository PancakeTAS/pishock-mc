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

        MS_100(100, 100, "100ms"), MS_300(300, 300, "300ms"),
        S_1(1, 1000, "1s"), S_2(2, 2000, "2s"),  S_3(3, 3000, "3s"),  S_4(4, 4000, "4s"), S_5(5, 5000, "5s"),
        S_6(6, 6000, "6s"), S_7(7, 7000, "7s"), S_8(8, 8000, "8s"), S_9(9, 9000, "9s"), S_10(10, 10000, "10s");

        /** API-encoded duration of the action. */
        public final int duration;
        /** Internal duration of the action. */
        public final int internal;
        /** Human-readable duration of the action. */
        public final String string;

        ActionDuration(int duration, int internal, String string) {
            this.duration = duration;
            this.internal = internal;
            this.string = string;
        }

    }

    /**
     * Type of action to perform.
     */
    public enum ActionType {

        SHOCK(0, "shock"), VIBRATE(1, "vibrate"), BEEP(2, "beep");

        /** API-encoded type of action. */
        public final int type;
        /** Internal code of the action. */
        public final String code;

        ActionType(int type, String code) {
            this.type = type;
            this.code = code;
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
