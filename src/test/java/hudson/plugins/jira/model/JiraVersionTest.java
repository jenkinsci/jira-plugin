package hudson.plugins.jira.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
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
}
