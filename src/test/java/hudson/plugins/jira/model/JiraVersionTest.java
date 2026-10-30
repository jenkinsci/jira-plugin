package hudson.plugins.jira.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JiraVersionTest {

    private static final Calendar START = new GregorianCalendar(2026, Calendar.JANUARY, 1);
    private static final Calendar RELEASE = new GregorianCalendar(2026, Calendar.FEBRUARY, 1);

    @Test
    void equalVersionsShareAHashCode() {
        JiraVersion one = new JiraVersion("1.0", "notes", START, RELEASE, true, false);
        JiraVersion other = new JiraVersion("1.0", "different notes", START, RELEASE, true, false);

        assertEquals(one, other);
        assertEquals(one.hashCode(), other.hashCode());
    }

    @Test
    void startDateTakesPartInTheHashCodeAsItDoesInEquals() {
        JiraVersion withStartDate = new JiraVersion("1.0", null, START, RELEASE, true, false);
        JiraVersion withoutStartDate = new JiraVersion("1.0", null, null, RELEASE, true, false);

        assertNotEquals(withStartDate, withoutStartDate);
        // hashCode used to ignore startDate, so these two compared unequal yet hashed identically
        assertNotEquals(withStartDate.hashCode(), withoutStartDate.hashCode());
    }

    @Test
    void aVersionCanBeFoundInAHashSetByAnEqualInstance() {
        Set<JiraVersion> versions = new HashSet<>();
        versions.add(new JiraVersion("1.0", "notes", START, RELEASE, true, false));

        assertTrue(versions.contains(new JiraVersion("1.0", "notes", START, RELEASE, true, false)));
    }

    @Test
    void versionsWithoutAReleaseDateSortLastInsteadOfThrowing() {
        JiraVersion released = new JiraVersion("1.0", null, START, RELEASE, true, false);
        JiraVersion unreleased = new JiraVersion("2.0", null, START, null, false, false);

        List<JiraVersion> versions = new ArrayList<>(Arrays.asList(unreleased, released));

        // compareTo used to dereference releaseDate, which two of the constructors set to null.
        Collections.sort(versions);

        assertEquals(Arrays.asList(released, unreleased), versions);
    }

    @Test
    void versionsWithTheSameReleaseDateAreOrderedByStartDateThenName() {
        JiraVersion early = new JiraVersion("b", null, START, RELEASE, true, false);
        JiraVersion late = new JiraVersion("a", null, RELEASE, RELEASE, true, false);

        assertTrue(early.compareTo(late) < 0);
        assertEquals(0, early.compareTo(new JiraVersion("b", "other notes", START, RELEASE, true, false)));
    }

    @Test
    void theFourArgConstructorLeavesDescriptionAndStartDateNull() {
        // This is what the JIRA-RPC-derived Version constructor path produces.
        JiraVersion version = new JiraVersion("1.0", RELEASE, true, false);

        assertNull(version.getDescription());
        assertNull(version.getStartDate());
        assertEquals(RELEASE, version.getReleaseDate());
    }

    @Test
    @SuppressWarnings("deprecation")
    void theDeprecatedFiveArgConstructorHonorsStartDateButLeavesDescriptionNull() {
        JiraVersion version = new JiraVersion("1.0", START, RELEASE, true, false);

        assertNull(version.getDescription());
        assertEquals(START, version.getStartDate());
        assertEquals(RELEASE, version.getReleaseDate());
    }
}
