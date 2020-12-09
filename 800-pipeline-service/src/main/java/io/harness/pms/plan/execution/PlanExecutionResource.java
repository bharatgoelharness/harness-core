package io.harness.pms.plan.execution;

import io.harness.engine.OrchestrationService;
import io.harness.execution.PlanExecution;
import io.harness.plan.Plan;
import io.harness.pms.plan.PlanCreationBlobResponse;
import io.harness.pms.plan.creation.PlanCreatorMergeService;

import com.google.inject.Inject;
import com.google.protobuf.util.JsonFormat;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Api("/pipeline/execute")
@Path("/pipeline/execute")
@Produces({"application/json"})
@Consumes({"application/json"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
public class PlanExecutionResource {
  @Inject private OrchestrationService orchestrationService;
  @Inject private PlanCreatorMergeService planCreatorMergeService;

  private static final String tempPipeline = "pipeline:\n"
      + "  name: \"Manager Service Deployment\"\n"
      + "  identifier: managerServiceDeployment\n"
      + "  stages:\n"
      + "    - stage:\n"
      + "        identifier: qaStage\n"
      + "        name: \"qa stage\"\n"
      + "        type: Deployment\n"
      + "        spec:\n"
      + "          service:\n"
      + "            identifier: manager\n"
      + "            serviceDefinition:\n"
      + "              type: \"Kubernetes\"\n"
      + "              spec:\n"
      + "                artifacts:\n"
      + "                  primary:\n"
      + "                    type: Dockerhub\n"
      + "                    spec:\n"
      + "                      connectorRef: \"https://registry.hub.docker.com/\"\n"
      + "                      imagePath: \"library/nginx\"\n"
      + "                      tag: \"1.18\"\n"
      + "                manifests:   # {k8s |  values | pcf |  helmSourceRepo | helmSourceRepo | kustomize | openShift}\n"
      + "                  - manifest:\n"
      + "                      identifier: baseValues\n"
      + "                      type: K8sManifest\n"
      + "                      spec:\n"
      + "                        store:\n"
      + "                          type: Git\n"
      + "                          # Git|Local\n"
      + "                          spec:\n"
      + "                            connectorRef: eJ9pksJFQDmjq6ZFbAoR-Q\n"
      + "                            gitFetchType: Branch\n"
      + "                            branch: master\n"
      + "                            paths:\n"
      + "                              - test/spec\n"
      + "            stageOverrides:\n"
      + "              manifests:   # {k8s |  values | pcf |  helmSourceRepo | helmSourceRepo | kustomize | openShift}\n"
      + "                - manifest:\n"
      + "                    identifier: qaOverride\n"
      + "                    type: Values\n"
      + "                    spec:\n"
      + "                      store:\n"
      + "                        type: Git\n"
      + "                        spec:\n"
      + "                          connectorRef: eJ9pksJFQDmjq6ZFbAoR-Q\n"
      + "                          gitFetchType: Branch\n"
      + "                          branch: master\n"
      + "                          paths:\n"
      + "                            - test/qa/values_1.yaml\n"
      + "              artifacts:\n"
      + "                primary:\n"
      + "                  type: Dockerhub\n"
      + "                  spec:\n"
      + "                    tag: \"1.18\"\n"
      + "          infrastructure:\n"
      + "            environment:\n"
      + "              identifier: stagingInfra\n"
      + "              type: PreProduction\n"
      + "              tags:\n"
      + "                cloud: GCP\n"
      + "                team: cdp\n"
      + "            # Infrastructure Type. Options: kubernetes-cluster, kubernetes-direct, kubernetes-gke, ecs, data-center, etc. See Infrastructure Types. REQUIRED\n"
      + "            # Dynamic type ???\n"
      + "            infrastructureDefinition:\n"
      + "              # Infrastructure Type. Options: kubernetes-cluster, kubernetes-direct, kubernetes-gke, ecs, data-center, etc. See Infrastructure Types. REQUIRED\n"
      + "              # Dynamic type ???\n"
      + "              type: KubernetesDirect\n"
      + "              spec:\n"
      + "                # Spec for Infrastructure Type kubernetes-direct\n"
      + "                connectorRef: pEIkEiNPSgSUsbWDDyjNKw\n"
      + "                # namespace\n"
      + "                namespace: harness\n"
      + "                # release name\n"
      + "                releaseName: testingqa\n"
      + "          execution:\n"
      + "            steps:\n"
      + "              - step:\n"
      + "                  name: \"Rollout Deployment\"\n"
      + "                  identifier: rolloutDeployment1\n"
      + "                  type: K8sRollingDeploy\n"
      + "                  spec:\n"
      + "                    timeout: 120000\n"
      + "                    skipDryRun: false\n"
      + "            rollbackSteps:\n"
      + "              - step:\n"
      + "                  name: \"Rollback Rollout Deployment\"\n"
      + "                  identifier: rollbackRolloutDeployment1\n"
      + "                  type: K8sRollingRollback\n"
      + "                  spec:\n"
      + "                    timeout: 120000\n"
      + "              - step:\n"
      + "                  identifier: shellScript1\n"
      + "                  type: ShellScript\n"
      + "                  spec:\n"
      + "                    executeOnDelegate: true\n"
      + "                    connectionType: SSH\n"
      + "                    scriptType: BASH\n"
      + "                    scriptString: |\n"
      + "                      echo 'I should be executed during rollback'\n"
      + "    - stage:\n"
      + "        identifier: prodStage\n"
      + "        name: \"prod stage\"\n"
      + "        type: Deployment\n"
      + "        spec:\n"
      + "          service:\n"
      + "            useFromStage:\n"
      + "              stage: qaStage\n"
      + "            stageOverrides:\n"
      + "              manifests:   # {k8s |  values | pcf |  helmSourceRepo | helmSourceRepo | kustomize | openShift}\n"
      + "                - manifest:\n"
      + "                    identifier: prodOverride\n"
      + "                    type: Values\n"
      + "                    spec:\n"
      + "                      store:\n"
      + "                        type: Git\n"
      + "                        spec:\n"
      + "                          connectorRef: eJ9pksJFQDmjq6ZFbAoR-Q\n"
      + "                          gitFetchType: Branch\n"
      + "                          branch: master\n"
      + "                          paths:\n"
      + "                            - test/prod/values.yaml\n"
      + "              artifacts:\n"
      + "                primary:\n"
      + "                  type: Dockerhub\n"
      + "                  spec:\n"
      + "                    tag: \"1.18\"\n"
      + "          infrastructure:\n"
      + "            useFromStage:\n"
      + "              stage: qaStage\n"
      + "              overrides:\n"
      + "                environment:\n"
      + "                  identifier: prodInfra\n"
      + "                infrastructureDefinition:\n"
      + "                  type: KubernetesDirect\n"
      + "                  spec:\n"
      + "                    releaseName: testingProd\n"
      + "          execution:\n"
      + "            steps:\n"
      + "              - stepGroup:\n"
      + "                  name: StepGroup1\n"
      + "                  identifier: StepGroup1\n"
      + "                  steps:\n"
      + "                    - parallel:\n"
      + "                        - step:\n"
      + "                            name: http step 1\n"
      + "                            identifier: httpStep1\n"
      + "                            type: Http\n"
      + "                            spec:\n"
      + "                              socketTimeoutMillis: 1000\n"
      + "                              method: GET\n"
      + "                              url: http://httpstat.us/200\n"
      + "              - step:\n"
      + "                  name: \"Rollout Deployment\"\n"
      + "                  identifier: rolloutDeployment2\n"
      + "                  type: K8sRollingDeploy\n"
      + "                  spec:\n"
      + "                    timeout: 120000\n"
      + "                    skipDryRun: false\n"
      + "            rollbackSteps:\n"
      + "              - step:\n"
      + "                  name: \"Rollback Rollout Deployment\"\n"
      + "                  identifier: rollbackRolloutDeployment2\n"
      + "                  type: K8sRollingRollback\n"
      + "                  spec:\n"
      + "                    timeout: 120000";

  @GET
  @ApiOperation(value = "Execute A Pipeline", nickname = "executePipeline")
  public Response executePipeline() throws IOException {
    PlanCreationBlobResponse resp = planCreatorMergeService.createPlan(tempPipeline);
    Plan plan = PlanExecutionUtils.extractPlan(resp);
    PlanExecution planExecution = orchestrationService.startExecution(plan);
    return Response.ok(planExecution, MediaType.APPLICATION_JSON_TYPE).build();
  }
}
