package net.aschemann.jqassistant.plugin.github.config.impl.scanner;

import com.buschmais.jqassistant.core.store.api.Store;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.ActionDescriptor;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.ConcurrencyDescriptor;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.EnvDescriptor;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.InputDescriptor;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.JobDescriptor;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.MatrixCombinationDescriptor;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.MatrixDimensionDescriptor;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.OutputDescriptor;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.PermissionDescriptor;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.ReusableWorkflowReferenceDescriptor;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.ScriptDescriptor;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.SecretDescriptor;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.ServiceDescriptor;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.StepDescriptor;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.StrategyDescriptor;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.TriggerDescriptor;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.WithDescriptor;
import net.aschemann.jqassistant.plugin.github.config.workflow.api.model.WorkflowDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps a parsed GitHub Workflow YAML tree to jQAssistant descriptors.
 * <p>
 * The first pass creates all jobs (so {@code needs:} can resolve job-to-job edges
 * in a second pass).
 */
class WorkflowParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowParser.class);

    private final Store store;

    WorkflowParser(Store store) {
        this.store = store;
    }

    void populate(WorkflowDescriptor workflow, Map<?, ?> root) {
        workflow.setName(asString(root.get("name")));
        workflow.setRunName(asString(root.get("run-name")));

        parseTriggers(workflow, root.get("on"));
        parseEnv(root.get("env"), workflow.getEnv());
        parsePermissions(root.get("permissions"), workflow.getPermissions());
        parseConcurrency(workflow, root.get("concurrency"));

        Map<String, JobDescriptor> jobs = new LinkedHashMap<>();
        Object jobsNode = root.get("jobs");
        if (jobsNode instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) jobsNode).entrySet()) {
                String jobId = asString(entry.getKey());
                if (jobId == null || !(entry.getValue() instanceof Map)) {
                    continue;
                }
                JobDescriptor job = createJob(jobId, (Map<?, ?>) entry.getValue());
                workflow.getJobs().add(job);
                jobs.put(jobId, job);
            }
            // Second pass: resolve `needs` references between jobs.
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) jobsNode).entrySet()) {
                String jobId = asString(entry.getKey());
                JobDescriptor job = jobs.get(jobId);
                if (job == null || !(entry.getValue() instanceof Map)) {
                    continue;
                }
                Object needs = ((Map<?, ?>) entry.getValue()).get("needs");
                for (String dep : asStringList(needs)) {
                    JobDescriptor target = jobs.get(dep);
                    if (target != null) {
                        job.getNeeds().add(target);
                    } else {
                        LOGGER.warn("GitHub Workflow: job '{}' needs unknown job '{}'", jobId, dep);
                    }
                }
            }
        }
    }

    /* ---------------- triggers ---------------- */

    private void parseTriggers(WorkflowDescriptor workflow, Object onNode) {
        if (onNode == null) {
            return;
        }
        if (onNode instanceof String) {
            workflow.getTriggers().add(simpleTrigger((String) onNode));
            return;
        }
        if (onNode instanceof List) {
            for (Object e : (List<?>) onNode) {
                workflow.getTriggers().add(simpleTrigger(asString(e)));
            }
            return;
        }
        if (onNode instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) onNode).entrySet()) {
                String event = asString(entry.getKey());
                Object cfg = entry.getValue();
                if ("schedule".equals(event) && cfg instanceof List) {
                    for (Object item : (List<?>) cfg) {
                        if (item instanceof Map) {
                            TriggerDescriptor t = simpleTrigger("schedule");
                            t.setCron(asString(((Map<?, ?>) item).get("cron")));
                            workflow.getTriggers().add(t);
                        }
                    }
                } else {
                    TriggerDescriptor t = simpleTrigger(event);
                    if (cfg instanceof Map) {
                        Map<?, ?> m = (Map<?, ?>) cfg;
                        t.setBranches(asStringArray(m.get("branches")));
                        t.setBranchesIgnore(asStringArray(m.get("branches-ignore")));
                        t.setTags(asStringArray(m.get("tags")));
                        t.setTagsIgnore(asStringArray(m.get("tags-ignore")));
                        t.setPaths(asStringArray(m.get("paths")));
                        t.setPathsIgnore(asStringArray(m.get("paths-ignore")));
                        t.setTypes(asStringArray(m.get("types")));
                    }
                    workflow.getTriggers().add(t);
                }
            }
        }
    }

    private TriggerDescriptor simpleTrigger(String event) {
        TriggerDescriptor t = store.create(TriggerDescriptor.class);
        t.setEvent(event);
        return t;
    }

    /* ---------------- jobs / steps ---------------- */

    private JobDescriptor createJob(String id, Map<?, ?> jobNode) {
        JobDescriptor job = store.create(JobDescriptor.class);
        job.setId(id);
        job.setName(asString(jobNode.get("name")));
        job.setRunsOn(asJoined(jobNode.get("runs-on")));
        job.setIfCondition(asString(jobNode.get("if")));
        job.setEnvironment(extractEnvironmentName(jobNode.get("environment")));
        job.setTimeoutMinutes(asInteger(jobNode.get("timeout-minutes")));
        job.setContinueOnError(asBoolean(jobNode.get("continue-on-error")));

        parseEnv(jobNode.get("env"), job.getEnv());
        parsePermissions(jobNode.get("permissions"), job.getPermissions());
        parseJobOutputs(jobNode.get("outputs"), job.getOutputs());
        parseStrategy(job, jobNode.get("strategy"));
        parseServices(jobNode.get("services"), job.getServices());

        Object usesNode = jobNode.get("uses");
        if (usesNode instanceof String) {
            // Reusable workflow caller (no steps).
            ReusableWorkflowReferenceDescriptor ref = parseReusableWorkflowReference((String) usesNode);
            job.setReusableWorkflow(ref);
            parseWith(jobNode.get("with"), job.getWith());
            parseSecretsCalled(jobNode.get("secrets"), job.getPassedSecrets());
        } else {
            Object stepsNode = jobNode.get("steps");
            if (stepsNode instanceof List) {
                int idx = 0;
                for (Object stepObj : (List<?>) stepsNode) {
                    if (stepObj instanceof Map) {
                        job.getSteps().add(createStep(idx++, (Map<?, ?>) stepObj));
                    }
                }
            }
        }
        return job;
    }

    private StepDescriptor createStep(int index, Map<?, ?> stepNode) {
        StepDescriptor step = store.create(StepDescriptor.class);
        step.setIndex(index);
        step.setId(asString(stepNode.get("id")));
        step.setName(asString(stepNode.get("name")));
        step.setIfCondition(asString(stepNode.get("if")));
        step.setShell(asString(stepNode.get("shell")));
        step.setWorkingDirectory(asString(stepNode.get("working-directory")));
        step.setContinueOnError(asBoolean(stepNode.get("continue-on-error")));
        step.setTimeoutMinutes(asInteger(stepNode.get("timeout-minutes")));

        parseEnv(stepNode.get("env"), step.getEnv());
        parseWith(stepNode.get("with"), step.getWith());

        Object uses = stepNode.get("uses");
        if (uses instanceof String) {
            step.setAction(parseAction((String) uses));
        }
        Object run = stepNode.get("run");
        if (run instanceof String) {
            ScriptDescriptor script = store.create(ScriptDescriptor.class);
            script.setContent((String) run);
            script.setShell(asString(stepNode.get("shell")));
            step.setScript(script);
        }
        return step;
    }

    /* ---------------- actions / reusable workflows ---------------- */

    private ActionDescriptor parseAction(String uses) {
        ActionDescriptor action = store.create(ActionDescriptor.class);
        action.setUses(uses);
        if (uses.startsWith("docker://")) {
            action.setKind("docker");
            action.setRepoOrName(uses.substring("docker://".length()));
            return action;
        }
        if (uses.startsWith("./") || uses.startsWith("../") || uses.startsWith("/")) {
            action.setKind("local");
            action.setRepoOrName(uses);
            return action;
        }
        action.setKind("marketplace");
        int at = uses.lastIndexOf('@');
        String coords = at >= 0 ? uses.substring(0, at) : uses;
        if (at >= 0) {
            action.setRef(uses.substring(at + 1));
        }
        String[] parts = coords.split("/", 3);
        if (parts.length >= 1) {
            action.setOwner(parts[0]);
        }
        if (parts.length >= 2) {
            action.setRepoOrName(parts.length == 3 ? parts[1] + "/" + parts[2] : parts[1]);
        }
        return action;
    }

    private ReusableWorkflowReferenceDescriptor parseReusableWorkflowReference(String uses) {
        ReusableWorkflowReferenceDescriptor ref = store.create(ReusableWorkflowReferenceDescriptor.class);
        ref.setUses(uses);
        if (uses.startsWith("./") || uses.startsWith("../") || uses.startsWith("/")) {
            ref.setKind("local");
            ref.setPath(uses);
            return ref;
        }
        ref.setKind("external");
        int at = uses.lastIndexOf('@');
        String coords = at >= 0 ? uses.substring(0, at) : uses;
        if (at >= 0) {
            ref.setRef(uses.substring(at + 1));
        }
        String[] parts = coords.split("/", 3);
        if (parts.length >= 1) {
            ref.setOwner(parts[0]);
        }
        if (parts.length >= 2) {
            ref.setRepo(parts[1]);
        }
        if (parts.length >= 3) {
            ref.setPath(parts[2]);
        }
        return ref;
    }

    /* ---------------- env / with / permissions ---------------- */

    private void parseEnv(Object node, List<EnvDescriptor> sink) {
        if (!(node instanceof Map)) {
            return;
        }
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) node).entrySet()) {
            EnvDescriptor env = store.create(EnvDescriptor.class);
            env.setName(asString(entry.getKey()));
            env.setValue(asString(entry.getValue()));
            sink.add(env);
        }
    }

    private void parseWith(Object node, List<WithDescriptor> sink) {
        if (!(node instanceof Map)) {
            return;
        }
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) node).entrySet()) {
            WithDescriptor w = store.create(WithDescriptor.class);
            w.setName(asString(entry.getKey()));
            w.setValue(asString(entry.getValue()));
            sink.add(w);
        }
    }

    private void parsePermissions(Object node, List<PermissionDescriptor> sink) {
        if (node == null) {
            return;
        }
        if (node instanceof String) {
            PermissionDescriptor p = store.create(PermissionDescriptor.class);
            p.setScope("all");
            p.setLevel((String) node);
            sink.add(p);
            return;
        }
        if (node instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) node).entrySet()) {
                PermissionDescriptor p = store.create(PermissionDescriptor.class);
                p.setScope(asString(entry.getKey()));
                p.setLevel(asString(entry.getValue()));
                sink.add(p);
            }
        }
    }

    private void parseConcurrency(WorkflowDescriptor workflow, Object node) {
        if (node == null) {
            return;
        }
        ConcurrencyDescriptor c = store.create(ConcurrencyDescriptor.class);
        if (node instanceof String) {
            c.setGroup((String) node);
        } else if (node instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) node;
            c.setGroup(asString(m.get("group")));
            c.setCancelInProgress(asBoolean(m.get("cancel-in-progress")));
        }
        workflow.setConcurrency(c);
    }

    private void parseJobOutputs(Object node, List<OutputDescriptor> sink) {
        if (!(node instanceof Map)) {
            return;
        }
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) node).entrySet()) {
            OutputDescriptor out = store.create(OutputDescriptor.class);
            out.setName(asString(entry.getKey()));
            out.setValue(asString(entry.getValue()));
            sink.add(out);
        }
    }

    private void parseSecretsCalled(Object node, List<SecretDescriptor> sink) {
        if ("inherit".equals(node)) {
            SecretDescriptor s = store.create(SecretDescriptor.class);
            s.setName("inherit");
            sink.add(s);
            return;
        }
        if (!(node instanceof Map)) {
            return;
        }
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) node).entrySet()) {
            SecretDescriptor s = store.create(SecretDescriptor.class);
            s.setName(asString(entry.getKey()));
            s.setValue(asString(entry.getValue()));
            sink.add(s);
        }
    }

    /* ---------------- strategy / matrix ---------------- */

    private void parseStrategy(JobDescriptor job, Object node) {
        if (!(node instanceof Map)) {
            return;
        }
        Map<?, ?> m = (Map<?, ?>) node;
        StrategyDescriptor strategy = store.create(StrategyDescriptor.class);
        strategy.setFailFast(asBoolean(m.get("fail-fast")));
        strategy.setMaxParallel(asInteger(m.get("max-parallel")));
        Object matrix = m.get("matrix");
        if (matrix instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) matrix).entrySet()) {
                String key = asString(entry.getKey());
                Object value = entry.getValue();
                if ("include".equals(key)) {
                    addCombinations(value, strategy.getIncludes());
                } else if ("exclude".equals(key)) {
                    addCombinations(value, strategy.getExcludes());
                } else {
                    MatrixDimensionDescriptor dim = store.create(MatrixDimensionDescriptor.class);
                    dim.setName(key);
                    dim.setValues(asStringArray(value));
                    strategy.getDimensions().add(dim);
                }
            }
        }
        job.setStrategy(strategy);
    }

    private void addCombinations(Object node, List<MatrixCombinationDescriptor> sink) {
        if (!(node instanceof List)) {
            return;
        }
        for (Object entry : (List<?>) node) {
            if (!(entry instanceof Map)) {
                continue;
            }
            MatrixCombinationDescriptor combo = store.create(MatrixCombinationDescriptor.class);
            for (Map.Entry<?, ?> kv : ((Map<?, ?>) entry).entrySet()) {
                WithDescriptor w = store.create(WithDescriptor.class);
                w.setName(asString(kv.getKey()));
                w.setValue(asString(kv.getValue()));
                combo.getParameters().add(w);
            }
            sink.add(combo);
        }
    }

    /* ---------------- services ---------------- */

    private void parseServices(Object node, List<ServiceDescriptor> sink) {
        if (!(node instanceof Map)) {
            return;
        }
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) node).entrySet()) {
            String id = asString(entry.getKey());
            if (!(entry.getValue() instanceof Map)) {
                continue;
            }
            Map<?, ?> svcNode = (Map<?, ?>) entry.getValue();
            ServiceDescriptor svc = store.create(ServiceDescriptor.class);
            svc.setId(id);
            svc.setImage(asString(svcNode.get("image")));
            parseEnv(svcNode.get("env"), svc.getEnv());
            sink.add(svc);
        }
    }

    /* ---------------- coercion helpers ---------------- */

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static Integer asInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }

    /**
     * Returns YAML scalar/list/map values as a list of strings.
     * Maps are flattened to their values (matrix axes use lists, but `runs-on` may use a map labels selector).
     */
    private static List<String> asStringList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof List) {
            List<String> out = new ArrayList<>();
            for (Object o : (List<?>) value) {
                if (o != null) {
                    out.add(o.toString());
                }
            }
            return out;
        }
        return Collections.singletonList(value.toString());
    }

    private static String[] asStringArray(Object value) {
        List<String> list = asStringList(value);
        return list.isEmpty() ? null : list.toArray(new String[0]);
    }

    private static String asJoined(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Collection) {
            return String.join(",", asStringList(value));
        }
        return value.toString();
    }

    /**
     * `environment:` may be a string or a map with a `name:` entry.
     */
    private static String extractEnvironmentName(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map) {
            return asString(((Map<?, ?>) value).get("name"));
        }
        return value.toString();
    }

}
