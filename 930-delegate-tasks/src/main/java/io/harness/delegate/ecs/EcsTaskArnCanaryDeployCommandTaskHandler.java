/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.ecs;

import static software.wings.beans.LogHelper.color;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.exception.EcsNGException;
import io.harness.delegate.task.ecs.EcsTaskHelperBase;
import io.harness.delegate.task.ecs.request.EcsCommandRequest;
import io.harness.delegate.task.ecs.request.EcsTaskArnCanaryDeployRequest;
import io.harness.delegate.task.ecs.response.EcsCommandResponse;
import io.harness.ecs.EcsCommandUnitConstants;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.logging.LogLevel;

import software.wings.beans.LogColor;
import software.wings.beans.LogWeight;

import com.google.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import software.amazon.awssdk.services.ecs.model.CreateServiceRequest;

@OwnedBy(HarnessTeam.CDP)
@NoArgsConstructor
@Slf4j
public class EcsTaskArnCanaryDeployCommandTaskHandler extends EcsCommandTaskNGHandler {
  @Inject private EcsTaskHelperBase ecsTaskHelperBase;
  @Inject private EcsDeploymentHelper ecsDeploymentHelper;

  @Override
  protected EcsCommandResponse executeTaskInternal(EcsCommandRequest ecsCommandRequest,
      ILogStreamingTaskClient iLogStreamingTaskClient, CommandUnitsProgress commandUnitsProgress) throws Exception {
    if (!(ecsCommandRequest instanceof EcsTaskArnCanaryDeployRequest)) {
      throw new InvalidArgumentsException(
          Pair.of("ecsCommandRequest", "Must be instance of EcsTaskArnCanaryDeployRequest"));
    }
    EcsTaskArnCanaryDeployRequest ecsTaskArnCanaryDeployRequest = (EcsTaskArnCanaryDeployRequest) ecsCommandRequest;

    LogCallback deployLogCallback = ecsTaskHelperBase.getLogCallback(
        iLogStreamingTaskClient, EcsCommandUnitConstants.deploy.toString(), true, commandUnitsProgress);
    try {
      CreateServiceRequest createServiceRequest =
          ecsDeploymentHelper.createServiceDefinitionRequest(deployLogCallback, ecsCommandRequest.getEcsInfraConfig(),
              null, ecsTaskArnCanaryDeployRequest.getEcsServiceDefinitionManifestContent(),
              ecsTaskArnCanaryDeployRequest.getEcsScalableTargetManifestContentList(),
              ecsTaskArnCanaryDeployRequest.getEcsScalingPolicyManifestContentList(),
              ecsTaskArnCanaryDeployRequest.getEcsTaskDefinitionArn());
      return ecsDeploymentHelper.deployCanaryService(deployLogCallback, createServiceRequest, ecsCommandRequest,
          ecsTaskArnCanaryDeployRequest.getEcsScalableTargetManifestContentList(),
          ecsTaskArnCanaryDeployRequest.getEcsScalingPolicyManifestContentList(),
          ecsTaskArnCanaryDeployRequest.getDesiredCountOverride(),
          ecsTaskArnCanaryDeployRequest.getEcsServiceNameSuffix());
    } catch (Exception ex) {
      deployLogCallback.saveExecutionLog(color(format("%n Deployment Failed."), LogColor.Red, LogWeight.Bold),
          LogLevel.ERROR, CommandExecutionStatus.FAILURE);
      throw new EcsNGException(ex);
    }
  }
}