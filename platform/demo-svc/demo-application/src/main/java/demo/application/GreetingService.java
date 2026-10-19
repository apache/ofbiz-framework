package demo.application;

import demo.domain.Greeting;
import demo.domain.Greetings;

/** Application use case: orchestrates domain ports. Depends inward only. */
public class GreetingService {

    private final Greetings greetings;

    public GreetingService(Greetings greetings) {
        this.greetings = greetings;
    }

    public Greeting greet(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return greetings.forName(name.trim());
    }
}
