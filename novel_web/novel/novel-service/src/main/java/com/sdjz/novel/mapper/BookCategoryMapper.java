package com.sdjz.novel.mapper;

import com.sdjz.novel.pojo.vo.reader.CategoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface BookCategoryMapper {
    List<CategoryVO> selectEnabledCategories(@Param("direction") Integer direction);
}
