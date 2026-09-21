package net.aschemann.jqassistant.plugin.github.config.impl.scanner;

import com.buschmais.jqassistant.core.scanner.api.Scanner;
import com.buschmais.jqassistant.core.scanner.api.ScannerContext;
import com.buschmais.jqassistant.core.scanner.api.ScannerPlugin;
import com.buschmais.jqassistant.core.scanner.api.Scope;
import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.jqassistant.plugin.common.api.model.FileDescriptor;
import com.buschmais.jqassistant.plugin.common.api.scanner.AbstractScannerPlugin;
import com.buschmais.jqassistant.plugin.common.api.scanner.filesystem.FileResource;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.WorkflowDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.IOException;
import java.io.InputStream;

@ScannerPlugin.Requires(FileDescriptor.class)
public class WorkflowScannerPlugin extends AbstractScannerPlugin<FileResource, WorkflowDescriptor> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowScannerPlugin.class);

    @Override
    public boolean accepts(FileResource item, String path, Scope scope) {
        if (path == null) {
            return false;
        }
        String normalized = path.replace('\\', '/').toLowerCase();
        // Accept both ".github/workflows/x.yml" and "/workflows/x.yml": when
        // ".github" is configured as a scan root of its own (jqassistant.yml
        // include.files), the scanner reports paths relative to it and the
        // ".github" segment is gone. Requiring it meant the plugin silently
        // matched nothing at all while the files were plainly in the graph.
        boolean inWorkflowsDir = normalized.contains(".github/workflows/")
                || normalized.startsWith("/workflows/")
                || normalized.startsWith("workflows/");
        boolean isYaml = normalized.endsWith(".yml") || normalized.endsWith(".yaml");
        boolean accepted = inWorkflowsDir && isYaml;
        if (accepted) {
            LOGGER.debug("GitHub Workflow: accepting '{}'", path);
        }
        return accepted;
    }

    @Override
    public WorkflowDescriptor scan(FileResource item, String path, Scope scope, Scanner scanner) throws IOException {
        ScannerContext context = scanner.getContext();
        Store store = context.getStore();
        FileDescriptor fileDescriptor = context.getCurrentDescriptor();
        WorkflowDescriptor workflowDescriptor = store.addDescriptorType(fileDescriptor, WorkflowDescriptor.class);

        Object root;
        try (InputStream in = item.createStream()) {
            Load load = new Load(LoadSettings.builder().build());
            root = load.loadFromInputStream(in);
        } catch (Exception e) {
            LOGGER.warn("GitHub Workflow: failed to parse '{}': {}", path, e.getMessage());
            return workflowDescriptor;
        }

        if (!(root instanceof java.util.Map)) {
            LOGGER.warn("GitHub Workflow: root of '{}' is not a YAML map", path);
            return workflowDescriptor;
        }

        new WorkflowParser(store).populate(workflowDescriptor, (java.util.Map<?, ?>) root);
        return workflowDescriptor;
    }
}
