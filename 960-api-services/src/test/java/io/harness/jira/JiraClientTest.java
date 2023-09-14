/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.jira;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.FERNANDOD;
import static io.harness.rule.OwnerRule.NAMANG;
import static io.harness.rule.OwnerRule.YUVRAJ;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.HttpResponseException;
import io.harness.rule.Owner;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@OwnedBy(CDC)
@RunWith(MockitoJUnitRunner.class)
public class JiraClientTest extends CategoryTest {
  /**
   * The Wire mock rule.
   */
  @Rule
  public WireMockRule wireMockRule =
      new WireMockRule(WireMockConfiguration.wireMockConfig()
                           .usingFilesUnderClasspath("960-api-services/src/test/resources")
                           .disableRequestJournal()
                           .port(0));
  public static JiraClient jiraClient;
  private static String url;
  private static final String ISSUE_KEY = "TJI-123";
  private static final String INVALID_ISSUE_KEY = "TJI-321";

  private static final String FILTER_FIELDS = "priority,project,issuetype,status,timetracking,labels";

  @Before
  public void setup() {
    url = String.format("http://localhost:%d", wireMockRule.port());
    JiraInternalConfig jiraInternalConfig = JiraInternalConfig.builder().jiraUrl(url).authToken("authToken").build();
    jiraClient = new JiraClient(jiraInternalConfig);
  }

  @Test
  @Owner(developers = YUVRAJ)
  @Category(UnitTests.class)
  public void testConvertNewIssueTypeMetaData() {
    String projectKey = "TES";
    JiraIssueCreateMetadataNG jiraIssueCreateMetadataNG = new JiraIssueCreateMetadataNG();
    JiraIssueCreateMetadataNGIssueTypes jiraIssueCreateMetadataNGIssueTypes = new JiraIssueCreateMetadataNGIssueTypes();
    Map<String, JiraIssueTypeNG> issueTypeNGMap = new HashMap<>();
    JiraIssueTypeNG jiraIssueTypeNG1 = new JiraIssueTypeNG();
    jiraIssueTypeNG1.setSubTask(true);
    jiraIssueTypeNG1.setId("10000");
    jiraIssueTypeNG1.setName("Sub-task");
    jiraIssueTypeNG1.setDescription("The sub-task of the issue");
    JiraIssueTypeNG jiraIssueTypeNG2 = new JiraIssueTypeNG();
    jiraIssueTypeNG2.setSubTask(false);
    jiraIssueTypeNG2.setId("10001");
    jiraIssueTypeNG2.setName("Task");
    jiraIssueTypeNG2.setDescription("A task that needs to be done.");
    issueTypeNGMap.put("Task", jiraIssueTypeNG2);
    issueTypeNGMap.put("Sub-task", jiraIssueTypeNG1);
    jiraIssueCreateMetadataNGIssueTypes.setIssueTypes(issueTypeNGMap);
    jiraClient.originalMetadataFromNewIssueTypeMetadata(
        projectKey, jiraIssueCreateMetadataNG, jiraIssueCreateMetadataNGIssueTypes);
    assertThat(jiraIssueCreateMetadataNG.getProjects().size()).isEqualTo(1);
    assertThat(jiraIssueCreateMetadataNG.getProjects().get("TES").getIssueTypes().size()).isEqualTo(2);
    assertThat(jiraIssueCreateMetadataNG.getProjects().get("TES").getIssueTypes().get("Task").getId())
        .isEqualTo("10001");
    assertThat(jiraIssueCreateMetadataNG.getProjects().get("TES").getIssueTypes().get("Sub-task").getId())
        .isEqualTo("10000");
  }

