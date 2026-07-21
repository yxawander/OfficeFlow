package com.officeflow.notice.mapper;

import com.officeflow.notice.dto.NoticeQueryDTO;
import com.officeflow.notice.entity.Notice;
import com.officeflow.notice.vo.AdminNoticeListVO;
import com.officeflow.notice.vo.NoticeDetailVO;
import com.officeflow.notice.vo.NoticeListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoticeMapper {
    int insert(Notice notice);

    int updateById(Notice notice);

    int deleteById(Long id);

    Notice selectById(Long id);

    List<NoticeListVO> selectUserNoticeList(@Param("dto") NoticeQueryDTO dto, @Param("userId") Long userId, @Param("deptId") Long deptId, @Param("roles") List<String> roles);

    Integer countUserNoticeList(@Param("dto") NoticeQueryDTO dto, @Param("userId") Long userId, @Param("deptId") Long deptId, @Param("roles") List<String> roles);

    NoticeDetailVO selectDetailById(@Param("id") Long id, @Param("userId") Long userId);

    List<AdminNoticeListVO> selectAdminNoticeList(@Param("dto") NoticeQueryDTO dto);

    Integer countAdminNoticeList(@Param("dto") NoticeQueryDTO dto);

    int incrementViewCount(Long id);

    int incrementReadCount(Long id);

    int updateStatusById(@Param("id") Long id, @Param("status") String status);

    List<Notice> selectExpiredNotices();
}