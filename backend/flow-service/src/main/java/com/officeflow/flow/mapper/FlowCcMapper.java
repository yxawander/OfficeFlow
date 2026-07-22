package com.officeflow.flow.mapper;

import com.officeflow.flow.entity.FlowCc;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FlowCcMapper {

    int insert(FlowCc flowCc);

    int batchInsert(@Param("list") List<FlowCc> list);

    int updateReadStatus(@Param("id") Long id, @Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    int batchUpdateReadStatus(@Param("ids") List<Long> ids, @Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    List<FlowCc> selectByFlowApplyId(@Param("flowApplyId") Long flowApplyId);
}
