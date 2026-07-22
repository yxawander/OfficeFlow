package com.officeflow.flow.mapper;

import com.officeflow.flow.entity.FlowApproveRecord;
import com.officeflow.flow.vo.FlowApproveRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FlowApproveRecordMapper {

    int insert(FlowApproveRecord record);

    List<FlowApproveRecordVO> selectByApplyId(@Param("flowApplyId") Long flowApplyId);
}
