package com.officeflow.flow.mapper;

import com.officeflow.flow.entity.FlowAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FlowAttachmentMapper {

    int insert(FlowAttachment attachment);

    int batchInsert(@Param("list") List<FlowAttachment> list);

    int updateFlowApplyId(@Param("ids") List<Long> ids, @Param("flowApplyId") Long flowApplyId);

    List<FlowAttachment> selectByApplyId(@Param("flowApplyId") Long flowApplyId);

    FlowAttachment selectById(@Param("id") Long id);

    int deleteById(@Param("id") Long id);

    int deleteByApplyId(@Param("flowApplyId") Long flowApplyId);
}
