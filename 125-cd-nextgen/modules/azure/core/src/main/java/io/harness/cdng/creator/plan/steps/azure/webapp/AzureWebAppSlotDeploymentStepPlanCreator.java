/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.creator.plan.steps.azure.webapp;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.azure.webapp.AzureWebAppSlotDeploymentStepNode;
import io.harness.cdng.creator.plan.steps.CDPMSStepPlanCreatorV2;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationContext;
import io.harness.pms.sdk.core.plan.creation.beans.PlanCreationResponse;

import com.google.common.collect.Sets;
import java.util.Set;

@OwnedBy(HarnessTeam.CDP)
public class AzureWebAppSlotDeploymentStepPlanCreator
    extends CDPMSStepPlanCreatorV2<AzureWebAppSlotDeploymentStepNode> {
  @Override
  public Set<String> getSupportedStepTypes() {
    return Sets.newHashSet(StepSpecTypeConstants.AZURE_SLOT_DEPLOYMENT);
  }

  @Override
  public Class<AzureWebAppSlotDeploymentStepNode> getFieldClass() {
    return AzureWebAppSlotDeploymentStepNode.class;
  }

  @Override
  public PlanCreationResponse createPlanForField(
      PlanCreationContext ctx, AzureWebAppSlotDeploymentStepNode stepElement) {
    return super.createPlanForField(ctx, stepElement);
  }
}