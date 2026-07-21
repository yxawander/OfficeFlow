package com.officeflow.notice.mapper;

import com.officeflow.notice.entity.NoticeScope;
import com.officeflow.notice.dto.NoticeScopeDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoticeScopeMapper {
    int insert(NoticeScope noticeScope);

    int deleteByNoticeId(Long noticeId);

    List<NoticeScope> selectByNoticeId(Long noticeId);

    List<String> selectScopeUsers(@Param("noticeId") Long noticeId);
}