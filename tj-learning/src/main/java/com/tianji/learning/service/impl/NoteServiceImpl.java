package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CategoryClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.learning.LearningClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.*;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.NoteFormDTO;
import com.tianji.learning.domain.po.Note;
import com.tianji.learning.domain.po.NoteGather;
import com.tianji.learning.domain.query.NotePageAdminQuery;
import com.tianji.learning.domain.query.NotePageQuery;
import com.tianji.learning.domain.vo.NoteDetailVO;
import com.tianji.learning.domain.vo.NoteFormAdminVO;
import com.tianji.learning.domain.vo.NoteFormVO;
import com.tianji.learning.mapper.NoteMapper;
import com.tianji.learning.service.INoteGatherService;
import com.tianji.learning.service.INoteService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * 学习笔记主表 服务实现类
 * </p>
 *
 * @author Amerain
 * @since 2026-05-08
 */
@Service
@RequiredArgsConstructor
public class NoteServiceImpl extends ServiceImpl<NoteMapper, Note> implements INoteService {


    private final INoteGatherService noteGatherService;

    private final UserClient userClient;

    private final CourseClient courseClient;

    private final CatalogueClient catalogueClient;

    private final CategoryClient categoryClient;

    private final LearningClient learningClient;

    private final RabbitMqHelper rabbitMqHelper;

    @Override
    public void addNote(NoteFormDTO formDTO) {
        Long userId = UserContext.getUser();
        LocalDateTime now = LocalDateTime.now();

        Note note = BeanUtils.copyBean(formDTO, Note.class);
        note.setAuthorId(userId);
        note.setCreateTime(now);
        note.setUpdateTime(now);
        this.save(note);

        //使用mq给用户发送加分操作
        rabbitMqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.WRITE_NOTE,
                userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void gatherNote(Long noteId) {
        Long userId = UserContext.getUser();
        LocalDateTime now = LocalDateTime.now();

        Note note = lambdaQuery()
                .eq(Note::getId, noteId)
                .eq(Note::getHidden, false)
                .one();
        // 判断笔记是否存在
        if (note == null) {
            throw new IllegalArgumentException("笔记不存在或已被隐藏");
        }
        if (note.getAuthorId().equals(userId)) {
            throw new IllegalArgumentException("不能采集自己的笔记");
        }

//        NoteGather noteGather = noteGatherService.lambdaQuery()
//                .eq(NoteGather::getNoteId, noteId)
//                .eq(NoteGather::getGatherUserId, userId)
//                .one();
//        // 判断笔记是否被采集过
//        if (noteGather != null) {
//            throw new IllegalArgumentException("请勿重复采集");
//        }
        // 采集笔记
        NoteGather newNoteGather = new NoteGather();
        newNoteGather.setNoteId(noteId);
        newNoteGather.setGatherUserId(userId);
        newNoteGather.setGatherTime(now);
//
//        noteGatherService.save(newNoteGather);
        try {
            noteGatherService.save(newNoteGather);
        } catch (DuplicateKeyException e) {
            // 捕获唯一索引冲突，说明已经采集过了
            throw new IllegalArgumentException("请勿重复采集");
        }

        // 更新笔记被采集次数
        this.lambdaUpdate()
                .setSql("used_times = used_times + 1")
                .eq(Note::getId, noteId)
                .update();

        //使用mq给被采集用户发送加分操作
        rabbitMqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE,
                MqConstants.Key.NOTE_GATHERED,
                note.getAuthorId());
    }

    @Override
    public void cancelGatherNote(Long noteId) {
        Long userId = UserContext.getUser();

        Note note = lambdaQuery()
                .eq(Note::getId, noteId)
                .eq(Note::getHidden, false)
                .one();
        // 判断笔记是否存在
        if (note == null) {
            throw new IllegalArgumentException("笔记不存在或已被隐藏");
        }

        // 取消采集笔记
        boolean success = noteGatherService.lambdaUpdate()
                .eq(NoteGather::getNoteId, noteId)
                .eq(NoteGather::getGatherUserId, userId)
                .remove();

        // 3. 判断删除结果：如果删除失败，说明用户根本没采集过这条笔记
        if (!success) {
            throw new IllegalArgumentException("您未采集过该笔记，无法取消");
        }

        // 更新笔记被采集次数
        this.lambdaUpdate()
                .setSql("used_times = used_times - 1")
                .eq(Note::getId, noteId)
                .ge(Note::getUsedTimes, 1)
                .update();

    }

