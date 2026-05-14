package com.tianji.api.client.course;

import com.tianji.api.dto.course.CataNoteVO;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseBase;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(contextId = "catalogue", value = "course-service",path = "catalogues")
public interface CatalogueClient {

    /**
     * 根据目录id列表查询目录信息
     *
     * @param ids 目录id列表
     * @return id列表中对应的目录基础信息
     */
    @GetMapping("/batchQuery")
    List<CataSimpleInfoDTO> batchQueryCatalogue(@RequestParam("ids") Iterable<Long> ids);


    @GetMapping("/catalogues/querySectionInfoByIds/{courseIds}")
    @ApiOperation("批量获取小节信息")
    List<CataNoteVO> querySectionInfoByIds(@PathVariable("courseIds") List<Long> courseIds) ;


    @ApiOperation("根据小节id批量查询课程与小节详情")
    @GetMapping("/catalogues/batch/query/section")
    List<CourseBase> batchQuerySection(@RequestParam("ids") List<Long> ids) ;
}