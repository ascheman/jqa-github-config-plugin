package net.aschemann.jqassistant.plugin.github.config.workflow.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import net.aschemann.jqassistant.plugin.github.config.api.model.GitHubDescriptor;

@Label("Action")
public interface ActionDescriptor extends GitHubDescriptor {

    String getUses();
    void setUses(String uses);

    String getOwner();
    void setOwner(String owner);

    String getRepoOrName();
    void setRepoOrName(String repoOrName);

    String getRef();
    void setRef(String ref);

    String getKind();
    void setKind(String kind);
}
