/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.common;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

/**
 * Name of the top element in yaml. For example:
 * <p>connector:
 * name: testname projectIdentifier: projectId
 * </p>
 * In this the top element is <b>connector</b>
 */
@UtilityClass
@OwnedBy(PL)
public class EntityYamlRootNames {
  public static final String GITOPS_CREATE_PR = "CreatePR";
  public static final String GITOPS_MERGE_PR = "MergePR";
  public static final String ENVIRONMENT_GROUP = "environmentGroup";
  public static final String PROJECT = "project";
  public static final String PIPELINE = "pipeline";
  public static final String PIPELINE_STEP = "pipelineStep";
  public static final String CONNECTOR = "connector";
  public static final String SECRET = "secret";
  public static final String FILE = "file";
  public static final String SERVICE = "service";
  public static final String ENVIRONMENT = "environment";
  public static final String INPUT_SET = "inputSet";
  public static final String OVERLAY_INPUT_SET = "overlayInputSet";
  public static final String CV_CONFIG = "cvConfig";
  public static final String VERIFY = "Verify";
  public static final String DELEGATE = "delegate";
  public static final String DELEGATE_CONFIGURATION = "delegateConfigurations";
  public static final String CV_VERIFICATION_JOB = "cvVerificationJob";
  public static final String INTEGRATION_STAGE = "integrationStage";

