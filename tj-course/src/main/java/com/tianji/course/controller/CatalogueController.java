package com.tianji.course.controller;

import com.tianji.api.dto.course.CatalogueDTO;
import com.tianji.course.domain.po.CourseBase;
import com.tianji.course.domain.vo.CataNoteVO;
import com.tianji.course.domain.vo.CataSimpleInfoVO;
import com.tianji.course.service.ICourseCatalogueService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 目录课程相关接口
 *
 * @ClassName CatalogueController
 * @Author wusongsong
 * @Date 2022/7/27 13:59
 * @Version
 **/
@Api(tags = "章节目录相关接口")
@RestController
@RequestMapping("catalogues")
public class CatalogueController {

    @Autowired
    private ICourseCatalogueService courseCatalogueService;

    @GetMapping("/batchQuery")
    @ApiOperation("根据章节目录批量查询基础信息")
    public List<CataSimpleInfoVO> batchQuery(@RequestParam("ids") List<Long> ids) {
        return courseCatalogueService.getManyCataSimpleInfo(ids);
    }

    @GetMapping("querySectionInfoById/{id}")
    @ApiOperation("获取小节信息")
    public CataSimpleInfoVO querySectionInfoById(@PathVariable("id") Long id) {
        return courseCatalogueService.querySectionInfoById(id);
    }

    @GetMapping("querySectionInfoByIds/{courseIds}")
    @ApiOperation("批量获取小节信息")
    public List<CataNoteVO> querySectionInfoByIds(@PathVariable("courseIds") List<Long> courseIds) {
        return courseCatalogueService.listCourseCataloguesNoteVO(courseIds);
    }

    @ApiOperation("根据小节id批量查询课程与小节详情")
    @GetMapping("/batch/query/section")
    public List<CourseBase> batchQuerySection(@RequestParam("ids") List<Long> ids) {
        return courseCatalogueService.batchQuerySectionInfoByIds(ids);
    }
}
