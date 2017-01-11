package software.wings.beans.artifact;

import static java.util.stream.Collectors.toSet;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * ArtifactStream bean class.
 *
 * @author Rishi
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "sourceType", include = As.EXISTING_PROPERTY)
@Entity(value = "artifactStream")
public abstract class ArtifactStream extends Base {
  /**
   * The Date format.
   */
  static final DateFormat dateFormat = new SimpleDateFormat("HHMMSS");
  @NotEmpty private String sourceName;
  @NotNull private SourceType sourceType;
  @NotEmpty private String settingId;
  @NotEmpty private String jobname;
  @NotEmpty @Valid private List<ArtifactPathServiceEntry> artifactPathServices = Lists.newArrayList();
  private boolean autoDownload = false;
  private boolean autoApproveForProduction = false;
  private List<ArtifactStreamAction> streamActions = new ArrayList<>();
  @Transient private Artifact lastArtifact;

  /**
   * Instantiates a new lastArtifact source.
   *
   * @param sourceType the source type
   */
  public ArtifactStream(SourceType sourceType) {
    this.sourceType = sourceType;
  }

  /**
   * Gets date format.
   *
   * @return the date format
   */
  public static DateFormat getDateFormat() {
    return dateFormat;
  }

  /**
   * Gets service ids.
   *
   * @return the service ids
   */
  public Set<String> getServiceIds() {
    return artifactPathServices.stream()
        .flatMap(artifactPathServiceEntry -> artifactPathServiceEntry.getServiceIds().stream())
        .collect(toSet());
  }

  public void setServiceIds(Set<String> serviceIds) {}

  /**
   * Gets artifact display name.
   *
   * @param buildNo the build no
   * @return the artifact display name
   */
  public abstract String getArtifactDisplayName(int buildNo);

  /**
   * Gets source name.
   *
   * @return the source name
   */
  public String getSourceName() {
    return sourceName;
  }

  /**
   * Sets source name.
   *
   * @param sourceName the source name
   */
  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }

  /**
   * Gets source type.
   *
   * @return the source type
   */
  public SourceType getSourceType() {
    return sourceType;
  }

  /**
   * Gets setting id.
   *
   * @return the setting id
   */
  public String getSettingId() {
    return settingId;
  }

  /**
   * Sets setting id.
   *
   * @param settingId the setting id
   */
  public void setSettingId(String settingId) {
    this.settingId = settingId;
  }

  /**
   * Gets jobname.
   *
   * @return the jobname
   */
  public String getJobname() {
    return jobname;
  }

  /**
   * Sets jobname.
   *
   * @param jobname the jobname
   */
  public void setJobname(String jobname) {
    this.jobname = jobname;
  }

  /**
   * Gets artifact path services.
   *
   * @return the artifact path services
   */
  public List<ArtifactPathServiceEntry> getArtifactPathServices() {
    return artifactPathServices;
  }

  /**
   * Sets artifact path services.
   *
   * @param artifactPathServices the artifact path services
   */
  public void setArtifactPathServices(List<ArtifactPathServiceEntry> artifactPathServices) {
    this.artifactPathServices = artifactPathServices;
  }

  /**
   * Is auto download boolean.
   *
   * @return the boolean
   */
  public boolean isAutoDownload() {
    return autoDownload;
  }

  /**
   * Sets auto download.
   *
   * @param autoDownload the auto download
   */
  public void setAutoDownload(boolean autoDownload) {
    this.autoDownload = autoDownload;
  }

  /**
   * Is auto approve for production boolean.
   *
   * @return the boolean
   */
  public boolean isAutoApproveForProduction() {
    return autoApproveForProduction;
  }

  /**
   * Sets auto approve for production.
   *
   * @param autoApproveForProduction the auto approve for production
   */
  public void setAutoApproveForProduction(boolean autoApproveForProduction) {
    this.autoApproveForProduction = autoApproveForProduction;
  }

  /**
   * Gets stream actions.
   *
   * @return the stream actions
   */
  public List<ArtifactStreamAction> getStreamActions() {
    return streamActions;
  }

  /**
   * Sets stream actions.
   *
   * @param streamActions the stream actions
   */
  public void setStreamActions(List<ArtifactStreamAction> streamActions) {
    this.streamActions = streamActions;
  }

  /**
   * Gets last artifact.
   *
   * @return the last artifact
   */
  public Artifact getLastArtifact() {
    return lastArtifact;
  }

  /**
   * Sets last artifact.
   *
   * @param lastArtifact the last artifact
   */
  public void setLastArtifact(Artifact lastArtifact) {
    this.lastArtifact = lastArtifact;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(sourceName, sourceType, settingId, jobname, artifactPathServices, autoDownload,
              autoApproveForProduction, streamActions, lastArtifact);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final ArtifactStream other = (ArtifactStream) obj;
    return Objects.equals(this.sourceName, other.sourceName) && Objects.equals(this.sourceType, other.sourceType)
        && Objects.equals(this.settingId, other.settingId) && Objects.equals(this.jobname, other.jobname)
        && Objects.equals(this.artifactPathServices, other.artifactPathServices)
        && Objects.equals(this.autoDownload, other.autoDownload)
        && Objects.equals(this.autoApproveForProduction, other.autoApproveForProduction)
        && Objects.equals(this.streamActions, other.streamActions)
        && Objects.equals(this.lastArtifact, other.lastArtifact);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("sourceName", sourceName)
        .add("sourceType", sourceType)
        .add("settingId", settingId)
        .add("jobname", jobname)
        .add("artifactPathServices", artifactPathServices)
        .add("autoDownload", autoDownload)
        .add("autoApproveForProduction", autoApproveForProduction)
        .add("streamActions", streamActions)
        .add("lastArtifact", lastArtifact)
        .toString();
  }

  /**
   * The Enum SourceType.
   */
  public enum SourceType {
    /**
     * Jenkins source type.
     */
    JENKINS, /**
              * BambooService source type.
              */
    BAMBOO, /**
             * Nexus source type.
             */
    NEXUS, /**
            * Artifactory source type.
            */
    ARTIFACTORY, /**
                  * Svn source type.
                  */
    SVN, /**
          * Git source type.
          */
    GIT, /**
          * Http source type.
          */
    HTTP, /**
           * File upload source type.
           */
    FILE_UPLOAD
  }
}