  public static final String INTEGRATION_STEP = "integrationSteps";
  public static final String CV_KUBERNETES_ACTIVITY_SOURCE = "cvKubernetesActivitySource";
  public static final String DEPLOYMENT_STEP = "deploymentSteps";
  public static final String DEPLOYMENT_STAGE = "deploymentStage";
  public static final String FEATURE_FLAG_STAGE = "featureFlagStage";
  public static final String APPROVAL_STAGE = "approvalStage";
  public static final String PIPELINE_STAGE = "pipelineStage";
  public static final String CUSTOM_STAGE = "customStage";
  public static final String TRIGGERS = "trigger";
  public static final String MONITORED_SERVICE = "monitoredService";
  public static final String TEMPLATE = "template";
  public static final String FETCH_INSTANCE_SCRIPT = "fetchInstanceScript";
  public static final String GIT_REPOSITORY = "gitRepository";
  public static final String FEATURE_FLAGS = "featureFlags";
  public static final String HTTP = "Http";
  public static final String JIRA_CREATE = "JiraCreate";
  public static final String JIRA_UPDATE = "JiraUpdate";
  public static final String SHELL_SCRIPT = "ShellScript";
  public static final String K8S_CANARY_DEPLOY = "K8sCanaryDeploy";
  public static final String K8S_APPLY = "K8sApply";
  public static final String K8S_BLUE_GREEN_DEPLOY = "K8sBlueGreenDeploy";
  public static final String K8S_ROLLING_DEPLOY = "K8sRollingDeploy";
  public static final String K8S_ROLLING_ROLLBACK = "K8sRollingRollback";
  public static final String K8S_SCALE = "K8sScale";
  public static final String K8S_DELETE = "K8sDelete";
  public static final String K8S_SWAP_SERVICES = "K8sBGSwapServices";
  public static final String K8S_CANARY_DELETE = "K8sCanaryDelete";
  public static final String TERRAFORM_APPLY = "TerraformApply";
  public static final String TERRAFORM_PLAN = "TerraformPlan";
  public static final String TERRAFORM_DESTROY = "TerraformDestroy";
  public static final String TERRAFORM_ROLLBACK = "TerraformRollback";
  public static final String HELM_DEPLOY = "HelmDeploy";
  public static final String HELM_ROLLBACK = "HelmRollback";
  public static final String SERVICENOW_CREATE = "ServiceNowCreate";
  public static final String SERVICENOW_UPDATE = "ServiceNowUpdate";
  public static final String SERVICENOW_IMPORT_SET = "ServiceNowImportSet";
  public static final String SERVICENOW_APPROVAL = "ServiceNowApproval";
  public static final String JIRA_APPROVAL = "JiraApproval";
  public static final String HARNESS_APPROVAL = "HarnessApproval";
  public static final String CUSTOM_APPROVAL = "CustomApproval";
  public static final String BARRIER = "Barrier";
  public static final String FLAG_CONFIGURATION = "FlagConfiguration";
  public static final String OPAPOLICY = "governancePolicy";
  public static final String POLICY_STEP = "Policy";
  public static final String RUN_STEP = "Run";
  public static final String BACKGROUND_STEP = "Background";
  public static final String RUN_TEST = "RunTests";
  public static final String PLUGIN = "Plugin";
  public static final String SECURITY = "Security";
  public static final String SECURITY_STAGE = "securityStage";
  public static final String SECURITY_STEP = "securitySteps";
  public static final String GIT_CLONE = "GitClone";
  public static final String RESTORE_CACHE_GCS = "RestoreCacheGCS";
  public static final String RESTORE_CACHE_S3 = "RestoreCacheS3";
  public static final String SAVE_CACHE_GCS = "SaveCacheGCS";
  public static final String SAVE_CACHE_S3 = "SaveCacheS3";
  public static final String ARTIFACTORY_UPLOAD = "ArtifactoryUpload";
  public static final String GCS_UPLOAD = "GCSUpload";
  public static final String S3_UPLOAD = "S3Upload";
  public static final String BUILD_AND_PUSH_GCR = "BuildAndPushGCR";
  public static final String BUILD_AND_PUSH_ACR = "BuildAndPushACR";
  public static final String BUILD_AND_PUSH_ECR = "BuildAndPushECR";
  public static final String BUILD_AND_PUSH_DOCKER_REGISTRY = "BuildAndPushDockerRegistry";
  public static final String NG_FILE = "NgFile";
  public static final String QUEUE = "Lock";
  public static final String CLOUDFORMATION_CREATE_STACK_STEP = "CreateStack";
  public static final String CLOUDFORMATION_DELETE_STACK_STEP = "DeleteStack";
  public static final String SERVERLESS_AWS_LAMBDA_DEPLOY = "ServerlessAwsLambdaDeploy";
  public static final String SERVERLESS_AWS_LAMBDA_ROLLBACK = "ServerlessAwsLambdaRollback";
  public static final String CLOUDFORMATION_ROLLBACK_STACK_STEP = "RollbackStack";
  public static final String INFRASTRUCTURE = "infrastructure";
  public static final String JENKINS_BUILD = "JenkinsBuild";
  public static final String COMMAND = "Command";
  public static final String STRATEGY_NODE = "StrategyNode";
  public static final String AZURE_SLOT_DEPLOYMENT_STEP = "AzureSlotDeployment";
  public static final String AZURE_TRAFFIC_SHIFT_STEP = "AzureTrafficShift";
  public static final String AZURE_SWAP_SLOT_STEP = "AzureSwapSlot";
  public static final String AZURE_WEBAPP_ROLLBACK_STEP = "AzureWebAppRollback";
  public static final String EMAIL = "EMAIL";
  public static final String ECS_ROLLING_DEPLOY = "EcsRollingDeploy";
  public static final String ECS_ROLLING_ROLLBACK = "EcsRollingRollback";
  public static final String ECS_CANARY_DEPLOY = "EcsCanaryDeploy";
  public static final String ECS_CANARY_DELETE = "EcsCanaryDelete";
  public static final String AZURE_CREATE_ARM_RESOURCE_STEP = "AzureCreateARMResource";
  public static final String AZURE_CREATE_BP_RESOURCE_STEP = "AzureCreateBPResource";
  public static final String AZURE_ROLLBACK_ARM_RESOURCE_STEP = "AzureARMRollback";
  public static final String ECS_BLUE_GREEN_CREATE_SERVICE = "EcsBlueGreenCreateService";
  public static final String ECS_BLUE_GREEN_SWAP_TARGET_GROUPS = "EcsBlueGreenSwapTargetGroups";
  public static final String ECS_BLUE_GREEN_ROLLBACK = "EcsBlueGreenRollback";
  public static final String WAIT_STEP = "Wait";
  public static final String SHELL_SCRIPT_PROVISION_STEP = "ShellScriptProvision";
}
