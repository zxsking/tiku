package com.example.questionbank.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充：
 * - created_at: 插入时填充
 * - updated_at: 插入和更新时填充
 *
 * 只对实体中存在对应字段且当前值为空时生效。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        if (getFieldValByName("createdAt", metaObject) == null) {
            strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        }
        if (getFieldValByName("updatedAt", metaObject) == null) {
            strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, now);
    }
}

