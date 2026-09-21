package net.aschemann.jqassistant.plugin.github.config.api.model;

import com.buschmais.jqassistant.core.store.api.model.Descriptor;
import com.buschmais.xo.neo4j.api.annotation.Label;

/**
 * Shared root label for all nodes contributed by the GitHub Config plugin
 * (workflows, and future config domains such as Dependabot, CODEOWNERS, ...).
 */
@Label("GitHub")
public interface GitHubDescriptor extends Descriptor {
}
