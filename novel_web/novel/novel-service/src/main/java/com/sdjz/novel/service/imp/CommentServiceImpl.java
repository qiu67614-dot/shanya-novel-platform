package com.sdjz.novel.service.imp;

import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.exception.ErrorCode;
import com.sdjz.novel.mapper.BookMapper;
import com.sdjz.novel.mapper.CommentMapper;
import com.sdjz.novel.pojo.dto.reader.CommentCreateDTO;
import com.sdjz.novel.pojo.vo.reader.BookSummaryVO;
import com.sdjz.novel.pojo.vo.reader.CommentVO;
import com.sdjz.novel.pojo.vo.reader.PageVO;
import com.sdjz.novel.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final CommentMapper commentMapper;
    private final BookMapper bookMapper;

    @Override
    public PageVO<CommentVO> getRootComments(Long bookId, Integer page, Integer size) {
        requireBook(bookId);
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        List<CommentVO> list = commentMapper.selectRootPage(bookId, offset, normalizedSize);
        return new PageVO<>(list, normalizedPage, normalizedSize, commentMapper.countRootComments(bookId));
    }

    @Override
    public PageVO<CommentVO> getReplies(Long bookId, Long commentId, Integer page, Integer size) {
        requireBook(bookId);
        if (commentMapper.countReadableThread(bookId, commentId) == 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "评论不存在");
        }
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        List<CommentVO> list = commentMapper.selectReplyPage(bookId, commentId, offset, normalizedSize);
        return new PageVO<>(list, normalizedPage, normalizedSize, commentMapper.countReplies(bookId, commentId));
    }

    @Override
    @Transactional
    public void createComment(Long userId, Long bookId, CommentCreateDTO dto) {
        requireBook(bookId);
        String content = dto.getContent() == null ? "" : dto.getContent().trim();
        if (content.isEmpty() || content.length() > 500) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "评论内容不能为空且不能超过500字");
        }
        Long parentCommentId = dto.getParentCommentId();
        if (parentCommentId != null && commentMapper.countActiveRoot(bookId, parentCommentId) == 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "回复目标不存在或已删除");
        }
        commentMapper.insert(bookId, userId, parentCommentId, content);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        if (commentMapper.softDelete(commentId, userId) == 0) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "评论不存在或无权删除");
        }
    }

    private BookSummaryVO requireBook(Long bookId) {
        BookSummaryVO book = bookMapper.selectBookById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "作品不存在或已下架");
        }
        return book;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(Integer size) {
        return size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }
}
