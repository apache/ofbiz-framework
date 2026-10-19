package demo.adapters.rest;

import demo.application.GreetingService;
import demo.domain.Greeting;
import demo.domain.Greetings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {

    @Bean
    Greetings greetings() {
        return name -> new Greeting("hello " + name);
    }

    @Bean
    GreetingService greetingService(Greetings greetings) {
        return new GreetingService(greetings);
    }

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
