package hudson.plugins.jira;

import hudson.util.FormValidation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

/**
 * Created by warden on 14.09.15.
 */
public class DescriptorImplTest {

//    @Rule
//    JenkinsRule rule = new JenkinsRule();

    JiraSite.DescriptorImpl descriptor = new JiraSite.DescriptorImpl();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testDoValidate() throws Exception {
        FormValidation validation = descriptor.doValidate(null, null, null, null, false, null, JiraSite.DEFAULT_TIMEOUT);
        assertEquals(FormValidation.Kind.ERROR, validation.kind);

        validation = descriptor.doValidate("invalid", null, null, null, false, null, JiraSite.DEFAULT_TIMEOUT);
        assertEquals(FormValidation.Kind.ERROR, validation.kind);

        validation = descriptor.doValidate("http://valid/", null, null, null, false, "invalid", JiraSite.DEFAULT_TIMEOUT);
        assertEquals(FormValidation.Kind.ERROR, validation.kind);

        validation = descriptor.doValidate("http://valid/", null, null, null, false, " ", JiraSite.DEFAULT_TIMEOUT);
        assertEquals(FormValidation.Kind.ERROR, validation.kind);
    }
}