  @Test
  @Owner(developers = YUVRAJ)
  @Category(UnitTests.class)
  public void testConvertNewFieldsMetaData() {
    String projectKey = "TES";
    JiraIssueCreateMetadataNG jiraIssueCreateMetadataNG = new JiraIssueCreateMetadataNG();
    JiraIssueCreateMetadataNGFields jiraIssueCreateMetadataNGFields = new JiraIssueCreateMetadataNGFields();
    JiraIssueTypeNG jiraIssueTypeNG = new JiraIssueTypeNG();
    jiraIssueTypeNG.setSubTask(false);
    jiraIssueTypeNG.setId("10001");
    jiraIssueTypeNG.setName("Task");
    jiraIssueTypeNG.setDescription("A task that needs to be done.");
    Map<String, JiraFieldNG> fieldNGMap = new HashMap<>();
    JiraFieldNG jiraFieldNG1 = new JiraFieldNG();
    jiraFieldNG1.setKey("assignee");
    jiraFieldNG1.setName("Assignee");
    jiraFieldNG1.setRequired(false);
    JiraFieldNG jiraFieldNG2 = new JiraFieldNG();
    jiraFieldNG2.setKey("duedate");
    jiraFieldNG2.setName("Due Date");
    jiraFieldNG2.setRequired(false);
    JiraFieldNG jiraFieldNG4 = new JiraFieldNG();
    jiraFieldNG4.setKey("description");
    jiraFieldNG4.setName("Description");
    jiraFieldNG4.setRequired(false);
    JiraFieldNG jiraFieldNG3 = new JiraFieldNG();
    jiraFieldNG3.setKey("summary");
    jiraFieldNG3.setName("Summary");
    jiraFieldNG3.setRequired(true);
    JiraFieldNG jiraFieldNG5 = new JiraFieldNG();
    jiraFieldNG5.setKey("labels");
    jiraFieldNG5.setName("Labels");
    jiraFieldNG5.setRequired(false);
    JiraFieldNG jiraFieldNG6 = new JiraFieldNG();
    jiraFieldNG6.setKey("priority");
    jiraFieldNG6.setName("Priority");
    jiraFieldNG6.setRequired(false);
    fieldNGMap.put("assignee", jiraFieldNG1);
    fieldNGMap.put("priority", jiraFieldNG6);
    fieldNGMap.put("labels", jiraFieldNG5);
    fieldNGMap.put("summary", jiraFieldNG3);
    fieldNGMap.put("description", jiraFieldNG4);
    fieldNGMap.put("duedate", jiraFieldNG2);
    jiraIssueCreateMetadataNGFields.setFields(fieldNGMap);
    jiraClient.originalMetadataFromNewFieldsMetadata(
        projectKey, jiraIssueCreateMetadataNG, jiraIssueTypeNG, jiraIssueCreateMetadataNGFields);
    assertThat(jiraIssueCreateMetadataNG.getProjects().size()).isEqualTo(1);
    assertThat(jiraIssueCreateMetadataNG.getProjects().get("TES").getIssueTypes().size()).isEqualTo(1);
    assertThat(jiraIssueCreateMetadataNG.getProjects().get("TES").getIssueTypes().get("Task").getId())
        .isEqualTo("10001");
    assertThat(jiraIssueCreateMetadataNG.getProjects().get("TES").getIssueTypes().get("Task").getFields().size())
        .isEqualTo(6);
    assertThat(jiraIssueCreateMetadataNG.getProjects().get("TES").getIssueTypes().get("Task").getFields().get("Labels"))
        .isEqualTo(jiraFieldNG5);
  }

  @Test
  @Owner(developers = FERNANDOD)
  @Category(UnitTests.class)
  public void shouldRetryOnExceptionNotMatch() {
    assertThat(jiraClient.createRetryOnException().test(new IOException())).isFalse();
    assertThat(jiraClient.createRetryOnException().test(new HttpResponseException(404, "NOT_FOUND"))).isFalse();
    assertThat(jiraClient.createRetryOnException().test(new HttpResponseException(500, "SERVER_ERROR"))).isFalse();
  }

