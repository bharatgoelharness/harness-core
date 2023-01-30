/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.provision.terraformcloud.params;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.provision.terraformcloud.TerraformCloudRunSpecParameters;
import io.harness.cdng.provision.terraformcloud.TerraformCloudRunType;
import io.harness.pms.yaml.ParameterField;
import io.harness.validation.Validator;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
@RecasterAlias("io.harness.cdng.provision.terraformcloud.params.TerraformCloudRefreshSpecParameters")
public class TerraformCloudRefreshSpecParameters implements TerraformCloudRunSpecParameters {
  ParameterField<String> connectorRef;
  ParameterField<String> organization;
  ParameterField<String> workspace;
  ParameterField<Boolean> discardPendingRuns;
  Map<String, Object> variables;

  @Override
  public TerraformCloudRunType getType() {
    return TerraformCloudRunType.REFRESH_STATE;
  }

  @Override
  public void validate() {
    Validator.notNullCheck("Connector ref for Terraform Cloud is null", connectorRef);
    Validator.notNullCheck("Organization is null", organization);
    Validator.notNullCheck("Workspace is null", workspace);
  }

  @Override
  public List<ParameterField<String>> extractConnectorRefs() {
    return List.of(connectorRef);
  }
}