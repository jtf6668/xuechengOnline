package com.xuecheng.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.TeachplanMapper;
import com.xuecheng.content.mapper.TeachplanMediaMapper;
import com.xuecheng.content.model.dto.BindTeachplanMediaDto;
import com.xuecheng.content.model.dto.SaveTeachplanDto;
import com.xuecheng.content.model.dto.TeachplanDto;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.model.po.TeachplanMedia;
import com.xuecheng.content.service.TeachplanService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeachplanServiceImpl implements TeachplanService {
    @Autowired
    TeachplanMapper teachplanMapper;
    @Autowired
    TeachplanMediaMapper teachplanMediaMapper;


    @Override
    public List<TeachplanDto> findTeachplanTree(Long courseId) {
        List<TeachplanDto> teachplanDtos = teachplanMapper.selectTreeNodes(courseId);
        return teachplanDtos;
    }

    @Override
    public void saveTeachplan(SaveTeachplanDto saveTeachplanDto) {
        //根据有没有课程计划id判断是新增还是修改
        Long teachplanDtoId = saveTeachplanDto.getId();
        if(teachplanDtoId == null){
            //新增
            Teachplan teachplan = new Teachplan();
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);
            //新增操作有一个排序字段要填入，而修改操作不需要，因为修改操作已经确定了它的排序位置
            //排序字段即新增的数据是第几个，例如数据库中有3章课程，新增的为第4章；又如有2节课程，新增的为第3个。即为同级节点个数+1
            int count = getTeachplanCount(saveTeachplanDto.getCourseId(), saveTeachplanDto.getParentid());
            //设置排序号
            teachplan.setOrderby(count+1);
            teachplanMapper.insert(teachplan);
        }else {
            //修改
            //先查出新增的课程计划在数据库中是哪个
            Teachplan teachplan = teachplanMapper.selectById(teachplanDtoId);
            //将参数赋值,先查出再赋值可以防止赋值后导致某些字段为空
            BeanUtils.copyProperties(saveTeachplanDto,teachplan);
            //更新数据库
            teachplanMapper.updateById(teachplan);
        }
    }


    @Override
    public void deleteTeachplan(Long id) {

        //1、删除大章节，大章节下有小章节时不允许删除。
        //查询数据库中该id是否为其他章节的父节点，如果是，则该id下面有小章节
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper = queryWrapper.eq(Teachplan::getParentid, id);
        Integer count = teachplanMapper.selectCount(queryWrapper);
        if(count != 0){
            XueChengPlusException.cast("课程计划信息还有子级信息，无法操作");
        }

        //2、删除大章节，大单节下没有小章节时可以正常删除。
        teachplanMapper.deleteById(id);

        //3、删除小章节，同时将关联的信息进行删除。
        //删除课程计划媒资表信息
        LambdaQueryWrapper<TeachplanMedia> Wrapper = new LambdaQueryWrapper<>();
        Wrapper = Wrapper.eq(TeachplanMedia::getTeachplanId, id);
        teachplanMediaMapper.delete(Wrapper);
    }

    @Override
    public void associationMedia(BindTeachplanMediaDto bindTeachplanMediaDto) {
        //通过课程计划id拿到课程计划
        Long teachplanId = bindTeachplanMediaDto.getTeachplanId();
        Teachplan teachplan = teachplanMapper.selectById(teachplanId);
        if(teachplan == null){
            XueChengPlusException.cast("课程计划不存在");
        }

        //先删除原有记录，根据课程计划id删除原来绑定的媒资
        teachplanMediaMapper.delete(new LambdaQueryWrapper<TeachplanMedia>().eq(TeachplanMedia::getTeachplanId,bindTeachplanMediaDto.getTeachplanId()));

        //添加新记录
        TeachplanMedia teachplanMedia = new TeachplanMedia();
        BeanUtils.copyProperties(bindTeachplanMediaDto,teachplanMedia);
        teachplanMedia.setCourseId(teachplan.getCourseId());
        teachplanMedia.setMediaFilename(bindTeachplanMediaDto.getFileName());
        teachplanMediaMapper.insert(teachplanMedia);
    }


    /**
     * 当前节点的排序，例如数据库中有3章课程，新增的为第4章；又如有2节课程，新增的为第3个。即为同级节点个数+1
     * @param courseId
     * @param parentId
     * @return
     */
    private int getTeachplanCount(Long courseId,Long parentId){
        LambdaQueryWrapper<Teachplan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper = queryWrapper.eq(Teachplan::getCourseId, courseId).eq(Teachplan::getParentid, parentId);//意思是把传入courseId当作mysql的courseId，传入的parentId当作mysql的parentId作条件
        Integer count = teachplanMapper.selectCount(queryWrapper);
        return  count+1;
    }
}
