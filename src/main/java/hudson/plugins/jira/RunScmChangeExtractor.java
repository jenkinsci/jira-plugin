package hudson.plugins.jira;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.AbstractBuild.DependencyChange;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import java.util.Collections;

public class RunScmChangeExtractor {

    // for reflection, until JENKINS-24141
    private static final String GET_CHANGESET_METHOD = "getChangeSets";
    private static final String CANNOT_ACCESS_GET_CHANGESET_METHOD = "cannot call " + GET_CHANGESET_METHOD;

    private RunScmChangeExtractor() {
    }

    public static List<ChangeLogSet<? extends Entry>> getChanges(Run<?, ?> run) {
        if (run instanceof AbstractBuild) {
            ChangeLogSet<? extends Entry> cs = ((AbstractBuild<?, ?>) run).getChangeSet();
            return cs.isEmptySet() ? Collections.<ChangeLogSet<? extends ChangeLogSet.Entry>>emptyList() : Collections.<ChangeLogSet<? extends ChangeLogSet.Entry>>singletonList(cs);
        } else if (run == null) {
            throw new IllegalStateException("run cannot be null!");
        } else {
            return getChangesUsingReflection(run);
        }
    }

    /**
     * return changeSets using java reflection api, for example for workflow
     * jobs.
     * 
     * until JENKINS-24141
     * 
     * @param run
     *            - run that implements some type with GET_CHANGESET_METHOD
     * @return collection of scm ChangeLogSet entries
     */
    @SuppressWarnings("unchecked")
    static List<ChangeLogSet<? extends Entry>> getChangesUsingReflection(Run<?, ?> run) {
        Method getChangeSetMethod = null;
        for (Method method : run.getClass().getMethods()) {
            if (method.getName().equals(GET_CHANGESET_METHOD) && List.class.isAssignableFrom(method.getReturnType())) {
                getChangeSetMethod = method;
                break;
            }
        }
        if (getChangeSetMethod != null) {
            try {
                Object result = getChangeSetMethod.invoke(run, new Object[] {});
                return (List<ChangeLogSet<? extends Entry>>) result;
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(CANNOT_ACCESS_GET_CHANGESET_METHOD, e);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(CANNOT_ACCESS_GET_CHANGESET_METHOD, e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException(CANNOT_ACCESS_GET_CHANGESET_METHOD, e);
            }
        } else {
            // if run don't have GET_CHANGESET_METHOD, we don't support it
            throw new IllegalArgumentException("Unsupported Run type " + run.getClass().getName());
        }
    }

    public static Map<AbstractProject, DependencyChange> getDependencyChanges(Run<?, ?> run) {
        if (run instanceof AbstractBuild) {
            Run<?, ?> previousBuild = run.getPreviousBuild();
            if (previousBuild instanceof AbstractBuild) {
                return ((AbstractBuild) run).getDependencyChanges((AbstractBuild) previousBuild);
            }
        }
        // jenkins workflow plugin etc.
        return Maps.newHashMap();
    }

}
