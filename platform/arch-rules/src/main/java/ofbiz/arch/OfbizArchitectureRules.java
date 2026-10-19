package ofbiz.arch;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.Architectures;

/**
 * Shared layering rules for all services (FUTURESTATE-LOWLEVEL §1, ADR-007):
 * dependencies only inward: adapters|infrastructure -> application -> domain.
 *
 * Every module applies these from an ArchUnit test; a module without the test
 * is not conformant (P0-F4-S4).
 */
public final class OfbizArchitectureRules {

    private OfbizArchitectureRules() {}

    /** Domain purity: no Spring, no JPA, no Hibernate (ADR-007). */
    public static ArchRule noFrameworkDependenciesInDomain() {
        return ArchRuleDefinition.noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat(DescribedPredicate.describe("Spring or JPA types",
                (JavaClass input) -> input.getPackageName().startsWith("org.springframework")
                    || input.getPackageName().startsWith("jakarta.persistence")
                    || input.getPackageName().startsWith("org.hibernate")))
            .because("the domain layer must stay pure Java with zero framework dependencies (ADR-007)");
    }

    /** Layered architecture check across a service's packages. */
    public static ArchRule layeredArchitecture() {
        return Architectures.layeredArchitecture().consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("Domain").definedBy("..domain..")
            .layer("Application").definedBy("..application..")
            .layer("Adapters").definedBy("..adapters..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .whereLayer("Adapters").mayNotBeAccessedByAnyLayer()
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyAccessLayers("Domain")
            .because("dependencies must point inward: adapters|infrastructure -> application -> domain");
    }
}
