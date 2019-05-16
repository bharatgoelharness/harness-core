package software.wings.licensing.violations.checkers;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.data.structure.CollectionUtils;
import io.harness.data.structure.EmptyPredicate;
import software.wings.beans.EntityType;
import software.wings.beans.FeatureUsageViolation;
import software.wings.beans.FeatureUsageViolation.Usage;
import software.wings.beans.FeatureViolation;
import software.wings.beans.GraphNode;
import software.wings.beans.Workflow;
import software.wings.beans.security.UserGroup;
import software.wings.licensing.violations.FeatureViolationChecker;
import software.wings.licensing.violations.RestrictedFeature;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import javax.validation.constraints.NotNull;

@Singleton
public class LiveNotificationViolationChecker implements FeatureViolationChecker, WorkflowViolationCheckerMixin {
  private UserGroupService userGroupService;
  private WorkflowService workflowService;

  @Inject
  public LiveNotificationViolationChecker(UserGroupService userGroupService, WorkflowService workflowService) {
    this.userGroupService = userGroupService;
    this.workflowService = workflowService;
  }

  public static final Predicate<GraphNode> IS_JIRA_STATE_PRESENT = gn -> {
    StateType stateType = StateType.valueOf(gn.getType());
    return stateType == StateType.JIRA_CREATE_UPDATE;
  };

  private static final Predicate<GraphNode> IS_SNOW_STATE_PRESENT = gn -> {
    StateType stateType = StateType.valueOf(gn.getType());
    return stateType == StateType.SERVICENOW_CREATE_UPDATE;
  };

  @Override
  public List<FeatureViolation> getViolationsForCommunityAccount(String accountId) {
    List<FeatureViolation> featureViolationList = null;
    List<Usage> usages = new ImmutableList.Builder<Usage>()
                             .addAll(getSlackUsages(accountId))
                             .addAll(getJiraUsages(accountId))
                             .addAll(getSnowUsages(accountId))
                             .build();
    if (isNotEmpty(usages)) {
      featureViolationList =
          Collections.singletonList(new FeatureUsageViolation(RestrictedFeature.LIVE_NOTIFICATIONS, usages));
    }

    return CollectionUtils.emptyIfNull(featureViolationList);
  }

  // get usages of Slack under user groups
  private List<Usage> getSlackUsages(String accountId) {
    List<Usage> usages = new ArrayList<>();
    PageRequest<UserGroup> req = aPageRequest().addFilter(ACCOUNT_ID_KEY, Operator.EQ, accountId).build();

    PageResponse<UserGroup> response = userGroupService.list(accountId, req, false);
    for (UserGroup userGroup : response) {
      if (hasSlack(userGroup)) {
        usages.add(Usage.builder()
                       .entityId(userGroup.getUuid())
                       .entityType(EntityType.USER_GROUP.toString())
                       .entityName(userGroup.getName())
                       .build());
      }
    }

    return usages;
  }

  // gets usages of JIRA state under workflows
  private List<Usage> getJiraUsages(String accountId) {
    return getWorkflowViolationUsages(getAllWorkflowsByAccountId(accountId), IS_JIRA_STATE_PRESENT);
  }

  // gets usages of Service Now state under workflows
  private List<Usage> getSnowUsages(String accountId) {
    return getWorkflowViolationUsages(getAllWorkflowsByAccountId(accountId), IS_SNOW_STATE_PRESENT);
  }

  private static boolean hasSlack(UserGroup userGroup) {
    return userGroup.getNotificationSettings() != null && userGroup.getNotificationSettings().getSlackConfig() != null
        && !EmptyPredicate.isEmpty(userGroup.getNotificationSettings().getSlackConfig().getOutgoingWebhookUrl());
  }

  private List<Workflow> getAllWorkflowsByAccountId(@NotNull String accountId) {
    PageRequest<Workflow> pageRequest =
        aPageRequest().withLimit(PageRequest.UNLIMITED).addFilter(ACCOUNT_ID_KEY, Operator.EQ, accountId).build();

    return workflowService.listWorkflows(pageRequest).getResponse();
  }
}
