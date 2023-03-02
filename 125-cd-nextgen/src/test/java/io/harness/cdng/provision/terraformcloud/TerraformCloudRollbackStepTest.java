/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.provision.terraformcloud;

import static io.harness.rule.OwnerRule.BUHA;

import static com.mongodb.assertions.Assertions.assertFalse;
import static com.mongodb.assertions.Assertions.assertNull;
import static com.mongodb.assertions.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import io.harness.CategoryTest;
import io.harness.account.services.AccountService;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.provision.terraformcloud.dal.TerraformCloudConfig;
import io.harness.cdng.provision.terraformcloud.dal.TerraformCloudConfigDAL;
import io.harness.cdng.provision.terraformcloud.output.TerraformCloudConfigSweepingOutput;
import io.harness.cdng.provision.terraformcloud.steps.TerraformCloudRollbackStep;
import io.harness.connector.helper.EncryptionHelper;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.terraformcloudconnector.TerraformCloudConnectorDTO;
import io.harness.delegate.beans.connector.terraformcloudconnector.TerraformCloudCredentialDTO;
import io.harness.delegate.beans.connector.terraformcloudconnector.TerraformCloudCredentialType;
import io.harness.delegate.beans.connector.terraformcloudconnector.TerraformCloudTokenCredentialsDTO;
import io.harness.delegate.beans.logstreaming.UnitProgressData;
import io.harness.delegate.beans.terraformcloud.RollbackType;
import io.harness.delegate.beans.terraformcloud.TerraformCloudTaskParams;
import io.harness.delegate.beans.terraformcloud.TerraformCloudTaskType;
import io.harness.delegate.task.terraformcloud.response.TerraformCloudRollbackTaskResponse;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.UnitProgress;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.persistence.HIterator;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.steps.StepHelper;
import io.harness.steps.TaskRequestsUtils;

import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.MockitoRule;
import org.powermock.core.classloader.annotations.PrepareForTest;

