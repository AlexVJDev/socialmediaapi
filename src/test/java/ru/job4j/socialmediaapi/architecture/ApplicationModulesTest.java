package ru.job4j.socialmediaapi.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.stereotype.Controller;
import ru.job4j.socialmediaapi.SocialMediaApiApplication;

import java.util.TreeSet;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Architecture rules of the modular monolith, described in {@code docs/architecture.md}.
 * Spring Modulith checks module boundaries, ArchUnit checks layers inside every module.
 */
@DisplayName("Архитектура: модули Spring Modulith и слои внутри модулей")
class ApplicationModulesTest {

    /** Base package of the application: business modules are its direct subpackages. */
    private static final String BASE_PACKAGE = SocialMediaApiApplication.class.getPackageName();

    /**
     * Packages not yet moved into modules (see «Переходный период» in {@code docs/architecture.md}).
     * TODO удалить после переноса: каждый перенесённый пакет убирать из списка.
     */
    private static final String[] LEGACY_PACKAGES = {
            BASE_PACKAGE + ".adapter.."
    };

    /** Production classes of the application, without tests. */
    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE_PACKAGE);

    @Test
    @DisplayName("Spring Modulith: нет циклов, обращения к модулям только через api из allowedDependencies")
    void verifiesModuleStructure() {
        ApplicationModules.of(SocialMediaApiApplication.class).verify();
    }

    @Test
    @DisplayName("Слои модуля: domain ← application ← infrastructure")
    void verifiesLayerDependenciesInAllModules() {
        var rule = layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .withOptionalLayers(true)
                .layer("Domain").definedBy(BASE_PACKAGE + ".*.internal.domain..")
                .layer("Application").definedBy(BASE_PACKAGE + ".*.internal.application..")
                .layer("Infrastructure").definedBy(BASE_PACKAGE + ".*.internal.infrastructure..")
                .whereLayer("Domain").mayNotAccessAnyLayer()
                .whereLayer("Application").mayOnlyAccessLayers("Domain")
                .whereLayer("Infrastructure").mayOnlyAccessLayers("Application", "Domain")
                .as("""
                    Правила зависимостей между слоями:
                      domain не зависит от application и infrastructure.
                      application может зависеть от domain, но не от infrastructure.
                      infrastructure может зависеть от application и domain.
                      Зависимости внутри одного слоя разрешены.""");

        assertNoViolations(rule);
    }

    @Test
    @DisplayName("Модели хранения *Row лежат в infrastructure.persistence.row")
    void persistenceRowsBelongToPersistence() {
        var rule = classes().that().haveSimpleNameEndingWith("Row")
                .and().resideOutsideOfPackage(BASE_PACKAGE + ".shared..")
                .and().resideOutsideOfPackages(LEGACY_PACKAGES)
                .should().resideInAPackage(BASE_PACKAGE + ".*.internal.infrastructure.persistence.row")
                .allowEmptyShould(true)
                .as("""
                    Правило размещения моделей хранения:
                      Классы *Row (записи, повторяющие строку таблицы) должны находиться
                      в infrastructure.persistence.row.
                      В api, domain и application модели хранения запрещены.""");

        assertNoViolations(rule);
    }

    @Test
    @DisplayName("Контроллеры лежат в infrastructure.web")
    void controllersBelongToWebInfrastructure() {
        var rule = classes().that().areAnnotatedWith(Controller.class)
                .or().areMetaAnnotatedWith(Controller.class)
                .and().resideOutsideOfPackage(BASE_PACKAGE + ".shared..")
                .and().resideOutsideOfPackages(LEGACY_PACKAGES)
                .should().resideInAPackage(BASE_PACKAGE + ".*.internal.infrastructure.web..")
                .allowEmptyShould(true)
                .as("""
                    Правило размещения контроллеров:
                      Классы с @Controller или его составными аннотациями, включая @RestController,
                      должны находиться в infrastructure.web.
                      В api, domain и application контроллеры запрещены.""");

        assertNoViolations(rule);
    }

    @Test
    @DisplayName("Каждый класс модуля лежит в api или internal.{domain,application,infrastructure}")
    void moduleClassesBelongToArchitecturalLayers() {
        var rule = classes().that().resideInAPackage(BASE_PACKAGE + ".*..")
                .and().resideOutsideOfPackage(BASE_PACKAGE + ".shared..")
                .and().resideOutsideOfPackages(LEGACY_PACKAGES)
                .and().doNotHaveSimpleName("package-info")
                .should().resideInAnyPackage(
                        BASE_PACKAGE + ".*.api..",
                        BASE_PACKAGE + ".*.internal.domain..",
                        BASE_PACKAGE + ".*.internal.application..",
                        BASE_PACKAGE + ".*.internal.infrastructure..")
                .allowEmptyShould(true)
                .as("""
                    Правило размещения классов бизнес-модулей:
                      Каждый класс должен находиться в api, internal.domain,
                      internal.application или internal.infrastructure.""");

        assertNoViolations(rule);
    }

    @Test
    @DisplayName("Domain не зависит от Spring, HTTP и хранения")
    void domainHasOnlyBusinessDependencies() {
        var rule = classes().that().resideInAPackage(BASE_PACKAGE + ".*.internal.domain..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        BASE_PACKAGE + ".*.internal.domain..", BASE_PACKAGE + ".*.api..",
                        BASE_PACKAGE + ".shared.exception", "java..", "lombok..")
                .andShould().onlyDependOnClassesThat().resideOutsideOfPackage("java.sql..")
                .allowEmptyShould(true)
                .as("""
                    Правило зависимостей domain:
                      Разрешены domain, публичный api, общие ошибки, стандартные типы Java и Lombok.
                      Spring, HTTP, хранение и java.sql в domain запрещены.
                      Межмодульные зависимости дополнительно проверяет Spring Modulith.""");

        assertNoViolations(rule);
    }

    @Test
    @DisplayName("Публичный api не раскрывает реализацию модуля")
    void publicApiDoesNotExposeImplementation() {
        var rule = classes().that().resideInAPackage(BASE_PACKAGE + ".*.api..")
                .should().onlyDependOnClassesThat().resideInAnyPackage(
                        BASE_PACKAGE + ".*.api..", BASE_PACKAGE + ".shared.exception", "java..", "lombok..",
                        "org.springframework.modulith..", "com.fasterxml.jackson.annotation..")
                .andShould().onlyDependOnClassesThat().resideOutsideOfPackage("java.sql..")
                .allowEmptyShould(true)
                .as("""
                    Правило зависимостей публичного api:
                      Разрешены публичные API, общие ошибки, стандартные типы Java,
                      Lombok и аннотации Spring Modulith и Jackson.
                      Внутренняя реализация, модели хранения, HTTP-типы и java.sql запрещены.""");

        assertNoViolations(rule);
    }

    @Test
    @DisplayName("Web-адаптеры вызывают сценарии, а не persistence и порты")
    void webAdaptersUseApplicationScenarios() {
        var rule = noClasses().that().resideInAPackage(BASE_PACKAGE + ".*.internal.infrastructure.web..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE_PACKAGE + ".*.internal.infrastructure.persistence..",
                        BASE_PACKAGE + ".*.internal.application.port")
                .allowEmptyShould(true)
                .as("""
                    Правило зависимостей web-адаптеров:
                      Контроллеры и остальные web-адаптеры обращаются к прикладным сценариям.
                      Прямые зависимости от persistence и application.port запрещены.
                      Модели запросов и результатов из application.port.model разрешены.""");

        assertNoViolations(rule);
    }

    @Test
    @DisplayName("Application обращается к хранилищу и HTTP только через порты")
    void applicationUsesPortsForTechnicalOperations() {
        var rule = noClasses().that().resideInAPackage(BASE_PACKAGE + ".*.internal.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "java.sql..", "org.postgresql..", "org.springframework.jdbc..",
                        "org.springframework.dao..", "org.springframework.web..",
                        "org.springframework.http..", "jakarta.servlet..")
                .allowEmptyShould(true)
                .as("""
                    Правило технических зависимостей application:
                      Хранение и внешние вызовы выполняются через прикладные порты.
                      Прямые зависимости от JDBC, драйвера PostgreSQL, исключений Spring DAO
                      и HTTP запрещены.
                      Публичные API других модулей разрешены в рамках allowedDependencies.""");

        assertNoViolations(rule);
    }

    private static void assertNoViolations(ArchRule rule) {
        var result = rule.evaluate(CLASSES);
        var violations = new TreeSet<String>();
        result.handleViolations((objects, message) -> {
            for (var object : objects) {
                if (object instanceof Dependency dependency) {
                    violations.add("  %s%n    -> %s".formatted(
                            describeClass(dependency.getOriginClass()),
                            describeClass(dependency.getTargetClass())));
                } else if (object instanceof JavaClass type) {
                    violations.add("  " + describeClass(type));
                } else {
                    violations.add(message);
                }
            }
        });

        assertThat(result.hasViolation())
                .withFailMessage("%s%n%nНарушения архитектуры:%n%s",
                        rule.getDescription(), String.join("\n", violations))
                .isFalse();
    }

    private static String describeClass(JavaClass type) {
        String className = type.getName().substring(type.getPackageName().length() + 1);
        String packageName = type.getPackageName();
        if (packageName.startsWith(BASE_PACKAGE + ".")) {
            packageName = packageName.substring(BASE_PACKAGE.length() + 1).replace(".internal.", "/");
        }
        return "%s [%s]".formatted(className, packageName);
    }
}
