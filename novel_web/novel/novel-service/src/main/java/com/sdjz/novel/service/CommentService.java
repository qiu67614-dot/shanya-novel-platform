package com.sdjz.novel.service;

import com.sdjz.novel.pojo.dto.reader.CommentCreateDTO;
import com.sdjz.novel.pojo.vo.reader.CommentVO;
import com.sdjz.novel.pojo.vo.reader.PageVO;

public interface CommentService {

    PageVO<CommentVO> getRootComments(Long bookId, Integer page, Integer size);

    PageVO<CommentVO> getReplies(Long bookId, Long commentId, Integer page, Integer size);

    void createComment(Long userId, Long bookId, CommentCreateDTO dto);

    void deleteComment(Long userId, Long commentId);
}
