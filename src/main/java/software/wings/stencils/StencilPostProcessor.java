package software.wings.stencils;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.MapUtils.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.utils.JsonUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

;

/**
 * Created by peeyushaggarwal on 6/27/16.
 */
@Singleton
public class StencilPostProcessor {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private Injector injector;

  public <T extends Stencil> List<Stencil> postProcess(List<T> stencils, String appId, String... args) {
    return stencils.stream().flatMap(t -> processStencil(t, appId, args)).collect(toList());
  }

  private <T extends Stencil> Stream<Stencil> processStencil(T t, String appId, String... args) {
    if (Arrays.stream(t.getTypeClass().getDeclaredFields())
            .filter(field -> field.getAnnotation(EnumData.class) != null || field.getAnnotation(Expand.class) != null)
            .findFirst()
            .isPresent()) {
      return Arrays.stream(t.getTypeClass().getDeclaredFields())
          .filter(field -> field.getAnnotation(EnumData.class) != null || field.getAnnotation(Expand.class) != null)
          .flatMap(field -> {
            EnumData enumData = field.getAnnotation(EnumData.class);
            Expand expand = field.getAnnotation(Expand.class);
            T stencil = t;
            if (enumData != null) {
              DataProvider dataProvider = injector.getInstance(enumData.enumDataProvider());
              Map<String, String> data = dataProvider.getData(appId, args);
              if (!isEmpty(data)) {
                if (enumData.expandIntoMultipleEntries()) {
                  return expandBasedOnEnumData(stencil, data, field);
                } else {
                  stencil = (T) addEnumDataToNode(stencil, data, field);
                }
              }
            }

            if (expand != null) {
              DataProvider dataProvider = injector.getInstance(expand.dataProvider());
              Map<String, String> data = dataProvider.getData(appId, args);
              if (!isEmpty(data)) {
                return expandBasedOnData(stencil, data, field);
              }
            }
            return Stream.of(stencil);
          });
    } else {
      return Stream.of(t);
    }
  }

  private <T extends Stencil> Stream<Stencil> expandBasedOnEnumData(T t, Map<String, String> data, Field field) {
    try {
      if (data != null) {
        return data.keySet().stream().map(key -> {
          JsonNode jsonSchema = t.getJsonSchema();
          ObjectNode jsonSchemaField = ((ObjectNode) jsonSchema.get("properties").get(field.getName()));
          jsonSchemaField.set("enum", JsonUtils.asTree(data.keySet()));
          jsonSchemaField.set("enumNames", JsonUtils.asTree(data.values()));
          jsonSchemaField.set("default", JsonUtils.asTree(key));
          OverridingStencil overridingStencil = t.getOverridingStencil();
          overridingStencil.setOverridingJsonSchema(jsonSchema);
          overridingStencil.setOverridingName(data.get(key));
          return overridingStencil;
        });
      }
    } catch (Exception e) {
      logger.warn("Unable to fill in values for stencil {}:field {} with data {}", t, field.getName(), data);
    }
    return Stream.of(t);
  }

  private <T extends Stencil> Stream<Stencil> expandBasedOnData(T t, Map<String, String> data, Field field) {
    try {
      if (data != null) {
        return data.keySet().stream().map(key -> {
          JsonNode jsonSchema = t.getJsonSchema();
          ObjectNode jsonSchemaField = ((ObjectNode) jsonSchema.get("properties").get(field.getName()));
          jsonSchemaField.set("default", JsonUtils.asTree(key));
          OverridingStencil overridingStencil = t.getOverridingStencil();
          overridingStencil.setOverridingJsonSchema(jsonSchema);
          overridingStencil.setOverridingName(data.get(key));
          return overridingStencil;
        });
      }
    } catch (Exception e) {
      logger.warn("Unable to fill in values for stencil {}:field {} with data {}", t, field.getName(), data);
    }
    return Stream.of(t);
  }

  private <T extends Stencil> Stencil addEnumDataToNode(T t, Map<String, String> data, Field field) {
    try {
      if (data != null) {
        JsonNode jsonSchema = t.getJsonSchema();
        ObjectNode jsonSchemaField = ((ObjectNode) jsonSchema.get("properties").get(field.getName()));
        jsonSchemaField.set("enum", JsonUtils.asTree(data.keySet()));
        jsonSchemaField.set("enumNames", JsonUtils.asTree(data.values()));
        OverridingStencil overridingStencil = t.getOverridingStencil();
        overridingStencil.setOverridingJsonSchema(jsonSchema);
        return overridingStencil;
      }
    } catch (Exception e) {
      logger.warn("Unable to fill in values for stencil {}:field {} with data {}", t, field.getName(), data);
    }
    return t;
  }
}
