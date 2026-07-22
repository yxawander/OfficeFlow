package com.officeflow.notice.mapper;

import com.officeflow.notice.entity.NoticeRead;
import com.officeflow.notice.vo.NoticeReadDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface NoticeReadMapper {
    int insert(NoticeRead noticeRead);

    int updateReadStatus(@Param("noticeId") Long noticeId, @Param("userId") Long userId, @Param("readStatus") Byte readStatus, @Param("readIp") String readIp);

    NoticeRead selectByNoticeIdAndUserId(@Param("noticeId") Long noticeId, @Param("userId") Long userId);

    Long countUnreadByUserId(Long userId);

    Long countUnreadByUserIdAndFilters(@Param("userId") Long userId, @Param("noticeType") String noticeType, @Param("priority") String priority);

    Map<String, Long> countUnreadByType(Long userId);

    Map<String, Long> countUnreadByPriority(Long userId);

    NoticeReadDetailVO selectReadDetailById(@Param("noticeId") Long noticeId, @Param("totalActiveUsers") Long totalActiveUsers);

    Long countReadUsersForNotice(Long noticeId);

    List<Long> selectReadUserIdsByNoticeId(Long noticeId);

    int batchInsert(@Param("list") List<NoticeRead> list);
}