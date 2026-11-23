package com.sdjz.novel.mapper;

import com.sdjz.novel.pojo.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MeMapper {
    void updateById(SysUser dbUser);
}
