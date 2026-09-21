package net.aschemann.jqassistant.plugin.github.config.workflow.api.model;

import com.buschmais.xo.neo4j.api.annotation.Label;
import net.aschemann.jqassistant.plugin.github.config.api.model.GitHubDescriptor;

@Label("Trigger")
public interface TriggerDescriptor extends GitHubDescriptor {

    String getEvent();
    void setEvent(String event);

    String[] getBranches();
    void setBranches(String[] branches);

    String[] getBranchesIgnore();
    void setBranchesIgnore(String[] branchesIgnore);

    String[] getTags();
    void setTags(String[] tags);

    String[] getTagsIgnore();
    void setTagsIgnore(String[] tagsIgnore);

    String[] getPaths();
    void setPaths(String[] paths);

    String[] getPathsIgnore();
    void setPathsIgnore(String[] pathsIgnore);

    String[] getTypes();
    void setTypes(String[] types);

    String getCron();
    void setCron(String cron);
}
