package com.xuecheng.media.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.media.model.po.MediaProcess;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface MediaProcessMapper extends BaseMapper<MediaProcess> {

    /**
     * 根据xxl-job的分片策略查询数据库中的待处理文件信息
     * @param shardTotal
     * @param shardIndex
     * @param count
     * @return
     */
    @Select("select * from media_process t where  t.id % #{shardTotal} = #{shardIndex} and (t.status=1 or t.status=3) and t.fail_count<3 limit #{count}")
    List<MediaProcess>  selectListByShardIndex(@Param("shardTotal") int shardTotal, @Param("shardIndex") int shardIndex, @Param("count") int count);
}
