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

    public int compare(Version rev1, Version rev2) {
        ComparableVersion comparableVersion1 = new ComparableVersion(getNumberVersion(rev1.getName()));
        ComparableVersion comparableVersion2 = new ComparableVersion(getNumberVersion(rev2.getName()));
        int comparisonResult = comparableVersion2.compareTo(comparableVersion1);
        if (comparisonResult == 0) {
            comparableVersion1 = new ComparableVersion(rev1.getName());
            comparableVersion2 = new ComparableVersion(rev2.getName());
            comparisonResult = comparableVersion1.compareTo(comparableVersion2);
        }
        return comparisonResult;
    }

    /**
     * For the cases like this:
     * PDFREPORT-2.3.4
     * return this
     * 2.3.4
     */
    private String getNumberVersion(String firstV) {
        String res = firstV;
        if (!firstV.matches("[0-9.]+") && firstV.contains("-")) {
            res = firstV.split("-")[1];
        }

        return res;
    }

}
