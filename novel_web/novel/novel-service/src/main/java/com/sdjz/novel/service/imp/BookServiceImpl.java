package com.sdjz.novel.service.imp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sdjz.novel.common.cache.CacheKeys;
import com.sdjz.novel.common.exception.BusinessException;
import com.sdjz.novel.common.exception.ErrorCode;
import com.sdjz.novel.common.util.JsonRedisCache;
import com.sdjz.novel.mapper.BookCategoryMapper;
import com.sdjz.novel.mapper.BookMapper;
import com.sdjz.novel.pojo.dto.reader.BookQueryDTO;
import com.sdjz.novel.pojo.vo.reader.BookSummaryVO;
import com.sdjz.novel.pojo.vo.reader.CategoryVO;
import com.sdjz.novel.pojo.vo.reader.HomeBooksVO;
import com.sdjz.novel.pojo.vo.reader.PageVO;
import com.sdjz.novel.service.BookService;
import com.sdjz.novel.service.support.BookVisitCounter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private static final Duration HOME_TTL = Duration.ofSeconds(30);
    private static final Duration RANK_TTL = Duration.ofSeconds(30);
    private static final Duration CATEGORY_TTL = Duration.ofMinutes(10);
    private static final TypeReference<HomeBooksVO> HOME_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<BookSummaryVO>> BOOK_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<CategoryVO>> CATEGORY_LIST_TYPE = new TypeReference<>() {};

    private final BookMapper bookMapper;
    private final BookCategoryMapper bookCategoryMapper;
    private final JsonRedisCache readerCache;
    private final BookVisitCounter visitCounter;

    @Override
    public HomeBooksVO getHomeBooks() {
        return readerCache.getOrLoad(CacheKeys.HOME, HOME_TYPE, HOME_TTL, () -> {
            HomeBooksVO homeBooksVO = new HomeBooksVO();
            homeBooksVO.setLatest(bookMapper.selectHomeLatest());
            homeBooksVO.setHot(bookMapper.selectHomeHot());
            homeBooksVO.setUpdated(bookMapper.selectHomeUpdated());
            return homeBooksVO;
        });
    }

    @Override
    public List<CategoryVO> getCategories(Integer direction) {
        if (direction != null && direction != 0 && direction != 1) {
            return bookCategoryMapper.selectEnabledCategories(direction);
        }
        return readerCache.getOrLoad(CacheKeys.categories(direction), CATEGORY_LIST_TYPE, CATEGORY_TTL,
                () -> bookCategoryMapper.selectEnabledCategories(direction));
    }

    @Override
    public PageVO<BookSummaryVO> getBookPage(BookQueryDTO query) {
        // 分页参数边界修正
        int page = query.getPage() < 1 ? 1 : query.getPage();
        int size = query.getSize() < 1 ? 20 : Math.min(query.getSize(), 100);
        query.setPage(page);
        query.setSize(size);

        // 排序白名单映射，禁止直接拼接前端参数
        String orderBy = switch (query.getSort()) {
            case "visit" -> "visit_count DESC, id DESC";
            case "new" -> "create_time DESC, id DESC";
            case "update" -> "last_chapter_update_time DESC, id DESC";
            default -> "last_chapter_update_time DESC, id DESC";
        };

        int offset = (page - 1) * size;
        List<BookSummaryVO> list = bookMapper.selectBookPage(query, orderBy, offset);
        Long total = bookMapper.countBooks(query);

        return new PageVO<>(list, page, size, total);
    }

    @Override
    public List<BookSummaryVO> getRankBooks(String type) {
        // 榜单类型白名单校验
        String orderBy = switch (type) {
            case "visit" -> "visit_count DESC";
            case "update" -> "last_chapter_update_time DESC";
            case "new" -> "create_time DESC";
            default -> throw new BusinessException(ErrorCode.INVALID_PARAM, "榜单类型不合法");
        };
        return readerCache.getOrLoad(CacheKeys.ranks(type), BOOK_LIST_TYPE, RANK_TTL,
                () -> bookMapper.selectRankBooks(orderBy));
    }

    @Override
    public BookSummaryVO getBookById(Long bookId) {
        BookSummaryVO book = bookMapper.selectBookById(bookId);
        if (book == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "作品不存在");
        }
        return book;
    }

    @Override
    public void increaseVisit(Long bookId) {
        // 只写 Redis 缓冲，由 BookVisitCounter 每 10 秒合并回写数据库。
        // 因此这里不再有「作品不存在」的即时校验：这个接口是埋点式的计数上报，
        // 作品是否可读由详情接口保证，回写时 WHERE status = 0 会过滤掉已下架的作品。
        visitCounter.record(bookId);
    }
}
