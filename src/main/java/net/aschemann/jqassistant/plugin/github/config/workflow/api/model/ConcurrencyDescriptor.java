package net.aschemann.jqassistant.plugin.github.config.workflow.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import net.aschemann.jqassistant.plugin.github.config.api.model.GitHubDescriptor;

@Label("Concurrency")
public interface ConcurrencyDescriptor extends GitHubDescriptor {

    String getGroup();
    void setGroup(String group);

    Boolean getCancelInProgress();
    void setCancelInProgress(Boolean cancelInProgress);
}
