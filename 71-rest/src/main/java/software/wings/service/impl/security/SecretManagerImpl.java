package software.wings.service.impl.security;

import static com.google.common.collect.Sets.newHashSet;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.encryption.EncryptionReflectUtils.getEncryptedFields;
import static io.harness.encryption.EncryptionReflectUtils.getEncryptedRefField;
import static io.harness.eraro.ErrorCode.CYBERARK_OPERATION_ERROR;
import static io.harness.eraro.ErrorCode.DEFAULT_ERROR_CODE;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.eraro.ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION;
import static io.harness.exception.WingsException.USER;
import static io.harness.expression.SecretString.SECRET_MASK;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.persistence.HQuery.excludeCount;
import static io.harness.security.encryption.EncryptionType.KMS;
import static io.harness.security.encryption.EncryptionType.LOCAL;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.beans.ServiceVariable.ENCRYPTED_VALUE_KEY;
import static software.wings.security.EnvFilter.FilterType.NON_PROD;
import static software.wings.security.EnvFilter.FilterType.PROD;
import static software.wings.security.encryption.SecretChangeLog.ENCRYPTED_DATA_ID_KEY;
import static software.wings.service.impl.security.VaultServiceImpl.VAULT_VAILDATION_URL;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;
import static software.wings.service.intfc.security.VaultService.DEFAULT_BASE_PATH;
import static software.wings.service.intfc.security.VaultService.DEFAULT_KEY_NAME;
import static software.wings.service.intfc.security.VaultService.KEY_SPEARATOR;
import static software.wings.service.intfc.security.VaultService.PATH_SEPARATOR;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.TreeBasedTable;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.inject.Inject;

import com.mongodb.DuplicateKeyException;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.eraro.ErrorCode;
import io.harness.exception.KmsOperationException;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import io.harness.persistence.UuidAware;
import io.harness.queue.Queue;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import io.harness.stream.BoundedInputStream;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.KmsTransitionEvent;
import software.wings.beans.Account;
import software.wings.beans.AwsSecretsManagerConfig;
import software.wings.beans.AzureVaultConfig;
import software.wings.beans.Base;
import software.wings.beans.ConfigFile;
import software.wings.beans.ConfigFile.ConfigFileKeys;
import software.wings.beans.CyberArkConfig;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Event.Type;
import software.wings.beans.KmsConfig;
import software.wings.beans.LocalEncryptionConfig;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.beans.VaultConfig;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.KmsSetupAlert;
import software.wings.dl.WingsPersistence;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.GenericEntityFilter.FilterType;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.UserThreadLocal;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedData.EncryptedDataKeys;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.security.encryption.SecretChangeLog;
import software.wings.security.encryption.SecretUsageLog;
import software.wings.service.impl.AuditServiceHelper;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.security.AwsSecretsManagerService;
import software.wings.service.intfc.security.AzureSecretsManagerService;
import software.wings.service.intfc.security.CyberArkService;
import software.wings.service.intfc.security.KmsService;
import software.wings.service.intfc.security.LocalEncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.security.SecretManagerConfigService;
import software.wings.service.intfc.security.VaultService;
import software.wings.settings.RestrictionsAndAppEnvMap;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;
import software.wings.settings.UsageRestrictions.AppEnvRestriction;
import software.wings.utils.Validator;
import software.wings.utils.WingsReflectionUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by rsingh on 10/30/17.
 */
@Slf4j
public class SecretManagerImpl implements SecretManager {
  private static final String ILLEGAL_CHARACTERS = "[~!@#$%^&*'\"/?<>,;]";
  private static final String URL_ROOT_PREFIX = "//";
  // Prefix YAML ingestion generated secret names with this prefix
  private static final String YAML_PREFIX = "YAML_";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private KmsService kmsService;
  @Inject private VaultService vaultService;
  @Inject private AwsSecretsManagerService secretsManagerService;
  @Inject private AzureVaultService azureVaultService;
  @Inject private CyberArkService cyberArkService;
  @Inject private AlertService alertService;
  @Inject private FileService fileService;
  @Inject private UsageRestrictionsService usageRestrictionsService;
  @Inject private SettingsService settingsService;
  @Inject private Queue<KmsTransitionEvent> transitionKmsQueue;
  @Inject private ServiceVariableService serviceVariableService;
  @Inject private ConfigService configService;
  @Inject private AppService appService;
  @Inject private EnvironmentService envService;
  @Inject private AuditServiceHelper auditServiceHelper;
  @Inject private LocalEncryptionService localEncryptionService;
  @Inject private UserService userService;
  @Inject private SecretManagerConfigService secretManagerConfigService;
  @Inject private AzureSecretsManagerService azureSecretsManagerService;

  @Override
  public EncryptionType getEncryptionType(String accountId) {
    return secretManagerConfigService.getEncryptionType(accountId);
  }

  @Override
  public List<SecretManagerConfig> listSecretManagers(String accountId) {
    return secretManagerConfigService.listSecretManagers(accountId, true);
  }

  @Override
  public EncryptedData encrypt(EncryptionType encryptionType, String accountId, SettingVariableTypes settingType,
      char[] secret, String path, EncryptedData encryptedData, String secretName, UsageRestrictions usageRestrictions) {
    String toEncrypt = secret == null ? null : String.valueOf(secret);
    // Need to initialize an EncryptedData instance to carry the 'path' value for delegate to validate against.
    if (encryptedData == null) {
      encryptedData = EncryptedData.builder()
                          .name(secretName)
                          .path(path)
                          .encryptionType(encryptionType)
                          .accountId(accountId)
                          .type(settingType)
                          .enabled(true)
                          .parentIds(new HashSet<>())
                          .build();
    }

    EncryptedData rv;
    switch (encryptionType) {
      case LOCAL:
        LocalEncryptionConfig localEncryptionConfig = localEncryptionService.getEncryptionConfig(accountId);
        rv = localEncryptionService.encrypt(secret, accountId, localEncryptionConfig);
        rv.setType(settingType);
        break;

      case KMS:
        final KmsConfig kmsConfig = (KmsConfig) secretManagerConfigService.getDefaultSecretManager(accountId);
        rv = kmsService.encrypt(secret, accountId, kmsConfig);
        rv.setKmsId(kmsConfig.getUuid());
        break;

      case VAULT:
        final VaultConfig vaultConfig = (VaultConfig) secretManagerConfigService.getDefaultSecretManager(accountId);
        encryptedData.setKmsId(vaultConfig.getUuid());
        rv = vaultService.encrypt(secretName, toEncrypt, accountId, settingType, vaultConfig, encryptedData);
        rv.setKmsId(vaultConfig.getUuid());
        break;

      case AWS_SECRETS_MANAGER:
        final AwsSecretsManagerConfig secretsManagerConfig =
            (AwsSecretsManagerConfig) secretManagerConfigService.getDefaultSecretManager(accountId);
        encryptedData.setKmsId(secretsManagerConfig.getUuid());
        rv = secretsManagerService.encrypt(
            secretName, toEncrypt, accountId, settingType, secretsManagerConfig, encryptedData);
        rv.setKmsId(secretsManagerConfig.getUuid());
        break;

      case AZURE_VAULT:
        final AzureVaultConfig azureConfig =
            (AzureVaultConfig) secretManagerConfigService.getDefaultSecretManager(accountId);
        encryptedData.setKmsId(azureConfig.getUuid());
        rv = azureVaultService.encrypt(secretName, toEncrypt, accountId, settingType, azureConfig, encryptedData);
        rv.setKmsId(azureConfig.getUuid());
        break;

      case CYBERARK:
        final CyberArkConfig cyberArkConfig =
            (CyberArkConfig) secretManagerConfigService.getDefaultSecretManager(accountId);
        encryptedData.setKmsId(cyberArkConfig.getUuid());
        if (isNotEmpty(encryptedData.getPath())) {
          // CyberArk encrypt need to use decrypt of the secret reference as a way of validating the reference is valid.
          // If the  CyberArk reference is not valid, an exception will be throw.
          cyberArkService.decrypt(encryptedData, accountId, cyberArkConfig);
        } else if (isNotEmpty(toEncrypt)) {
          KmsConfig fallbackKmsConfig = kmsService.getGlobalKmsConfig();
          if (fallbackKmsConfig != null) {
            logger.info(
                "CyberArk doesn't support creating new secret. This new secret text will be created in the global KMS SecretStore instead");
            rv = kmsService.encrypt(secret, accountId, fallbackKmsConfig);
            rv.setEncryptionType(KMS);
          } else {
            logger.info(
                "CyberArk doesn't support creating new secret. This new secret text will be created in the local Harness SecretStore instead");
            localEncryptionConfig = localEncryptionService.getEncryptionConfig(accountId);
            rv = localEncryptionService.encrypt(secret, accountId, localEncryptionConfig);
            rv.setEncryptionType(LOCAL);
          }
          rv.setName(secretName);
          rv.setType(settingType);
          rv.setUsageRestrictions(usageRestrictions);
          return rv;
        }
        rv = encryptedData;
        break;

      default:
        throw new IllegalStateException("Invalid type:  " + encryptionType);
    }
    rv.setName(secretName);
    rv.setEncryptionType(encryptionType);
    rv.setType(settingType);
    rv.setUsageRestrictions(usageRestrictions);
    return rv;
  }

  public String encrypt(String accountId, String secret, UsageRestrictions usageRestrictions) {
    EncryptedData encryptedData =
        encrypt(getEncryptionType(accountId), accountId, SettingVariableTypes.APM_VERIFICATION, secret.toCharArray(),
            null, null, UUID.randomUUID().toString(), usageRestrictions);
    String recordId = wingsPersistence.save(encryptedData);
    generateAuditForEncryptedRecord(accountId, null, recordId);
    return recordId;
  }

