package seedu.snap.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Verifies the complete canonical-command and short-alias mapping. */
class CommandAliasesTest {
    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("aliases")
    void everyAlias_mapsToItsCanonicalCommand(String canonical, String alias) {
        assertEquals(canonical, CommandAliases.canonicalCommand(alias));
        assertEquals(alias, CommandAliases.aliasFor(canonical));
        assertEquals(canonical + " sample/argument", CommandAliases.normalizeInput(alias + " sample/argument"));
    }

    @Test
    void canonicalCommandsAndUnknownTokens_areUnchanged() {
        assertEquals("add-eqm sample/argument", CommandAliases.normalizeInput("  add-eqm   sample/argument  "));
        assertEquals("unknown sample/argument", CommandAliases.normalizeInput("unknown sample/argument"));
        assertEquals("help", CommandAliases.normalizeInput("h"));
        assertNull(CommandAliases.normalizeInput(null));
        assertNull(CommandAliases.aliasFor("unknown"));
    }

    /** Supplies every supported command alias so omissions cannot pass unnoticed. */
    private static Stream<Arguments> aliases() {
        return Stream.of(
                Arguments.of("add-eqm", "ae"), Arguments.of("list-eqm", "le"),
                Arguments.of("eqm-show", "es"), Arguments.of("eqm-delete", "ed"),
                Arguments.of("filter-eqm", "fe"), Arguments.of("eqm-update", "eu"),
                Arguments.of("eqm-maintain", "em"), Arguments.of("eqm-loan-history", "elh"),
                Arguments.of("add-loan", "al"), Arguments.of("loan-return", "lr"),
                Arguments.of("loan-delete", "ld"), Arguments.of("loan-update", "lu"),
                Arguments.of("list-loan", "ll"), Arguments.of("loan-show", "ls"),
                Arguments.of("list-loan-borrower", "llb"), Arguments.of("list-loan-overdue", "llo"),
                Arguments.of("add-bundle", "ab"), Arguments.of("bundle-add-eqm", "bae"),
                Arguments.of("bundle-remove-eqm", "bre"), Arguments.of("list-bundle", "lb"),
                Arguments.of("bundle-history", "bh"), Arguments.of("backup", "bk"),
                Arguments.of("restore", "rs"), Arguments.of("help", "h"), Arguments.of("exit", "x"));
    }
}
