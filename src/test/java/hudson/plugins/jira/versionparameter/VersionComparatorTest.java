package hudson.plugins.jira.versionparameter;

import com.atlassian.jira.rest.client.api.domain.Version;
import hudson.plugins.jira.versionparameter.VersionComparator;

import static org.hamcrest.MatcherAssert.assertThat;
import org.hamcrest.collection.ArrayMatching;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VersionComparatorTest
{

    @Test
    public void complexCompare() {
        String[] input =
            {   "9.9.9.9.9", //
                "V-5.2.3", //
                "PDFREPORT-2.3.4", //
                "PDFREPORT-2.3", //
                "1.12.2.3.4", //
                "1.3.4", //
                "1.1.1.2", //
                "VER 1.0", //
                "1.1.1.1" };

        String[] expected =
            {   "9.9.9.9.9", //
                "V-5.2.3", //
                "PDFREPORT-2.3.4", //
                "PDFREPORT-2.3", //
                "1.12.2.3.4", //
                "1.3.4", //
                "1.1.1.2", //
                "1.1.1.1", //
                "VER 1.0" };

        List<String> result =
            Arrays.asList(input).stream().
                map( s -> new Version(null,null, s, null, false, false, null) ).
                sorted( VersionComparator.INSTANCE ).
                map( version -> version.getName() ).
                collect( Collectors.toList());
        assertThat(expected, ArrayMatching.arrayContaining( result.toArray( new String[result.size()] ) ));

    }

    @Test
    public void singleComparisonsTests() {

        assertEquals(0, compare("1.1.1.1", "1.1.1.1"));
        assertEquals( -1, compare("A-1.1.1.1","1.1.1.1"));
        assertEquals(1, compare("1.1.1.1","A-1.1.1.1"));
        assertEquals(1, compare("1.1.1.1","1.1.1.1.1"));
        assertEquals(-1, compare("1.1.1.1","1.1.1"));
        assertEquals(1, compare("1.1.1.2","1.1.1.3"));
        assertEquals(-1, compare("2.2.2.1","1.1.1.2"));
        assertEquals(-1, compare( "2.2.2", "1.1.1.2"));
        assertEquals( -1, compare( "2.0", "1.0.15.3"));
        assertEquals(1, compare( "2.0.5.4", "4.0"));
        assertEquals(-1, compare( "1.12.1.1", "1.1.1.2"));
        assertEquals(1, compare( "1.1.1-RC1", "1.1.1-RC2"));
        assertEquals(-1, compare( "PDFREPORT-2.3.4", "1.2.3"));
        assertEquals(1, compare( "PDFREPORT-2.3.4", "4.5.6"));
        assertEquals(-1, compare( "PDFREPORT-2.3.4", "x"));

    }

    private int compare(String v1, String v2) {
        return VersionComparator.INSTANCE. //
            compare( //
                new Version(null, null, v1, null, false, false, null), //
                new Version(null, null, v2, null, false, false, null) //
            );
    }


    @Test
    public void getNumberVersionTest() {
        assertEquals( "2.3.4", VersionComparator.INSTANCE.getNumberVersion( "PDFREPORT-2.3.4" ) );
    }
}
