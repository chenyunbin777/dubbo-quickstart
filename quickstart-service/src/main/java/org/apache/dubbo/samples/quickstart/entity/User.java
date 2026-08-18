package org.apache.dubbo.samples.quickstart.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@TableName("t_user")
@Schema(description = "用户信息")
@Data
public class User {

    @TableId(type = IdType.AUTO)
    @Schema(description = "用户 ID，由数据库自动生成", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "用户名", example = "张三")
    private String name;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Schema(description = "用户密码，仅用于写入，不在响应中返回", example = "P@ssw0rd", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;

    @Schema(description = "年龄", example = "28")
    private Integer age;

    @Schema(description = "性别", example = "男")
    private String sex;

    @TableLogic
    @Schema(description = "是否已逻辑删除", example = "false", accessMode = Schema.AccessMode.READ_ONLY)
    private Boolean isDeleted;

    @Schema(description = "创建时间", example = "2026-08-18T10:30:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    @Schema(description = "最后更新时间", example = "2026-08-18T11:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

}
