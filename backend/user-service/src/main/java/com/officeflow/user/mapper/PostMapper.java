package com.officeflow.user.mapper;

import com.officeflow.user.dto.PostRequest;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface PostMapper {
    @Select("""
            SELECT id, post_name AS postName, post_code AS postCode, sort_order AS sortOrder, status
            FROM sys_post
            WHERE is_deleted = 0
            ORDER BY sort_order ASC, id ASC
            """)
    List<Map<String, Object>> listAll();

    @Insert("""
            INSERT INTO sys_post (post_name, post_code, sort_order, status)
            VALUES (#{req.postName}, #{req.postCode}, #{sortOrder}, #{status})
            """)
    int insert(@Param("req") PostRequest request, @Param("sortOrder") Integer sortOrder, @Param("status") Integer status);

    @Update("""
            UPDATE sys_post
            SET post_name = #{req.postName}, post_code = #{req.postCode}, sort_order = #{sortOrder}, status = #{status}
            WHERE id = #{id} AND is_deleted = 0
            """)
    int update(@Param("id") Long id, @Param("req") PostRequest request, @Param("sortOrder") Integer sortOrder, @Param("status") Integer status);

    @Update("UPDATE sys_post SET is_deleted = 1 WHERE id = #{id}")
    int softDelete(@Param("id") Long id);
}
