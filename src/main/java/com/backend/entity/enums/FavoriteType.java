package com.backend.entity.enums;

public enum FavoriteType {
    BOARD("게시판"),
    POST("게시글");

    private final String description;

    FavoriteType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    // 문자열로 Enum 찾기
    public static FavoriteType fromString(String type) {
        for (FavoriteType favoriteType : FavoriteType.values()) {
            if (favoriteType.name().equalsIgnoreCase(type)) {
                return favoriteType;
            }
        }
        throw new IllegalArgumentException("Invalid FavoriteType: " + type);
    }
}
