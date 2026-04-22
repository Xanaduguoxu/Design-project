package org.example.config.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.example.config.security.SecurityUtils;
import org.springframework.context.annotation.Configuration;

import java.util.Date;

/**
 * 功能描述: 公共字段自动补充配置
 */
@Configuration
public class MybatisColumnsConfig implements MetaObjectHandler {

    /**
     * 功能描述: 设置数据新增时候的,字段自动赋值规则
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "del", Integer.class, 0);

        this.strictInsertFill(metaObject, "createTime", Date.class, new Date());
        this.strictInsertFill(metaObject, "updateTime", Date.class, new Date());

        this.strictInsertFill(metaObject, "createBy", String.class, SecurityUtils.getUsername());
        this.strictInsertFill(metaObject, "updateBy", String.class, SecurityUtils.getUsername());
    }

    /**
     * 功能描述: 设置数据修改update时候的,字段自动赋值规则
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", Date.class, new Date());
        this.strictUpdateFill(metaObject, "updateBy", String.class, SecurityUtils.getUsername());
    }
}