    @Override
    public void updateNote(Long id, NoteFormDTO formDTO) {
        //获取用户ID
        Long userId = UserContext.getUser();

        // 查询笔记
        boolean success = lambdaUpdate()
                .eq(Note::getId, id)
                .eq(Note::getHidden, false)
                .eq(Note::getAuthorId, userId)// 关键：只能修改自己的笔记 防止恶意改别人的笔记
                // 设置修改的字段 ，前端传什么就更什么 只更新非空的字段
                .set(formDTO.getContent() != null, Note::getContent, formDTO.getContent())
                .set(formDTO.getIsPrivate() != null, Note::getIsPrivate, formDTO.getIsPrivate())
                // 执行更新
                .update();

        // 判断更新结果
        if (!success) {
            throw new IllegalArgumentException("更新笔记更新失败");
        }
//        // 判断笔记是否存在
//        if (note == null) {
//            throw new IllegalArgumentException("笔记不存在或已被隐藏");
//        }
//        // 更新笔记内容
//        note.setContent(formDTO.getContent());
//        note.setIsPrivate(formDTO.getIsPrivate());
//        this.updateById(note);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNoteById(Long id) {
        Long userId = UserContext.getUser();

        // 删除条件必须加上「作者是当前登录用户」，只能删自己的笔记
        boolean removeSuccess = this.lambdaUpdate()
                .eq(Note::getId, id)
                .eq(Note::getAuthorId, userId) // 越权防护核心
                .eq(Note::getHidden, false) // 只允许删除未被管理员隐藏的正常笔记
                .remove();

        // 删除失败，说明无权限/笔记不存在
        if (!removeSuccess) {
            throw new IllegalArgumentException("笔记不存在或您无权限删除该笔记");
        }

        // 同步删除采集关联表中这条笔记的所有采集记录，避免脏数据
        noteGatherService.lambdaUpdate()
                .eq(NoteGather::getNoteId, id)
                .remove();
        //不必要的代码内容 校验逻辑不合规 可能存在未被采集过的笔记返回false
//        if (!removeGatherSuccess) {
//            throw new IllegalArgumentException("同步采集记录删除失败，请稍后重试或联系管理员");
//        }
    }


    @Override
    public PageDTO<NoteFormVO> listNotes(NotePageQuery pageQuery) {
        Long userId = UserContext.getUser();

        //查询用户购买的课程的情况
        Long lessonValid = learningClient.isLessonValid(pageQuery.getCourseId());
        if (lessonValid  == null) {
            throw new IllegalArgumentException("用户未购买课程，请先购买课程");
        }

        //创建分页对象
        Page<Note> notePageList ;

        //无论哪种查询 都需要获取当前用户采集的笔记ID

        //查询自己采集的笔记
        List<NoteGather> noteGatherList = noteGatherService.lambdaQuery()
                .eq(NoteGather::getGatherUserId, userId)
                .list();

        //获取所有采集的笔记ID
        //这里其实还有可优化的地方 这里查询的是全量的笔记ID采集记录 实际上当采集笔记记录极大时会影响数据库查询
        //其实只需要获取当前页的即可 但是这里为了简单起见以及代码的可读性 先不做优化 后续可以添加对用户采集笔记数量的限制来防止大量数据查询
        Set<Long> gatherNoteIds = noteGatherList.stream()
                .map(NoteGather::getNoteId)
                .collect(Collectors.toSet());

        // 判断分页查询类型
        if (pageQuery.getOnlyMine()) {
            // 只查询我的笔记

            //查询自己编写的笔记以及采集的笔记
            notePageList = lambdaQuery()
                    //通用查询条件
                    .eq(Note::getHidden, false)
                    .eq(pageQuery.getCourseId() != null, Note::getCourseId, pageQuery.getCourseId())
                    .eq(pageQuery.getSectionId() != null, Note::getSectionId, pageQuery.getSectionId())
                    .and(w -> w
                            // 自己的笔记查询条件
                            .eq(Note::getAuthorId, userId)
                            .or(!gatherNoteIds.isEmpty(), wr -> wr
                                    // 采集笔记查询条件
                                    .eq(Note::getIsPrivate, false)
                                    .in(Note::getId, gatherNoteIds))//使用or()方法，将两个条件进行逻辑或运算 二者满足任意一个即可查询到
                    )
                    .page(pageQuery.toMpPageDefaultSortByCreateTimeDesc());

        }else{
            // 查询全部笔记
            notePageList = lambdaQuery()
                    .eq(Note::getHidden, false)
                    .eq( Note::getIsPrivate, false)
                    .eq(pageQuery.getCourseId() != null, Note::getCourseId, pageQuery.getCourseId())
                    .eq(pageQuery.getSectionId() != null, Note::getSectionId, pageQuery.getSectionId())
                    .page(pageQuery.toMpPageDefaultSortByCreateTimeDesc());
        }
        //获取所有笔记的信息records
        List<Note> records = notePageList.getRecords();

        //获取所有笔记的作者 ID
        Set<Long> authorIds = records.stream()
                .map(Note::getAuthorId)
                .collect(Collectors.toSet());

        //远程调用获取作者信息
        List<UserDTO> userList ;
        try {
            userList = userClient.queryUserByIds(authorIds);
        } catch (Exception e) {
            log.error("远程调用用户服务失败", e);
            // 降级处理：返回空集合，后续循环不会报错，作者信息会留空
            userList = Collections.emptyList();
        }
        //封装笔记信息为Map 键为作者ID，值为作者信息
        Map<Long, UserDTO> userMap = userList.stream().collect(Collectors.toMap(UserDTO::getId, user -> user));

        //遍历所有笔记，填充作者信息
        List<NoteFormVO> collectList = records.stream().map(record -> {

            //把笔记信息拷贝为VO对象
            NoteFormVO noteFormVO = BeanUtils.copyBean(record, NoteFormVO.class);

            //根据笔记作者ID获取作者信息
            UserDTO userDTO = userMap.get(record.getAuthorId());
            if (userDTO != null) {
                //填充作者信息 如果作者信息存在
                noteFormVO.setAuthorName(userDTO.getUsername());
                noteFormVO.setAuthorIcon(userDTO.getIcon());
            }else {
                //填充作者信息 如果作者信息不存在
                noteFormVO.setAuthorName("用户已注销");
                noteFormVO.setAuthorIcon("null");
            }
            //判断笔记是否被当前用户采集
            noteFormVO.setIsGathered( gatherNoteIds.contains(record.getId()) );

            return noteFormVO;
        }).collect(Collectors.toList());

        return PageDTO.of(notePageList, collectList);
    }

