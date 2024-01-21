package com.xuecheng.content.service.impl;

import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.model.dto.CourseCategoryTreeDto;
import com.xuecheng.content.service.CourseCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CourseCategoryServiceImpl implements CourseCategoryService {
    @Autowired
    CourseCategoryMapper courseCategoryMapper;
    @Override
    public List<CourseCategoryTreeDto> queryTreeNodes(String id) {

        //调用mapper递归查询出全部的分类信息
        List<CourseCategoryTreeDto> courseCategoryTreeDtos = courseCategoryMapper.selectTreeNodes(id);

        //找到每个节点的子节点，最终封装成List<CourseCategoryTreeDto>
        //总体逻辑是将查到的所有数据以id为key放进map中，然后一个一个遍历，找到当前节点的父节点，然后把遍历里面的节点放在父节点下面

        //先将list转成map，key就是结点的id，value就是CourseCategoryTreeDto对象，目的就是为了方便从map获取结点,filter(item->!id.equals(item.getId()))把根结点排除
        Map<String, CourseCategoryTreeDto> mapTemp = courseCategoryTreeDtos.stream().filter(item -> !id.equals(item.getId()))
                .collect(Collectors.toMap(key -> key.getId(), value -> value, (key1, key2) -> key2));//(key1, key2) -> key2)意思是如果重复则以新的为准

        //定义一个list作为最终返回的list
        List<CourseCategoryTreeDto> courseCategoryList = new ArrayList<>();

        //从头遍历 List<CourseCategoryTreeDto> ，一边遍历一边找子节点放在父节点的childrenTreeNodes
        courseCategoryTreeDtos.stream().filter(item -> !id.equals(item.getId())).forEach(item -> {
            if (item.getParentid().equals(id)) {
                //将要查询的id的下一层节点直接放入返回的list，如前端请求“1”，则放入1-1，1-2...；请求“1-1”，返回“1-1-1”，“1-1-2”
                courseCategoryList.add(item);
            }
            //找到遍历（数据库中查到的）节点的父节点
            CourseCategoryTreeDto courseCategoryParent = mapTemp.get(item.getParentid());
            if(courseCategoryParent!=null){//当前节点的父节点不为空，因为map中过滤了前端传入的节点。
                // 不为空的意思是传入1，则1-1，1-2的父节点不在map之中。（1-1，1-2已经在37行放入list
                if(courseCategoryParent.getChildrenTreeNodes()==null){
                    //初始化当前父节点：如果该父节点的ChildrenTreeNodes属性为空要new一个集合，因为要向该集合中放它的子节点
                    courseCategoryParent.setChildrenTreeNodes(new ArrayList<CourseCategoryTreeDto>());
                }
                //到每个节点的子节点放在父节点的childrenTreeNodes属性中
                courseCategoryParent.getChildrenTreeNodes().add(item);
            }



        });

        return courseCategoryList;
    }
}
