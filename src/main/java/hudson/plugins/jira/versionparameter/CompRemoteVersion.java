package hudson.plugins.jira.versionparameter;

import hudson.plugins.jira.soap.RemoteVersion;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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
public class CompRemoteVersion implements Comparator<RemoteVersion> {

    public int compare(RemoteVersion rev1, RemoteVersion rev2) {
        int result = 0;
        boolean startWithoutLetters = true;
        List<String> listRev1 = Arrays.asList(rev1.getName().split("\\."));
        String oldRev1 = listRev1.get(0);
        List<String> listRev2 = Arrays.asList(rev2.getName().split("\\."));
        String oldRev2 = listRev2.get(0);
        listRev1.set(0, getNumberVersion(listRev1.get(0)));
        listRev2.set(0, getNumberVersion(listRev2.get(0)));
        if (oldRev1.equals(listRev1.get(0)) && oldRev2.equals(listRev2.get(0))) {
            startWithoutLetters = false;
        }
        int lenRev1 = listRev1.size();
        int lenRev2 = listRev2.size();
        Integer min = Math.min(lenRev1, lenRev2);
        for (int i = 0; i < min; i++) {
            String s1 = listRev1.get(i);
            String s2 = listRev2.get(i);
            try {
                Integer coor1 = Integer.parseInt(s1);
                Integer coor2 = Integer.parseInt(s2);
                result = coor2.compareTo(coor1);
                if (result != 0) {
                    break;
                }
            } catch (Exception e) {
            }
        }
        if (result == 0) {
            if (lenRev1 > lenRev2) {
                result = -1;
            } else if (lenRev2 > lenRev1) {
                result = 1;
            } else if (startWithoutLetters) {
                result = 1;
            }
        }
        return result;
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
