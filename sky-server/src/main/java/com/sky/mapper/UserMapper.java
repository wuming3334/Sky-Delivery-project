package com.sky.mapper;

import com.sky.dto.DateGroupDTO;
import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper {

    /**
     * 根据openid查询用户
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 插入用户数据
     * @param user
     */
    void insert(User user);
    /**
     * 根据id查询用户
     * @param id
     * @return
     */
    @Select("select * from user where id = #{id}")
    User getById(Long id);

    /**
     * 根据时间范围统计用户数量（旧方法，保留兼容）
     */
    Integer countByTime(LocalDateTime begin, LocalDateTime end);

    /**
     * 批量查询新增用户统计（按日期分组）
     */
    List<DateGroupDTO> countNewUserGroupByDate(LocalDateTime begin, LocalDateTime end);
}
