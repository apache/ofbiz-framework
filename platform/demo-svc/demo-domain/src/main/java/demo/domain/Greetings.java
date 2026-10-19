package demo.domain;

/** Port owned by the domain; adapters provide the implementation (hexagonal). */
public interface Greetings {
    Greeting forName(String name);
}
