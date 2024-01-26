package com.xuecheng.media.model.dto;

import com.xuecheng.media.model.po.MediaFiles;
import lombok.Data;


/**
 * 上传图片返回前端类  这个类用于返回前端。如果前端要求返回更多数据，就往这里加属性
 */
@Data
public class UploadFileResultDto extends MediaFiles {
}
