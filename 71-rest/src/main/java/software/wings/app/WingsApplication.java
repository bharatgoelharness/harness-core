package software.wings.app;

import static com.google.common.collect.ImmutableMap.of;
import static com.google.inject.matcher.Matchers.not;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.time.Duration.ofSeconds;
import static software.wings.app.LoggingInitializer.initializeLogging;
import static software.wings.beans.FeatureName.MONGO_QUEUE_VERSIONING;
import static software.wings.common.Constants.USER_CACHE;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.name.Names;

import com.codahale.metrics.MetricRegistry;
import com.deftlabs.lock.mongo.DistributedLockSvc;
import com.github.dirkraft.dropwizard.fileassets.FileAssetsBundle;
import com.hazelcast.core.HazelcastInstance;
import com.palominolabs.metrics.guice.MetricsInstrumentationModule;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.bundles.assets.AssetsConfiguration;
import io.dropwizard.bundles.assets.ConfiguredAssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jersey.errors.EarlyEofExceptionMapper;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.harness.exception.WingsException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.mongo.MongoModule;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.model.Resource;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.PluginManager;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.app.MainConfiguration.AssetsConfigurationMixin;
import software.wings.beans.User;
import software.wings.common.Constants;
import software.wings.core.maintenance.MaintenanceController;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.core.queue.AbstractQueueListener;
import software.wings.core.queue.QueueListenerController;
import software.wings.dl.WingsPersistence;
import software.wings.exception.ConstraintViolationExceptionMapper;
import software.wings.exception.GenericExceptionMapper;
import software.wings.exception.JsonProcessingExceptionMapper;
import software.wings.exception.WingsExceptionMapper;
import software.wings.filter.AuditRequestFilter;
import software.wings.filter.AuditResponseFilter;
import software.wings.health.WingsHealthCheck;
import software.wings.jersey.JsonViews;
import software.wings.jersey.KryoFeature;
import software.wings.resources.AppResource;
import software.wings.scheduler.AdministrativeJob;
import software.wings.scheduler.ArchivalManager;
import software.wings.scheduler.BarrierBackupJob;
import software.wings.scheduler.PersistentLockCleanupJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.scheduler.ResourceConstraintBackupJob;
import software.wings.scheduler.WorkflowExecutionMonitorJob;
import software.wings.scheduler.YamlChangeSetPruneJob;
import software.wings.scheduler.ZombieHunterJob;
import software.wings.security.AuthResponseFilter;
import software.wings.security.AuthRuleFilter;
import software.wings.security.AuthenticationFilter;
import software.wings.service.impl.DelegateServiceImpl;
import software.wings.service.impl.SettingsServiceImpl;
import software.wings.service.impl.WorkflowExecutionServiceImpl;
import software.wings.service.impl.workflow.WorkflowServiceImpl;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.MigrationService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateMachineExecutor;
import software.wings.utils.CacheHelper;
import software.wings.utils.JsonSubtypeResolver;
import software.wings.waitnotify.Notifier;
import software.wings.waitnotify.NotifyResponseCleanupHandler;
import software.wings.yaml.gitSync.GitChangeSetRunnable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.cache.Caching;
import javax.cache.configuration.Configuration;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.validation.Validation;
import javax.validation.ValidatorFactory;
import javax.ws.rs.Path;

/**
 * The main application - entry point for the entire Wings Application.
 *
 * @author Rishi
 */
public class WingsApplication extends Application<MainConfiguration> {
  private static final Logger logger = LoggerFactory.getLogger(WingsApplication.class);

  private final MetricRegistry metricRegistry = new MetricRegistry();

