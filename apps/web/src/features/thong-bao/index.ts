// Barrel export cho feature "thong-bao" — thông báo và học phí trên trang chủ.
export { NoticeList } from './components/NoticeList'
export type { NoticeListProps } from './components/NoticeList'

export { NoticeRow } from './components/NoticeRow'
export type { NoticeRowProps } from './components/NoticeRow'

export { NoticeSpotlight } from './components/NoticeSpotlight'
export type { NoticeSpotlightProps } from './components/NoticeSpotlight'

export { Inbox } from './components/Inbox'

export { NoticeDetailDialog } from './components/NoticeDetailDialog'
export type { DetailRow, NoticeDetailDialogProps } from './components/NoticeDetailDialog'

export { Pills } from './components/Pills'
export type { PillOption, PillsProps } from './components/Pills'

export { SentBox } from './components/SentBox'

export { UnreadBadge, useUnreadCount } from './components/UnreadBadge'

export { GvNoticeHub } from './components/GvNoticeHub'

export { NoticeComposer } from './components/NoticeComposer'
export type { NoticeComposerProps } from './components/NoticeComposer'
export { createDraft, previewNotice, sendDraft } from './api/composeApi'
export type { ComposeInput, ComposeTarget, DraftRef, PreviewCount } from './api/composeTypes'
export { fetchInbox, fetchUnreadCount, markAllRead, markRead } from './api/inboxApi'
export type { InboxItem, InboxPageData, UnreadCount } from './api/inboxTypes'

export { NOTICES, SPOTLIGHT_NOTICE, TUITION_NOTICES } from './data/notices'
export type { Notice } from './types'
