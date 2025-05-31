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

        /*
        This is to handle comparison scenarios like:
        PDFREPORT-2.3.4 and 1.2.3 where PDFREPORT is treated as an unknown low priority qualifier
        */
        if (mismatchInSuffixVersion(rev1.getName(), rev2.getName())) {
            comparableVersion1 = new ComparableVersion(getNumberVersion(rev1.getName()));
            comparableVersion2 = new ComparableVersion(getNumberVersion(rev2.getName()));
            comparisonResult = comparableVersion2.compareTo(comparableVersion1);
            if (comparisonResult == 0) {
                comparableVersion1 = new ComparableVersion(rev1.getName());
                comparableVersion2 = new ComparableVersion(rev2.getName());
                comparisonResult = comparableVersion1.compareTo(comparableVersion2);
            }
        }

        return comparisonResult;
    }

    protected boolean mismatchInSuffixVersion(String ver1, String ver2) {
        String[] splittedVer1 = ver1.split("-");
        String[] splittedVEr2 = ver2.split("-");
        if (splittedVer1.length != splittedVEr2.length) {
            return true;
        }
        return false;
    }

    /**
     * For the cases like this:
     * PDFREPORT-2.3.4
     * return this
     * 2.3.4
     */
    protected String getNumberVersion(String firstV) {
        String res = firstV;
        if (firstV.contains("-")) {
            String[] splittedVersion = firstV.split("-");
            if (splittedVersion.length > 1) {
                res = splittedVersion[1];
            }
        }

        return res;
    }
}