  /**
   * The entry point of application.
   *
   * @param args the input arguments
   * @throws Exception the exception
   */
  public static void main(String[] args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Shutdown hook, entering maintenance...");
      MaintenanceController.forceMaintenance(true);
    }));

    new WingsApplication().run(args);
  }

  @Override
  public String getName() {
    return "Wings Application";
  }

  @Override
  public void initialize(Bootstrap<MainConfiguration> bootstrap) {
    initializeLogging();
    logger.info("bootstrapping ...");
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    bootstrap.addBundle(new ConfiguredAssetsBundle("/static", "/", "index.html"));
    bootstrap.addBundle(new SwaggerBundle<MainConfiguration>() {
      @Override
      protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(MainConfiguration mainConfiguration) {
        return mainConfiguration.getSwaggerBundleConfiguration();
      }
    });
    bootstrap.addBundle(new FileAssetsBundle("/.well-known"));
    bootstrap.getObjectMapper().addMixIn(AssetsConfiguration.class, AssetsConfigurationMixin.class);
    bootstrap.getObjectMapper().setSubtypeResolver(
        new JsonSubtypeResolver(bootstrap.getObjectMapper().getSubtypeResolver()));
    bootstrap.getObjectMapper().setConfig(
        bootstrap.getObjectMapper().getSerializationConfig().withView(JsonViews.Public.class));
    bootstrap.setMetricRegistry(metricRegistry);

    logger.info("bootstrapping done.");
  }

  @Override
  public void run(final MainConfiguration configuration, Environment environment) {
    logger.info("Starting app ...");

    logger.info("Entering startup maintenance mode");
    MaintenanceController.forceMaintenance(true);

    MongoModule databaseModule = new MongoModule(configuration.getMongoConnectionFactory());
    List<Module> modules = new ArrayList<>();
    modules.add(databaseModule);

    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();

    CacheModule cacheModule = new CacheModule(configuration);
    modules.add(cacheModule);
    StreamModule streamModule = new StreamModule(environment, cacheModule.getHazelcastInstance());

    modules.add(streamModule);

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(HazelcastInstance.class).toInstance(cacheModule.getHazelcastInstance());
        bind(MetricRegistry.class).toInstance(metricRegistry);
      }
    });

    modules.add(MetricsInstrumentationModule.builder()
                    .withMetricRegistry(metricRegistry)
                    .withMatcher(not(new AbstractMatcher<TypeLiteral<?>>() {
                      @Override
                      public boolean matches(TypeLiteral<?> typeLiteral) {
                        return typeLiteral.getRawType().isAnnotationPresent(Path.class);
                      }
                    }))
                    .build());

    modules.add(new ValidationModule(validatorFactory));
    modules.addAll(new WingsModule(configuration).cumulativeDependencies());
    modules.add(new YamlModule());
    modules.add(new QueueModule(databaseModule.getPrimaryDatastore(),
        StringUtils.contains(configuration.getFeatureNames(), MONGO_QUEUE_VERSIONING.toString())));
    modules.add(new ExecutorModule());
    modules.add(new TemplateModule());

    Injector injector = Guice.createInjector(modules);

    Caching.getCachingProvider().getCacheManager().createCache(USER_CACHE, new Configuration<String, User>() {
      public static final long serialVersionUID = 1L;

      @Override
      public Class<String> getKeyType() {
        return String.class;
      }

      @Override
      public Class<User> getValueType() {
        return User.class;
      }

      @Override
      public boolean isStoreByValue() {
        return true;
      }
    });

    streamModule.getAtmosphereServlet().framework().objectFactory(new GuiceObjectFactory(injector));

    registerResources(environment, injector);

    registerManagedBeans(environment, injector);

    registerQueueListeners(injector);

    scheduleJobs(injector);

    registerObservers(injector);

    registerCronJobs(injector);

    registerCorsFilter(configuration, environment);

    registerAuditResponseFilter(environment, injector);

    registerJerseyProviders(environment);

    registerCharsetResponseFilter(environment, injector);

    // Authentication/Authorization filters
    registerAuthFilters(configuration, environment, injector);

    environment.healthChecks().register("WingsApp", new WingsHealthCheck());

    startPlugins(injector);

    environment.lifecycle().addServerLifecycleListener(server -> {
      for (Connector connector : server.getConnectors()) {
        if (connector instanceof ServerConnector) {
          ServerConnector serverConnector = (ServerConnector) connector;
          if (serverConnector.getName().equalsIgnoreCase("application")) {
            configuration.setSslEnabled(
                serverConnector.getDefaultConnectionFactory().getProtocol().equalsIgnoreCase("ssl"));
            configuration.setApplicationPort(serverConnector.getLocalPort());
            return;
          }
        }
      }
    });

    startArchival(injector);
    // TODO purge behavior is buggy.
    // TODO it needs to be revisited
    // startAnalysisLogPurger(injector);

    initializeFeatureFlags(injector);

    initializeServiceSecretKeys(injector);

    runMigrations(injector);

    // Access all caches before coming out of maintenance
    CacheHelper cacheHelper = injector.getInstance(CacheHelper.class);

    cacheHelper.getUserCache();
    cacheHelper.getUserPermissionInfoCache();
    cacheHelper.getNewRelicApplicationCache();
    cacheHelper.getWhitelistConfigCache();

    String deployMode = System.getenv("DEPLOY_MODE");

    if (DeployMode.isOnPrem(deployMode)) {
      AccountService accountService = injector.getInstance(AccountService.class);
      String encryptedLicenseInfoBase64String = System.getenv(Constants.LICENSE_INFO);
      if (isEmpty(encryptedLicenseInfoBase64String)) {
        logger.error("No license info is provided");
      } else {
        try {
          accountService.updateAccountLicenseForOnPrem(encryptedLicenseInfoBase64String);
        } catch (WingsException ex) {
          logger.error("Error while updating license info", ex);
        }
      }
    }

    logger.info("Leaving startup maintenance mode");
    MaintenanceController.resetForceMaintenance();

    logger.info("Starting app done");
  }

  private void registerAuditResponseFilter(Environment environment, Injector injector) {
    environment.servlets()
        .addFilter("AuditResponseFilter", injector.getInstance(AuditResponseFilter.class))
        .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
    environment.jersey().register(injector.getInstance(AuditRequestFilter.class));
  }

  private void registerCorsFilter(MainConfiguration configuration, Environment environment) {
    FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
    String allowedOrigins = configuration.getPortal().getUrl();
    if (!configuration.getPortal().getAllowedOrigins().isEmpty()) {
      allowedOrigins = configuration.getPortal().getAllowedOrigins();
    }
    cors.setInitParameters(of("allowedOrigins", allowedOrigins, "allowedHeaders",
        "X-Requested-With,Content-Type,Accept,Origin,Authorization", "allowedMethods",
        "OPTIONS,GET,PUT,POST,DELETE,HEAD", "preflightMaxAge", "86400"));
    cors.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
  }

  private void registerResources(Environment environment, Injector injector) {
    Reflections reflections = new Reflections(AppResource.class.getPackage().getName());

    Set<Class<? extends Object>> resourceClasses = reflections.getTypesAnnotatedWith(Path.class);
    for (Class<?> resource : resourceClasses) {
      if (Resource.isAcceptable(resource)) {
        environment.jersey().register(injector.getInstance(resource));
      }
    }
  }

  private void registerManagedBeans(Environment environment, Injector injector) {
    environment.lifecycle().manage((Managed) injector.getInstance(WingsPersistence.class));
    environment.lifecycle().manage((Managed) injector.getInstance(DistributedLockSvc.class));
    environment.lifecycle().manage(injector.getInstance(QueueListenerController.class));
    environment.lifecycle().manage(injector.getInstance(MaintenanceController.class));
    environment.lifecycle().manage(injector.getInstance(ConfigurationController.class));
    environment.lifecycle().manage(
        (Managed) injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("timer"))));
    environment.lifecycle().manage(
        (Managed) injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("notifier"))));
    environment.lifecycle().manage((Managed) injector.getInstance(ExecutorService.class));
  }

  private void registerQueueListeners(Injector injector) {
    logger.info("Initializing queuelisteners...");
    Reflections reflections = new Reflections("software.wings");

    QueueListenerController queueListenerController = injector.getInstance(QueueListenerController.class);
    Set<Class<? extends AbstractQueueListener>> queueListeners = reflections.getSubTypesOf(AbstractQueueListener.class);
    for (Class<? extends AbstractQueueListener> queueListener : queueListeners) {
      logger.info("Registering queue listener for queue {}", injector.getInstance(queueListener).getQueue().name());
      queueListenerController.register(injector.getInstance(queueListener), 5);
    }
  }

  private void scheduleJobs(Injector injector) {
    logger.info("Initializing scheduled jobs...");
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("notifier")))
        .scheduleWithFixedDelay(injector.getInstance(Notifier.class), 0L, 30L, TimeUnit.SECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("notifyResponseCleaner")))
        .scheduleWithFixedDelay(injector.getInstance(NotifyResponseCleanupHandler.class), 0L, 30L, TimeUnit.SECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("delegateTaskNotifier")))
        .scheduleWithFixedDelay(injector.getInstance(DelegateQueueTask.class), 0L, 12L, TimeUnit.SECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("gitChangeSet")))
        .scheduleWithFixedDelay(injector.getInstance(GitChangeSetRunnable.class), 0L, 2L, TimeUnit.SECONDS);
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("taskPollExecutor")))
        .scheduleWithFixedDelay(injector.getInstance(DelegateServiceImpl.class), 0L, 2L, TimeUnit.SECONDS);
  }

  public static void registerObservers(Injector injector) {
    SettingsServiceImpl settingsService = (SettingsServiceImpl) injector.getInstance(Key.get(SettingsService.class));
    StateMachineExecutor stateMachineExecutor = injector.getInstance(Key.get(StateMachineExecutor.class));
    WorkflowServiceImpl workflowService = (WorkflowServiceImpl) injector.getInstance(Key.get(WorkflowService.class));
    WorkflowExecutionServiceImpl workflowExecutionService =
        (WorkflowExecutionServiceImpl) injector.getInstance(Key.get(WorkflowExecutionService.class));

    settingsService.getManipulationSubject().register(workflowService);
    stateMachineExecutor.getStatusUpdateSubject().register(workflowExecutionService);
  }

  private void registerCronJobs(Injector injector) {
    logger.info("Register cron jobs...");
    final QuartzScheduler jobScheduler =
        injector.getInstance(Key.get(QuartzScheduler.class, Names.named("JobScheduler")));

    PersistentLocker persistentLocker = injector.getInstance(Key.get(PersistentLocker.class));

    try (AcquiredLock acquiredLock = persistentLocker.waitToAcquireLock(
             WingsApplication.class, "Initialization", ofSeconds(5), ofSeconds(10))) {
      // If we do not get the lock, that's not critical - that's most likely because other managers took it
      // and they will initialize the jobs.
      if (acquiredLock != null) {
        WorkflowExecutionMonitorJob.add(jobScheduler);
        BarrierBackupJob.addJob(jobScheduler);
        ResourceConstraintBackupJob.addJob(jobScheduler);
        PersistentLockCleanupJob.add(jobScheduler);
        ZombieHunterJob.scheduleJobs(jobScheduler);
        AdministrativeJob.addJob(jobScheduler);
        YamlChangeSetPruneJob.add(jobScheduler);
      }
    }
  }

  private void registerJerseyProviders(Environment environment) {
    environment.jersey().register(KryoFeature.class);
    environment.jersey().register(EarlyEofExceptionMapper.class);
    environment.jersey().register(JsonProcessingExceptionMapper.class);
    environment.jersey().register(ConstraintViolationExceptionMapper.class);
    environment.jersey().register(WingsExceptionMapper.class);
    environment.jersey().register(GenericExceptionMapper.class);
    environment.jersey().register(MultiPartFeature.class);
  }

  private void registerAuthFilters(MainConfiguration configuration, Environment environment, Injector injector) {
    if (configuration.isEnableAuth()) {
      environment.jersey().register(new AuthValueFactoryProvider.Binder<>(User.class));
      environment.jersey().register(injector.getInstance(AuthRuleFilter.class));
      environment.jersey().register(injector.getInstance(AuthResponseFilter.class));
      environment.jersey().register(injector.getInstance(AuthenticationFilter.class));
    }
  }

  private void registerCharsetResponseFilter(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(CharsetResponseFilter.class));
  }

  private void startPlugins(Injector injector) {
    PluginManager pluginManager = injector.getInstance(PluginManager.class);
    pluginManager.loadPlugins();
    pluginManager.startPlugins();
  }

  private void startArchival(Injector injector) {
    final ArchivalManager archivalManager = new ArchivalManager(injector.getInstance(WingsPersistence.class));
    archivalManager.startArchival();
  }

  private void initializeFeatureFlags(Injector injector) {
    injector.getInstance(FeatureFlagService.class).initializeFeatureFlags();
  }

  private void initializeServiceSecretKeys(Injector injector) {
    injector.getInstance(LearningEngineService.class).initializeServiceSecretKeys();
  }

  private void runMigrations(Injector injector) {
    injector.getInstance(MigrationService.class).runMigrations();
  }
}
