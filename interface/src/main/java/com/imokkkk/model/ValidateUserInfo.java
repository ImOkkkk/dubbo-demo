package com.imokkkk.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotBlank;

/**
 * @author wyliu
 * @date 2024/7/29 21:28
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidateUserInfo implements Serializable {
    // 添加了 @NotBlank 注解
    @NotBlank(message = "id 不能为空")
    private String id;

    // 添加了 @Length 注解
    @Length(min = 5, max = 10, message = "name 必须在 5~10 个长度之间")
    private String name;

    // 无注解修饰
    private String sex;
}
