/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.idp.scorecard.datasourcelocations.constants;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.IDP)
@UtilityClass
public class DataSourceLocations {
  // Github
  public static final String GITHUB_MEAN_TIME_TO_MERGE_PR = "github_mean_time_to_merge_pr";
  public static final String GITHUB_IS_BRANCH_PROTECTION_SET = "github_is_branch_protection_set";
  public static final String GITHUB_FILE_EXISTS = "github_is_file_exists";

  public static final String REPO_SCM = "{REPO_SCM}";
  public static final String REPOSITORY_OWNER = "{REPOSITORY_OWNER}";
  public static final String REPOSITORY_NAME = "{REPOSITORY_NAME}";
  public static final String REPOSITORY_BRANCH = "{REPOSITORY_BRANCH}";

  // Catalog
  public static final String CATALOG = "catalog";

  // PagerDuty
  public static final String PAGERDUTY_SERVICE_ID = "{SERVICE_ID}";
  public static final String PAGERDUTY_ANNOTATION_MISSING_ERROR =
      "PagerDuty annotation is missing in the catalog info YAML";
}
