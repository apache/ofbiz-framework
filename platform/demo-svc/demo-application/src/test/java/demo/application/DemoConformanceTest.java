package demo.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import demo.domain.Greeting;
import ofbiz.arch.OfbizArchitectureRules;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * P0-F4-S4 evidence: the shared layering rule is green on this clean module
 * and provably RED on a violation.
 */
class DemoConformanceTest {

    @Nested
    class Layering {
        @Test
        void cleanModulePasses() {
            JavaClasses classes = new ClassFileImporter().importPackages("demo");
            OfbizArchitectureRules.layeredArchitecture().check(classes);
            OfbizArchitectureRules.noFrameworkDependenciesInDomain().check(classes);
        }

        @Test
        void violationIsRejected() {
            // Fixture lives in ..application.. but depends on ..adapters.. — inward-only rule must fire.
            JavaClasses bad = new ClassFileImporter()
                .importPackages("violation.application", "violation.adapters");
            assertThrows(AssertionError.class,
                () -> OfbizArchitectureRules.layeredArchitecture().check(bad));
        }
    }

    @Nested
    class Behavior {
        @Test
        void blankNameRejected() {
            GreetingService service = new GreetingService(name -> new Greeting("hi " + name));
            assertThrows(IllegalArgumentException.class, () -> service.greet("  "));
        }

        @Test
        void greetsByName() {
            GreetingService service = new GreetingService(name -> new Greeting("hello " + name));
            assertEquals("hello kadir", service.greet("kadir").message());
        }
    }
}