    @Override
    public PageDTO<NoteFormAdminVO> listNotesAdmin(NotePageAdminQuery pageQuery) {


        //管理端 查询所有笔记
        Page<Note> notePageList = lambdaQuery()
                .eq(pageQuery.getHidden() != null, Note::getHidden, pageQuery.getHidden())
                .ge(pageQuery.getBeginTime() != null, Note::getCreateTime,pageQuery.getBeginTime())
                .le(pageQuery.getEndTime() != null, Note::getCreateTime,pageQuery.getEndTime())
                .page(pageQuery.toMpPageDefaultSortByCreateTimeDesc());

        //获取所有笔记的信息records
        List<Note> records = notePageList.getRecords();
        //判断笔记列表是否为空 若为空 则返回空集合节省资源
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(notePageList);
        }

        //不为空 提取所以笔记的课程 利用set去重
        Set<Long> courseIds = records.stream().map(Note::getCourseId).collect(Collectors.toSet());

        //获取所有笔记的课程目录ID 即章id或节id
        Set<Long> cataIds = new HashSet<>();
        for (Note note : records) {
            if (note.getChapterId() != null) {
                cataIds.add(note.getChapterId());
            }
            if (note.getSectionId() != null) {
                cataIds.add(note.getSectionId());
            }
        }

        //根据课程ID查询课程信息
        List<CourseNoteVO> courseInfoList = courseClient.batchQueryCourse(new ArrayList<>(courseIds));
        Map<Long, CourseNoteVO> courseInfoMap = courseInfoList.stream().collect(Collectors.toMap(CourseNoteVO::getCourseId, course -> course));

