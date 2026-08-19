package com.example.search.content;

import com.example.common.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="content_chunk",indexes={@Index(columnList="source_type, source_id, chunk_index"),@Index(columnList="workspace_id"),@Index(columnList="owner_id"),@Index(columnList="embedding_status, embedding_model")})
@Entity
public class ContentChunk extends Auditable {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(name="source_type",nullable=false,length=30) private ContentSourceType sourceType;
    @Column(name="source_id",nullable=false) private Long sourceId;
    @Column(name="workspace_id") private Long workspaceId;
    @Column(name="owner_id") private Long ownerId;
    @Column(name="resource_type",nullable=false,length=30) private String resourceType;
    @Column(name="resource_id",nullable=false) private Long resourceId;
    @Column(name="source_title",nullable=false) private String sourceTitle;
    @Column(name="chunk_index",nullable=false) private int chunkIndex;
    @Column(columnDefinition="TEXT",nullable=false) private String content;
    @Column(name="content_hash",nullable=false,length=64) private String contentHash;
    @Column(name="source_version",nullable=false,length=64) private String sourceVersion;
    @Column(name="source_updated_at") private java.time.LocalDateTime sourceUpdatedAt;
    @Column(columnDefinition="TEXT") private String embedding;
    @Column(name="embedding_model") private String embeddingModel;
    @Enumerated(EnumType.STRING) @Column(name="embedding_status",nullable=false,length=20) private EmbeddingStatus embeddingStatus;
    @Column(name="embedding_attempts",nullable=false) private int embeddingAttempts;
    @Column(name="embedding_error",columnDefinition="TEXT") private String embeddingError;

    public ContentChunk(ContentSourceSnapshot source,int index,String content,String hash){
        sourceType=source.type();sourceId=source.sourceId();workspaceId=source.workspaceId();ownerId=source.ownerId();
        resourceType=source.resourceType();resourceId=source.resourceId();sourceTitle=source.title();sourceVersion=version(source);
        sourceUpdatedAt=source.sourceUpdatedAt();chunkIndex=index;this.content=content;contentHash=hash;embeddingStatus=EmbeddingStatus.PENDING;
    }
    public void retain(ContentSourceSnapshot source,int index){workspaceId=source.workspaceId();ownerId=source.ownerId();resourceType=source.resourceType();
        resourceId=source.resourceId();sourceTitle=source.title();sourceVersion=version(source);sourceUpdatedAt=source.sourceUpdatedAt();chunkIndex=index;}
    public void startEmbedding(){embeddingStatus=EmbeddingStatus.PROCESSING;embeddingAttempts++;embeddingError=null;}
    public void saveEmbedding(String value,String model){embedding=value;embeddingModel=model;embeddingStatus=EmbeddingStatus.READY;embeddingError=null;}
    public void failEmbedding(String message){embeddingStatus=EmbeddingStatus.FAILED;embeddingError=message;}
    private static String version(ContentSourceSnapshot source){return source.revision()==null?source.contentHash():String.valueOf(source.revision());}
}
