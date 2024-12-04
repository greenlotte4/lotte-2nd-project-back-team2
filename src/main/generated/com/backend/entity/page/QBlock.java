package com.backend.entity.page;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBlock is a Querydsl query type for Block
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBlock extends EntityPathBase<Block> {

    private static final long serialVersionUID = -381810638L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBlock block = new QBlock("block");

    public final StringPath content = createString("content");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final ComparablePath<java.util.UUID> id = createComparable("id", java.util.UUID.class);

    public final NumberPath<Integer> order = createNumber("order", Integer.class);

    public final QPage page;

    public final QBlock parent;

    public final StringPath style = createString("style");

    public final EnumPath<Block.BlockType> type = createEnum("type", Block.BlockType.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QBlock(String variable) {
        this(Block.class, forVariable(variable), INITS);
    }

    public QBlock(Path<? extends Block> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBlock(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBlock(PathMetadata metadata, PathInits inits) {
        this(Block.class, metadata, inits);
    }

    public QBlock(Class<? extends Block> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.page = inits.isInitialized("page") ? new QPage(forProperty("page"), inits.get("page")) : null;
        this.parent = inits.isInitialized("parent") ? new QBlock(forProperty("parent"), inits.get("parent")) : null;
    }

}