  @Test
  @Owner(developers = FERNANDOD)
  @Category(UnitTests.class)
  public void shouldRetryOnExceptionMatch() {
    assertThat(jiraClient.createRetryOnException().test(new HttpResponseException(429, "TOO_MANY_REQUESTS"))).isTrue();
  }

  @Test
  @Owner(developers = NAMANG)
  @Category(UnitTests.class)
  public void testGetIssueWithFilterFields() {
    JiraIssueNG jiraIssueNG = jiraClient.getIssue(ISSUE_KEY, FILTER_FIELDS);
    assertThat(jiraIssueNG.getId()).isEqualTo("476152");
    assertThat(jiraIssueNG.getUrl())
        .isEqualTo(String.format("http://localhost:%d/browse/TJI-135040", wireMockRule.port()));
    assertThat(jiraIssueNG.getKey()).isEqualTo("TJI-135040");

    Map<String, Object> fields = jiraIssueNG.getFields();
    assertThat(fields).hasSize(15);
    // status field
    assertThat(fields).containsEntry(JiraConstantsNG.STATUS_NAME, "To Do");
    assertThat(fields).containsEntry("Priority", "P4");
    // project field
    assertThat(fields).containsEntry("Project Name", "Test - JIRA Integration");
    assertThat(fields).containsEntry("Project Key", "TJI");
    // issueType field
    assertThat(fields).containsEntry("Issue Type", "Task");
    // time tracking field
    assertThat(fields).containsEntry(JiraConstantsNG.ORIGINAL_ESTIMATE_NAME, "1h");
    assertThat(fields).containsEntry(JiraConstantsNG.REMAINING_ESTIMATE_NAME, "1h");
    // array field
    assertThat((ArrayList<String>) fields.get("Labels")).hasSize(1);
    assertThat(((ArrayList<String>) fields.get("Labels")).get(0)).isEqualTo("edf");

    Map<String, String> namesToKeys = jiraIssueNG.getFieldNameToKeys();
    assertThat(namesToKeys).hasSize(5);
    assertThat(namesToKeys).containsEntry(JiraConstantsNG.STATUS_NAME, JiraConstantsNG.STATUS_KEY);
    assertThat(namesToKeys).containsEntry("Priority", "priority");
    assertThat(namesToKeys).containsEntry("Labels", "labels");
    assertThat(namesToKeys).containsEntry(JiraConstantsNG.ORIGINAL_ESTIMATE_NAME, JiraConstantsNG.TIME_TRACKING_KEY);
    assertThat(namesToKeys).containsEntry(JiraConstantsNG.REMAINING_ESTIMATE_NAME, JiraConstantsNG.TIME_TRACKING_KEY);

    // throws a 404 exception but is ignored
    assertThat(jiraClient.getIssue(INVALID_ISSUE_KEY, FILTER_FIELDS)).isNull();
  }

