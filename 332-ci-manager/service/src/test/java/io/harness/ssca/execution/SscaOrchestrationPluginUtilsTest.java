/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ssca.execution;

import static io.harness.rule.OwnerRule.INDER;
import static io.harness.ssca.execution.SscaOrchestrationPluginUtils.PLUGIN_FORMAT;
import static io.harness.ssca.execution.SscaOrchestrationPluginUtils.PLUGIN_SBOMDESTINATION;
import static io.harness.ssca.execution.SscaOrchestrationPluginUtils.PLUGIN_SBOMSOURCE;
import static io.harness.ssca.execution.SscaOrchestrationPluginUtils.PLUGIN_TOOL;
import static io.harness.ssca.execution.SscaOrchestrationPluginUtils.PLUGIN_TYPE;
import static io.harness.ssca.execution.SscaOrchestrationPluginUtils.SKIP_NORMALISATION;
import static io.harness.ssca.execution.SscaOrchestrationPluginUtils.getSscaOrchestrationStepEnvVariables;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ci.executionplan.CIExecutionTestBase;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.ssca.beans.source.ImageSbomSource;
import io.harness.ssca.beans.source.SbomSource;
import io.harness.ssca.beans.source.SbomSourceType;
import io.harness.ssca.beans.stepinfo.SscaOrchestrationStepInfo;
import io.harness.ssca.beans.tools.SbomOrchestrationTool;
import io.harness.ssca.beans.tools.SbomOrchestrationToolType;
import io.harness.ssca.beans.tools.syft.SyftSbomOrchestration;
import io.harness.ssca.beans.tools.syft.SyftSbomOrchestration.SyftOrchestrationFormat;

import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.SSCA)
public class SscaOrchestrationPluginUtilsTest extends CIExecutionTestBase {
  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void testGetSscaOrcherstrationStepEnvVariables() {
    SscaOrchestrationStepInfo stepInfo =
        SscaOrchestrationStepInfo.builder()
            .tool(SbomOrchestrationTool.builder()
                      .type(SbomOrchestrationToolType.SYFT)
                      .sbomOrchestrationSpec(
                          SyftSbomOrchestration.builder().format(SyftOrchestrationFormat.SPDX_JSON).build())
                      .build())
            .source(SbomSource.builder()
                        .type(SbomSourceType.IMAGE)
                        .sbomSourceSpec(ImageSbomSource.builder()
                                            .image(ParameterField.createValueField("image:tag"))
                                            .connector(ParameterField.createValueField("conn1"))
                                            .build())
                        .build())
            .build();
    Map<String, String> sscaEnvVarMap = getSscaOrchestrationStepEnvVariables(stepInfo, "id1");
    assertThat(sscaEnvVarMap).isNotNull().isNotEmpty();
    assertThat(sscaEnvVarMap).hasSize(6);
    assertThat(sscaEnvVarMap.get(PLUGIN_TOOL)).isEqualTo(SbomOrchestrationToolType.SYFT.toString());
    assertThat(sscaEnvVarMap.get(PLUGIN_FORMAT)).isEqualTo(SyftOrchestrationFormat.SPDX_JSON.toString());
    assertThat(sscaEnvVarMap.get(PLUGIN_SBOMSOURCE)).isEqualTo("image:tag");
    assertThat(sscaEnvVarMap.get(PLUGIN_TYPE)).isEqualTo("Orchestrate");
    assertThat(sscaEnvVarMap.get(PLUGIN_SBOMDESTINATION)).isEqualTo("harness/sbom");
    assertThat(sscaEnvVarMap.get(SKIP_NORMALISATION)).isEqualTo("true");
  }
}