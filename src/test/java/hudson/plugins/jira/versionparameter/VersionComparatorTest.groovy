package hudson.plugins.jira.versionparameter

import com.atlassian.jira.rest.client.api.domain.Version
import spock.lang.*

class VersionComparatorTest extends Specification {

    void testComplexCompare() {
        def input = [
                "9.9.9.9.9",
                "V-5.2.3",
                "PDFREPORT-2.3.4",
                "PDFREPORT-2.3",
                "1.12.2.3.4",
                "1.3.4",
                "1.1.1.2",
                "1.1.1.1"
        ]
        def expected = [
                "1.1.1.1",
                "1.1.1.2",
                "1.3.4",
                "1.12.2.3.4",
                "9.9.9.9.9",
                "PDFREPORT-2.3",
                "PDFREPORT-2.3.4",
                "V-5.2.3",
        ]

        SortedSet<Version> sorted = new TreeSet<Version>(new VersionComparator());
        sorted.addAll(input);

        assert sorted == expected;
    }

    def singleComparisonsTests() {

        given:
        def comparator = new VersionComparator();

        when:
        def o1 = new Version(null, null, v1, null, v1arch, v1rel, null)
        def o2 = new Version(null, null, v2, null, v2arch, v2rel, null)

        then:
        comparator.compare(o1, o2) == expected

        where:
        v1                | v1rel | v1arch | v2          | v2rel | v2arch | expected
        "1.1.1.1"         | false | false  | "1.1.1.1"   | false | false  | 0
        "A-1.1.1.1"       | false | false  | "1.1.1.1"   | false | false  | -1
        "1.1.1.1"         | false | false  | "A-1.1.1.1" | false | false  | 1
        "1.1.1.1"         | false | false  | "1.1.1.1.1" | false | false  | 1
        "1.1.1.1"         | false | false  | "1.1.1"     | false | false  | -1
        "1.1.1.2"         | false | false  | "1.1.1.3"   | false | false  | 1
        "2.2.2.1"         | false | false  | "1.1.1.2"   | false | false  | -1
        "2.2.2"           | false | false  | "1.1.1.2"   | false | false  | -1
        "2.0"             | false | false  | "1.0.15.3"  | false | false  | -1
        "2.0.5.4"         | false | false  | "4.0"       | false | false  | 1
        "1.12.1.1"        | false | false  | "1.1.1.2"   | false | false  | -1
        "1.1.1-RC1"       | false | false  | "1.1.1-RC2" | false | false  | 1
        "PDFREPORT-2.3.4" | false | false  | "1.2.3"     | false | false  | -1
        "PDFREPORT-2.3.4" | false | false  | "4.5.6"     | false | false  | 1
        "PDFREPORT-2.3.4" | false | false  | "x"         | false | false  | -1 //exception

    }

    def getNumberVersionTest() {
        given:
        def comp = new VersionComparator();

        expect:
        comp.getNumberVersion(v) == expect

        where:
        v                 | expect
        "PDFREPORT-2.3.4" | "2.3.4"

    }

}
