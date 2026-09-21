package net.aschemann.jqassistant.plugin.github.config.workflow.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import net.aschemann.jqassistant.plugin.github.config.api.model.GitHubDescriptor;

@Label("ReusableWorkflowReference")
public interface ReusableWorkflowReferenceDescriptor extends GitHubDescriptor {

    String getUses();
    void setUses(String uses);

    String getOwner();
    void setOwner(String owner);

    String getRepo();
    void setRepo(String repo);

    String getPath();
    void setPath(String path);

    String getRef();
    void setRef(String ref);

    String getKind();
    void setKind(String kind);
}
