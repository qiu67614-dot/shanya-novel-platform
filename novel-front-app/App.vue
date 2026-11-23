<script>
export default {}
</script>

<style>
page {
  --paper: #f5f6f2;
  --surface: #ffffff;
  --ink: #232d29;
  --muted: #66736e;
  --line: #dce3dd;
  --vermillion: #ae4938;
  --forest: #37614e;
  background: var(--paper);
  color: var(--ink);
  font-family: -apple-system, BlinkMacSystemFont, "Microsoft YaHei", sans-serif;
  font-size: 28rpx;
}
view, text, input, textarea, button { box-sizing: border-box; }
button { border-radius: 4rpx; line-height: 1.3; }
button::after { border: 0; }
button:active, .book-row:active, .chip:active { opacity: .78; transform: translateY(1px); }
.screen { width: 100%; max-width: 920px; min-height: 100vh; margin: 0 auto; padding: 44rpx 42rpx calc(156rpx + env(safe-area-inset-bottom)); }
.screen.reading { max-width: 800px; padding-bottom: calc(170rpx + env(safe-area-inset-bottom)); }
.eyebrow { display: block; color: var(--vermillion); font-size: 21rpx; font-weight: 700; line-height: 1.5; letter-spacing: 2rpx; }
.page-title { display: block; margin: 10rpx 0 34rpx; color: var(--ink); font-family: "Noto Serif CJK SC", "Source Han Serif SC", "Songti SC", "SimSun", serif; font-size: 51rpx; font-weight: 700; line-height: 1.34; word-break: break-word; }
.section-title { display: block; color: var(--ink); font-family: "Noto Serif CJK SC", "Source Han Serif SC", "Songti SC", "SimSun", serif; font-size: 33rpx; font-weight: 700; line-height: 1.35; }
.muted { color: var(--muted); font-size: 24rpx; line-height: 1.5; }
.error-text { color: var(--vermillion); font-size: 24rpx; line-height: 1.5; }
.state { min-height: 180rpx; padding: 65rpx 22rpx; color: var(--muted); text-align: center; font-size: 26rpx; line-height: 1.6; }
.state-error { color: var(--vermillion); }
.primary-btn, .secondary-btn, .quiet-btn { display: inline-flex; align-items: center; justify-content: center; min-height: 88rpx; padding: 0 28rpx; font-size: 27rpx; font-weight: 650; }
.primary-btn { color: #fff; background: var(--vermillion); }
.secondary-btn { color: var(--forest); background: #e9f0eb; }
.quiet-btn { color: var(--ink); background: var(--surface); border: 1rpx solid var(--line); }
.primary-btn[disabled], .secondary-btn[disabled], .quiet-btn[disabled] { opacity: .45; }
.actions { display: flex; flex-wrap: wrap; gap: 14rpx; }
.field { display: block; margin-bottom: 28rpx; }
.field-label { display: block; margin-bottom: 12rpx; color: var(--ink); font-size: 24rpx; font-weight: 650; }
.field-input, .field-textarea { width: 100%; min-height: 92rpx; padding: 19rpx 22rpx; border: 1rpx solid var(--line); border-radius: 3rpx; background: var(--surface); color: var(--ink); font-size: 27rpx; }
.field-textarea { min-height: 160rpx; line-height: 1.55; }
.divider { height: 1rpx; margin: 34rpx 0; background: var(--line); }
.section-head { display: flex; align-items: center; justify-content: space-between; gap: 18rpx; margin: 46rpx 0 18rpx; }
.link-text { display: inline-flex; align-items: center; min-height: 64rpx; color: var(--vermillion); font-size: 24rpx; font-weight: 600; }
.chip-row { display: flex; gap: 10rpx; overflow-x: auto; padding: 4rpx 0 16rpx; white-space: nowrap; }
.chip { display: inline-flex; align-items: center; justify-content: center; min-height: 66rpx; padding: 0 20rpx; border: 1rpx solid var(--line); border-radius: 3rpx; background: var(--surface); color: var(--muted); font-size: 23rpx; }
.chip.active { border-color: var(--forest); background: var(--forest); color: #fff; }
.bottom-nav { position: fixed; z-index: 9; right: 0; bottom: 0; left: 0; display: flex; justify-content: space-around; width: 100%; max-width: 920px; min-height: 110rpx; margin: auto; padding: 5rpx 0 env(safe-area-inset-bottom); border-top: 1rpx solid var(--line); background: rgba(255, 255, 255, .98); }
.bottom-nav__item { display: flex; flex: 1; flex-direction: column; align-items: center; justify-content: center; gap: 2rpx; min-height: 98rpx; color: var(--muted); font-size: 21rpx; }
.bottom-nav__item.active { color: var(--vermillion); font-weight: 700; }
.bottom-nav__symbol { height: 37rpx; font-family: Georgia, serif; font-size: 34rpx; line-height: 1; }
.book-list { display: flex; flex-direction: column; }
.book-row { display: flex; align-items: flex-start; gap: 22rpx; min-height: 198rpx; padding: 22rpx 0; border-bottom: 1rpx solid var(--line); }
.book-cover { position: relative; display: flex; flex: 0 0 128rpx; align-items: center; justify-content: center; width: 128rpx; height: 172rpx; padding: 15rpx; overflow: hidden; border-radius: 3rpx; background: #334d45; color: #fff; text-align: center; box-shadow: inset 8rpx 0 rgba(0,0,0,.17), 3rpx 5rpx 10rpx rgba(34,42,36,.1); }
.book-cover::after { position: absolute; right: 9rpx; bottom: 12rpx; left: 18rpx; height: 1rpx; background: rgba(255,255,255,.37); content: ""; }
.book-cover__title { display: -webkit-box; max-height: 130rpx; overflow: hidden; font-family: "Noto Serif CJK SC", "Songti SC", "SimSun", serif; font-size: 26rpx; font-weight: 700; line-height: 1.3; word-break: break-all; -webkit-box-orient: vertical; -webkit-line-clamp: 4; }
.book-cover.alt { background: #a45442; }
.book-cover.gold { background: #6d7055; }
.book-row__body { flex: 1; min-width: 0; padding-top: 3rpx; }
.book-row__title { display: block; margin: 0 0 10rpx; overflow: hidden; color: var(--ink); font-family: "Noto Serif CJK SC", "Songti SC", "SimSun", serif; font-size: 30rpx; font-weight: 700; line-height: 1.38; text-overflow: ellipsis; white-space: nowrap; }
.book-row__meta { display: block; overflow: hidden; color: var(--muted); font-size: 23rpx; line-height: 1.45; text-overflow: ellipsis; white-space: nowrap; }
.book-row__desc { display: -webkit-box; margin-top: 17rpx; overflow: hidden; color: var(--muted); font-size: 23rpx; line-height: 1.5; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.badge { display: inline-flex; align-items: center; min-height: 42rpx; padding: 3rpx 10rpx; border-radius: 2rpx; background: #f8eae5; color: var(--vermillion); font-size: 21rpx; }
.row-trail { display: block; margin-top: 17rpx; color: var(--forest); font-size: 23rpx; line-height: 1.4; }
.pager { display: flex; align-items: center; justify-content: center; gap: 22rpx; padding: 32rpx 0; color: var(--muted); font-size: 23rpx; }
.pager button { min-height: 76rpx; padding: 0 24rpx; border: 1rpx solid var(--line); background: var(--surface); color: var(--forest); font-size: 23rpx; }
@media (max-width: 480px) { .screen { padding-right: 28rpx; padding-left: 28rpx; } .page-title { font-size: 46rpx; } }
@media (prefers-reduced-motion: reduce) { button:active, .book-row:active, .chip:active { transform: none; } }
</style>
