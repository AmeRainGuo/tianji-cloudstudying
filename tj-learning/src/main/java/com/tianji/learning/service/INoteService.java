package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domain.dto.NoteFormDTO;
import com.tianji.learning.domain.po.Note;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domain.query.NotePageAdminQuery;
import com.tianji.learning.domain.query.NotePageQuery;
import com.tianji.learning.domain.vo.NoteDetailVO;
import com.tianji.learning.domain.vo.NoteFormAdminVO;
import com.tianji.learning.domain.vo.NoteFormVO;

/**
 * <p>
 * 学习笔记主表 服务类
 * </p>
 *
 * @author Amerain
 * @since 2026-05-08
 */
public interface INoteService extends IService<Note> {

    void addNote(NoteFormDTO formDTO);

    void gatherNote(Long id);

    void cancelGatherNote(Long id);

    void updateNote(Long id, NoteFormDTO formDTO);

    void deleteNoteById(Long id);

    PageDTO<NoteFormVO> listNotes(NotePageQuery pageQuery);

    PageDTO<NoteFormAdminVO> listNotesAdmin(NotePageAdminQuery pageQuery);

    NoteDetailVO getNoteDetailAdmin(Long id);

    void hiddenNote(Long id, Boolean hidden);
}
