package net.aschemann.jqassistant.plugin.github.config.workflow.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import net.aschemann.jqassistant.plugin.github.config.api.model.GitHubDescriptor;

import java.util.List;

@Label("Job")
public interface JobDescriptor extends GitHubDescriptor {

    String getId();
    void setId(String id);

    String getName();
    void setName(String name);

    String getRunsOn();
    void setRunsOn(String runsOn);

    @Property("if")
    String getIfCondition();
    void setIfCondition(String ifCondition);

    String getEnvironment();
    void setEnvironment(String environment);

    Integer getTimeoutMinutes();
    void setTimeoutMinutes(Integer timeoutMinutes);

    Boolean getContinueOnError();
    void setContinueOnError(Boolean continueOnError);

    @Relation("HAS_STEP")
    List<StepDescriptor> getSteps();

    @Relation("NEEDS")
    List<JobDescriptor> getNeeds();

    @Relation("HAS_STRATEGY")
    StrategyDescriptor getStrategy();
    void setStrategy(StrategyDescriptor strategy);

    @Relation("HAS_ENV")
    List<EnvDescriptor> getEnv();

    @Relation("HAS_PERMISSION")
    List<PermissionDescriptor> getPermissions();

    @Relation("HAS_OUTPUT")
    List<OutputDescriptor> getOutputs();

    @Relation("USES_REUSABLE_WORKFLOW")
    ReusableWorkflowReferenceDescriptor getReusableWorkflow();
    void setReusableWorkflow(ReusableWorkflowReferenceDescriptor reusableWorkflow);

    @Relation("HAS_WITH")
    List<WithDescriptor> getWith();

    @Relation("PASSES_SECRET")
    List<SecretDescriptor> getPassedSecrets();

    @Relation("HAS_SERVICE")
    List<ServiceDescriptor> getServices();
}