  public Optional<EncryptedDataDetail> encryptedDataDetails(String accountId, String fieldName, String refId) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, refId);
    if (encryptedData == null) {
      logger.info("No encrypted record set for field {} for id: {}", fieldName, refId);
      return Optional.empty();
    }
    EncryptionConfig encryptionConfig =
        getSecretManager(accountId, encryptedData.getKmsId(), encryptedData.getEncryptionType());

    return Optional.of(EncryptedDataDetail.builder()
                           .encryptionType(encryptedData.getEncryptionType())
                           .encryptedData(encryptedData)
                           .encryptionConfig(encryptionConfig)
                           .fieldName(fieldName)
                           .build());
  }

  @Override
  public List<EncryptedDataDetail> getEncryptionDetails(
      EncryptableSetting object, String appId, String workflowExecutionId) {
    // NOTE: appId should not used anywhere in this method
    if (object.isDecrypted()) {
      return Collections.emptyList();
    }

    List<Field> encryptedFields = object.getEncryptedFields();
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    try {
      for (Field f : encryptedFields) {
        f.setAccessible(true);
        Field encryptedRefField = getEncryptedRefField(f, object);
        encryptedRefField.setAccessible(true);
        if (f.get(object) != null && !WingsReflectionUtils.isSetByYaml(object, encryptedRefField)) {
          Preconditions.checkState(
              encryptedRefField.get(object) == null, "both encrypted and non encrypted field set for " + object);
          encryptedDataDetails.add(EncryptedDataDetail.builder()
                                       .encryptionType(LOCAL)
                                       .encryptedData(EncryptedData.builder()
                                                          .encryptionKey(object.getAccountId())
                                                          .encryptedValue((char[]) f.get(object))
                                                          .build())
                                       .fieldName(f.getName())
                                       .build());
        } else {
          String id = (String) encryptedRefField.get(object);
          if (WingsReflectionUtils.isSetByYaml(object, encryptedRefField)) {
            id = id.substring(id.indexOf(':') + 1);
          }

          EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, id);
          if (encryptedData == null) {
            logger.info("No encrypted record set for field {} for id: {}", f.getName(), id);
            continue;
          }
          EncryptionConfig encryptionConfig =
              getSecretManager(object.getAccountId(), encryptedData.getKmsId(), encryptedData.getEncryptionType());

          EncryptedDataDetail encryptedDataDetail = EncryptedDataDetail.builder()
                                                        .encryptionType(encryptedData.getEncryptionType())
                                                        .encryptedData(encryptedData)
                                                        .encryptionConfig(encryptionConfig)
                                                        .fieldName(f.getName())
                                                        .build();

          encryptedDataDetails.add(encryptedDataDetail);

          if (isNotEmpty(workflowExecutionId)) {
            WorkflowExecution workflowExecution = wingsPersistence.get(WorkflowExecution.class, workflowExecutionId);
            if (workflowExecution == null) {
              logger.warn("No workflow execution with id {} found.", workflowExecutionId);
            } else {
              SecretUsageLog usageLog = SecretUsageLog.builder()
                                            .encryptedDataId(encryptedData.getUuid())
                                            .workflowExecutionId(workflowExecutionId)
                                            .accountId(encryptedData.getAccountId())
                                            .envId(workflowExecution.getEnvId())
                                            .build();
              usageLog.setAppId(workflowExecution.getAppId());
              wingsPersistence.save(usageLog);
            }
          }
        }
      }
    } catch (IllegalAccessException e) {
      throw new WingsException(e);
    }

    return encryptedDataDetails;
  }

  @Override
  public void maskEncryptedFields(EncryptableSetting object) {
    List<Field> encryptedFields = object.getEncryptedFields();
    try {
      for (Field f : encryptedFields) {
        f.setAccessible(true);
        f.set(object, ENCRYPTED_FIELD_MASK.toCharArray());
      }
    } catch (IllegalAccessException e) {
      throw new WingsException(e);
    }
  }

  @Override
  public void resetUnchangedEncryptedFields(EncryptableSetting sourceObject, EncryptableSetting destinationObject) {
    Validator.equalCheck(sourceObject.getClass().getName(), destinationObject.getClass().getName());

    List<Field> encryptedFields = sourceObject.getEncryptedFields();
    try {
      for (Field f : encryptedFields) {
        f.setAccessible(true);
        if (java.util.Arrays.equals((char[]) f.get(destinationObject), ENCRYPTED_FIELD_MASK.toCharArray())) {
          f.set(destinationObject, f.get(sourceObject));
        }
      }
    } catch (IllegalAccessException e) {
      throw new WingsException(e);
    }
  }

  @Override
  public PageResponse<SecretUsageLog> getUsageLogs(PageRequest<SecretUsageLog> pageRequest, String accountId,
      String entityId, SettingVariableTypes variableType) throws IllegalAccessException {
    final List<String> secretIds = getSecretIds(entityId, variableType);

    pageRequest.addFilter("encryptedDataId", Operator.IN, secretIds.toArray());
    pageRequest.addFilter(ACCOUNT_ID_KEY, Operator.EQ, accountId);
    PageResponse<SecretUsageLog> response = wingsPersistence.query(SecretUsageLog.class, pageRequest);
    response.getResponse().forEach(secretUsageLog -> {
      if (isNotEmpty(secretUsageLog.getWorkflowExecutionId())) {
        WorkflowExecution workflowExecution =
            wingsPersistence.get(WorkflowExecution.class, secretUsageLog.getWorkflowExecutionId());
        if (workflowExecution != null) {
          secretUsageLog.setWorkflowExecutionName(workflowExecution.normalizedName());
        }
      }
    });
    return response;
  }

  @Override
  public long getUsageLogsSize(String entityId, SettingVariableTypes variableType) throws IllegalAccessException {
    final List<String> secretIds = getSecretIds(entityId, variableType);
    return wingsPersistence.createQuery(SecretUsageLog.class, excludeAuthority)
        .field(SecretUsageLog.ENCRYPTED_DATA_ID_KEY)
        .in(secretIds)
        .count();
  }

  @Override
  public List<SecretChangeLog> getChangeLogs(String accountId, String entityId, SettingVariableTypes variableType)
      throws IllegalAccessException {
    EncryptedData encryptedData = null;
    if (variableType == SettingVariableTypes.SECRET_TEXT) {
      encryptedData = wingsPersistence.get(EncryptedData.class, entityId);
    }

    return getChangeLogsInternal(accountId, entityId, encryptedData, variableType);
  }

  private List<SecretChangeLog> getChangeLogsInternal(String accountId, String entityId, EncryptedData encryptedData,
      SettingVariableTypes variableType) throws IllegalAccessException {
    final List<String> secretIds = getSecretIds(entityId, variableType);
    List<SecretChangeLog> secretChangeLogs = wingsPersistence.createQuery(SecretChangeLog.class, excludeCount)
                                                 .filter(ACCOUNT_ID_KEY, accountId)
                                                 .field(ENCRYPTED_DATA_ID_KEY)
                                                 .hasAnyOf(secretIds)
                                                 .order("-" + CREATED_AT_KEY)
                                                 .asList();

    // HAR-7150: Retrieve version history/changelog from Vault if secret text is a path reference.
    if (variableType == SettingVariableTypes.SECRET_TEXT && encryptedData != null) {
      EncryptionType encryptionType = encryptedData.getEncryptionType();
      if (encryptionType == EncryptionType.VAULT && isNotEmpty(encryptedData.getPath())) {
        EncryptionConfig encryptionConfig = secretManagerConfigService.getDefaultSecretManager(accountId);
        if (encryptionConfig instanceof VaultConfig) {
          secretChangeLogs.addAll(vaultService.getVaultSecretChangeLogs(encryptedData, (VaultConfig) encryptionConfig));
          // Sort the change log by change time in descending order.
          secretChangeLogs.sort(
              (SecretChangeLog o1, SecretChangeLog o2) -> (int) (o2.getLastUpdatedAt() - o1.getLastUpdatedAt()));
        }
      }
    }

    return secretChangeLogs;
  }

  @Override
  public Collection<UuidAware> listEncryptedValues(String accountId) {
    Set<Parent> parents = new HashSet<>();
    try (HIterator<EncryptedData> query = new HIterator<>(
             wingsPersistence.createQuery(EncryptedData.class)
                 .filter(ACCOUNT_ID_KEY, accountId)
                 .field(EncryptedDataKeys.type)
                 .hasNoneOf(Lists.newArrayList(SettingVariableTypes.SECRET_TEXT, SettingVariableTypes.CONFIG_FILE))
                 .fetch())) {
      for (EncryptedData data : query) {
        if (!isEmpty(data.getParentIds()) && data.getType() != SettingVariableTypes.KMS) {
          data.getParentIds().forEach(parentId
              -> parents.add(Parent.builder()
                                 .id(parentId)
                                 .variableType(data.getType())
                                 .encryptionDetail(EncryptionDetail.builder()
                                                       .encryptionType(data.getEncryptionType())
                                                       .secretManagerName(getSecretManagerName(data.getType(), parentId,
                                                           data.getKmsId(), data.getEncryptionType()))
                                                       .build())
                                 .build()));
        }
      }
    }
    return fetchParents(accountId, parents);
  }

  @Override
  public PageResponse<UuidAware> listEncryptedValues(String accountId, PageRequest<EncryptedData> pageRequest) {
    Set<Parent> parents = new HashSet<>();
    PageResponse<EncryptedData> pageResponse = wingsPersistence.query(EncryptedData.class, pageRequest);
    pageResponse.getResponse().forEach(data -> {
      if (data.getParentIds() != null && data.getType() != SettingVariableTypes.KMS) {
        for (String parentId : data.getParentIds()) {
          parents.add(Parent.builder()
                          .id(parentId)
                          .variableType(data.getType())
                          .encryptionDetail(EncryptionDetail.builder()
                                                .encryptionType(data.getEncryptionType())
                                                .secretManagerName(getSecretManagerName(data.getType(), parentId,
                                                    data.getKmsId(), data.getEncryptionType()))
                                                .build())
                          .build());
        }
      }
    });
    List<UuidAware> rv = fetchParents(accountId, parents);
    return aPageResponse()
        .withResponse(rv)
        .withTotal(rv.size())
        .withOffset(pageResponse.getOffset())
        .withLimit(pageResponse.getLimit())
        .build();
  }

  @Override
  public String getEncryptedYamlRef(EncryptableSetting object, String... fieldNames) throws IllegalAccessException {
    if (object.getSettingType() == SettingVariableTypes.CONFIG_FILE) {
      String encryptedFieldRefId = ((ConfigFile) object).getEncryptedFileId();
      EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, encryptedFieldRefId);
      Preconditions.checkNotNull(encryptedData, "no encrypted record found for " + object);
      if (encryptedData.getEncryptionType() == EncryptionType.VAULT) {
        return encryptedData.getEncryptionType().getYamlName() + ":" + getVaultSecretRefUrl(encryptedData);
      } else {
        return encryptedData.getEncryptionType().getYamlName() + ":" + encryptedFieldRefId;
      }
    }
    Preconditions.checkState(fieldNames.length <= 1, "can't give more than one field in the call");
    Field encryptedField = null;
    if (fieldNames.length == 0) {
      encryptedField = object.getEncryptedFields().get(0);
    } else {
      String fieldName = fieldNames[0];
      List<Field> encryptedFields = object.getEncryptedFields();
      for (Field f : encryptedFields) {
        if (f.getName().equals(fieldName)) {
          encryptedField = f;
          break;
        }
      }
    }
    Preconditions.checkNotNull(
        encryptedField, "encrypted field not found " + object + ", args:" + Joiner.on(",").join(fieldNames));

    encryptedField.setAccessible(true);

    Field encryptedFieldRef = getEncryptedRefField(encryptedField, object);
    encryptedFieldRef.setAccessible(true);
    String encryptedFieldRefId = (String) encryptedFieldRef.get(object);
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, encryptedFieldRefId);
    Preconditions.checkNotNull(encryptedData, "no encrypted record found for " + object);

    if (encryptedData.getEncryptionType() == EncryptionType.VAULT) {
      return encryptedData.getEncryptionType().getYamlName() + ":" + getVaultSecretRefUrl(encryptedData);
    } else {
      return encryptedData.getEncryptionType().getYamlName() + ":" + encryptedFieldRefId;
    }
  }

  private String getVaultSecretRefUrl(EncryptedData encryptedData) {
    VaultConfig vaultConfig = vaultService.getVaultConfig(encryptedData.getAccountId(), encryptedData.getKmsId());
    String basePath = vaultConfig.getBasePath() == null ? DEFAULT_BASE_PATH : vaultConfig.getBasePath();
    String vaultPath = isEmpty(encryptedData.getPath())
        ? basePath + "/" + encryptedData.getEncryptionKey() + KEY_SPEARATOR + DEFAULT_KEY_NAME
        : encryptedData.getPath();
    return URL_ROOT_PREFIX + vaultConfig.getName() + vaultPath;
  }

  @Override
  public EncryptedData getEncryptedDataFromYamlRef(String encryptedYamlRef, String accountId) {
    Preconditions.checkState(isNotEmpty(encryptedYamlRef));
    String[] tags = encryptedYamlRef.split(":");
    String encryptionTypeYamlName = tags[0];
    String encryptedDataRef = tags[1];

    EncryptedData encryptedData;
    if (EncryptionType.VAULT.getYamlName().equals(encryptionTypeYamlName)
        && encryptedDataRef.startsWith(URL_ROOT_PREFIX)) {
      if (!encryptedDataRef.contains(KEY_SPEARATOR)) {
        throw new WingsException(
            "No key name separator # found in the Vault secret reference " + encryptedDataRef, USER);
      }

      // This is a new Vault path based reference;
      ParsedVaultSecretRef vaultSecretRef = parse(encryptedDataRef, accountId);

      Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class);
      query.criteria(ACCOUNT_ID_KEY)
          .equal(accountId)
          .criteria(EncryptedDataKeys.encryptionType)
          .equal(EncryptionType.VAULT);
      if (isNotEmpty(vaultSecretRef.relativePath)) {
        query.criteria(EncryptedDataKeys.encryptionKey).equal(vaultSecretRef.relativePath);
      } else if (isNotEmpty(vaultSecretRef.fullPath)) {
        query.criteria(EncryptedDataKeys.path).equal(vaultSecretRef.fullPath);
      }
      encryptedData = query.get();

      if (encryptedData == null) {
        encryptedData = createNewSecretTextFromVaultPathReference(vaultSecretRef, accountId);
      }
    } else {
      // This is an old id based reference
      encryptedData = wingsPersistence.get(EncryptedData.class, encryptedDataRef);
    }
    return encryptedData;
  }

  private EncryptedData createNewSecretTextFromVaultPathReference(
      ParsedVaultSecretRef vaultSecretRef, String accountId) {
    String secretName = getEncryptedDataNameFromRef(vaultSecretRef.fullPath);
    SettingVariableTypes type = SettingVariableTypes.SECRET_TEXT;
    String encryptionKey = type + PATH_SEPARATOR + secretName;
    EncryptedData encryptedData = EncryptedData.builder()
                                      .name(secretName)
                                      .encryptionKey(encryptionKey)
                                      .encryptedValue(encryptionKey.toCharArray())
                                      .encryptionType(EncryptionType.VAULT)
                                      .type(type)
                                      .accountId(accountId)
                                      .kmsId(vaultSecretRef.vaultConfigId)
                                      .usageRestrictions(getDefaultUsageRestrictions())
                                      .build();

    if (vaultSecretRef.relativePath != null) {
      encryptedData.setEncryptionKey(vaultSecretRef.relativePath);
      encryptedData.setEncryptedValue(vaultSecretRef.relativePath.toCharArray());
    } else if (vaultSecretRef.fullPath != null) {
      encryptedData.setPath(vaultSecretRef.fullPath);
    }

    String encryptedDataId = wingsPersistence.save(encryptedData);
    generateAuditForEncryptedRecord(accountId, null, encryptedDataId);
    encryptedData = wingsPersistence.get(EncryptedData.class, encryptedDataId);

    char[] decryptedValue = null;
    try {
      // To test if the encrypted Data path is valid.
      decryptedValue = vaultService.decrypt(encryptedData, accountId, vaultSecretRef.vaultConfig);
    } catch (Exception e) {
      logger.error("Failed to decrypted vault secret at path " + encryptedData.getPath(), e);
    }

    if (isNotEmpty(decryptedValue)) {
      logger.info("Created a vault path and key reference secret '{}' to refer to the Vault secret at {}", secretName,
          vaultSecretRef.fullPath);
    } else {
      // If invalid reference, delete the encrypted data instance.
      EncryptedData record = wingsPersistence.get(EncryptedData.class, encryptedDataId);
      if (record != null) {
        deleteAndReportForAuditRecord(accountId, record);
      }
      throw new WingsException("Vault path '" + vaultSecretRef.fullPath + "' is invalid", USER);
    }
    return encryptedData;
  }

  private UsageRestrictions getDefaultUsageRestrictions() {
    GenericEntityFilter appFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
    EnvFilter envFilter = EnvFilter.builder().filterTypes(newHashSet(PROD, NON_PROD)).build();
    AppEnvRestriction appEnvRestriction = AppEnvRestriction.builder().appFilter(appFilter).envFilter(envFilter).build();
    UsageRestrictions usageRestrictions = new UsageRestrictions();
    usageRestrictions.setAppEnvRestrictions(newHashSet(appEnvRestriction));
    return usageRestrictions;
  }

  private ParsedVaultSecretRef parse(String encryptedDataRef, String accountId) {
    if (!encryptedDataRef.startsWith(URL_ROOT_PREFIX) || !encryptedDataRef.contains(KEY_SPEARATOR)) {
      throw new WingsException("Vault secret reference '" + encryptedDataRef + "' has illegal format", USER);
    } else {
      String secretMangerNameAndPath = encryptedDataRef.substring(2);

      int index = secretMangerNameAndPath.indexOf(PATH_SEPARATOR);
      String fullPath = secretMangerNameAndPath.substring(index);
      String secretManagerName = secretMangerNameAndPath.substring(0, index);
      VaultConfig vaultConfig = vaultService.getVaultConfigByName(accountId, secretManagerName);
      if (vaultConfig == null) {
        throw new WingsException("Vault secret manager '" + secretManagerName + "' doesn't exist", USER);
      }

      String basePath = vaultConfig.getBasePath() == null ? DEFAULT_BASE_PATH : vaultConfig.getBasePath();
      index = fullPath.indexOf(KEY_SPEARATOR);
      String keyName = fullPath.substring(index + 1);

      String vaultPath = null;
      if (fullPath.startsWith(basePath)) {
        vaultPath = fullPath.substring(basePath.length() + 1, index);
      }

      return ParsedVaultSecretRef.builder()
          .secretManagerName(secretManagerName)
          .vaultConfigId(vaultConfig.getUuid())
          .vaultConfig(vaultConfig)
          .basePath(basePath)
          .fullPath(fullPath)
          .relativePath(vaultPath)
          .keyName(keyName)
          .build();
    }
  }

  private String getEncryptedDataNameFromRef(String fullVaultPath) {
    return YAML_PREFIX + fullVaultPath.replaceAll(PATH_SEPARATOR, "_").replaceAll(KEY_SPEARATOR, "_");
  }

  @Override
  public boolean transitionSecrets(String accountId, EncryptionType fromEncryptionType, String fromSecretManagerId,
      EncryptionType toEncryptionType, String toSecretManagerId) {
    Preconditions.checkState(isNotEmpty(accountId), "accountId can't be blank");
    Preconditions.checkNotNull(fromEncryptionType, "fromEncryptionType can't be blank");
    Preconditions.checkState(isNotEmpty(fromSecretManagerId), "fromSecretManagerId can't be blank");
    Preconditions.checkNotNull(toEncryptionType, "toEncryptionType can't be blank");
    Preconditions.checkState(isNotEmpty(toSecretManagerId), "toSecretManagerId can't be blank");

    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                     .filter(ACCOUNT_ID_KEY, accountId)
                                     .filter(EncryptedDataKeys.kmsId, fromSecretManagerId);

    if (toEncryptionType == EncryptionType.VAULT) {
      query = query.field(EncryptedDataKeys.type).notEqual(SettingVariableTypes.VAULT);
    }

    try (HIterator<EncryptedData> iterator = new HIterator<>(query.fetch())) {
      for (EncryptedData dataToTransition : iterator) {
        transitionKmsQueue.send(KmsTransitionEvent.builder()
                                    .accountId(accountId)
                                    .entityId(dataToTransition.getUuid())
                                    .fromEncryptionType(fromEncryptionType)
                                    .fromKmsId(fromSecretManagerId)
                                    .toEncryptionType(toEncryptionType)
                                    .toKmsId(toSecretManagerId)
                                    .build());
      }
    }
    return true;
  }

  @Override
  public void changeSecretManager(String accountId, String entityId, EncryptionType fromEncryptionType,
      String fromKmsId, EncryptionType toEncryptionType, String toKmsId) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, entityId);
    // This is needed as encryptedData will be updated in the process of
    EncryptedData existingEncryptedData = wingsPersistence.get(EncryptedData.class, entityId);
    Preconditions.checkNotNull(encryptedData, "No encrypted data with id " + entityId);
    Preconditions.checkState(encryptedData.getEncryptionType() == fromEncryptionType,
        "mismatch between saved encrypted type and from encryption type");
    EncryptionConfig fromConfig = getSecretManager(accountId, fromKmsId, fromEncryptionType);
    Preconditions.checkNotNull(
        fromConfig, "No kms found for account " + accountId + " with id " + entityId + " type: " + fromEncryptionType);
    EncryptionConfig toConfig = getSecretManager(accountId, toKmsId, toEncryptionType);
    Preconditions.checkNotNull(
        toConfig, "No kms found for account " + accountId + " with id " + entityId + " type: " + fromEncryptionType);

    // Can't not transition secrets with path reference to a different secret manager. Customer has to manually
    // transfer.
    if (isNotEmpty(encryptedData.getPath())) {
      logger.warn(
          "Encrypted secret '{}' with path '{}' in account '{}' is not allowed to be transferred to a different secret manager.",
          encryptedData.getName(), encryptedData.getPath(), accountId);
      return;
    }

    if (encryptedData.getType() == SettingVariableTypes.CONFIG_FILE) {
      changeFileSecretManager(accountId, encryptedData, fromEncryptionType, fromConfig, toEncryptionType, toConfig);
      return;
    }

    char[] decrypted;
    switch (fromEncryptionType) {
      case LOCAL:
        decrypted = localEncryptionService.decrypt(encryptedData, accountId, (LocalEncryptionConfig) fromConfig);
        break;
      case KMS:
        decrypted = kmsService.decrypt(encryptedData, accountId, (KmsConfig) fromConfig);
        break;
      case VAULT:
        decrypted = vaultService.decrypt(encryptedData, accountId, (VaultConfig) fromConfig);
        break;
      case AWS_SECRETS_MANAGER:
        decrypted = secretsManagerService.decrypt(encryptedData, accountId, (AwsSecretsManagerConfig) fromConfig);
        // Delete the AWS secrets after it's transitioned out.
        String awsSecretName =
            isEmpty(encryptedData.getPath()) ? encryptedData.getEncryptionKey() : encryptedData.getPath();
        secretsManagerService.deleteSecret(accountId, awsSecretName, (AwsSecretsManagerConfig) fromConfig);
        break;
      case AZURE_VAULT:
        decrypted = azureVaultService.decrypt(encryptedData, accountId, (AzureVaultConfig) fromConfig);
        // delete the Azure secret since it's transitioning out. TODO Discuss with mark to delete after the secret
        // has been moved to a new vault

        String azureSecretName =
            isEmpty(encryptedData.getPath()) ? encryptedData.getEncryptionKey() : encryptedData.getPath();
        logger.info("Deleting encrypted data name {} from Azure config {} in account {}", azureSecretName,
            fromConfig.getUuid(), accountId);
        azureVaultService.delete(accountId, azureSecretName, (AzureVaultConfig) fromConfig);
        break;
      case CYBERARK:
        decrypted = cyberArkService.decrypt(encryptedData, accountId, (CyberArkConfig) fromConfig);
        break;
      default:
        throw new IllegalStateException("Invalid type : " + fromEncryptionType);
    }

    EncryptedData encrypted;
    String encryptionKey;
    String secretValue;
    switch (toEncryptionType) {
      case LOCAL:
        encrypted = localEncryptionService.encrypt(decrypted, accountId, (LocalEncryptionConfig) toConfig);
        break;
      case KMS:
        encrypted = kmsService.encrypt(decrypted, accountId, (KmsConfig) toConfig);
        break;
      case VAULT:
        encryptionKey = encryptedData.getEncryptionKey();
        secretValue = decrypted == null ? null : String.valueOf(decrypted);
        encrypted = vaultService.encrypt(encryptedData.getName(), secretValue, accountId, encryptedData.getType(),
            (VaultConfig) toConfig, EncryptedData.builder().encryptionKey(encryptionKey).build());
        break;
      case AWS_SECRETS_MANAGER:
        encryptionKey = encryptedData.getEncryptionKey();
        secretValue = decrypted == null ? null : String.valueOf(decrypted);
        encrypted =
            secretsManagerService.encrypt(encryptedData.getName(), secretValue, accountId, encryptedData.getType(),
                (AwsSecretsManagerConfig) toConfig, EncryptedData.builder().encryptionKey(encryptionKey).build());
        break;
      case AZURE_VAULT:
        encryptionKey = encryptedData.getEncryptionKey();
        secretValue = decrypted == null ? null : String.valueOf(decrypted);
        encrypted = azureVaultService.encrypt(encryptedData.getName(), secretValue, accountId, encryptedData.getType(),
            (AzureVaultConfig) toConfig, EncryptedData.builder().encryptionKey(encryptionKey).build());
        break;
      case CYBERARK:
        throw new WingsException(
            UNSUPPORTED_OPERATION_EXCEPTION, "Deprecate operation is not supported for CyberArk secret manager");
      default:
        throw new IllegalStateException("Invalid type : " + toEncryptionType);
    }

    encryptedData.setKmsId(toKmsId);
    encryptedData.setEncryptionType(toEncryptionType);
    encryptedData.setEncryptionKey(encrypted.getEncryptionKey());
    encryptedData.setEncryptedValue(encrypted.getEncryptedValue());

    String recordId = wingsPersistence.save(encryptedData);
    generateAuditForEncryptedRecord(accountId, existingEncryptedData, recordId);
  }

  private void changeFileSecretManager(String accountId, EncryptedData encryptedData, EncryptionType fromEncryptionType,
      EncryptionConfig fromConfig, EncryptionType toEncryptionType, EncryptionConfig toConfig) {
    byte[] decryptedFileContent = getFileContents(accountId, encryptedData.getUuid());
    EncryptedData existingEncryptedRecord =
        isBlank(encryptedData.getUuid()) ? null : wingsPersistence.get(EncryptedData.class, encryptedData.getUuid());

    EncryptedData encryptedFileData;
    switch (toEncryptionType) {
      case LOCAL:
        encryptedFileData = localEncryptionService.encryptFile(
            accountId, (LocalEncryptionConfig) toConfig, encryptedData.getName(), decryptedFileContent);
        break;

      case KMS:
        encryptedFileData =
            kmsService.encryptFile(accountId, (KmsConfig) toConfig, encryptedData.getName(), decryptedFileContent);
        break;

      case VAULT:
        encryptedFileData = vaultService.encryptFile(
            accountId, (VaultConfig) toConfig, encryptedData.getName(), decryptedFileContent, encryptedData);
        break;

      case AWS_SECRETS_MANAGER:
        encryptedFileData = secretsManagerService.encryptFile(accountId, (AwsSecretsManagerConfig) toConfig,
            encryptedData.getName(), decryptedFileContent, encryptedData);
        break;

      case AZURE_VAULT:
        encryptedFileData = azureVaultService.encryptFile(
            accountId, (AzureVaultConfig) toConfig, encryptedData.getName(), decryptedFileContent, encryptedData);
        break;

      case CYBERARK:
        throw new WingsException(
            UNSUPPORTED_OPERATION_EXCEPTION, "Deprecate operation is not supported for CyberArk secret manager");

      default:
        throw new IllegalArgumentException("Invalid target encryption type " + toEncryptionType);
    }

    String savedFileId = String.valueOf(encryptedData.getEncryptedValue());
    switch (fromEncryptionType) {
      case LOCAL:
        // Fall through so as the old file will be deleted just like in KMS case.
      case KMS:
        // Delete file from file service only if the source secret manager is of KMS type.
        fileService.deleteFile(savedFileId, CONFIGS);
        break;
      case VAULT:
        // Delete the Vault secret corresponding to the encrypted file if it's not a path reference
        if (isEmpty(encryptedData.getPath())) {
          vaultService.deleteSecret(accountId, encryptedData.getEncryptionKey(), (VaultConfig) fromConfig);
        }
        break;
      case AWS_SECRETS_MANAGER:
        // Delete the AWS secrets (encrypted file type) after it's transitioned out if it's not a referenced secret
        if (isEmpty(encryptedData.getPath())) {
          secretsManagerService.deleteSecret(
              accountId, encryptedData.getEncryptionKey(), (AwsSecretsManagerConfig) fromConfig);
        }
        break;
      case AZURE_VAULT:
        if (isEmpty(encryptedData.getPath())) {
          azureVaultService.delete(accountId, encryptedData.getEncryptionKey(), (AzureVaultConfig) fromConfig);
        }
        break;
      case CYBERARK:
        throw new WingsException(
            UNSUPPORTED_OPERATION_EXCEPTION, "Deprecate operation is not supported for CyberArk secret manager");

      default:
        break;
    }

    encryptedData.setEncryptionKey(encryptedFileData.getEncryptionKey());
    encryptedData.setEncryptedValue(encryptedFileData.getEncryptedValue());
    encryptedData.setEncryptionType(toEncryptionType);
    encryptedData.setKmsId(toConfig.getUuid());
    encryptedData.setBase64Encoded(true);
    String recordId = wingsPersistence.save(encryptedData);
    generateAuditForEncryptedRecord(accountId, existingEncryptedRecord, recordId);
  }

  @Override
  public void checkAndAlertForInvalidManagers() {
    Query<Account> query = wingsPersistence.createQuery(Account.class, excludeAuthority);
    try (HIterator<Account> records = new HIterator<>(query.fetch())) {
      for (Account account : records) {
        try {
          validateSecretManagerConfigs(account.getUuid());
          vaultService.renewTokens(account.getUuid());
          vaultService.appRoleLogin(account.getUuid());
        } catch (Exception e) {
          logger.info(
              "Failed to validate secret manager for {} account id {}", account.getAccountName(), account.getUuid(), e);
        }
      }
    }
  }

  @Override
  public EncryptedData getSecretMappedToAccountByName(String accountId, String name) {
    Query<EncryptedData> query = wingsPersistence.createQuery(EncryptedData.class)
                                     .filter(ACCOUNT_ID_KEY, accountId)
                                     .filter(EncryptedDataKeys.name, name)
                                     .field(EncryptedDataKeys.usageRestrictions)
                                     .doesNotExist();
    return query.get();
  }

  @Override
  public EncryptedData getSecretMappedToAppByName(String accountId, String appId, String envId, String name) {
    PageRequest<EncryptedData> pageRequest = aPageRequest()
                                                 .addFilter(EncryptedDataKeys.name, Operator.EQ, name)
                                                 .addFilter(ACCOUNT_ID_KEY, Operator.EQ, accountId)
                                                 .build();
    try {
      PageResponse<EncryptedData> response = listSecrets(accountId, pageRequest, appId, envId, false);
      List<EncryptedData> secrets = response.getResponse();
      return isNotEmpty(secrets) ? secrets.get(0) : null;
    } catch (Exception e) {
      throw new WingsException(ErrorCode.GENERAL_ERROR, e).addParam("message", "Failed to list secrets");
    }
  }

  @Override
  public EncryptedData getSecretById(String accountId, String id) {
    return wingsPersistence.createQuery(EncryptedData.class)
        .filter(ACCOUNT_ID_KEY, accountId)
        .filter(EncryptedData.ID_KEY, id)
        .get();
  }

  @Override
  public EncryptedData getSecretByName(String accountId, String name) {
    return wingsPersistence.createQuery(EncryptedData.class)
        .filter(ACCOUNT_ID_KEY, accountId)
        .filter(EncryptedDataKeys.name, name)
        .get();
  }

  @Override
  public String saveSecret(
      String accountId, String name, String value, String path, UsageRestrictions usageRestrictions) {
    return upsertSecretInternal(accountId, null, name, value, path, getEncryptionType(accountId), usageRestrictions);
  }

  @Override
  public List<String> importSecrets(String accountId, List<SecretText> secretTexts) {
    List<String> secretIds = new ArrayList<>();
    EncryptionType encryptionType = getEncryptionType(accountId);
    for (SecretText secretText : secretTexts) {
      try {
        String secretId = upsertSecretInternal(accountId, null, secretText.getName(), secretText.getValue(),
            secretText.getPath(), encryptionType, secretText.getUsageRestrictions());
        secretIds.add(secretId);
        logger.info("Imported secret '{}' successfully with uid: {}", secretText.getName(), secretId);
      } catch (WingsException e) {
        logger.warn("Failed to save import secret '{}' with error: {}", secretText.getName(), e.getMessage());
      }
    }
    return secretIds;
  }

  @Override
  public String saveSecretUsingLocalMode(
      String accountId, String name, String value, String path, UsageRestrictions usageRestrictions) {
    return upsertSecretInternal(accountId, null, name, value, path, EncryptionType.LOCAL, usageRestrictions);
  }

  @Override
  public boolean transitionAllSecretsToHarnessSecretManager(String accountId) {
    // For now, the default/harness secret manager is the LOCAL secret manager, and it's HIDDEN from end-user
    EncryptionConfig harnessSecretManager = localEncryptionService.getEncryptionConfig(accountId);
    List<SecretManagerConfig> allEncryptionConfigs = listSecretManagers(accountId);
    for (EncryptionConfig encryptionConfig : allEncryptionConfigs) {
      logger.info("Transitioning secret from secret manager {} of type {} into Harness secret manager for account {}",
          encryptionConfig.getUuid(), encryptionConfig.getEncryptionType(), accountId);
      transitionSecrets(accountId, encryptionConfig.getEncryptionType(), encryptionConfig.getUuid(),
          harnessSecretManager.getEncryptionType(), harnessSecretManager.getUuid());
    }
    return true;
  }

  @Override
  public void clearDefaultFlagOfSecretManagers(String accountId) {
    // Set custom secret managers as non-default
    List<SecretManagerConfig> defaultSecretManagerConfigs =
        listSecretManagers(accountId).stream().filter(EncryptionConfig::isDefault).collect(Collectors.toList());

    for (EncryptionConfig config : defaultSecretManagerConfigs) {
      switch (config.getEncryptionType()) {
        case VAULT:
          VaultConfig vaultConfig = vaultService.getVaultConfig(accountId, config.getUuid());
          vaultConfig.setDefault(false);
          vaultService.saveVaultConfig(accountId, vaultConfig);
          break;
        case LOCAL:
          break;
        case KMS:
          KmsConfig kmsConfig = kmsService.getKmsConfig(accountId, config.getUuid());
          if (!kmsConfig.getAccountId().equals(GLOBAL_ACCOUNT_ID) && kmsConfig.isDefault()) {
            kmsConfig.setDefault(false);
            kmsService.saveKmsConfig(accountId, kmsConfig);
          }
          break;
        case AWS_SECRETS_MANAGER:
          AwsSecretsManagerConfig awsConfig =
              secretsManagerService.getAwsSecretsManagerConfig(accountId, config.getUuid());
          awsConfig.setDefault(false);
          secretsManagerService.saveAwsSecretsManagerConfig(accountId, awsConfig);
          break;
        case AZURE_VAULT:
          AzureVaultConfig azureConfig = azureSecretsManagerService.getEncryptionConfig(accountId, config.getUuid());
          azureConfig.setDefault(false);
          azureSecretsManagerService.saveAzureSecretsManagerConfig(accountId, azureConfig);
          break;
        case CYBERARK:
          CyberArkConfig cyberArkConfig = cyberArkService.getConfig(accountId, config.getUuid());
          cyberArkConfig.setDefault(false);
          cyberArkService.saveConfig(accountId, cyberArkConfig);
          break;
        default:
          throw new IllegalStateException("Unexpected value: " + config.getEncryptionType());
      }
    }
  }

  @Override
  public boolean updateSecret(
      String accountId, String uuId, String name, String value, String path, UsageRestrictions usageRestrictions) {
    String encryptedDataId =
        upsertSecretInternal(accountId, uuId, name, value, path, getEncryptionType(accountId), usageRestrictions);
    return encryptedDataId != null;
  }

  /**
   * This API is to combine multiple secret operations such as INSERT/UPDATE/UPSERT.
   *
   * If 'uuid' passed in is null, this is an INSERT operation. If 'upsert' flag is true, it's an UPSERT operation it
   * will UPDATE if the record already exists, and INSERT if it doesn't exists.
   *
   * It will return the generated UUID in a INSERT operation, and return null in the UPDATE operation if the record
   * doesn't exist.
   */
  private String upsertSecretInternal(String accountId, String uuid, String name, String value, String path,
      EncryptionType encryptionType, UsageRestrictions usageRestrictions) {
    String auditMessage;
    String encryptedDataId;

    if (containsIllegalCharacters(name)) {
      throw new WingsException("Secret name '" + name + "' contains illegal characters", USER);
    }

    if (isNotEmpty(path)) {
      switch (encryptionType) {
        case VAULT:
          // Path should always have a "#" in and a key name after the #.
          if (path.indexOf('#') < 0) {
            throw new WingsException(
                "Secret path need to include the # sign with the the key name after. E.g. /foo/bar/my-secret#my-key.");
          }
          break;
        case AWS_SECRETS_MANAGER:
          break;
        case CYBERARK:
          break;
        default:
          throw new WingsException(
              "Secret path can be specified only if the secret manager is of VAULT/AWS_SECRETS_MANAGER/CYBERARK type!");
      }
    }

    char[] secretValue = isEmpty(value) ? null : value.toCharArray();
    if (isEmpty(uuid)) {
      // INSERT use case
      usageRestrictionsService.validateUsageRestrictionsOnEntitySave(accountId, usageRestrictions);

      EncryptedData encryptedData = encrypt(encryptionType, accountId, SettingVariableTypes.SECRET_TEXT, secretValue,
          path, null, name, usageRestrictions);
      encryptedData.addSearchTag(name);
      try {
        encryptedDataId = wingsPersistence.save(encryptedData);
        generateAuditForEncryptedRecord(accountId, null, encryptedDataId);
      } catch (DuplicateKeyException e) {
        String reason = "Variable " + name + " already exists";
        throw new KmsOperationException(reason, e, USER);
      }

      auditMessage = "Created";
    } else {
      // UPDATE use case
      EncryptedData savedData = wingsPersistence.get(EncryptedData.class, uuid);

      // savedData will be updated and saved again as a part of update, so need this oldEntity
      EncryptedData oldEntity = wingsPersistence.get(EncryptedData.class, uuid);
      if (savedData == null) {
        // UPDATE use case. Return directly when record doesn't exist.
        return null;
      }

      // validate usage restriction.
      usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(
          accountId, savedData.getUsageRestrictions(), usageRestrictions);
      if (!Objects.equals(savedData.getUsageRestrictions(), usageRestrictions)) {
        // Validate if change of the usage scope is resulting in with dangling references in service/environments.
        validateAppEnvChangesInUsageRestrictions(savedData, usageRestrictions);
      }

      encryptedDataId = uuid;
      boolean nameChanged = !Objects.equals(name, savedData.getName());
      boolean valueChanged = isNotEmpty(value) && !value.equals(SECRET_MASK);
      boolean pathChanged = !Objects.equals(path, savedData.getPath());

      boolean needReencryption = false;

      StringBuilder builder = new StringBuilder();
      if (nameChanged) {
        builder.append("Changed name");
        savedData.removeSearchTag(null, savedData.getName(), null);
        savedData.setName(name);
        savedData.addSearchTag(name);

        // PL-1125: Remove old secret name in Vault if secret text's name changed to not have dangling entries.
        if (isEmpty(savedData.getPath())) {
          // For harness managed secrets, we need to delete the corresponding entries in the secret manager.
          String secretName = savedData.getEncryptionKey();
          switch (savedData.getEncryptionType()) {
            case VAULT:
              needReencryption = true;
              VaultConfig vaultConfig = vaultService.getVaultConfig(accountId, savedData.getKmsId());
              if (!valueChanged) {
                // retrieve/decrypt the old secret value.
                secretValue = vaultService.decrypt(savedData, accountId, vaultConfig);
              }
              vaultService.deleteSecret(accountId, secretName, vaultConfig);
              break;
            case AWS_SECRETS_MANAGER:
              needReencryption = true;
              AwsSecretsManagerConfig secretsManagerConfig =
                  secretsManagerService.getAwsSecretsManagerConfig(accountId, savedData.getKmsId());
              if (!valueChanged) {
                // retrieve/decrypt the old secret value.
                secretValue = secretsManagerService.decrypt(savedData, accountId, secretsManagerConfig);
              }
              secretsManagerService.deleteSecret(accountId, secretName, secretsManagerConfig);
              break;
            case AZURE_VAULT:
              needReencryption = true;
              AzureVaultConfig azureVaultConfig =
                  azureSecretsManagerService.getEncryptionConfig(accountId, savedData.getKmsId());
              if (!valueChanged) {
                // retrieve/decrypt the old secret value.
                secretValue = azureVaultService.decrypt(savedData, accountId, azureVaultConfig);
              }
              azureVaultService.delete(accountId, secretName, azureVaultConfig);
              break;
            default:
              // Not relevant for other secret manager types
              break;
          }
        }
      }
      if (valueChanged) {
        needReencryption = true;
        builder.append(builder.length() > 0 ? " & value" : " Changed value");
      }
      if (pathChanged) {
        needReencryption = true;
        builder.append(builder.length() > 0 ? " & path" : " Changed path");
        savedData.setPath(path);
      }
      if (usageRestrictions != null) {
        builder.append(builder.length() > 0 ? " & usage restrictions" : "Changed usage restrictions");
      }
      auditMessage = builder.toString();

      // Re-encrypt if secret value or path has changed. Update should not change the existing Encryption type and
      // secret manager if the secret is 'path' enabled!
      if (needReencryption) {
        EncryptedData encryptedData = encrypt(getEncryptionType(accountId), accountId, SettingVariableTypes.SECRET_TEXT,
            secretValue, path, savedData, name, usageRestrictions);
        savedData.setEncryptionKey(encryptedData.getEncryptionKey());
        savedData.setEncryptedValue(encryptedData.getEncryptedValue());
        savedData.setEncryptionType(encryptedData.getEncryptionType());
        savedData.setKmsId(encryptedData.getKmsId());
      }
      savedData.setUsageRestrictions(usageRestrictions);
      wingsPersistence.save(savedData);
      if (eligibleForCrudAudit(savedData)) {
        auditServiceHelper.reportForAuditingUsingAccountId(savedData.getAccountId(), oldEntity, savedData, Type.UPDATE);
      }
    }

    if (UserThreadLocal.get() != null) {
      wingsPersistence.save(SecretChangeLog.builder()
                                .accountId(accountId)
                                .encryptedDataId(encryptedDataId)
                                .description(auditMessage)
                                .user(EmbeddedUser.builder()
                                          .uuid(UserThreadLocal.get().getUuid())
                                          .email(UserThreadLocal.get().getEmail())
                                          .name(UserThreadLocal.get().getName())
                                          .build())
                                .build());
    }

    return encryptedDataId;
  }

  private boolean eligibleForCrudAudit(EncryptedData savedData) {
    return SettingVariableTypes.CONFIG_FILE.equals(savedData.getType())
        || SettingVariableTypes.SECRET_TEXT.equals(savedData.getType());
  }

  @Override
  public boolean updateUsageRestrictionsForSecretOrFile(
      String accountId, String uuId, UsageRestrictions usageRestrictions) {
    EncryptedData savedData = wingsPersistence.get(EncryptedData.class, uuId);
    if (savedData == null) {
      return false;
    }
    usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(
        accountId, savedData.getUsageRestrictions(), usageRestrictions);
    // No validation of `validateAppEnvChangesInUsageRestrictions` is performed in this method
    // because usually this update is a result of removing application/environment.

    savedData.setUsageRestrictions(usageRestrictions);

    try {
      wingsPersistence.save(savedData);
    } catch (DuplicateKeyException e) {
      throw new KmsOperationException("Unable to save Restrictions");
    }

    if (UserThreadLocal.get() != null) {
      wingsPersistence.save(SecretChangeLog.builder()
                                .accountId(accountId)
                                .encryptedDataId(uuId)
                                .description("Changed restrictions")
                                .user(EmbeddedUser.builder()
                                          .uuid(UserThreadLocal.get().getUuid())
                                          .email(UserThreadLocal.get().getEmail())
                                          .name(UserThreadLocal.get().getName())
                                          .build())
                                .build());
    }
    return true;
  }

  @Override
  public boolean deleteSecret(String accountId, String uuId) {
    List<ServiceVariable> serviceVariables = wingsPersistence.createQuery(ServiceVariable.class)
                                                 .filter(ACCOUNT_ID_KEY, accountId)
                                                 .filter(ENCRYPTED_VALUE_KEY, uuId)
                                                 .asList();
    if (!serviceVariables.isEmpty()) {
      String reason = "Can't delete this secret because it is still being used in the Harness component(s): "
          + serviceVariables.stream().map(ServiceVariable::getName).collect(joining(", "))
          + ". Please remove the usages of this secret and try again.";
      throw new KmsOperationException(reason, USER);
    }

    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuId);
    Preconditions.checkNotNull(encryptedData, "No encrypted record found with id " + uuId);
    if (!usageRestrictionsService.userHasPermissionsToChangeEntity(accountId, encryptedData.getUsageRestrictions())) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED, USER);
    }

    EncryptionType encryptionType = encryptedData.getEncryptionType();
    switch (encryptionType) {
      case AWS_SECRETS_MANAGER:
        if (isEmpty(encryptedData.getPath())) {
          // For harness managed secrets, we need to delete the corresponding entries in the Vault service.
          String keyName = encryptedData.getEncryptionKey();
          AwsSecretsManagerConfig secretsManagerConfig =
              secretsManagerService.getAwsSecretsManagerConfig(accountId, encryptedData.getKmsId());
          secretsManagerService.deleteSecret(accountId, keyName, secretsManagerConfig);
        }
        break;
      case VAULT:
        if (isEmpty(encryptedData.getPath())) {
          // For harness managed secrets, we need to delete the corresponding entries in the Vault service.
          String keyName = encryptedData.getEncryptionKey();
          VaultConfig vaultConfig = vaultService.getVaultConfig(accountId, encryptedData.getKmsId());
          vaultService.deleteSecret(accountId, keyName, vaultConfig);
        }
        break;
      case AZURE_VAULT:
        String keyName = encryptedData.getEncryptionKey();
        AzureVaultConfig encryptionConfig =
            azureSecretsManagerService.getEncryptionConfig(accountId, encryptedData.getKmsId());
        azureVaultService.delete(accountId, keyName, encryptionConfig);
        break;
      default:
        break;
    }

    return deleteAndReportForAuditRecord(accountId, encryptedData);
  }

  @Override
  public boolean deleteSecretUsingUuid(String uuId) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuId);
    Preconditions.checkNotNull(encryptedData, "No encrypted record found with id " + uuId);
    return deleteAndReportForAuditRecord(encryptedData.getAccountId(), encryptedData);
  }

  @Override
  public String saveFile(
      String accountId, String name, UsageRestrictions usageRestrictions, BoundedInputStream inputStream) {
    return upsertFileInternal(accountId, name, null, usageRestrictions, inputStream);
  }

  @Override
  public File getFile(String accountId, String uuid, File readInto) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuid);
    Preconditions.checkNotNull(encryptedData, "could not find file with id " + uuid);
    EncryptionType encryptionType = encryptedData.getEncryptionType();
    switch (encryptionType) {
      case LOCAL:
        return localEncryptionService.decryptFile(readInto, accountId, encryptedData);

      case KMS:
        fileService.download(String.valueOf(encryptedData.getEncryptedValue()), readInto, CONFIGS);
        return kmsService.decryptFile(readInto, accountId, encryptedData);

      case VAULT:
        return vaultService.decryptFile(readInto, accountId, encryptedData);

      case AWS_SECRETS_MANAGER:
        return secretsManagerService.decryptFile(readInto, accountId, encryptedData);

      case AZURE_VAULT:
        return azureVaultService.decryptFile(readInto, accountId, encryptedData);

      case CYBERARK:
        throw new WingsException(
            CYBERARK_OPERATION_ERROR, "Encrypted file operations are not supported for CyberArk secret manager");

      default:
        throw new IllegalArgumentException("Invalid type " + encryptionType);
    }
  }

  @Override
  public byte[] getFileContents(String accountId, String uuid) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuid);
    Preconditions.checkNotNull(encryptedData, "could not find file with id " + uuid);
    return getFileContents(accountId, encryptedData);
  }

  private byte[] getFileContents(String accountId, EncryptedData encryptedData) {
    EncryptionType encryptionType = encryptedData.getEncryptionType();
    File file = null;
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      switch (encryptionType) {
        case LOCAL:
          localEncryptionService.decryptToStream(accountId, encryptedData, output);
          break;

        case KMS:
          file = new File(Files.createTempDir(), generateUuid());
          logger.info("Temp file path [{}]", file.getAbsolutePath());
          fileService.download(String.valueOf(encryptedData.getEncryptedValue()), file, CONFIGS);
          kmsService.decryptToStream(file, accountId, encryptedData, output);
          break;

        case VAULT:
          vaultService.decryptToStream(accountId, encryptedData, output);
          break;

        case AWS_SECRETS_MANAGER:
          secretsManagerService.decryptToStream(accountId, encryptedData, output);
          break;

        case AZURE_VAULT:
          azureVaultService.decryptToStream(accountId, encryptedData, output);
          break;

        case CYBERARK:
          throw new WingsException(
              CYBERARK_OPERATION_ERROR, "Encrypted file operations are not supported for CyberArk secret manager");

        default:
          throw new IllegalArgumentException("Invalid type " + encryptionType);
      }
      output.flush();
      return output.toByteArray();
    } catch (IOException e) {
      throw new WingsException(INVALID_ARGUMENT, e).addParam("args", "Failed to get content");
    } finally {
      // Delete temporary file if it exists.
      if (file != null && file.exists()) {
        boolean deleted = file.delete();
        if (!deleted) {
          logger.warn("Temporary file {} can't be deleted.", file.getAbsolutePath());
        }
      }
    }
  }

  @Override
  public boolean updateFile(
      String accountId, String name, String uuid, UsageRestrictions usageRestrictions, BoundedInputStream inputStream) {
    String recordId = upsertFileInternal(accountId, name, uuid, usageRestrictions, inputStream);
    return isNotEmpty(recordId);
  }

  /**
   * This internal method should be able to handle the UPSERT of encrypted record. It will UPDATE the existing record if
   * the uuid exists, and INSERT if this record is new. The refactoring of this method helps taking care of the IMPORT
   * use case in which we would like to preserve the 'uuid' field while importing the exported encrypted keys from other
   * system.
   */
  private String upsertFileInternal(
      String accountId, String name, String uuid, UsageRestrictions usageRestrictions, BoundedInputStream inputStream) {
    boolean update = false;
    String oldName = null;
    String savedFileId = null;
    EncryptedData encryptedData = null;
    EncryptedData oldEntityData = null;
    final EncryptionType encryptionType;

    if (containsIllegalCharacters(name)) {
      throw new WingsException("Encrypted file name '" + name + "' contains illegal characters", USER);
    }

    if (isNotEmpty(uuid)) {
      encryptedData = wingsPersistence.get(EncryptedData.class, uuid);
      if (encryptedData == null) {
        // Pure UPDATE case, need to throw exception is the record doesn't exist.
        throw new WingsException(DEFAULT_ERROR_CODE, "could not find file with id " + uuid);
      }

      // This is needed for auditing as encryptedData will be changed in the process of update
      oldEntityData = wingsPersistence.get(EncryptedData.class, uuid);
    }

    if (encryptedData == null) {
      // INSERT in UPSERT case, get the system default encryption type.
      encryptionType = getEncryptionType(accountId);
      usageRestrictionsService.validateUsageRestrictionsOnEntitySave(accountId, usageRestrictions);
    } else {
      // UPDATE in UPSERT case
      update = true;
      usageRestrictionsService.validateUsageRestrictionsOnEntityUpdate(
          accountId, encryptedData.getUsageRestrictions(), usageRestrictions);
      if (!Objects.equals(encryptedData.getUsageRestrictions(), usageRestrictions)) {
        // Validate if change of the usage scope is resulting in with dangling references in service/environments.
        validateAppEnvChangesInUsageRestrictions(encryptedData, usageRestrictions);
      }

      oldName = encryptedData.getName();
      savedFileId = String.valueOf(encryptedData.getEncryptedValue());
      encryptionType = encryptedData.getEncryptionType();
    }

    byte[] inputBytes;
    try {
      inputBytes = ByteStreams.toByteArray(inputStream);
    } catch (IOException e) {
      throw new WingsException(DEFAULT_ERROR_CODE, e);
    }

    EncryptionConfig encryptionConfig;
    EncryptedData newEncryptedFile = null;
    // HAR-9736: Update of encrypted file may not pick a new file for upload and no need to encrypt empty file.
    if (isNotEmpty(inputBytes)) {
      switch (encryptionType) {
        case LOCAL:
          LocalEncryptionConfig localEncryptionConfig = localEncryptionService.getEncryptionConfig(accountId);
          newEncryptedFile = localEncryptionService.encryptFile(accountId, localEncryptionConfig, name, inputBytes);
          if (update) {
            fileService.deleteFile(savedFileId, CONFIGS);
          }
          break;

        case KMS:
          encryptionConfig = update ? secretManagerConfigService.getSecretManager(accountId, encryptedData.getKmsId())
                                    : secretManagerConfigService.getDefaultSecretManager(accountId);
          KmsConfig kmsConfig = (KmsConfig) encryptionConfig;
          newEncryptedFile = kmsService.encryptFile(accountId, kmsConfig, name, inputBytes);
          newEncryptedFile.setKmsId(kmsConfig.getUuid());
          if (update) {
            fileService.deleteFile(savedFileId, CONFIGS);
          }
          break;

        case VAULT:
          encryptionConfig = update ? secretManagerConfigService.getSecretManager(accountId, encryptedData.getKmsId())
                                    : secretManagerConfigService.getDefaultSecretManager(accountId);
          VaultConfig vaultConfig = (VaultConfig) encryptionConfig;
          newEncryptedFile = vaultService.encryptFile(accountId, vaultConfig, name, inputBytes, encryptedData);
          newEncryptedFile.setKmsId(vaultConfig.getUuid());
          break;

        case AWS_SECRETS_MANAGER:
          encryptionConfig = update ? secretManagerConfigService.getSecretManager(accountId, encryptedData.getKmsId())
                                    : secretManagerConfigService.getDefaultSecretManager(accountId);
          AwsSecretsManagerConfig secretsManagerConfig = (AwsSecretsManagerConfig) encryptionConfig;
          newEncryptedFile =
              secretsManagerService.encryptFile(accountId, secretsManagerConfig, name, inputBytes, encryptedData);
          newEncryptedFile.setKmsId(secretsManagerConfig.getUuid());
          break;

        case AZURE_VAULT:
          // if it's an update call, we need to update the secret value in the same secret store.
          // Otherwise it should be saved in the default secret store of the account.
          encryptionConfig = update
              ? azureSecretsManagerService.getEncryptionConfig(accountId, encryptedData.getKmsId())
              : secretManagerConfigService.getDefaultSecretManager(accountId);
          AzureVaultConfig azureConfig = (AzureVaultConfig) encryptionConfig;
          logger.info("Creating file in azure vault with secret name: {}, in vault: {}, in accountName: {}", name,
              azureConfig.getName(), accountId);
          newEncryptedFile = azureVaultService.encryptFile(accountId, azureConfig, name, inputBytes, encryptedData);
          newEncryptedFile.setKmsId(azureConfig.getUuid());
          break;

        case CYBERARK:
          throw new WingsException(
              CYBERARK_OPERATION_ERROR, "Encrypted file operations are not supported for CyberArk secret manager");

        default:
          throw new IllegalArgumentException("Invalid type " + encryptionType);
      }
      newEncryptedFile.setEncryptionType(encryptionType);
      newEncryptedFile.setType(SettingVariableTypes.CONFIG_FILE);
    }

    long uploadFileSize = inputBytes.length;
    if (update) {
      if (newEncryptedFile != null) {
        // PL-1125: Remove old encrypted file in Vault if its name has changed so as not to have dangling entries.
        if (!Objects.equals(oldName, name)) {
          String secretName = encryptedData.getEncryptionKey();
          switch (encryptionType) {
            case VAULT:
              VaultConfig vaultConfig = vaultService.getVaultConfig(accountId, encryptedData.getKmsId());
              vaultService.deleteSecret(accountId, secretName, vaultConfig);
              break;
            case AWS_SECRETS_MANAGER:
              AwsSecretsManagerConfig secretsManagerConfig =
                  secretsManagerService.getAwsSecretsManagerConfig(accountId, encryptedData.getKmsId());
              secretsManagerService.deleteSecret(accountId, secretName, secretsManagerConfig);
              break;
            case AZURE_VAULT:
              AzureVaultConfig azureConfig =
                  azureSecretsManagerService.getEncryptionConfig(accountId, encryptedData.getKmsId());
              azureVaultService.delete(accountId, secretName, azureConfig);
              logger.info("Deleting file {} after update from vault {} in accountid {}", secretName,
                  azureConfig.getName(), accountId);
              break;
            default:
              // Does not apply to other secret manager types
              break;
          }
        }

        encryptedData.setEncryptionKey(newEncryptedFile.getEncryptionKey());
        encryptedData.setEncryptedValue(newEncryptedFile.getEncryptedValue());
        encryptedData.setKmsId(newEncryptedFile.getKmsId());
        encryptedData.setEncryptionType(newEncryptedFile.getEncryptionType());
        encryptedData.setFileSize(uploadFileSize);
      }
    } else {
      encryptedData = newEncryptedFile;
      encryptedData.setUuid(uuid);
      encryptedData.setType(SettingVariableTypes.CONFIG_FILE);
      encryptedData.setAccountId(accountId);
      encryptedData.setFileSize(uploadFileSize);
    }
    encryptedData.setName(name);
    encryptedData.setEncryptionType(encryptionType);
    encryptedData.setUsageRestrictions(usageRestrictions);
    encryptedData.setBase64Encoded(true);

    String recordId;
    try {
      recordId = wingsPersistence.save(encryptedData);
      generateAuditForEncryptedRecord(accountId, oldEntityData, recordId);
    } catch (DuplicateKeyException e) {
      throw new KmsOperationException("File " + name + " already exists");
    }

    if (update && newEncryptedFile != null) {
      // update parent's file size
      Set<Parent> parents = new HashSet<>();
      if (isNotEmpty(encryptedData.getParentIds())) {
        for (String parentId : encryptedData.getParentIds()) {
          parents.add(Parent.builder()
                          .id(parentId)
                          .variableType(SettingVariableTypes.CONFIG_FILE)
                          .encryptionDetail(
                              EncryptionDetail.builder().encryptionType(encryptedData.getEncryptionType()).build())
                          .build());
        }
      }
      List<UuidAware> configFiles = fetchParents(accountId, parents);
      configFiles.forEach(configFile -> {
        ((ConfigFile) configFile).setSize(uploadFileSize);
        wingsPersistence.save((ConfigFile) configFile);
      });
    }

    // Logging the secret file changes.
    if (UserThreadLocal.get() != null) {
      String auditMessage;
      if (update) {
        auditMessage = (isNotEmpty(oldName) && oldName.equals(name)) ? "Changed File" : "Changed Name and File";
        auditMessage = usageRestrictions == null ? auditMessage : auditMessage + " or Usage Restrictions";
      } else {
        auditMessage = "File uploaded";
      }
      wingsPersistence.save(SecretChangeLog.builder()
                                .accountId(accountId)
                                .encryptedDataId(uuid)
                                .encryptedDataId(recordId)
                                .description(auditMessage)
                                .user(EmbeddedUser.builder()
                                          .uuid(UserThreadLocal.get().getUuid())
                                          .email(UserThreadLocal.get().getEmail())
                                          .name(UserThreadLocal.get().getName())
                                          .build())
                                .build());
    }

    return recordId;
  }

  private void generateAuditForEncryptedRecord(String accountId, EncryptedData oldEntityData, String newRecordId) {
    Type type = oldEntityData == null ? Type.CREATE : Type.UPDATE;
    EncryptedData newRecordData = wingsPersistence.get(EncryptedData.class, newRecordId);
    if (eligibleForCrudAudit(newRecordData)) {
      auditServiceHelper.reportForAuditingUsingAccountId(accountId, oldEntityData, newRecordData, type);
    }
  }

  @Override
  public boolean deleteFile(String accountId, String uuId) {
    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, uuId);
    Preconditions.checkNotNull(encryptedData, "No encrypted record found with id " + uuId);
    if (!usageRestrictionsService.userHasPermissionsToChangeEntity(accountId, encryptedData.getUsageRestrictions())) {
      throw new WingsException(ErrorCode.USER_NOT_AUTHORIZED, USER);
    }

    List<ConfigFile> configFiles = wingsPersistence.createQuery(ConfigFile.class)
                                       .filter(ACCOUNT_ID_KEY, accountId)
                                       .filter(ConfigFileKeys.encryptedFileId, uuId)
                                       .asList();
    if (!configFiles.isEmpty()) {
      StringBuilder errorMessage = new StringBuilder("Being used by ");
      for (ConfigFile configFile : configFiles) {
        errorMessage.append(configFile.getFileName()).append(", ");
      }

      throw new KmsOperationException(errorMessage.toString(), USER);
    }

    switch (encryptedData.getEncryptionType()) {
      case LOCAL:
      case KMS:
        fileService.deleteFile(String.valueOf(encryptedData.getEncryptedValue()), CONFIGS);
        break;
      case VAULT:
        vaultService.deleteSecret(accountId, encryptedData.getEncryptionKey(),
            vaultService.getVaultConfig(accountId, encryptedData.getKmsId()));
        break;
      case AWS_SECRETS_MANAGER:
        secretsManagerService.deleteSecret(accountId, encryptedData.getEncryptionKey(),
            secretsManagerService.getAwsSecretsManagerConfig(accountId, encryptedData.getKmsId()));
        break;
      case AZURE_VAULT:
        azureVaultService.delete(accountId, encryptedData.getEncryptionKey(),
            azureSecretsManagerService.getEncryptionConfig(accountId, encryptedData.getKmsId()));
        break;
      case CYBERARK:
        throw new WingsException(
            CYBERARK_OPERATION_ERROR, "Delete file operation is not supported for CyberArk secret manager");
      default:
        throw new IllegalStateException("Invalid type " + encryptedData.getEncryptionType());
    }
    return deleteAndReportForAuditRecord(accountId, encryptedData);
  }

  @Override
  public PageResponse<EncryptedData> listSecrets(String accountId, PageRequest<EncryptedData> pageRequest,
      String appIdFromRequest, String envIdFromRequest, boolean details) throws IllegalAccessException {
    List<EncryptedData> filteredEncryptedDataList = Lists.newArrayList();

    int batchOffset = pageRequest.getStart();
    final int batchPageSize;

    // Increase the batch fetch page size to 2 times the requested, just in case some of the data
    // are filtered out based on usage restrictions. Or decrease the batch fetch size to 1000 if
    // the requested page size is too big (>1000);
    final int inputPageSize = pageRequest.getPageSize();
    if (2 * inputPageSize > PageRequest.DEFAULT_UNLIMITED) {
      batchPageSize = PageRequest.DEFAULT_UNLIMITED;
    } else {
      batchPageSize = 2 * inputPageSize;
    }

    boolean isAccountAdmin = userService.isAccountAdmin(accountId);

    RestrictionsAndAppEnvMap restrictionsAndAppEnvMap =
        usageRestrictionsService.getRestrictionsAndAppEnvMapFromCache(accountId, Action.READ);
    Map<String, Set<String>> appEnvMapFromPermissions = restrictionsAndAppEnvMap.getAppEnvMap();
    UsageRestrictions restrictionsFromUserPermissions = restrictionsAndAppEnvMap.getUsageRestrictions();

    int numRecordsReturnedCurrentBatch;
    do {
      PageRequest<EncryptedData> batchPageRequest = pageRequest.copy();
      batchPageRequest.setOffset(String.valueOf(batchOffset));
      batchPageRequest.setLimit(String.valueOf(batchPageSize));

      PageResponse<EncryptedData> batchPageResponse = wingsPersistence.query(EncryptedData.class, batchPageRequest);
      List<EncryptedData> encryptedDataList = batchPageResponse.getResponse();
      numRecordsReturnedCurrentBatch = encryptedDataList.size();

      // Set the new offset if another batch retrieval is needed if the requested page size is not fulfilled yet.
      batchOffset = filterSecreteDataBasedOnUsageRestrictions(accountId, isAccountAdmin, appIdFromRequest,
          envIdFromRequest, details, inputPageSize, batchOffset, appEnvMapFromPermissions,
          restrictionsFromUserPermissions, encryptedDataList, filteredEncryptedDataList);
    } while (numRecordsReturnedCurrentBatch == batchPageSize && filteredEncryptedDataList.size() < inputPageSize);

    // UI should read the adjust batchOffset while sending another page request!
    return aPageResponse()
        .withOffset(String.valueOf(batchOffset))
        .withLimit(String.valueOf(inputPageSize))
        .withResponse(filteredEncryptedDataList)
        .withTotal(Long.valueOf(filteredEncryptedDataList.size()))
        .build();
  }

  /**
   * Filter the retrieved encrypted data list based on usage restrictions. Some of the
   * encrypted data won't be presented to the end-user if the end-user doesn't have
   * access permissions.
   *
   * The filtered list size should never exceed the over page size from the page request.
   *
   * It will return an adjusted batch offset if another batch retrieval is needed as the original page request
   * has not fulfilled. The new batch load will start from the adjusted offset.
   */
  private int filterSecreteDataBasedOnUsageRestrictions(String accountId, boolean isAccountAdmin,
      String appIdFromRequest, String envIdFromRequest, boolean details, int inputPageSize, int batchOffset,
      Map<String, Set<String>> appEnvMapFromPermissions, UsageRestrictions restrictionsFromUserPermissions,
      List<EncryptedData> encryptedDataList, List<EncryptedData> filteredEncryptedDataList)
      throws IllegalAccessException {
    int index = 0;
    Set<String> appsByAccountId = appService.getAppIdsAsSetByAccountId(accountId);
    Map<String, List<Base>> appIdEnvMap = envService.getAppIdEnvMap(appsByAccountId);

    for (EncryptedData encryptedData : encryptedDataList) {
      index++;

      UsageRestrictions usageRestrictionsFromEntity = encryptedData.getUsageRestrictions();
      if (usageRestrictionsService.hasAccess(accountId, isAccountAdmin, appIdFromRequest, envIdFromRequest,
              usageRestrictionsFromEntity, restrictionsFromUserPermissions, appEnvMapFromPermissions, appIdEnvMap)) {
        filteredEncryptedDataList.add(encryptedData);
        encryptedData.setEncryptedValue(SECRET_MASK.toCharArray());
        encryptedData.setEncryptionKey(SECRET_MASK);

        if (details) {
          encryptedData.setEncryptedBy(getSecretManagerName(encryptedData.getType(), encryptedData.getUuid(),
              encryptedData.getKmsId(), encryptedData.getEncryptionType()));

          encryptedData.setSetupUsage(getSecretUsage(encryptedData.getAccountId(), encryptedData.getUuid()).size());
          encryptedData.setRunTimeUsage(getUsageLogsSize(encryptedData.getUuid(), SettingVariableTypes.SECRET_TEXT));
          encryptedData.setChangeLog(getChangeLogsInternal(
              encryptedData.getAccountId(), encryptedData.getUuid(), encryptedData, SettingVariableTypes.SECRET_TEXT)
                                         .size());
        }

        // Already got all data the page request wanted. Break out of the loop, no more filtering
        // to save some CPU cycles and reduce the latency.
        if (filteredEncryptedDataList.size() == inputPageSize) {
          break;
        }
      }
    }

    // The requested page size may have not been filled, may need to fetch another batch and adjusting the offset
    // accordingly;
    return batchOffset + index;
  }

  @Override
  public PageResponse<EncryptedData> listSecretsMappedToAccount(
      String accountId, PageRequest<EncryptedData> pageRequest, boolean details) throws IllegalAccessException {
    // Also covers the case where its system originated call
    boolean isAccountAdmin = userService.isAccountAdmin(accountId);

    if (!isAccountAdmin) {
      return aPageResponse().withResponse(Collections.emptyList()).build();
    }

    pageRequest.addFilter(EncryptedDataKeys.usageRestrictions, Operator.NOT_EXISTS);

    PageResponse<EncryptedData> pageResponse = wingsPersistence.query(EncryptedData.class, pageRequest);

    List<EncryptedData> encryptedDataList = pageResponse.getResponse();

    for (EncryptedData encryptedData : encryptedDataList) {
      encryptedData.setEncryptedValue(SECRET_MASK.toCharArray());
      encryptedData.setEncryptionKey(SECRET_MASK);
      if (details) {
        encryptedData.setEncryptedBy(getSecretManagerName(encryptedData.getType(), encryptedData.getUuid(),
            encryptedData.getKmsId(), encryptedData.getEncryptionType()));

        encryptedData.setSetupUsage(getSecretUsage(encryptedData.getAccountId(), encryptedData.getUuid()).size());
        encryptedData.setRunTimeUsage(getUsageLogsSize(encryptedData.getUuid(), SettingVariableTypes.SECRET_TEXT));
        encryptedData.setChangeLog(
            getChangeLogs(encryptedData.getAccountId(), encryptedData.getUuid(), SettingVariableTypes.SECRET_TEXT)
                .size());
      }
    }

    pageResponse.setResponse(encryptedDataList);
    pageResponse.setTotal((long) encryptedDataList.size());
    return pageResponse;
  }

  @Override
  public List<UuidAware> getSecretUsage(String accountId, String secretTextId) {
    EncryptedData secretText = wingsPersistence.get(EncryptedData.class, secretTextId);
    Preconditions.checkNotNull(secretText, "could not find secret with id " + secretTextId);
    if (secretText.getParentIds() == null) {
      return Collections.emptyList();
    }

    SettingVariableTypes type = secretText.getType() == SettingVariableTypes.SECRET_TEXT
        ? SettingVariableTypes.SERVICE_VARIABLE
        : secretText.getType();
    Set<Parent> parents = new HashSet<>();
    for (String parentId : secretText.getParentIds()) {
      parents.add(Parent.builder()
                      .id(parentId)
                      .variableType(type)
                      .encryptionDetail(EncryptionDetail.builder()
                                            .encryptionType(secretText.getEncryptionType())
                                            .secretManagerName(getSecretManagerName(
                                                type, parentId, secretText.getKmsId(), secretText.getEncryptionType()))
                                            .build())
                      .build());
    }

    return fetchParents(accountId, parents);
  }

  @Override
  public SecretManagerConfig getSecretManager(String accountId, String entityId, EncryptionType encryptionType) {
    switch (encryptionType) {
      case LOCAL:
        return localEncryptionService.getEncryptionConfig(accountId);
      case KMS:
        return kmsService.getKmsConfig(accountId, entityId);
      case VAULT:
        return vaultService.getVaultConfig(accountId, entityId);
      case AWS_SECRETS_MANAGER:
        return secretsManagerService.getAwsSecretsManagerConfig(accountId, entityId);
      case AZURE_VAULT:
        return azureSecretsManagerService.getEncryptionConfig(accountId, entityId);
      case CYBERARK:
        return cyberArkService.getConfig(accountId, entityId);
      default:
        throw new IllegalStateException("invalid type: " + encryptionType);
    }
  }

  private List<String> getSecretIds(String entityId, SettingVariableTypes variableType) throws IllegalAccessException {
    final List<String> secretIds = new ArrayList<>();
    switch (variableType) {
      case SERVICE_VARIABLE:
        ServiceVariable serviceVariable =
            wingsPersistence.createQuery(ServiceVariable.class).filter(ID_KEY, entityId).get();

        if (serviceVariable != null) {
          List<Field> encryptedFields = getEncryptedFields(serviceVariable.getClass());

          for (Field field : encryptedFields) {
            Field encryptedRefField = getEncryptedRefField(field, serviceVariable);
            encryptedRefField.setAccessible(true);
            secretIds.add((String) encryptedRefField.get(serviceVariable));
          }
        }
        break;

      case CONFIG_FILE:
      case SECRET_TEXT:
        secretIds.add(entityId);
        break;

      default:
        SettingAttribute settingAttribute =
            wingsPersistence.createQuery(SettingAttribute.class).filter(ID_KEY, entityId).get();

        if (settingAttribute != null) {
          List<Field> encryptedFields = getEncryptedFields(settingAttribute.getValue().getClass());

          for (Field field : encryptedFields) {
            Field encryptedRefField = getEncryptedRefField(field, (EncryptableSetting) settingAttribute.getValue());
            encryptedRefField.setAccessible(true);
            secretIds.add((String) encryptedRefField.get(settingAttribute.getValue()));
          }
        }
    }
    return secretIds;
  }

  private List<UuidAware> fetchParents(String accountId, Set<Parent> parents) {
    TreeBasedTable<SettingVariableTypes, EncryptionDetail, List<Parent>> parentByTypes = TreeBasedTable.create();
    parents.forEach(parent -> {
      if (parentByTypes.get(parent.getVariableType(), parent.getEncryptionDetail()) == null) {
        parentByTypes.put(parent.getVariableType(), parent.getEncryptionDetail(), new ArrayList<>());
      }
      parentByTypes.get(parent.getVariableType(), parent.getEncryptionDetail()).add(parent);
    });

    List<UuidAware> rv = new ArrayList<>();
    parentByTypes.cellSet().forEach(cell -> {
      List<String> parentIds = cell.getValue().stream().map(parent -> parent.getId()).collect(Collectors.toList());
      switch (cell.getRowKey()) {
        case KMS:
          EncryptionConfig encryptionConfig = secretManagerConfigService.getDefaultSecretManager(accountId);
          if (encryptionConfig instanceof KmsConfig) {
            rv.add((KmsConfig) encryptionConfig);
          }
          break;
        case SERVICE_VARIABLE:
          List<ServiceVariable> serviceVariables = serviceVariableService
                                                       .list(aPageRequest()
                                                                 .addFilter(ID_KEY, Operator.IN, parentIds.toArray())
                                                                 .addFilter(ACCOUNT_ID_KEY, Operator.EQ, accountId)
                                                                 .build())
                                                       .getResponse();
          serviceVariables.forEach(serviceVariable -> {
            serviceVariable.setValue(SECRET_MASK.toCharArray());
            if (serviceVariable.getEntityType() == EntityType.SERVICE_TEMPLATE) {
              ServiceTemplate serviceTemplate =
                  wingsPersistence.get(ServiceTemplate.class, serviceVariable.getEntityId());
              Preconditions.checkNotNull(serviceTemplate, "can't find service template " + serviceVariable);
              serviceVariable.setServiceId(serviceTemplate.getServiceId());
            }
            serviceVariable.setEncryptionType(cell.getColumnKey().getEncryptionType());
            serviceVariable.setEncryptedBy(cell.getColumnKey().getSecretManagerName());
          });
          rv.addAll(serviceVariables);
          break;

        case CONFIG_FILE:
          List<ConfigFile> configFiles = configService
                                             .list(aPageRequest()
                                                       .addFilter(ID_KEY, Operator.IN, parentIds.toArray())
                                                       .addFilter(ACCOUNT_ID_KEY, Operator.EQ, accountId)
                                                       .build())
                                             .getResponse();

          configFiles.forEach(configFile -> {
            if (configFile.getEntityType() == EntityType.SERVICE_TEMPLATE) {
              ServiceTemplate serviceTemplate = wingsPersistence.get(ServiceTemplate.class, configFile.getEntityId());
              Preconditions.checkNotNull(serviceTemplate, "can't find service template " + configFile);
              configFile.setServiceId(serviceTemplate.getServiceId());
            }
            configFile.setEncryptionType(cell.getColumnKey().getEncryptionType());
            configFile.setEncryptedBy(cell.getColumnKey().getSecretManagerName());
          });
          rv.addAll(configFiles);
          break;

        case VAULT:
          List<VaultConfig> vaultConfigs = wingsPersistence.createQuery(VaultConfig.class)
                                               .field(ID_KEY)
                                               .in(parentIds)
                                               .field(ACCOUNT_ID_KEY)
                                               .equal(accountId)
                                               .asList();
          vaultConfigs.forEach(vaultConfig -> {
            vaultConfig.setEncryptionType(cell.getColumnKey().getEncryptionType());
            vaultConfig.setEncryptedBy(cell.getColumnKey().getSecretManagerName());
          });
          rv.addAll(vaultConfigs);
          break;

        case AWS_SECRETS_MANAGER:
          List<AwsSecretsManagerConfig> secretsManagerConfigs =
              wingsPersistence.createQuery(AwsSecretsManagerConfig.class)
                  .field(ID_KEY)
                  .in(parentIds)
                  .field(ACCOUNT_ID_KEY)
                  .equal(accountId)
                  .asList();
          secretsManagerConfigs.forEach(secretsManagerConfig -> {
            secretsManagerConfig.setEncryptionType(cell.getColumnKey().getEncryptionType());
            secretsManagerConfig.setEncryptedBy(cell.getColumnKey().getSecretManagerName());
          });
          rv.addAll(secretsManagerConfigs);
          break;

        case CYBERARK:
          List<CyberArkConfig> cyberArkConfigs = wingsPersistence.createQuery(CyberArkConfig.class)
                                                     .field(ID_KEY)
                                                     .in(parentIds)
                                                     .field(ACCOUNT_ID_KEY)
                                                     .equal(accountId)
                                                     .asList();
          cyberArkConfigs.forEach(cyberArkConfig -> {
            cyberArkConfig.setEncryptionType(cell.getColumnKey().getEncryptionType());
            cyberArkConfig.setEncryptedBy(cell.getColumnKey().getSecretManagerName());
          });
          rv.addAll(cyberArkConfigs);
          break;

        default:
          List<SettingAttribute> settingAttributes = settingsService
                                                         .list(aPageRequest()
                                                                   .addFilter(ID_KEY, Operator.IN, parentIds.toArray())
                                                                   .addFilter(ACCOUNT_ID_KEY, Operator.EQ, accountId)
                                                                   .build(),
                                                             null, null)
                                                         .getResponse();
          settingAttributes.forEach(settingAttribute -> {
            settingAttribute.setEncryptionType(cell.getColumnKey().getEncryptionType());
            settingAttribute.setEncryptedBy(cell.getColumnKey().getSecretManagerName());
          });
          rv.addAll(settingAttributes);
          break;
      }
    });
    return rv;
  }

  private String getSecretManagerName(
      SettingVariableTypes type, String parentId, String kmsId, EncryptionType encryptionType) {
    switch (encryptionType) {
      case LOCAL:
        return HARNESS_DEFAULT_SECRET_MANAGER;
      case KMS:
        KmsConfig kmsConfig = wingsPersistence.get(KmsConfig.class, kmsId);
        Preconditions.checkNotNull(kmsConfig,
            "could not find kmsId " + kmsId + " for " + type + " id: " + parentId + " encryptionType" + encryptionType);
        return kmsConfig.getName();
      case VAULT:
        VaultConfig vaultConfig = wingsPersistence.get(VaultConfig.class, kmsId);
        Preconditions.checkNotNull(vaultConfig,
            "could not find kmsId " + kmsId + " for " + type + " id: " + parentId + " encryptionType" + encryptionType);
        return vaultConfig.getName();
      case AWS_SECRETS_MANAGER:
        AwsSecretsManagerConfig secretsManagerConfig = wingsPersistence.get(AwsSecretsManagerConfig.class, kmsId);
        Preconditions.checkNotNull(secretsManagerConfig,
            "could not find kmsId " + kmsId + " for " + type + " id: " + parentId + " encryptionType" + encryptionType);
        return secretsManagerConfig.getName();
      case AZURE_VAULT:
        AzureVaultConfig azureConfig = wingsPersistence.get(AzureVaultConfig.class, kmsId);
        Preconditions.checkNotNull(azureConfig,
            "could not find azureid " + kmsId + " for " + type + " id: " + parentId + " encryptionType"
                + encryptionType);
        return azureConfig.getName();
      case CYBERARK:
        CyberArkConfig cyberArkConfig = wingsPersistence.get(CyberArkConfig.class, kmsId);
        Preconditions.checkNotNull(cyberArkConfig,
            "could not find azureid " + kmsId + " for " + type + " id: " + parentId + " encryptionType"
                + encryptionType);
        return cyberArkConfig.getName();
      default:
        throw new IllegalArgumentException("Invalid type: " + encryptionType);
    }
  }

  private void validateSecretManagerConfigs(String accountId) {
    List<SecretManagerConfig> encryptionConfigs = secretManagerConfigService.listSecretManagers(accountId, false);
    for (EncryptionConfig encryptionConfig : encryptionConfigs) {
      KmsSetupAlert kmsSetupAlert =
          KmsSetupAlert.builder()
              .kmsId(encryptionConfig.getUuid())
              .message("Secret manager " + encryptionConfig.getName() + " of type "
                  + encryptionConfig.getEncryptionType() + " is not able to encrypt/decrypt. Please check your setup")
              .build();
      try {
        switch (encryptionConfig.getEncryptionType()) {
          case KMS:
            kmsService.encrypt(UUID.randomUUID().toString().toCharArray(), accountId, (KmsConfig) encryptionConfig);
            break;
          case VAULT:
            vaultService.encrypt(VAULT_VAILDATION_URL, Boolean.TRUE.toString(), accountId, SettingVariableTypes.VAULT,
                (VaultConfig) encryptionConfig, null);
            break;
          case AWS_SECRETS_MANAGER:
            secretsManagerService.validateSecretsManagerConfig((AwsSecretsManagerConfig) encryptionConfig);
            break;
          case AZURE_VAULT:
            azureSecretsManagerService.validateSecretsManagerConfig((AzureVaultConfig) encryptionConfig);
            break;
          case CYBERARK:
            cyberArkService.validateConfig((CyberArkConfig) encryptionConfig);
            break;
          default:
            break;
        }
        alertService.closeAlert(accountId, GLOBAL_APP_ID, AlertType.InvalidKMS, kmsSetupAlert);
        logger.info("Successfully validated secret manager {} of type {} for account {} ", encryptionConfig.getUuid(),
            encryptionConfig.getEncryptionType(), accountId);
      } catch (Exception e) {
        logger.info("Could not validate secret manager {} of type {} for account {}", encryptionConfig.getUuid(),
            encryptionConfig.getEncryptionType(), accountId, e);
        alertService.openAlert(accountId, GLOBAL_APP_ID, AlertType.InvalidKMS, kmsSetupAlert);
      }
    }
  }

  private static boolean containsIllegalCharacters(String name) {
    String[] parts = name.split(ILLEGAL_CHARACTERS, 2);
    return parts.length > 1;
  }

  @Override
  public void deleteByAccountId(String accountId) {
    List<EncryptedData> encryptedDataList =
        wingsPersistence.createQuery(EncryptedData.class).filter(ACCOUNT_ID_KEY, accountId).asList();
    for (EncryptedData encryptedData : encryptedDataList) {
      deleteSecret(accountId, encryptedData.getUuid());
    }
  }

  @Data
  @Builder
  @EqualsAndHashCode(exclude = {"encryptionDetail", "variableType"})
  private static class Parent {
    private String id;
    private EncryptionDetail encryptionDetail;
    private SettingVariableTypes variableType;
  }

  @Data
  @Builder
  private static class EncryptionDetail implements Comparable<EncryptionDetail> {
    private EncryptionType encryptionType;
    private String secretManagerName;

    @Override
    public int compareTo(EncryptionDetail o) {
      return this.encryptionType.compareTo(o.encryptionType);
    }
  }

  // Creating a map of appId/envIds which are referring the specific secret through service variable etc.
  private Map<String, Set<String>> getSetupAppEnvMap(EncryptedData encryptedData) {
    Map<String, Set<String>> referredAppEnvMap = new HashMap<>();
    List<UuidAware> secretUsages = getSecretUsage(encryptedData.getAccountId(), encryptedData.getUuid());
    for (UuidAware uuidAware : secretUsages) {
      String appId = null;
      String envId = null;
      String entityId = null;
      EntityType entityType = null;
      if (uuidAware instanceof ServiceVariable) {
        ServiceVariable serviceVariable = (ServiceVariable) uuidAware;
        appId = serviceVariable.getAppId();
        envId = serviceVariable.getEnvId();
        entityType = serviceVariable.getEntityType();
        entityId = serviceVariable.getEntityId();
      } else if (uuidAware instanceof ConfigFile) {
        ConfigFile configFile = (ConfigFile) uuidAware;
        appId = configFile.getAppId();
        envId = configFile.getEnvId();
        entityType = configFile.getEntityType();
        entityId = configFile.getEntityId();
      }

      // Retrieve envId from entity Id reference.
      if (entityType == EntityType.ENVIRONMENT) {
        Environment environment = envService.get(appId, entityId);
        if (environment != null) {
          envId = environment.getUuid();
        }
      }

      if (isNotEmpty(appId) && !GLOBAL_APP_ID.equals(appId)) {
        Set<String> envIds = referredAppEnvMap.computeIfAbsent(appId, k -> new HashSet<>());
        if (isNotEmpty(envId) && !GLOBAL_ENV_ID.equals(envId)) {
          envIds.add(envId);
        }
      }
    }
    return referredAppEnvMap;
  }

  /**
   * We should fail the secret usage restriction update if the app/env is still referred by other setup entities
   * but the update will remove such references. It will result in RBAC enforcement inconsistencies if this type of
   * operations are not prevented.
   */
  private void validateAppEnvChangesInUsageRestrictions(
      EncryptedData encryptedData, UsageRestrictions usageRestrictions) {
    Map<String, Set<String>> setupAppEnvMap = getSetupAppEnvMap(encryptedData);
    if (setupAppEnvMap.size() == 0) {
      // This secret is not referred by any setup entities. no need to check.
      return;
    }

    usageRestrictionsService.validateSetupUsagesOnUsageRestrictionsUpdate(
        encryptedData.getAccountId(), setupAppEnvMap, usageRestrictions);
  }

  private boolean deleteAndReportForAuditRecord(String accountId, EncryptedData encryptedData) {
    boolean deleted = wingsPersistence.delete(EncryptedData.class, encryptedData.getUuid());
    if (deleted && eligibleForCrudAudit(encryptedData)) {
      auditServiceHelper.reportDeleteForAuditingUsingAccountId(accountId, encryptedData);
    }

    return deleted;
  }

  @Builder
  private static class ParsedVaultSecretRef {
    String secretManagerName;
    String vaultConfigId;
    VaultConfig vaultConfig;
    String basePath;
    String relativePath;
    String fullPath;
    String keyName;
  }
}