  @Test
  @Owner(developers = NAMANG)
  @Category(UnitTests.class)
  public void testGetIssueWithFilterFieldsAsNull() {
    JiraIssueNG jiraIssueNG = jiraClient.getIssue(ISSUE_KEY, null);
    assertThat(jiraIssueNG.getId()).isEqualTo("476152");
    assertThat(jiraIssueNG.getUrl())
        .isEqualTo(String.format("http://localhost:%d/browse/TJI-135040", wireMockRule.port()));
    assertThat(jiraIssueNG.getKey()).isEqualTo("TJI-135040");

    Map<String, Object> fields = jiraIssueNG.getFields();
    assertThat(fields).hasSize(15);
    // status field
    assertThat(fields).containsEntry(JiraConstantsNG.STATUS_NAME, "To Do");
    assertThat(fields).containsEntry("Priority", "P4");
    // project field
    assertThat(fields).containsEntry("Project Name", "Test - JIRA Integration");
    assertThat(fields).containsEntry("Project Key", "TJI");
    // issueType field
    assertThat(fields).containsEntry("Issue Type", "Task");
    // timetracking field
    assertThat(fields).containsEntry(JiraConstantsNG.ORIGINAL_ESTIMATE_NAME, "1h");
    assertThat(fields).containsEntry(JiraConstantsNG.REMAINING_ESTIMATE_NAME, "1h");
    // array field
    assertThat((ArrayList<String>) fields.get("Labels")).hasSize(1);
    assertThat(((ArrayList<String>) fields.get("Labels")).get(0)).isEqualTo("edf");

    Map<String, String> namesToKeys = jiraIssueNG.getFieldNameToKeys();
    assertThat(namesToKeys).hasSize(6);
    assertThat(namesToKeys).containsEntry(JiraConstantsNG.STATUS_NAME, JiraConstantsNG.STATUS_KEY);
    assertThat(namesToKeys).containsEntry("Priority", "priority");
    assertThat(namesToKeys).containsEntry("Labels", "labels");
    assertThat(namesToKeys).containsEntry(JiraConstantsNG.ORIGINAL_ESTIMATE_NAME, JiraConstantsNG.TIME_TRACKING_KEY);
    assertThat(namesToKeys).containsEntry(JiraConstantsNG.REMAINING_ESTIMATE_NAME, JiraConstantsNG.TIME_TRACKING_KEY);
    assertThat(namesToKeys).containsEntry("One Line Update", "customfield_0001");

    // throws a 404 exception but is ignored
    // filterFields fields will be called via null (no mapping for blank query param)
    assertThat(jiraClient.getIssue(INVALID_ISSUE_KEY, "   ")).isNull();
  }

  @Test
  @Owner(developers = NAMANG)
  @Category(UnitTests.class)
  public void testGetIssue() {
    JiraIssueNG jiraIssueNG = jiraClient.getIssue(ISSUE_KEY);
    assertThat(jiraIssueNG.getId()).isEqualTo("476152");
    assertThat(jiraIssueNG.getUrl())
        .isEqualTo(String.format("http://localhost:%d/browse/TJI-135040", wireMockRule.port()));
    assertThat(jiraIssueNG.getKey()).isEqualTo("TJI-135040");

    Map<String, Object> fields = jiraIssueNG.getFields();
    assertThat(fields).hasSize(15);
    // status field
    assertThat(fields).containsEntry(JiraConstantsNG.STATUS_NAME, "To Do");
    assertThat(fields).containsEntry("Priority", "P4");
    // project field
    assertThat(fields).containsEntry("Project Name", "Test - JIRA Integration");
    assertThat(fields).containsEntry("Project Key", "TJI");
    // issueType field
    assertThat(fields).containsEntry("Issue Type", "Task");
    // timetracking field
    assertThat(fields).containsEntry(JiraConstantsNG.ORIGINAL_ESTIMATE_NAME, "1h");
    assertThat(fields).containsEntry(JiraConstantsNG.REMAINING_ESTIMATE_NAME, "1h");
    // array field
    assertThat((ArrayList<String>) fields.get("Labels")).hasSize(1);
    assertThat(((ArrayList<String>) fields.get("Labels")).get(0)).isEqualTo("edf");

    Map<String, String> namesToKeys = jiraIssueNG.getFieldNameToKeys();
    assertThat(namesToKeys).hasSize(5);
    assertThat(namesToKeys).containsEntry(JiraConstantsNG.STATUS_NAME, JiraConstantsNG.STATUS_KEY);
    assertThat(namesToKeys).containsEntry("Priority", "priority");
    assertThat(namesToKeys).containsEntry("Labels", "labels");
    assertThat(namesToKeys).containsEntry(JiraConstantsNG.ORIGINAL_ESTIMATE_NAME, JiraConstantsNG.TIME_TRACKING_KEY);
    assertThat(namesToKeys).containsEntry(JiraConstantsNG.REMAINING_ESTIMATE_NAME, JiraConstantsNG.TIME_TRACKING_KEY);

    // throws a 404 exception but is ignored
    assertThat(jiraClient.getIssue(INVALID_ISSUE_KEY)).isNull();
  }
}
