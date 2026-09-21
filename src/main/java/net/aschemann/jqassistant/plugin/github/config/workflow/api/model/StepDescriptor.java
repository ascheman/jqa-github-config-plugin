package net.aschemann.jqassistant.plugin.github.config.workflow.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import net.aschemann.jqassistant.plugin.github.config.api.model.GitHubDescriptor;

import java.util.List;

@Label("Step")
public interface StepDescriptor extends GitHubDescriptor {

    String getId();
    void setId(String id);

    String getName();
    void setName(String name);

    Integer getIndex();
    void setIndex(Integer index);

    @Property("if")
    String getIfCondition();
    void setIfCondition(String ifCondition);

    String getShell();
    void setShell(String shell);

    String getWorkingDirectory();
    void setWorkingDirectory(String workingDirectory);

    Boolean getContinueOnError();
    void setContinueOnError(Boolean continueOnError);

    Integer getTimeoutMinutes();
    void setTimeoutMinutes(Integer timeoutMinutes);

    @Relation("USES_ACTION")
    ActionDescriptor getAction();
    void setAction(ActionDescriptor action);

    @Relation("RUNS_SCRIPT")
    ScriptDescriptor getScript();
    void setScript(ScriptDescriptor script);

    @Relation("HAS_WITH")
    List<WithDescriptor> getWith();

    @Relation("HAS_ENV")
    List<EnvDescriptor> getEnv();
}
