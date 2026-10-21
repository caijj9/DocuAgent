package com.docuagent.docuagent.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.docuagent.docuagent.common.CodeEnum;
import com.docuagent.docuagent.common.Result;
import com.docuagent.docuagent.entity.Course;
import com.docuagent.docuagent.exception.BizException;
import com.docuagent.docuagent.mapper.CourseMapper;
import com.docuagent.docuagent.vo.CourseVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * 课程管理。
 * <p>
 * 上传课件时必须带 courseId，如果连"建课程"的入口都没有，链路就是断的
 * （之前只能手动插 SQL，这是个真实的缺口）。
 */
@RestController
@RequestMapping("/course")
public class CourseController {

    @Autowired
    private CourseMapper courseMapper;

    /**
     * 建课程。同名课程不允许重复建 —— 否则会造出「编译原理」和「编译原理 」这种废数据。
     *
     * @return 新课程的 id（前端拿到后就能用它传课件了）
     */
    @PostMapping
    public Result<Long> create(@RequestParam String name,
                               @RequestParam(required = false) String description) {

        Long dup = courseMapper.selectCount(
                new LambdaQueryWrapper<Course>().eq(Course::getName, name));
        if (dup != null && dup > 0) {
            throw new BizException(CodeEnum.COURSE_ALREADY_EXISTS);
        }

        Course course = new Course();
        course.setName(name);
        course.setDescription(description);
        courseMapper.insert(course);      // id 会被回填

        return Result.success(course.getId());
    }

    /**
     * 列出所有课程（前端下拉框要用：用户传课件时从这里选课程）
     */
    @GetMapping
    public Result<List<CourseVO>> list() {
        List<Course> list = courseMapper.selectList(null);
        List<CourseVO> vos = new ArrayList<>();
        for (Course c : list) {
            vos.add(new CourseVO(c.getId(), c.getName(), c.getDescription()));
        }
        return Result.success(vos);
    }
}
