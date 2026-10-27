package com.example.rewardspoints.shared;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guard for the plan to lift {@code shared} into its own library: it may depend on frameworks,
 * never on the application's own packages outside {@code shared}.
 */
class SharedPackageIndependenceTest {

    private static final String APP_PACKAGE = "com.example.rewardspoints.";
    private static final String SHARED_PACKAGE = APP_PACKAGE + "shared.";

    @Test
    void sharedSources_doNotImportApplicationCode() throws IOException {
        Path root = Path.of("src/main/java/com/example/rewardspoints/shared");

        try (Stream<Path> files = Files.walk(root)) {
            List<String> violations = files
                    .filter(p -> p.toString().endsWith(".java"))
                    .flatMap(SharedPackageIndependenceTest::offendingImports)
                    .toList();

            assertThat(violations).as("shared must not depend on feature modules").isEmpty();
        }
    }

    private static Stream<String> offendingImports(Path file) {
        try {
            return Files.readAllLines(file).stream()
                    .map(String::strip)
                    .filter(l -> l.startsWith("import ") && l.contains(APP_PACKAGE) && !l.contains(SHARED_PACKAGE))
                    .map(l -> file.getFileName() + ": " + l);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
