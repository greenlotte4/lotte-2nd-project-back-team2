package com.backend.entity.community;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QFavoriteBoard is a Querydsl query type for FavoriteBoard
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFavoriteBoard extends EntityPathBase<FavoriteBoard> {

    private static final long serialVersionUID = -528544799L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QFavoriteBoard favoriteBoard = new QFavoriteBoard("favoriteBoard");

    public final NumberPath<Long> favoriteId = createNumber("favoriteId", Long.class);

    public final NumberPath<Long> itemId = createNumber("itemId", Long.class);

    public final EnumPath<com.backend.entity.enums.FavoriteType> itemType = createEnum("itemType", com.backend.entity.enums.FavoriteType.class);

    public final com.backend.entity.user.QUser user;

    public QFavoriteBoard(String variable) {
        this(FavoriteBoard.class, forVariable(variable), INITS);
    }

    public QFavoriteBoard(Path<? extends FavoriteBoard> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QFavoriteBoard(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QFavoriteBoard(PathMetadata metadata, PathInits inits) {
        this(FavoriteBoard.class, metadata, inits);
    }

    public QFavoriteBoard(Class<? extends FavoriteBoard> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.backend.entity.user.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

