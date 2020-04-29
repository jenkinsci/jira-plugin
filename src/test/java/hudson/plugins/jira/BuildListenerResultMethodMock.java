package hudson.plugins.jira;

import hudson.model.Result;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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
