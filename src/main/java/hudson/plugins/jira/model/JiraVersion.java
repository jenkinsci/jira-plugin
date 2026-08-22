package hudson.plugins.jira.model;

import com.atlassian.jira.rest.client.api.domain.Version;
import hudson.plugins.jira.extension.ExtendedVersion;
import java.util.Calendar;
import java.util.Objects;

public class JiraVersion implements Comparable<JiraVersion> {

    private final String name;
    private final String description;
    private final Calendar startDate;
    private final Calendar releaseDate;
    private final boolean released;
    private final boolean archived;

    public JiraVersion(String name, Calendar releaseDate, boolean released, boolean archived) {
        this.name = name;
        this.description = null;
        this.startDate = null;
        this.releaseDate = releaseDate;
        this.released = released;
        this.archived = archived;
    }

    @Deprecated
    public JiraVersion(String name, Calendar startDate, Calendar releaseDate, boolean released, boolean archived) {
        this.name = name;
        this.description = null;
        this.startDate = startDate;
        this.releaseDate = releaseDate;
        this.released = released;
        this.archived = archived;
    }

    public JiraVersion(
            String name,
            String description,
            Calendar startDate,
            Calendar releaseDate,
            boolean released,
            boolean archived) {
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.releaseDate = releaseDate;
        this.released = released;
        this.archived = archived;
    }

    public JiraVersion(Version version) {
        this(
                version.getName(),
                version.getReleaseDate() == null
                        ? null
                        : version.getReleaseDate().toGregorianCalendar(),
                version.isReleased(),
                version.isArchived());
    }

    public JiraVersion(ExtendedVersion version) {
        this(
                version.getName(),
                version.getDescription(),
                version.getStartDate() == null ? null : version.getStartDate().toGregorianCalendar(),
                version.getReleaseDate() == null
                        ? null
                        : version.getReleaseDate().toGregorianCalendar(),
                version.isReleased(),
                version.isArchived());
    }

    @Override
    public int compareTo(JiraVersion that) {
        int result = this.releaseDate.compareTo(that.releaseDate);
        if (result == 0) {
            return this.name.compareTo(that.name);
        }
        return result;
    }

    @Override
    public int hashCode() {
        // Must cover exactly the fields equals() compares. startDate used to be missing, so two versions
        // that differ only in their start date - which is what you get from the Version and the
        // ExtendedVersion constructor for one and the same Jira version - compared unequal while hashing
        // to the same bucket.
        return Objects.hash(name, startDate, releaseDate, released, archived);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        JiraVersion other = (JiraVersion) obj;
        if (archived != other.archived) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (startDate == null) {
            if (other.startDate != null) {
                return false;
            }
        } else if (!startDate.equals(other.startDate)) {
            return false;
        }
        if (releaseDate == null) {
            if (other.releaseDate != null) {
                return false;
            }
        } else if (!releaseDate.equals(other.releaseDate)) {
            return false;
        }
        if (released != other.released) {
            return false;
        }
        return true;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Calendar getStartDate() {
        return startDate;
    }

    public Calendar getReleaseDate() {
        return releaseDate;
    }

    public boolean isReleased() {
        return released;
    }

    public boolean isArchived() {
        return archived;
    }
}
