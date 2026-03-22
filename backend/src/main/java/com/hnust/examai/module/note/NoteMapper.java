package com.hnust.examai.module.note;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hnust.examai.entity.Note;
import org.apache.ibatis.annotations.Mapper;

/**
 * 笔记 Mapper
 */
@Mapper
public interface NoteMapper extends BaseMapper<Note> {
}
