package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessHistoryMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.model.po.MediaProcessHistory;
import com.xuecheng.media.service.MediaFileProcessService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MediaFileProcessServiceImpl implements MediaFileProcessService {
    @Autowired
    MediaProcessMapper mediaProcessMapper;

    @Autowired
    MediaFilesMapper mediaFilesMapper;

    @Autowired
    MediaProcessHistoryMapper mediaProcessHistoryMapper;
    @Override
    public List<MediaProcess> getMediaProcessList(int shardIndex, int shardTotal, int count) {
        List<MediaProcess> mediaProcesses = mediaProcessMapper.selectListByShardIndex(shardTotal, shardIndex, count);
        return mediaProcesses;
    }

    @Transactional
    @Override
    public void saveProcessFinishStatus(Long taskId, String status, String fileId, String url, String errorMsg) {
        MediaProcess mediaProcess = mediaProcessMapper.selectById(taskId);
        if(mediaProcess == null){
            return;
        }

        LambdaQueryWrapper<MediaProcess> queryWrapper = new LambdaQueryWrapper<MediaProcess>().eq(MediaProcess::getId, taskId);
        // 如果任务失败
        if ("3".equals(status)) {
            MediaProcess mediaProcess_u = new MediaProcess();
            mediaProcess_u.setStatus("3");
            mediaProcess_u.setErrormsg(errorMsg);
            mediaProcess_u.setFinishDate(LocalDateTime.now());
            mediaProcessMapper.update(mediaProcess_u, queryWrapper);
            return;
        }


        //如果任务执行成功

        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileId);
        //更新文件表的url
        mediaFiles.setUrl(url);
        mediaFilesMapper.updateById(mediaFiles);

        //更新MediaProcess的状态
        mediaProcess.setStatus("2");
        mediaProcess.setFinishDate(LocalDateTime.now());
        mediaProcess.setUrl(url);

        //将mediaProcess拷贝到历史表MediaProcess
        MediaProcessHistory mediaProcessHistory = new MediaProcessHistory();
        BeanUtils.copyProperties(mediaProcess,mediaProcessHistory);
        mediaProcessHistoryMapper.insert(mediaProcessHistory);

        //从MediaProcess中删除任务
        mediaProcessMapper.deleteById(taskId);
    }

    public boolean startTask(long id){
        int result = mediaProcessMapper.startTask(id);
        return result <= 0 ? false : true;
    }
}
