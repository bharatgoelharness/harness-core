package io.harness.delegate.task.k8s;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.expression.Expression;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class K8sRollingDeployRequest implements K8sDeployRequest {
  boolean skipDryRun;
  String releaseName;
  String commandName;
  K8sTaskType taskType;
  Integer timeoutIntervalInMin;
  @Expression(ALLOW_SECRETS) List<String> valuesYamlList;
  K8sInfraDelegateConfig k8sInfraDelegateConfig;
  ManifestDelegateConfig manifestDelegateConfig;
  boolean inCanaryWorkflow;
  boolean localOverrideFeatureFlag;
  String accountId;
  boolean deprecateFabric8Enabled;
}
