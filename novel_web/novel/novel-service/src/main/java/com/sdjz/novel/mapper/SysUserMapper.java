package com.sdjz.novel.mapper;

import com.sdjz.novel.pojo.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Map;

@Mapper
public interface SysUserMapper {
    int insert(SysUser sysUser);
    SysUser selectByUsername(@Param("username") String username);
    SysUser selectById(@Param("id") Long id);

}

