package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamsDto;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.service.MediaFileService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import io.minio.errors.*;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/9/10 8:58
 */
@Service
@Slf4j
public class MediaFileServiceImpl implements MediaFileService {

 @Autowired
 MediaFilesMapper mediaFilesMapper;

 @Autowired
 MinioClient minioClient;

 //存储普通文件的桶
 @Value("${minio.bucket.files}")
 private String bucket_mediafiles;

 //存储视频文件的桶
 @Value("${minio.bucket.vediofiles}")
 private String bucket_vedio;

 @Override
 public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

  //构建查询条件对象
  LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

  //分页对象
  Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
  // 查询数据内容获得结果
  Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
  // 获取数据列表
  List<MediaFiles> list = pageResult.getRecords();
  // 获取数据总数
  long total = pageResult.getTotal();
  // 构建结果集
  PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
  return mediaListResult;

 }

 @Transactional
 @Override
 public UploadFileResultDto uploadFile(Long companyId, UploadFileParamsDto uploadFileParamsDto, String localFilePath) {

  //将文件上传到minio
  //获取文件的扩展名
  String filename = uploadFileParamsDto.getFilename();//拿到文件名
  String extension = filename.substring(filename.lastIndexOf("."));//取出文件中的扩展名
  String mimeType = getMimeType(extension);//转换成要的扩展名

  //此时的时间，文件路径的形式,作为文件存储的目录
  String defaultFolderPath = getDefaultFolderPath();

  //文件的md5值
  String fileMd5 = getFileMd5(new File(localFilePath));

  //文件名
  String objectName = defaultFolderPath + fileMd5 + extension;

  //上传文件到minio
  boolean result = addMediaFilesToMinIO(localFilePath, mimeType, bucket_mediafiles, objectName);
  if(!result){
   XueChengPlusException.cast("文件上传失败");
  }


  //将文件信息保存到数据库

  return null;
 }

 /**
  * 转换日期
  * @return
  */
 private String getDefaultFolderPath() {
  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
  String folder = sdf.format(new Date()).replace("-", "/")+"/";
  //2024-1-1变成2024/1/1/，因为/是路径的意思，要把日期作为文件存储的路径
  return folder;
 }

 /**
  * 获取文件的md5值
  * @param file
  * @return
  */
 private String getFileMd5(File file) {
  try (FileInputStream fileInputStream = new FileInputStream(file)) {
   String fileMd5 = DigestUtils.md5Hex(fileInputStream);
   return fileMd5;
  } catch (Exception e) {
   e.printStackTrace();
   return null;
  }
 }

 /**
  * 将文件上传到minio
  * @param localFilePath 文件本地路径
  * @param mimeType 媒体类型
  * @param bucket 桶
  * @param objectName 对象名，文件在minio存储的名字
  * @return
  */
 public boolean addMediaFilesToMinIO(String localFilePath,String mimeType,String bucket, String objectName) {
  //上传文件的参数信息
  try {
   UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
           .bucket(bucket)//桶
           .filename(localFilePath) //指定本地文件路径
 //                .object("1.mp4")//对象名 在桶下存储该文件
           .object(objectName)//对象名 放在子目录下
           .contentType(mimeType)//设置媒体文件类型
           .build();
   minioClient.uploadObject(uploadObjectArgs);
   return true;
  } catch (Exception e) {
   e.printStackTrace();
   log.error("上传文件出错,bucket:{},objectName:{},错误信息：{}",bucket,objectName,e.getMessage());
  }
  return false;
 }

 /**
  * 根据扩展名获取mimeType
  */
 private String getMimeType(String extension) {

  if (extension == null) {
   extension = "";//如果扩展名为空，就给一个空字符串，不然会报空指针异常
  }
  //根据扩展名取出mimeType
  ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".mp4");
  String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;//通用mimeType，字节流
  if (extensionMatch != null) {
   mimeType = extensionMatch.getMimeType();
  }
  return mimeType;
 }


}