        //根据课程ID查询课程目录目录信息 ID为目录id 目录分为章和节 主键唯一 可以直接返回目录名称
        List<CataSimpleInfoDTO> cataInfos = catalogueClient.batchQueryCatalogue(new ArrayList<>(cataIds));
        Map<Long, String> cataMap = cataInfos.stream()
                .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName, (v1, v2) -> v1));

        //判断前端是否提供了筛选课程名称 若提供了 则根据课程名称筛选
        if (StringUtils.isNotBlank(pageQuery.getName())) {
            records = records.stream()
                    .filter(vo -> {
                        CourseNoteVO course = courseInfoMap.get(vo.getCourseId());
                        // 这里就是 模糊包含
                        return course != null && course.getCourseName().contains(pageQuery.getName());
                    })
                    .collect(Collectors.toList());
        }

        List<NoteFormAdminVO> collectList = records.stream().map(record -> {
            //把笔记信息拷贝为VO对象
            NoteFormAdminVO noteFormAdminVO = BeanUtils.copyBean(record, NoteFormAdminVO.class);
            //填充课程信息
            CourseNoteVO course = courseInfoMap.get(record.getCourseId());
            noteFormAdminVO.setCourseName(course != null ? course.getCourseName() : "课程名称异常！");

            //填充章和节信息
            noteFormAdminVO.setChapterName(cataMap.getOrDefault(record.getChapterId(), "章名称异常！"));
            noteFormAdminVO.setSectionName(cataMap.getOrDefault(record.getSectionId(), "节名称异常！"));
            return noteFormAdminVO;
        }).collect(Collectors.toList());


        return PageDTO.of(notePageList, collectList);
    }

    @Override
    public NoteDetailVO getNoteDetailAdmin(Long id) {

        NoteDetailVO noteDetailVO = new NoteDetailVO();

        //根据笔记ID查询笔记信息
        Note note = lambdaQuery().eq(Note::getId, id).one();
        if (note == null) {
            throw new IllegalArgumentException("笔记不存在");
        }

        //根据课程ID查询课程信息
        CourseSearchDTO courseInfo = courseClient.getSearchInfo(note.getCourseId());

        //获取课程分类id列表
        List<Long> categoryIds = new ArrayList<>( 3) {{
            add(courseInfo.getCategoryIdLv1());
            add(courseInfo.getCategoryIdLv2());
            add(courseInfo.getCategoryIdLv3());
        }};

        //查询课程分类列表
        List<CategoryInfoVO> list = categoryClient.getCategoryInfoVoByIds(categoryIds);
        Map<Integer, String> categoryMap = list.stream().collect(Collectors.toMap(CategoryInfoVO::getCategoryLevel,CategoryInfoVO::getName));
        // 拿到 1级、2级、3级 的名称
        String first = categoryMap.get(1);
        String second = categoryMap.get(2);
        String third = categoryMap.get(3);

        // 拼接 防止3级分类为空 导致拼接异常
        String categoryNames = Stream.of(first, second, third)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("/"));

        //获取处理笔记的课程目录ID 即章id或节id
        List<Long> cataIds = new ArrayList<>();
        if (note.getChapterId() != null) {
            cataIds.add(note.getChapterId());
        }
        if (note.getSectionId() != null) {
            cataIds.add(note.getSectionId());
        }
        //根据课程ID查询课程目录目录信息 ID为目录id 目录分为章和节 主键唯一 可以直接返回目录名称
        List<CataSimpleInfoDTO> cataInfos = catalogueClient.batchQueryCatalogue(cataIds);
        Map<Long, String> cataMap = cataInfos.stream()
                .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));

        //获取笔记作者信息
        Long authorId = note.getAuthorId();
        UserDTO user = userClient.queryUserById(authorId);

        //获取笔记采集人信息
        List<NoteGather> gatherList = noteGatherService.lambdaQuery()
                .eq(NoteGather::getNoteId, id)
                .list();
        // 转换为用户id列表
        List<Long> gatherUserIdList = gatherList.stream()
                .map(NoteGather::getGatherUserId)
                .collect(Collectors.toList());
        //根据用户id列表查询用户信息
        List<UserDTO> gatherUserList = userClient.queryUserByIds(gatherUserIdList);
        //转换为用户名称列表
        List<String> gatherUserNamesList = gatherUserList.stream()
                .map(UserDTO::getName)
                .collect(Collectors.toList());

        //把笔记信息拷贝为VO对象
        BeanUtils.copyProperties(note, noteDetailVO);

        noteDetailVO.setCourseName(courseInfo.getName());
        noteDetailVO.setCategoryNames(categoryNames);

        noteDetailVO.setChapterName(cataMap.getOrDefault(note.getChapterId(), "章名称异常！"));
        noteDetailVO.setSectionName(cataMap.getOrDefault(note.getSectionId(), "节名称异常！"));

        if (user != null) {
            noteDetailVO.setAuthorName(user.getName());
            noteDetailVO.setAuthorPhone(user.getCellPhone());
        } else {
            noteDetailVO.setAuthorName("未知作者");
            noteDetailVO.setAuthorPhone("");
        }

        noteDetailVO.setGathers(gatherUserNamesList);

        return noteDetailVO;
    }

    @Override
    public void hiddenNote(Long id, Boolean hidden) {
        // 先判断笔记是否存在
        if (!lambdaQuery().eq(Note::getId, id).exists()) {
            throw new IllegalArgumentException("笔记不存在");
        }

        lambdaUpdate()
                .eq(Note::getId, id)
                .set(Note::getHidden, hidden)
                .update();
    }
}
