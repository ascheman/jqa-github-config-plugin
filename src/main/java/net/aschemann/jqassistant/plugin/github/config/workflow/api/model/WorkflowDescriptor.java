package net.aschemann.jqassistant.plugin.github.config.workflow.api.model;

import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Relation;
import net.aschemann.jqassistant.plugin.github.config.api.model.GitHubDescriptor;

import java.util.List;

@Label("Workflow")
public interface WorkflowDescriptor extends GitHubDescriptor, FileDescriptor {

    String getName();
    void setName(String name);

    String getRunName();
    void setRunName(String runName);

    @Relation("HAS_TRIGGER")
    List<TriggerDescriptor> getTriggers();

    @Relation("HAS_JOB")
    List<JobDescriptor> getJobs();

    @Relation("HAS_INPUT")
    List<InputDescriptor> getInputs();

    @Relation("HAS_OUTPUT")
    List<OutputDescriptor> getOutputs();

    @Relation("HAS_SECRET")
    List<SecretDescriptor> getSecrets();

    @Relation("HAS_ENV")
    List<EnvDescriptor> getEnv();

    @Relation("HAS_PERMISSION")
    List<PermissionDescriptor> getPermissions();

    @Relation("HAS_CONCURRENCY")
    ConcurrencyDescriptor getConcurrency();
    void setConcurrency(ConcurrencyDescriptor concurrency);
}
