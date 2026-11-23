package com.sdjz.novel.service;

import com.sdjz.novel.pojo.vo.reader.FollowingBookVO;

import java.util.List;

public interface FollowingService {

    List<FollowingBookVO> list(Long userId);

    FollowingBookVO follow(Long userId, Long bookId);

    void unfollow(Long userId, Long bookId);

    FollowingBookVO markRead(Long userId, Long bookId);
}
