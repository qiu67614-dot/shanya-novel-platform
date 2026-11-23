package com.sdjz.novel.mapper;

import com.sdjz.novel.pojo.entity.AdminUser;
import com.sdjz.novel.pojo.entity.BookInfo;
import com.sdjz.novel.pojo.entity.NovelSubmission;
import com.sdjz.novel.pojo.vo.reader.AdminAuditLogVO;
import com.sdjz.novel.pojo.vo.reader.AdminSubmissionVO;
import com.sdjz.novel.pojo.vo.reader.PrivateNovelVO;
import com.sdjz.novel.pojo.vo.reader.WithdrawalRequestVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PublicationMapper {
    AdminUser selectAdminByUsername(@Param("username") String username);
    AdminUser selectAdminById(@Param("adminId") Long adminId);
    int insertAdmin(AdminUser admin);
    int touchAdminLogin(@Param("adminId") Long adminId);

    int countEnabledCategory(@Param("categoryId") Long categoryId);
    int insertSubmission(NovelSubmission submission);
    NovelSubmission selectSubmissionByNovel(@Param("userId") Long userId, @Param("novelId") Long novelId);
    NovelSubmission lockSubmissionByNovel(@Param("userId") Long userId, @Param("novelId") Long novelId);
    List<PrivateNovelVO> selectPublications(@Param("userId") Long userId);
    int updateSubmissionForResubmit(NovelSubmission submission);
    int cancelSubmission(@Param("userId") Long userId, @Param("novelId") Long novelId);
    int cancelSubmissionForDeletedNovel(@Param("userId") Long userId, @Param("novelId") Long novelId);

    List<AdminSubmissionVO> selectSubmissionPage(@Param("status") String status,
                                                  @Param("offset") long offset, @Param("size") int size);
    Long countSubmissions(@Param("status") String status);
    AdminSubmissionVO selectAdminSubmission(@Param("submissionId") Long submissionId);
    AdminSubmissionVO lockAdminSubmission(@Param("submissionId") Long submissionId);
    List<com.sdjz.novel.pojo.entity.UserNovelChapter> selectAdminChapters(@Param("submissionId") Long submissionId);
    com.sdjz.novel.pojo.entity.UserNovelChapter selectAdminChapter(@Param("submissionId") Long submissionId,
                                                                    @Param("chapterId") Long chapterId);
    int rejectSubmission(@Param("submissionId") Long submissionId, @Param("adminId") Long adminId,
                          @Param("reason") String reason);
    int updateSubmissionMetadata(@Param("submissionId") Long submissionId,
                                 @Param("bookName") String bookName, @Param("authorName") String authorName,
                                 @Param("categoryId") Long categoryId, @Param("bookDesc") String bookDesc);
    int insertBookInfo(BookInfo book);
    int copyPublicChapters(@Param("novelId") Long novelId, @Param("bookId") Long bookId);
    int copyPublicContents(@Param("novelId") Long novelId, @Param("bookId") Long bookId);
    int updatePublicBookSummary(@Param("bookId") Long bookId);
    int approveSubmission(@Param("submissionId") Long submissionId, @Param("adminId") Long adminId,
                          @Param("bookId") Long bookId);
    int takeDownPublication(@Param("bookId") Long bookId);
    int setSubmissionPublicStatus(@Param("bookId") Long bookId, @Param("publicStatus") String publicStatus);
    int countPendingWithdrawal(@Param("userId") Long userId, @Param("bookId") Long bookId);
    NovelSubmission lockActivePublication(@Param("userId") Long userId, @Param("bookId") Long bookId);
    Integer lockPublicBook(@Param("bookId") Long bookId);

    List<WithdrawalRequestVO> selectWithdrawalPage(@Param("status") String status,
                                                    @Param("offset") long offset, @Param("size") int size);
    Long countWithdrawals(@Param("status") String status);
    WithdrawalRequestVO selectWithdrawal(@Param("requestId") Long requestId);
    WithdrawalRequestVO lockWithdrawal(@Param("requestId") Long requestId);
    int insertWithdrawal(@Param("userId") Long userId, @Param("novelId") Long novelId,
                          @Param("bookId") Long bookId, @Param("reason") String reason);
    int approveWithdrawal(@Param("requestId") Long requestId, @Param("adminId") Long adminId);
    int rejectWithdrawal(@Param("requestId") Long requestId, @Param("adminId") Long adminId,
                         @Param("reason") String reason);
    int closePendingWithdrawals(@Param("bookId") Long bookId, @Param("adminId") Long adminId);

    int insertAudit(@Param("adminId") Long adminId, @Param("targetType") String targetType,
                    @Param("targetId") Long targetId, @Param("action") String action,
                    @Param("reason") String reason);
    List<AdminAuditLogVO> selectAuditPage(@Param("offset") long offset, @Param("size") int size);
    Long countAudit();
}
