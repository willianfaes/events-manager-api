package dev.will.eventsmanager;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "dev.will.eventsmanager", importOptions = ImportOption.OnlyIncludeTests.class)
class TestConventionsArchTest {

    @ArchTest
    static final ArchRule jpaTestsShareOneContext = classes()
            .that().haveSimpleNameEndingWith("JpaTest")
            .should().beAssignableTo(AbstractJpaTest.class)
            .because("a JPA test with its own configuration builds its own context and connection pool (#108)");
}
