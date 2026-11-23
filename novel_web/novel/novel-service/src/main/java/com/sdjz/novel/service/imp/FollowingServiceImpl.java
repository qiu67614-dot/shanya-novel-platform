package com.sdjz.novel.service.imp;

import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.exception.ErrorCode;
import com.sdjz.novel.mapper.BookMapper;
import com.sdjz.novel.mapper.FollowingMapper;
import com.sdjz.novel.pojo.vo.reader.BookSummaryVO;
import com.sdjz.novel.pojo.vo.reader.FollowingBookVO;
import com.sdjz.novel.service.FollowingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowingServiceImpl implements FollowingService {

    private final FollowingMapper followingMapper;
    private final BookMapper bookMapper;

    @Override
    public List<FollowingBookVO> list(Long userId) {
        return followingMapper.selectByUserId(userId);
    }

    @Override
    @Transactional
    public FollowingBookVO follow(Long userId, Long bookId) {
        BookSummaryVO book = requireBook(bookId);
        followingMapper.upsert(userId, bookId, book.getLastChapterId());
        return requireFollowing(userId, bookId);
    }

    @Override
    @Transactional
    public void unfollow(Long userId, Long bookId) {
        followingMapper.deleteByUserAndBook(userId, bookId);
    }

    @Override
    @Transactional
    public FollowingBookVO markRead(Long userId, Long bookId) {
        BookSummaryVO book = requireBook(bookId);
        FollowingBookVO following = followingMapper.selectOne(userId, bookId);
        if (following == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "尚未关注该作品");
        }
        followingMapper.markRead(userId, bookId, book.getLastChapterId());
        return requireFollowing(userId, bookId);
    }

    private BookSummaryVO requireBook(Long bookId) {
        BookSummaryVO book = bookMapper.selectBookById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "作品不存在或已下架");
        }
        return book;
    }

    private FollowingBookVO requireFollowing(Long userId, Long bookId) {
        FollowingBookVO following = followingMapper.selectOne(userId, bookId);
        if (following == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "关注记录不存在");
        }
        return following;
    }
}
