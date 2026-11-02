// @@author yarnieee
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
        assertEquals("eqm-add   sample/argument", CommandAliases.normalizeInput("  eqm-add   sample/argument  "));
        assertEquals("unknown sample/argument", CommandAliases.normalizeInput("unknown sample/argument"));
        assertEquals("help", CommandAliases.normalizeInput("h"));
        assertNull(CommandAliases.normalizeInput(null));
        assertNull(CommandAliases.aliasFor("unknown"));
    }

    /** Supplies every supported command alias so omissions cannot pass unnoticed. */
    private static Stream<Arguments> aliases() {
        return Stream.of(
                Arguments.of("eqm-add", "ae"), Arguments.of("eqm-list", "le"),
                Arguments.of("eqm-show", "es"), Arguments.of("eqm-delete", "ed"),
                Arguments.of("eqm-filter", "fe"), Arguments.of("eqm-update", "eu"),
                Arguments.of("eqm-maintain", "em"), Arguments.of("eqm-loan-history", "elh"),
                Arguments.of("loan-add", "al"), Arguments.of("loan-return", "lr"),
                Arguments.of("loan-delete", "ld"), Arguments.of("loan-update", "lu"),
                Arguments.of("loan-list", "ll"), Arguments.of("loan-view", "lv"), Arguments.of("loan-show", "ls"),
                Arguments.of("loan-list-borrower", "llb"), Arguments.of("loan-list-overdue", "llo"),
                Arguments.of("bundle-create", "ab"), Arguments.of("bundle-add-eqm", "bae"),
                Arguments.of("bundle-remove-eqm", "bre"), Arguments.of("bundle-list", "lb"),
                Arguments.of("bundle-history", "bh"), Arguments.of("backup", "bk"),
                Arguments.of("restore", "rs"), Arguments.of("help", "h"), Arguments.of("exit", "x"));
    }
}
// @@author
