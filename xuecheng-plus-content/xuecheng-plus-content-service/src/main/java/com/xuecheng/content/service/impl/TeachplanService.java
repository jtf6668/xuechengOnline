package com.xuecheng.content.service.impl;

import com.xuecheng.content.model.dto.TeachplanDto;

import java.util.List;

public interface TeachplanService {
    /**
     * 根据id查询课程计划
     * @param course
     * @return
     */
    public List<TeachplanDto> findTeachplanTree(Long course);
}
