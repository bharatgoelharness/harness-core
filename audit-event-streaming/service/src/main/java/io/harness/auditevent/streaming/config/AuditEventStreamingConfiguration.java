/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.auditevent.streaming.config;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.harness.serializer.YamlUtils;

import java.io.File;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Slf4j
@Configuration
public class AuditEventStreamingConfiguration {
  @Bean
  public static AuditEventStreamingConfig auditEventStreamingConfig(Environment environment) throws IOException {
    String configFilePath = environment.getProperty("config-file", "application.yml");
    log.info("audit-event-streaming configFilePath: {}", configFilePath);
    File configFile = new File(configFilePath);
    return new YamlUtils().read(FileUtils.readFileToString(configFile, UTF_8), AuditEventStreamingConfig.class);
  }
}