const firstValue = (...values) => values.find((value) => value !== undefined && value !== null)

export const normalizeUser = (source) => {
  if (!source) return null
  return {
    id: firstValue(source.id, source.uid),
    username: source.username,
    nickName: firstValue(source.nickName, source.nickname),
    userPhoto: firstValue(source.userPhoto, source.userPhotp, source.avatar),
    userSex: firstValue(source.userSex, source.gender),
    userBirthday: firstValue(source.userBirthday, source.birthday),
    userPhone: firstValue(source.userPhone, source.phone),
    userEmail: firstValue(source.userEmail, source.email),
    status: source.status,
    createTime: source.createTime,
    updateTime: source.updateTime,
  }
}

export const normalizeAuthData = (source = {}) => ({
  token: source.token,
  uid: firstValue(source.uid, source.user?.id),
  nickName: firstValue(source.nickName, source.user?.nickName, source.user?.nickname),
  user: normalizeUser(source.user || source),
})

export const normalizeCaptcha = (source = {}) => {
  const image = source.image || (source.img
    ? (source.img.startsWith('data:') ? source.img : `data:image/png;base64,${source.img}`)
    : '')
  return {
    captchaKey: firstValue(source.captchaKey, source.sessionId),
    sessionId: firstValue(source.sessionId, source.captchaKey),
    image,
    img: source.img || image,
  }
}

export const normalizeCategory = (source = {}) => ({
  id: source.id,
  categoryName: firstValue(source.categoryName, source.name),
  direction: source.direction,
  sort: source.sort,
  status: source.status,
  createTime: source.createTime,
  updateTime: source.updateTime,
})

export const normalizeBook = (source) => {
  if (!source) return null
  return {
    id: source.id,
    bookName: source.bookName,
    authorName: source.authorName,
    categoryId: source.categoryId,
    categoryName: source.categoryName,
    picUrl: source.picUrl,
    bookDesc: source.bookDesc,
    bookStatus: source.bookStatus,
    wordCount: source.wordCount,
    visitCount: source.visitCount,
    chapterCount: source.chapterCount,
    firstChapterId: source.firstChapterId,
    lastChapterId: source.lastChapterId,
    lastChapterName: source.lastChapterName,
    lastChapterUpdateTime: source.lastChapterUpdateTime,
    status: source.status,
    createTime: source.createTime,
    updateTime: source.updateTime,
  }
}

export const normalizeChapter = (source) => {
  if (!source) return null
  return {
    id: source.id,
    bookId: source.bookId,
    chapterNum: source.chapterNum,
    chapterName: source.chapterName,
    wordCount: source.wordCount,
    updateTime: source.updateTime,
    createTime: source.createTime,
  }
}

export const mapResponse = (response, transform) => ({
  ...response,
  data: transform(response.data),
})

export const normalizeBookPage = (data = {}) => ({
  ...data,
  list: (data.list || []).map(normalizeBook),
})

export const normalizeChapterPage = (data = {}) => ({
  ...data,
  list: (data.list || []).map(normalizeChapter),
})

export const normalizeChapterContent = (source = {}) => ({
  ...source,
  book: normalizeBook(source.book),
  chapter: normalizeChapter(source.chapter),
})

export const normalizeBookshelfEntry = (source = {}) => ({
  book: normalizeBook(source.book),
  lastChapterId: firstValue(source.lastChapterId, source.chapterId),
  lastChapterName: firstValue(source.lastChapterName, source.chapterName),
  lastReadAt: firstValue(source.lastReadAt, source.readAt),
  createTime: source.createTime,
  updateTime: source.updateTime,
})

export const normalizeHistoryEntry = (source = {}) => ({
  book: normalizeBook(source.book),
  chapterId: firstValue(source.chapterId, source.lastChapterId),
  chapterName: firstValue(source.chapterName, source.lastChapterName),
  readAt: firstValue(source.readAt, source.lastReadAt),
  createTime: source.createTime,
  updateTime: source.updateTime,
})

export const normalizeHistoryPage = (data = {}) => ({
  ...data,
  list: (data.list || []).map(normalizeHistoryEntry),
})

export const normalizeFollowingEntry = (source = {}) => ({
  book: normalizeBook(source.book),
  followedAt: source.followedAt,
  lastNotifiedChapterId: source.lastNotifiedChapterId,
  lastNotifiedAt: source.lastNotifiedAt,
  hasUpdate: Boolean(source.hasUpdate),
})

export const normalizeComment = (source = {}) => ({
  id: source.id,
  userId: source.userId,
  parentCommentId: source.parentCommentId,
  nickName: firstValue(source.nickName, source.nickname) || '书友',
  userPhoto: firstValue(source.userPhoto, source.userPhotp, source.avatar),
  replyToNickName: source.replyToNickName,
  content: source.content || '',
  replyCount: Number(source.replyCount || 0),
  deleted: Boolean(source.deleted),
  createTime: source.createTime,
  updateTime: source.updateTime,
})

export const normalizeCommentPage = (data = {}) => ({
  ...data,
  list: (data.list || []).map(normalizeComment),
})
