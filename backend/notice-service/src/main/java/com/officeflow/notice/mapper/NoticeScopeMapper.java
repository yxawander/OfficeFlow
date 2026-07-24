package com.officeflow.notice.mapper;

import com.officeflow.notice.entity.NoticeScope;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NoticeScopeMapper {
    int insert(NoticeScope noticeScope);

    int deleteByNoticeId(Long noticeId);

    List<NoticeScope> selectByNoticeId(Long noticeId);
}