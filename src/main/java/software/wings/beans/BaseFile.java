package software.wings.beans;

/**
 * Created by anubhaw on 4/13/16.
 */

public class BaseFile extends Base {
  private String fileUUID;
  private String name;
  private String mimeType;
  private long size;
  private ChecksumType checksumType;
  private String checksum;

  public String getFileUUID() {
    return fileUUID;
  }

  public void setFileUUID(String fileUUID) {
    this.fileUUID = fileUUID;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public ChecksumType getChecksumType() {
    return checksumType;
  }

  public void setChecksumType(ChecksumType checksumType) {
    this.checksumType = checksumType;
  }

  public String getChecksum() {
    return checksum;
  }

  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }
}
