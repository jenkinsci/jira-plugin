package hudson.plugins.jira;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test unmasking email. Eg: <code>john dot doe at example dot com</code> to <code>john.doe@example.com</code>
 *
 * @author Honza Brázdil <jbrazdil@redhat.com>
 */
public class UnmaskMailTest {

    @Test
    public void unmaskMailTest() {
        test("user@example.com", "user at example dot com");
        test("user@example.com", "user AT example DOT com");
        test("user@example.com", "user aT example dOt com");
        test("user@example.com", "user At example d0t com");
        test("user@example.com", "user at example D0t com");

        test("user@example.com", "user[at]example[dot]com");
        test("user@example.com", "user{AT}example{DOT}com");
        test("user@example.com", "user<aT>example<dOt>com");
        test("user@example.com", "user\"At\"example\"d0t\"com");
        test("user@example.com", "user_at_example_D0t_com");
        test("user@example.com", "user(at)example(dot)com");

        test("user@example.com", "user [at] example [dot] com");
        test("user@example.com", "user {AT} example {DOT} com");
        test("user@example.com", "user <aT> example <dOt> com");
        test("user@example.com", "user \"At\" example \"d0t\" com");
        test("user@example.com", "user _at_ example _D0t_ com");
        test("user@example.com", "user (at) example (dot) com");

        test("john.doe.junior@my.site.eu", "john dot doe DOT junior at my dOt site D0T eu");
        test("john.doe.junior@my.site.eu", "john(dot)doe[DOT]junior{at}my<dOt>site_D0T_eu");
        test("john.doe.junior@my.site.eu", "john (dot) doe [DOT] junior {at} my <dOt> site _D0T_ eu");

        test("atdot@dotat.com", "atdot at dotat dot com");
        test("atdot@dotat.com", "atdot AT dotat DOT com");
        test("atdot@dotat.com", "atdot aT dotat dOt com");
        test("atdot@dotat.com", "atdot At dotat d0t com");
        test("atdot@dotat.com", "atdot at dotat D0t com");
    }

    private void test(String expected, String masked) {
        Assert.assertEquals(expected, JiraMailAddressResolver.unmaskEmail(masked));
    }

}
