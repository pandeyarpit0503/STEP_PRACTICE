import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class UsernameAvailabilityService {

    private final ConcurrentHashMap<String, Long> usernameToUserId;

    private final ConcurrentHashMap<String, AtomicInteger> attemptFrequency;

    public UsernameAvailabilityService() {
        usernameToUserId = new ConcurrentHashMap<>();
        attemptFrequency = new ConcurrentHashMap<>();
    }

    public boolean registerUsername(String username, long userId) {
        return usernameToUserId.putIfAbsent(username, userId) == null;
    }

    public boolean checkAvailability(String username) {
        incrementAttempt(username);
        return !usernameToUserId.containsKey(username);
    }

    public List<String> suggestAlternatives(String username) {
        List<String> suggestions = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            String suggestion = username + i;
            if (!usernameToUserId.containsKey(suggestion)) {
                suggestions.add(suggestion);
            }
        }

        if (username.contains("_")) {
            String modified = username.replace("_", ".");
            if (!usernameToUserId.containsKey(modified)) {
                suggestions.add(modified);
            }
        }

        return suggestions;
    }

    private void incrementAttempt(String username) {
        attemptFrequency
                .computeIfAbsent(username, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    public String getMostAttempted() {
        return attemptFrequency.entrySet()
                .stream()
                .max(Comparator.comparingInt(e -> e.getValue().get()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public static void main(String[] args) {
        UsernameAvailabilityService service = new UsernameAvailabilityService();

        service.registerUsername("john_doe", 1L);
        service.registerUsername("admin", 2L);

        System.out.println(service.checkAvailability("john_doe")); // false
        System.out.println(service.checkAvailability("jane_smith")); // true

        System.out.println(service.suggestAlternatives("john_doe"));

        for (int i = 0; i < 10543; i++) {
            service.checkAvailability("admin");
        }

        System.out.println(service.getMostAttempted()); // admin
    }
}
