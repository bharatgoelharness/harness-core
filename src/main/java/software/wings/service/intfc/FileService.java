package software.wings.service.intfc;

import com.mongodb.DBObject;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import software.wings.app.WingsBootstrap;
import software.wings.beans.BaseFile;
import software.wings.beans.FileMetadata;
import software.wings.dl.WingsPersistence;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface FileService {
  public String saveFile(FileMetadata fileMetadata, InputStream in, FileBucket fileBucket);
  public String saveFile(BaseFile baseFile, InputStream uploadedInputStream, FileBucket configs);

  public File download(String fileId, File file, FileBucket fileBucket);

  public void downloadToStream(String fileId, OutputStream op, FileBucket fileBucket);

  public GridFSFile getGridFsFile(String fileId, FileBucket fileBucket);

  public String uploadFromStream(String filename, InputStream in, FileBucket fileBucket, GridFSUploadOptions options);

  List<DBObject> getFilesMetaData(List<String> fileIDs, FileBucket fileBucket);

  public static enum FileBucket {
    LOB("lob"),
    ARTIFACTS("artifacts"),
    AUDITS("audits"),
    CONFIGS("configs"),
    LOGS("logs");

    FileBucket(String name, int chunkSize) {
      this.name = name;
      this.chunkSize = chunkSize;
      this.gridFSBucket = WingsBootstrap.lookup(WingsPersistence.class).createGridFSBucket(name);
    }

    FileBucket(String name) {
      this(name, 16 * 1024 * 1024);
    }

    private WingsPersistence wingsPersistence;
    private String name;
    private int chunkSize;
    private GridFSBucket gridFSBucket;

    public String getName() {
      return name;
    }

    public int getChunkSize() {
      return chunkSize;
    }

    public GridFSBucket getGridFSBucket() {
      return gridFSBucket;
    }
  }
}
