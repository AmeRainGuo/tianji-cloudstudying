package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.NoteFormDTO;
import com.tianji.learning.domain.query.NotePageQuery;
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
@RequestMapping("/notes")
@Api(tags = "学习笔记的相关接口")
@RequiredArgsConstructor
public class NoteController {

    private final INoteService noteService;

    @ApiOperation("新增我的笔记")
    @PostMapping("/add")
    public void addNote(NoteFormDTO formDTO){
        noteService.addNote(formDTO);
    }

    @ApiOperation("采集笔记")
    @PostMapping("/gathers/{id}")
    public void gatherNote(@PathVariable Long id){
        noteService.gatherNote(id);
    }

    @ApiOperation("取消采集笔记")
    @DeleteMapping("/gathers/{id}")
    public void cancelGatherNote(@PathVariable Long id){
        noteService.cancelGatherNote(id);
    }

    @ApiOperation("编辑我的笔记")
    @PutMapping("/{id}")
    public void editNote(@PathVariable Long id, @RequestBody NoteFormDTO formDTO){
        noteService.updateNote(id, formDTO);
    }

    @ApiOperation("删除我的笔记")
    @DeleteMapping("/{id}")
    public void deleteNote(@PathVariable Long id){
        noteService.deleteNoteById(id);
    }

    @ApiOperation("用户端分页查询笔记")
    @GetMapping("/page")
    public PageDTO<NoteFormVO> listNotes(@RequestParam NotePageQuery pageQuery){
        return noteService.listNotes(pageQuery);
    }

    @ApiOperation("管理员端显示或隐藏笔记")
    @PutMapping("/{id}/hidden/{hidden}")
    public void hiddenNote(@PathVariable Long id, @PathVariable Boolean hidden){
        noteService.hiddenNote(id, hidden);
    }
}
