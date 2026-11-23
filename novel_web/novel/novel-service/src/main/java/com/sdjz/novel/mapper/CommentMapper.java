package com.sdjz.novel.mapper;

import com.sdjz.novel.pojo.vo.reader.CommentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentMapper {

    List<CommentVO> selectRootPage(
            @Param("bookId") Long bookId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    Long countRootComments(@Param("bookId") Long bookId);

    List<CommentVO> selectReplyPage(
            @Param("bookId") Long bookId,
            @Param("commentId") Long commentId,
            @Param("offset") int offset,
            @Param("size") int size
    );

    Long countReplies(
            @Param("bookId") Long bookId,
            @Param("commentId") Long commentId
    );

    int countReadableThread(
            @Param("bookId") Long bookId,
            @Param("commentId") Long commentId
    );

    int countActiveRoot(
            @Param("bookId") Long bookId,
            @Param("commentId") Long commentId
    );

    int insert(
            @Param("bookId") Long bookId,
            @Param("userId") Long userId,
            @Param("parentCommentId") Long parentCommentId,
            @Param("content") String content
    );

    int softDelete(
            @Param("commentId") Long commentId,
            @Param("userId") Long userId
    );
}
