// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: io/harness/delegate/ng_delegate_task.proto

package io.harness.delegate;

/**
 * Protobuf type {@code io.harness.delegate.SendTaskAsyncRequest}
 */
@javax.annotation.Generated(value = "protoc", comments = "annotations:SendTaskAsyncRequest.java.pb.meta")
public final class SendTaskAsyncRequest extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:io.harness.delegate.SendTaskAsyncRequest)
    SendTaskAsyncRequestOrBuilder {
  private static final long serialVersionUID = 0L;
  // Use SendTaskAsyncRequest.newBuilder() to construct.
  private SendTaskAsyncRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private SendTaskAsyncRequest() {}

  @java.
  lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(UnusedPrivateParameter unused) {
    return new SendTaskAsyncRequest();
  }

  @java.
  lang.Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }
  private SendTaskAsyncRequest(
      com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new java.lang.NullPointerException();
    }
    com.google.protobuf.UnknownFieldSet.Builder unknownFields = com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          case 10: {
            io.harness.delegate.TaskId.Builder subBuilder = null;
            if (taskId_ != null) {
              subBuilder = taskId_.toBuilder();
            }
            taskId_ = input.readMessage(io.harness.delegate.TaskId.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(taskId_);
              taskId_ = subBuilder.buildPartial();
            }

            break;
          }
          default: {
            if (!parseUnknownField(input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
    return io.harness.delegate.NgDelegateTask.internal_static_io_harness_delegate_SendTaskAsyncRequest_descriptor;
  }

  @java.
  lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
    return io.harness.delegate.NgDelegateTask
        .internal_static_io_harness_delegate_SendTaskAsyncRequest_fieldAccessorTable.ensureFieldAccessorsInitialized(
            io.harness.delegate.SendTaskAsyncRequest.class, io.harness.delegate.SendTaskAsyncRequest.Builder.class);
  }

  public static final int TASK_ID_FIELD_NUMBER = 1;
  private io.harness.delegate.TaskId taskId_;
  /**
   * <code>.io.harness.delegate.TaskId task_id = 1[json_name = "taskId"];</code>
   * @return Whether the taskId field is set.
   */
  public boolean hasTaskId() {
    return taskId_ != null;
  }
  /**
   * <code>.io.harness.delegate.TaskId task_id = 1[json_name = "taskId"];</code>
   * @return The taskId.
   */
  public io.harness.delegate.TaskId getTaskId() {
    return taskId_ == null ? io.harness.delegate.TaskId.getDefaultInstance() : taskId_;
  }
  /**
   * <code>.io.harness.delegate.TaskId task_id = 1[json_name = "taskId"];</code>
   */
  public io.harness.delegate.TaskIdOrBuilder getTaskIdOrBuilder() {
    return getTaskId();
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1)
      return true;
    if (isInitialized == 0)
      return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
    if (taskId_ != null) {
      output.writeMessage(1, getTaskId());
    }
    unknownFields.writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    if (taskId_ != null) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, getTaskId());
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof io.harness.delegate.SendTaskAsyncRequest)) {
      return super.equals(obj);
    }
    io.harness.delegate.SendTaskAsyncRequest other = (io.harness.delegate.SendTaskAsyncRequest) obj;

    if (hasTaskId() != other.hasTaskId())
      return false;
    if (hasTaskId()) {
      if (!getTaskId().equals(other.getTaskId()))
        return false;
    }
    if (!unknownFields.equals(other.unknownFields))
      return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    if (hasTaskId()) {
      hash = (37 * hash) + TASK_ID_FIELD_NUMBER;
      hash = (53 * hash) + getTaskId().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static io.harness.delegate.SendTaskAsyncRequest parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.delegate.SendTaskAsyncRequest parseFrom(
      java.nio.ByteBuffer data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.delegate.SendTaskAsyncRequest parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.delegate.SendTaskAsyncRequest parseFrom(
      com.google.protobuf.ByteString data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.delegate.SendTaskAsyncRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static io.harness.delegate.SendTaskAsyncRequest parseFrom(
      byte[] data, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static io.harness.delegate.SendTaskAsyncRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.delegate.SendTaskAsyncRequest parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.delegate.SendTaskAsyncRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }
  public static io.harness.delegate.SendTaskAsyncRequest parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static io.harness.delegate.SendTaskAsyncRequest parseFrom(com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }
  public static io.harness.delegate.SendTaskAsyncRequest parseFrom(com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() {
    return newBuilder();
  }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(io.harness.delegate.SendTaskAsyncRequest prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code io.harness.delegate.SendTaskAsyncRequest}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:io.harness.delegate.SendTaskAsyncRequest)
      io.harness.delegate.SendTaskAsyncRequestOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return io.harness.delegate.NgDelegateTask.internal_static_io_harness_delegate_SendTaskAsyncRequest_descriptor;
    }

    @java.
    lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internalGetFieldAccessorTable() {
      return io.harness.delegate.NgDelegateTask
          .internal_static_io_harness_delegate_SendTaskAsyncRequest_fieldAccessorTable.ensureFieldAccessorsInitialized(
              io.harness.delegate.SendTaskAsyncRequest.class, io.harness.delegate.SendTaskAsyncRequest.Builder.class);
    }

    // Construct using io.harness.delegate.SendTaskAsyncRequest.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
      }
    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      if (taskIdBuilder_ == null) {
        taskId_ = null;
      } else {
        taskId_ = null;
        taskIdBuilder_ = null;
      }
      return this;
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return io.harness.delegate.NgDelegateTask.internal_static_io_harness_delegate_SendTaskAsyncRequest_descriptor;
    }

    @java.
    lang.Override
    public io.harness.delegate.SendTaskAsyncRequest getDefaultInstanceForType() {
      return io.harness.delegate.SendTaskAsyncRequest.getDefaultInstance();
    }

    @java.
    lang.Override
    public io.harness.delegate.SendTaskAsyncRequest build() {
      io.harness.delegate.SendTaskAsyncRequest result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.
    lang.Override
    public io.harness.delegate.SendTaskAsyncRequest buildPartial() {
      io.harness.delegate.SendTaskAsyncRequest result = new io.harness.delegate.SendTaskAsyncRequest(this);
      if (taskIdBuilder_ == null) {
        result.taskId_ = taskId_;
      } else {
        result.taskId_ = taskIdBuilder_.build();
      }
      onBuilt();
      return result;
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field, int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field, java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof io.harness.delegate.SendTaskAsyncRequest) {
        return mergeFrom((io.harness.delegate.SendTaskAsyncRequest) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(io.harness.delegate.SendTaskAsyncRequest other) {
      if (other == io.harness.delegate.SendTaskAsyncRequest.getDefaultInstance())
        return this;
      if (other.hasTaskId()) {
        mergeTaskId(other.getTaskId());
      }
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
      io.harness.delegate.SendTaskAsyncRequest parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (io.harness.delegate.SendTaskAsyncRequest) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private io.harness.delegate.TaskId taskId_;
    private com.google.protobuf.SingleFieldBuilderV3<io.harness.delegate.TaskId, io.harness.delegate.TaskId.Builder,
        io.harness.delegate.TaskIdOrBuilder> taskIdBuilder_;
    /**
     * <code>.io.harness.delegate.TaskId task_id = 1[json_name = "taskId"];</code>
     * @return Whether the taskId field is set.
     */
    public boolean hasTaskId() {
      return taskIdBuilder_ != null || taskId_ != null;
    }
    /**
     * <code>.io.harness.delegate.TaskId task_id = 1[json_name = "taskId"];</code>
     * @return The taskId.
     */
    public io.harness.delegate.TaskId getTaskId() {
      if (taskIdBuilder_ == null) {
        return taskId_ == null ? io.harness.delegate.TaskId.getDefaultInstance() : taskId_;
      } else {
        return taskIdBuilder_.getMessage();
      }
    }
    /**
     * <code>.io.harness.delegate.TaskId task_id = 1[json_name = "taskId"];</code>
     */
    public Builder setTaskId(io.harness.delegate.TaskId value) {
      if (taskIdBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        taskId_ = value;
        onChanged();
      } else {
        taskIdBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.io.harness.delegate.TaskId task_id = 1[json_name = "taskId"];</code>
     */
    public Builder setTaskId(io.harness.delegate.TaskId.Builder builderForValue) {
      if (taskIdBuilder_ == null) {
        taskId_ = builderForValue.build();
        onChanged();
      } else {
        taskIdBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.io.harness.delegate.TaskId task_id = 1[json_name = "taskId"];</code>
     */
    public Builder mergeTaskId(io.harness.delegate.TaskId value) {
      if (taskIdBuilder_ == null) {
        if (taskId_ != null) {
          taskId_ = io.harness.delegate.TaskId.newBuilder(taskId_).mergeFrom(value).buildPartial();
        } else {
          taskId_ = value;
        }
        onChanged();
      } else {
        taskIdBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.io.harness.delegate.TaskId task_id = 1[json_name = "taskId"];</code>
     */
    public Builder clearTaskId() {
      if (taskIdBuilder_ == null) {
        taskId_ = null;
        onChanged();
      } else {
        taskId_ = null;
        taskIdBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.io.harness.delegate.TaskId task_id = 1[json_name = "taskId"];</code>
     */
    public io.harness.delegate.TaskId.Builder getTaskIdBuilder() {
      onChanged();
      return getTaskIdFieldBuilder().getBuilder();
    }
    /**
     * <code>.io.harness.delegate.TaskId task_id = 1[json_name = "taskId"];</code>
     */
    public io.harness.delegate.TaskIdOrBuilder getTaskIdOrBuilder() {
      if (taskIdBuilder_ != null) {
        return taskIdBuilder_.getMessageOrBuilder();
      } else {
        return taskId_ == null ? io.harness.delegate.TaskId.getDefaultInstance() : taskId_;
      }
    }
    /**
     * <code>.io.harness.delegate.TaskId task_id = 1[json_name = "taskId"];</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<io.harness.delegate.TaskId, io.harness.delegate.TaskId.Builder,
        io.harness.delegate.TaskIdOrBuilder>
    getTaskIdFieldBuilder() {
      if (taskIdBuilder_ == null) {
        taskIdBuilder_ =
            new com.google.protobuf.SingleFieldBuilderV3<io.harness.delegate.TaskId, io.harness.delegate.TaskId.Builder,
                io.harness.delegate.TaskIdOrBuilder>(getTaskId(), getParentForChildren(), isClean());
        taskId_ = null;
      }
      return taskIdBuilder_;
    }
    @java.lang.Override
    public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }

    // @@protoc_insertion_point(builder_scope:io.harness.delegate.SendTaskAsyncRequest)
  }

  // @@protoc_insertion_point(class_scope:io.harness.delegate.SendTaskAsyncRequest)
  private static final io.harness.delegate.SendTaskAsyncRequest DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new io.harness.delegate.SendTaskAsyncRequest();
  }

  public static io.harness.delegate.SendTaskAsyncRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<SendTaskAsyncRequest> PARSER =
      new com.google.protobuf.AbstractParser<SendTaskAsyncRequest>() {
        @java.lang.Override
        public SendTaskAsyncRequest parsePartialFrom(
            com.google.protobuf.CodedInputStream input, com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new SendTaskAsyncRequest(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<SendTaskAsyncRequest> parser() {
    return PARSER;
  }

  @java.
  lang.Override
  public com.google.protobuf.Parser<SendTaskAsyncRequest> getParserForType() {
    return PARSER;
  }

  @java.
  lang.Override
  public io.harness.delegate.SendTaskAsyncRequest getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }
}