@RunWith(MockitoJUnitRunner.class)
@PrepareForTest({TaskRequestsUtils.class})
@OwnedBy(HarnessTeam.CDP)
public class TerraformCloudRollbackStepTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private TerraformCloudStepHelper terraformCloudStepHelper;
  @Mock private ExecutionSweepingOutputService executionSweepingOutputService;
  @Mock private AccountService accountService;
  @Mock private TerraformCloudConfigDAL terraformCloudConfigDAL;
  @Mock private EncryptionHelper encryptionHelper;
  @Mock private StepHelper stepHelper;
  @InjectMocks private TerraformCloudRollbackStep terraformCloudRollbackStep = new TerraformCloudRollbackStep();

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testObtainTaskDestroyScenario() {
    Ambiance ambiance = Ambiance.newBuilder()
                            .setPlanExecutionId("executionId")
                            .putSetupAbstractions("accountId", "test-account")
                            .build();
    TerraformCloudRollbackStepParameters rollbackSpec =
        TerraformCloudRollbackStepParameters.infoBuilder()
            .provisionerIdentifier(ParameterField.createValueField("id"))
            .discardPendingRuns(ParameterField.createValueField(true))
            .message(ParameterField.createValueField("test"))
            .build();
    StepElementParameters stepElementParameters = StepElementParameters.builder().spec(rollbackSpec).build();

    doReturn("fullId").when(terraformCloudStepHelper).generateFullIdentifier("id", ambiance);

    HIterator<TerraformCloudConfig> iterator = mock(HIterator.class);
    doReturn(iterator).when(terraformCloudConfigDAL).getIterator(ambiance, "fullId");
    when(iterator.hasNext()).thenReturn(true, true, false);

    TerraformCloudConfig terraformCloudConfig = TerraformCloudConfig.builder()
                                                    .pipelineExecutionId("executionId")
                                                    .runId("runId")
                                                    .connectorRef("connectorRef")
                                                    .build();
    doReturn(terraformCloudConfig).when(iterator).next();

    doReturn(null).when(executionSweepingOutputService).consume(any(), any(), any(), any());
    doReturn(TerraformCloudConnectorDTO.builder()
                 .terraformCloudUrl("https://dummy.com")
                 .credential(TerraformCloudCredentialDTO.builder()
                                 .type(TerraformCloudCredentialType.API_TOKEN)
                                 .spec(TerraformCloudTokenCredentialsDTO.builder().build())
                                 .build())
                 .build())
        .when(terraformCloudStepHelper)
        .getTerraformCloudConnectorWithRef(any(), any());
    doReturn(Collections.emptyList()).when(encryptionHelper).getEncryptionDetail(any(), any(), any(), any());
    Mockito.mockStatic(TaskRequestsUtils.class);
    when(TaskRequestsUtils.prepareCDTaskRequest(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(TaskRequest.newBuilder().build());
    ArgumentCaptor<TaskData> taskDataArgumentCaptor = ArgumentCaptor.forClass(TaskData.class);

    TaskRequest taskRequest = terraformCloudRollbackStep.obtainTask(ambiance, stepElementParameters, null);
    assertThat(taskRequest).isNotNull();
    verifyStatic(TaskRequestsUtils.class, times(1));
    TaskRequestsUtils.prepareCDTaskRequest(
        any(), taskDataArgumentCaptor.capture(), any(), any(), eq("Terraform Cloud Task NG"), any(), any());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    assertThat(taskDataArgumentCaptor.getValue().getParameters()).isNotNull();

    TerraformCloudTaskParams params = (TerraformCloudTaskParams) taskDataArgumentCaptor.getValue().getParameters()[0];
    assertThat(params.getAccountId()).isEqualTo("test-account");
    assertThat(params.getTerraformCloudTaskType()).isEqualTo(TerraformCloudTaskType.ROLLBACK);
    assertThat(params.getRunId()).isEqualTo("runId");
    assertTrue(params.isDiscardPendingRuns());
    assertThat(params.getRollbackType()).isEqualTo(RollbackType.DESTROY);
    assertThat(params.getTerraformCloudConnectorDTO().getTerraformCloudUrl()).isEqualTo("https://dummy.com");
    assertThat(params.getMessage()).isEqualTo("test");
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testObtainTaskApplyScenario() {
    Ambiance ambiance = Ambiance.newBuilder()
                            .setPlanExecutionId("executionId")
                            .putSetupAbstractions("accountId", "test-account")
                            .build();
    TerraformCloudRollbackStepParameters rollbackSpec =
        TerraformCloudRollbackStepParameters.infoBuilder()
            .provisionerIdentifier(ParameterField.createValueField("id"))
            .build();
    StepElementParameters stepElementParameters = StepElementParameters.builder().spec(rollbackSpec).build();

    doReturn("fullId").when(terraformCloudStepHelper).generateFullIdentifier("id", ambiance);

    HIterator<TerraformCloudConfig> iterator = mock(HIterator.class);
    doReturn(iterator).when(terraformCloudConfigDAL).getIterator(ambiance, "fullId");
    when(iterator.hasNext()).thenReturn(true, true, false);

    TerraformCloudConfig terraformCloudConfig = TerraformCloudConfig.builder()
                                                    .pipelineExecutionId("executionId")
                                                    .runId("runId")
                                                    .connectorRef("connectorRef")
                                                    .build();
    doReturn(terraformCloudConfig).when(iterator).next();

    doReturn(null).when(executionSweepingOutputService).consume(any(), any(), any(), any());
    doReturn(TerraformCloudConnectorDTO.builder()
                 .terraformCloudUrl("https://dummy.com")
                 .credential(TerraformCloudCredentialDTO.builder()
                                 .type(TerraformCloudCredentialType.API_TOKEN)
                                 .spec(TerraformCloudTokenCredentialsDTO.builder().build())
                                 .build())
                 .build())
        .when(terraformCloudStepHelper)
        .getTerraformCloudConnectorWithRef(any(), any());
    doReturn(Collections.emptyList()).when(encryptionHelper).getEncryptionDetail(any(), any(), any(), any());
    ArgumentCaptor<TaskData> taskDataArgumentCaptor = ArgumentCaptor.forClass(TaskData.class);
    Mockito.mockStatic(TaskRequestsUtils.class);
    when(TaskRequestsUtils.prepareCDTaskRequest(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(TaskRequest.newBuilder().build());

    TaskRequest taskRequest = terraformCloudRollbackStep.obtainTask(ambiance, stepElementParameters, null);

    assertThat(taskRequest).isNotNull();
    verifyStatic(TaskRequestsUtils.class, times(1));
    TaskRequestsUtils.prepareCDTaskRequest(
        any(), taskDataArgumentCaptor.capture(), any(), any(), eq("Terraform Cloud Task NG"), any(), any());
    assertThat(taskDataArgumentCaptor.getValue()).isNotNull();
    assertThat(taskDataArgumentCaptor.getValue().getParameters()).isNotNull();

    TerraformCloudTaskParams params = (TerraformCloudTaskParams) taskDataArgumentCaptor.getValue().getParameters()[0];
    assertThat(params.getAccountId()).isEqualTo("test-account");
    assertThat(params.getTerraformCloudTaskType()).isEqualTo(TerraformCloudTaskType.ROLLBACK);
    assertThat(params.getRunId()).isEqualTo("runId");
    assertFalse(params.isDiscardPendingRuns());
    assertThat(params.getRollbackType()).isEqualTo(RollbackType.DESTROY);
    assertThat(params.getTerraformCloudConnectorDTO().getTerraformCloudUrl()).isEqualTo("https://dummy.com");
    assertNull(params.getMessage());
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testHandleTaskRequestForApplyWithSuccessTaskResponse() throws Exception {
    Ambiance ambiance =
        Ambiance.newBuilder().setPlanExecutionId("executionId").putSetupAbstractions("accountId", "accId").build();
    TerraformCloudRollbackStepParameters rollbackSpec =
        TerraformCloudRollbackStepParameters.infoBuilder()
            .provisionerIdentifier(ParameterField.createValueField("id"))
            .build();
    StepElementParameters stepElementParameters = StepElementParameters.builder().spec(rollbackSpec).build();
    List<UnitProgress> unitProgresses = Collections.singletonList(UnitProgress.newBuilder().build());
    UnitProgressData unitProgressData = UnitProgressData.builder().unitProgresses(unitProgresses).build();

    TerraformCloudRollbackTaskResponse response = TerraformCloudRollbackTaskResponse.builder()
                                                      .unitProgressData(unitProgressData)
                                                      .runId("runId")
                                                      .tfOutput("output")
                                                      .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                                      .build();

    TerraformCloudConfig terraformCloudConfig = TerraformCloudConfig.builder()
                                                    .pipelineExecutionId("executionId")
                                                    .runId("runId")
                                                    .connectorRef("connectorRef")
                                                    .build();
    TerraformCloudConfigSweepingOutput terraformCloudConfigSweepingOutput =
        TerraformCloudConfigSweepingOutput.builder()
            .terraformCloudConfig(terraformCloudConfig)
            .rollbackTaskType(RollbackType.APPLY)
            .build();

    OptionalSweepingOutput optionalSweepingOutput =
        OptionalSweepingOutput.builder().output(terraformCloudConfigSweepingOutput).build();
    doReturn(optionalSweepingOutput).when(executionSweepingOutputService).resolveOptional(any(), any());
    doNothing().when(terraformCloudStepHelper).saveTerraformCloudConfig(terraformCloudConfig, ambiance);
    AccountDTO accountDTO = AccountDTO.builder().name("TestAccountName").build();
    doReturn(accountDTO).when(accountService).getAccount(any());

    StepResponse stepResponse =
        terraformCloudRollbackStep.handleTaskResult(ambiance, stepElementParameters, () -> response);

    assertThat(response).isNotNull();
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
    assertThat(stepResponse.getUnitProgressList()).isEqualTo(unitProgresses);
    verify(terraformCloudStepHelper, times(1)).saveTerraformCloudConfig(terraformCloudConfig, ambiance);
  }

  @Test
  @Owner(developers = BUHA)
  @Category(UnitTests.class)
  public void testHandleTaskRequestForDestroyWithSuccessTaskResponse() throws Exception {
    Ambiance ambiance =
        Ambiance.newBuilder().setPlanExecutionId("executionId").putSetupAbstractions("accountId", "accId").build();
    TerraformCloudRollbackStepParameters rollbackSpec =
        TerraformCloudRollbackStepParameters.infoBuilder()
            .provisionerIdentifier(ParameterField.createValueField("id"))
            .build();
    StepElementParameters stepElementParameters = StepElementParameters.builder().spec(rollbackSpec).build();
    List<UnitProgress> unitProgresses = Collections.singletonList(UnitProgress.newBuilder().build());
    UnitProgressData unitProgressData = UnitProgressData.builder().unitProgresses(unitProgresses).build();

    TerraformCloudRollbackTaskResponse response = TerraformCloudRollbackTaskResponse.builder()
                                                      .unitProgressData(unitProgressData)
                                                      .runId("runId")
                                                      .tfOutput("output")
                                                      .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                                      .build();

    TerraformCloudConfig terraformCloudConfig = TerraformCloudConfig.builder()
                                                    .pipelineExecutionId("executionId")
                                                    .runId("runId")
                                                    .connectorRef("connectorRef")
                                                    .build();
    TerraformCloudConfigSweepingOutput terraformCloudConfigSweepingOutput =
        TerraformCloudConfigSweepingOutput.builder()
            .terraformCloudConfig(terraformCloudConfig)
            .rollbackTaskType(RollbackType.DESTROY)
            .build();

    OptionalSweepingOutput optionalSweepingOutput =
        OptionalSweepingOutput.builder().output(terraformCloudConfigSweepingOutput).build();
    doReturn(optionalSweepingOutput).when(executionSweepingOutputService).resolveOptional(any(), any());
    doNothing().when(terraformCloudStepHelper).saveTerraformCloudConfig(terraformCloudConfig, ambiance);
    AccountDTO accountDTO = AccountDTO.builder().name("TestAccountName").build();
    doReturn(accountDTO).when(accountService).getAccount(any());

    StepResponse stepResponse =
        terraformCloudRollbackStep.handleTaskResult(ambiance, stepElementParameters, () -> response);

    assertThat(response).isNotNull();
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
    assertThat(stepResponse.getUnitProgressList()).isEqualTo(unitProgresses);
    verify(terraformCloudConfigDAL, times(1)).clearTerraformCloudConfig(any(), any());
  }
}