package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 视频处理任务类
 */
@Component
@Slf4j
public class VideoXxlJob {

    @Autowired
    MediaFileProcessService mediaFileProcessService;

    @Autowired
    MediaFileService mediaFileService;

    /**
     * 视频处理任务任务
     */
    @XxlJob("videoJobHandler")
    public void shardingJobHandler() throws Exception {

        // 分片参数
        int shardIndex = XxlJobHelper.getShardIndex();//执行器的序号，从0开始
        int shardTotal = XxlJobHelper.getShardTotal();//执行器总数

        //确定cpu核心数，能够运行多少个线程同时执行
        int processors = Runtime.getRuntime().availableProcessors();



        //查询待处理任务，查询的任务数最多是cpu核心数
        List<MediaProcess> mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, processors);

        int size = mediaProcessList.size();

        if(size<=0){
            log.debug("查询到的待处理任务数为0");
            return;
        }

        //创建线程池
        ExecutorService executorService = Executors.newFixedThreadPool(size);

        //使用计数器,初始值是要执行的任务值，直到全部执行完任务才结束
        CountDownLatch countDownLatch = new CountDownLatch(size);

        //每遍历一个任务就把它放到线程池中
        mediaProcessList.forEach(mediaProcess -> {
            executorService.execute(() ->{
                try {
                    //每个线程执行一个任务
                    Long id = mediaProcess.getId();

                    //文件id就是文件的md5值
                    String fileId = mediaProcess.getFileId();

                    //开启任务,乐观锁抢任务
                    boolean b = mediaFileProcessService.startTask(id);

                    if (!b) {
                        log.debug("抢占任务失败，任务id:{}", id);
                        return;
                    }

                    //桶
                    String bucket = mediaProcess.getBucket();
                    String filePath = mediaProcess.getFilePath();

                    //下载minio视频到本地
                    File file = mediaFileService.downloadFileFromMinIO(bucket, filePath);
                    if (file == null) {
                        log.debug("下载任务出错，任务id：{}", id);
                        //保存错误信息的数据库
                        mediaFileProcessService.saveProcessFinishStatus(id, "3", fileId, null, "下载视频到本地失败");
                        return;
                    }

                    //下载的avi视频的路径
                    String video_path = file.getAbsolutePath();
                    //转换后mp4的名称
                    String mp4_name = fileId + ".mp4";

                    //创建一个临时文件，作为将avi转换成mp4的文件
                    File mp4File = null;
                    try {
                        mp4File = File.createTempFile("minio", ".mp4");
                    } catch (IOException e) {
                        log.debug("创建临时文件异常，{}", e.getMessage());
                        //保存错误信息的数据库
                        mediaFileProcessService.saveProcessFinishStatus(id, "3", fileId, null, "创建临时文件异常");
                        return;
                    }

                    //临时MP4文件的路径
                    String mp4_path = mp4File.getAbsolutePath();

                    //执行视频转码
                    //视频转码工具类
                    Mp4VideoUtil mp4VideoUtil = new Mp4VideoUtil("D:\\develop\\ffmpeg\\ffmpeg.exe", video_path, mp4_name, mp4_path);
                    //开始视频转换，成功返回success，失败返回原因
                    String result = mp4VideoUtil.generateMp4();
                    if (!result.equals("success")) {
                        log.debug("视频转码失败，原因：{},bucket:{}", result, bucket);
                        //保存错误信息的数据库
                        mediaFileProcessService.saveProcessFinishStatus(id, "3", fileId, null, "视频转码失败");
                        return;
                    }

                    //上传到minio
                    boolean b1 = mediaFileService.addMediaFilesToMinIO(mp4File.getAbsolutePath(), "video/mp4", bucket, mp4_name);
                    if (!b1) {
                        log.debug("视频上传minio失败，id:{}", id);
                        //保存错误信息的数据库
                        mediaFileProcessService.saveProcessFinishStatus(id, "3", fileId, null, "上传minion失败");
                        return;
                    }

                    //保存任务处理结果
                    String url = "/" + bucket + "/" + filePath;
                    mediaFileProcessService.saveProcessFinishStatus(mediaProcess.getId(), "2", fileId, url, result);
                }finally {
                    //每执行完一个任务计数器就-1
                    countDownLatch.countDown();
                }

            });
        });

        //阻塞，直到计数器变成0才结束这个方法,如果有特殊情况，某个线程不能运行，避免堵塞，最多等30分钟
        countDownLatch.await(30, TimeUnit.MINUTES);
    }

}
