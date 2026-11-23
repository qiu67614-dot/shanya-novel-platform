package com.sdjz.novel.mapper;

import com.sdjz.novel.pojo.vo.reader.FollowingBookVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FollowingMapper {

    List<FollowingBookVO> selectByUserId(@Param("userId") Long userId);

    FollowingBookVO selectOne(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId
    );

    int upsert(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId,
            @Param("chapterId") Long chapterId
    );

    int deleteByUserAndBook(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId
    );

    int markRead(
            @Param("userId") Long userId,
            @Param("bookId") Long bookId,
            @Param("chapterId") Long chapterId
    );
}
