package com.officeflow.notice.mapper;

import com.officeflow.notice.entity.NoticeAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoticeAttachmentMapper {

    int insert(NoticeAttachment attachment);

    int batchInsert(@Param("list") List<NoticeAttachment> list);

    int updateNoticeId(@Param("ids") List<Long> ids, @Param("noticeId") Long noticeId);

    List<NoticeAttachment> selectByNoticeId(@Param("noticeId") Long noticeId);

    NoticeAttachment selectById(@Param("id") Long id);

    int deleteById(@Param("id") Long id);

    int deleteByNoticeId(@Param("noticeId") Long noticeId);

    int unbindByNoticeId(@Param("noticeId") Long noticeId);
}
