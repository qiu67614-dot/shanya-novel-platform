-- ============================================================================
-- 搜索优化：LIKE '%kw%' → FULLTEXT ngram
-- ----------------------------------------------------------------------------
-- 什么时候需要跑这份脚本？
--   book_info 数据量到 10 万行以上，且搜索接口的 p99 明显高于其他列表接口时。
--   千级数据量下 LIKE 全表扫描是毫秒级，改全文索引属于过早优化。
--
-- 为什么是 ngram 而不是默认分词器？
--   MySQL 默认分词器按空格切词，中文整句会被当成一个词，「斗罗」搜不到「斗罗大陆」。
--   ngram 按固定长度（默认 2）切，中文才能按字匹配。InnoDB 从 5.7 起内置支持。
--
-- 执行前确认：
--   1. MySQL >= 5.7（SELECT VERSION();）
--   2. 业务低峰执行，建索引会锁表；数据量大时用 ALGORITHM=INPLACE, LOCK=NONE
--   3. 只建索引，不改数据，可安全回滚（DROP INDEX）
-- ============================================================================

ALTER TABLE book_info
    ADD FULLTEXT INDEX ft_book_info_name_author (book_name, author_name) WITH PARSER ngram;

-- 验证：应能命中书名中间的关键词
-- SELECT id, book_name FROM book_info
--  WHERE MATCH(book_name, author_name) AGAINST ('大陆' IN BOOLEAN MODE) LIMIT 10;

-- ----------------------------------------------------------------------------
-- 配套的 SQL 改法（改 mapper 前先确认上面的查询结果符合预期）
-- ----------------------------------------------------------------------------
-- selectBookPage / countBooks 里的：
--   AND (b.book_name LIKE CONCAT('%', #{query.q}, '%')
--        OR b.author_name LIKE CONCAT('%', #{query.q}, '%'))
-- 换成：
--   AND MATCH(b.book_name, b.author_name) AGAINST (#{query.q} IN BOOLEAN MODE)
--
-- 三个注意点：
--   1. MATCH 的列必须和索引定义的列完全一致，否则用不上索引；
--   2. ngram 下小于 ngram_token_size（默认 2）的关键词搜不到，需要在接口层提示「至少 2 个字」；
--   3. BOOLEAN MODE 不做相关性排序，若需要按相关度排序得用
--      ORDER BY MATCH(...) AGAINST(...) DESC，会和现有的 visit/update/new 排序冲突，
--      需要产品层面决定「搜索默认按相关度还是按热度」。
--
-- 回滚：
-- ALTER TABLE book_info DROP INDEX ft_book_info_name_author;
