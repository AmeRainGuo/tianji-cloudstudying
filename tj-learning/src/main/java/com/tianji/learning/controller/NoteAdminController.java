package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.query.NotePageAdminQuery;
import com.tianji.learning.domain.vo.NoteDetailVO;
import com.tianji.learning.domain.vo.NoteFormAdminVO;
import com.tianji.learning.domain.vo.NoteFormVO;
import com.tianji.learning.service.INoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 学习笔记主表 前端控制器
 * </p>
 *
 * @author Amerain
 * @since 2026-05-08
 */
@RestController
@RequestMapping("/admin/notes")
@Api(tags = "学习笔记的管理员端接口")
@RequiredArgsConstructor
public class NoteAdminController {

    private final INoteService noteService;

    @GetMapping("/page")
    @ApiOperation("分页查询学习笔记")
    public PageDTO<NoteFormAdminVO> listNotes(@RequestParam NotePageAdminQuery pageQuery){
        return noteService.listNotesAdmin(pageQuery);
    }

    @GetMapping("/{id}")
    @ApiOperation("管理端查询学习笔记详情")
    public NoteDetailVO getNoteDetail(@PathVariable Long id){
        return noteService.getNoteDetailAdmin(id);
    }
}
