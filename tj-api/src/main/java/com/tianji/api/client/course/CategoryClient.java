package com.tianji.api.client.course;

import com.tianji.api.dto.course.CategoryBasicDTO;
import com.tianji.api.dto.course.CategoryInfoVO;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(contextId = "category",value = "course-service",path = "categorys")
public interface CategoryClient {

    /**
     * 获取所有课程及课程分类
     * @return  所有课程及课程分类
     */
    @GetMapping("/categorys/getAllOfOneLevel")
    List<CategoryBasicDTO> getAllOfOneLevel();

    @GetMapping("/categorys/{ids}")
    @ApiOperation("批量获取课程分类信息")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "ids", value = "分类id列表")
    )
    List<CategoryInfoVO> getCategoryInfoVoByIds(@PathVariable("ids") List<Long> ids);
}
