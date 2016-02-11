package hudson.plugins.jira;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import hudson.model.Result;

public class BuildListenerResultMethodMock implements Answer<Void> {

    private Result result;
    
    @Override
    public Void answer(InvocationOnMock invocation) throws Throwable {
        this.result = invocation.getArgumentAt(0, Result.class);
        return null;
    }

    public Result getResult() {
        return result;
    }
    
}
