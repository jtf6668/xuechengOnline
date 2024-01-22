package com.xuecheng.content.model.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 修改课程信息的类,比添加课程信息的类多一个id
 */
@Data
public class EditCourseDto extends AddCourseDto{
    @ApiModelProperty(value = "课程id",required = true)
    private Long id;
}
