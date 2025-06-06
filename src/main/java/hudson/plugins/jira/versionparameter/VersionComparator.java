package hudson.plugins.jira.versionparameter;

import com.atlassian.jira.rest.client.api.domain.Version;
import java.util.Comparator;
import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * This comparator can ordering the following formats versions:
 * 9.9.9.9.9
 * V-5.2.3
 * PDFREPORT-2.3.4
 * PDFREPORT-2.3
 * 1.12.2.3.4
 * 1.3.4
 * 1.1.1.2
 * 1.1.1.1
 */
public class VersionComparator implements Comparator<Version> {

    public static final VersionComparator INSTANCE = new VersionComparator();

    @Override
    public int compare(Version rev1, Version rev2) {
        ComparableVersion comparableVersion1 = new ComparableVersion(rev1.getName());
        ComparableVersion comparableVersion2 = new ComparableVersion(rev2.getName());
        int comparisonResult = comparableVersion2.compareTo(comparableVersion1);
        if (comparisonResult > 0) {
            return 1;
        } else if (comparisonResult < 0) {
            return -1;
        } else return 0;
    }
}
