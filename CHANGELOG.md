# 迭代升级说明 / Changelog

> 本文档记录迭代升级的所有变更内容。
> This document records all changes in iterative upgrades.

---

## v4.0.0.50 (2026-09-11) 🔥 话题榜完整列表页可点击

### 🇨🇳 中文

**话题榜完整列表页支持点击跳转搜索**

- 补齐热榜话题榜第二条入口：「更多话题」进入的话题榜完整列表页条目现可点击跳转搜索（#39）
- 与页内话题卡片一致的过渡方案：跳转搜索页并自动搜索话题关键词，URI 编码保证特殊字符无损
- 热榜话题榜全链路可点：页内卡片 / 完整列表页全覆盖，无剩余死区

### 🇬🇧 English

**Full topic list page now tappable**

- Completed the second topic-rank entry: items in the full topic list page (via "More topics") now open the search page with the topic pre-filled ( #39 )
- Same transitional approach as inline cards; topic names are URI-encoded so special characters survive intact
- The whole topic-rank surface is now tappable — inline cards and the full list page, no dead zones left

### 🇯🇵 日本語

**トピックランキング完全リストページがタップ可能に**

- 「もっと話題」から入る完全リストページの項目がクリック可能に——検索ページへ遷移し自動検索（ #39 ）
- インラインカードと同一の暫定方式：URI エンコード済みで特殊文字も無損
- トピックランキングの全導線がクリック可能に——デッドゾーンなし

### 🇰🇷 한국어

**토픽 랭킹 전체 목록 페이지 클릭 지원**

- 「더 많은 토픽」으로 진입하는 전체 목록 페이지 항목이 클릭 가능——검색 페이지로 이동해 자동 검색 ( #39 )
- 인라인 카드와 동일한 과도 방식: URI 인코딩 적용, 특수문자 무손실
- 토픽 랭킹의 모든 경로가 클릭 가능——남은 죽은 구역 없음

---

## v4.0.0.49 (2026-09-09) 🔥 热榜话题卡片可点击

### 🇨🇳 中文

**热榜话题卡片支持点击跳转搜索**

- 修复热榜页话题榜卡片点击无响应：点击话题卡片现在跳转搜索页并自动搜索该话题关键词（#39）
- 话题名经 URI 编码，含表情/特殊字符/逗号的话题均可正常跳转（真机实测中文+逗号+英文样本无损往返）
- 搜索页新增可选 query 参数（默认空），既有裸导航与 deepLink 行为不变

### 🇬🇧 English

**Hot page topic cards now tappable**

- Fixed unresponsive topic cards on the hot page: tapping a topic card now opens the search page and searches for that topic automatically ( #39 )
- Topic names are URI-encoded — topics with emoji/special characters/commas all navigate correctly (verified with CJK+comma+Latin samples)
- Search page gains an optional query argument (default empty); existing bare navigation and deepLink behavior unchanged

### 🇯🇵 日本語

**ホットページのトピックカードがタップ可能に**

- ホットページのトピックカードが反応しない問題を修正：カードをタップすると検索ページが開き、トピック名で自動検索（ #39 ）
- トピック名は URI エンコード済み——絵文字/特殊文字/カンマを含むトピックも正常に遷移（実機検証済み）
- 検索ページにオプションの query 引数を追加（デフォルト空）；既存のナビゲーションと deepLink の動作は変更なし

### 🇰🇷 한국어

**핫 페이지 토픽 카드 클릭 지원**

- 핫 페이지 토픽 카드가 반응하지 않던 문제 수정: 카드 탭 시 검색 페이지가 열리고 해당 토픽명으로 자동 검색 ( #39 )
- 토픽명은 URI 인코딩 적용——이모지/특수문자/쉼표 포함 토픽도 정상 이동 (실기기 검증)
- 검색 페이지에 선택적 query 인자 추가(기본값 빈 문자열); 기존 내비게이션과 deepLink 동작 변화 없음

---

## v4.0.0.48 (2026-09-10) 🖼️ 楼中楼带图评论完整渲染

### 🇨🇳 中文

**楼中楼带图评论真图渲染（接口版本号升级）**

- 修复楼中楼带图评论显示为「[图片]」占位的问题：楼中楼详情接口客户端版本号升级至 22.10.1.0，服务端现直接下发完整图片数据（#27）
- 图片支持点击查看大图，宽高缺失时按内容自带尺寸兜底
- 未知内容类型不再静默丢弃：新增日志兜底，官方后续调整数据格式时可观测
- 楼中楼文字/表情/引用渲染经真机回归确认无变化

### 🇬🇧 English

**Full image rendering for sub-thread comments (API version bump)**

- Fixed sub-thread image comments showing "[Image]" placeholder: the sub-thread detail API client version is bumped to 22.10.1.0, and the server now delivers full image data ( #27 )
- Images support tap-to-view; missing dimensions fall back to content-intrinsic size
- Unknown content types are no longer silently dropped: a log fallback is added for observability when upstream changes data formats
- Text/emote/quote rendering in sub-threads verified unchanged on real devices

### 🇯🇵 日本語

**サブスレッド画像コメントの完全レンダリング（APIバージョン更新）**

- サブスレッドの画像コメントが「[画像]」プレースホルダで表示される問題を修正：詳細 API のクライアントバージョンを 22.10.1.0 に更新し、サーバーが完全な画像データを配信（ #27 ）
- 画像はタップで拡大表示、サイズ欠損時はコンテンツ固有サイズにフォールバック
- 未知のコンテンツタイプを黙って破棄しない：ログフォールバックを追加
- サブスレッドのテキスト/絵文字/引用の描画は実機回帰で変化なし

### 🇰🇷 한국어

**하위 스레드 이미지 댓글 완전 렌더링 (API 버전 업그레이드)**

- 하위 스레드 이미지 댓글이 "[이미지]" 자리표시로 보이던 문제 수정: 상세 API 클라이언트 버전을 22.10.1.0 으로 올려 서버가 완전한 이미지 데이터 전달 ( #27 )
- 이미지 탭으로 확대 보기 지원, 치수 누락 시 콘텐츠 고유 치수로 폴백
- 알 수 없는 콘텐츠 타입을 조용히 버리지 않음: 로그 폴백 추가
- 하위 스레드 텍스트/이모티콘/인용 렌더링은 실기기 회귀에서 변화 없음

---

## v4.0.0.47 (2026-09-09) 📱 主页面底栏导航条适配

### 🇨🇳 中文

**主页面底部导航栏适配系统导航条**

- 修复主页面底栏与系统导航条重叠：底栏背景色延伸到底部安全区，暗黑模式下不再出现透明断层
- 补齐手势热区确定性兜底：手势导航且系统未上报导航条高度时，以 28dp 兜底保证底栏不贴底（与帖子页行为一致）（#36）
- 三键导航设备无视觉变化；四 tab 切换、启动页设置、动态入口隐藏联动均不受影响

### 🇬🇧 English

**Main page bottom bar navigation bar adaptation**

- Fixed the main page bottom bar overlapping the system navigation bar: bar background now extends into the bottom safe area, no more transparent gap in dark mode
- Added deterministic gesture-area fallback: 28dp when gesture navigation reports no navigation bar height, consistent with the thread page ( #36 )
- No visual change on 3-button devices; tab switching / startup page / hide-explore unaffected

### 🇯🇵 日本語

**メインページのボトムバーとナビゲーションバーの適合**

- メインページのボトムバーがシステムナビゲーションバーと重なる問題を修正：バー背景を下部セーフエリアまで延伸、ダークモードの透明な断層を解消
- ジェスチャーエリアの決定論的フォールバックを追加：28dp で底バーが貼り付かないように保証（スレッドページと一致）（ #36 ）
- 3 ボタンデバイスで視覚変化なし；タブ切替・起動ページ・動的非表示に影響なし

### 🇰🇷 한국어

**메인 페이지 하단 바 내비게이션 바 적응**

- 메인 페이지 하단 바가 시스템 내비게이션 바와 겹치는 문제 수정: 바 배경이 하단 세이프 에어리어까지 확장, 다크 모드 투명 단절 해소
- 제스처 영역 결정론적 폴백 추가: 28dp 로 하단 바가 바닥에 붙지 않도록 보장 (스레드 페이지와 일치) ( #36 )
- 3버튼 기기 시각 변화 없음; 탭 전환·시작 페이지·동적 숨김 영향 없음

---

## v4.0.0.46 (2026-09-08) 🖼️ 图片查看沉浸过渡 + 列表滚动位置记忆

### 🇨🇳 中文

**图片查看沉浸过渡 + 列表滚动位置记忆**

- 修复图片查看页进入/退出时状态栏瞬间消失导致的画面跳变：改用系统过渡模式平滑淡入淡出，从分享面板返回时也会正确恢复沉浸态（#34）
- 新增列表滚动位置记忆：楼中楼列表与主贴楼层列表在跳转个人主页后返回，自动停留在离开前的浏览位置，不再回到顶部（#8）
- 回复定位、续读定位链路经真机回归确认不受影响

### 🇬🇧 English

**Immersive transition for photo viewer + list scroll position memory**

- Fixed the jarring jump when opening/leaving the photo viewer: status bar now fades smoothly via system transient mode, and immersive state is correctly restored when returning from the share sheet ( #34 )
- Added scroll position memory: sub-thread lists and main-thread floor lists now restore the browsing position after navigating to a user profile, instead of jumping back to the top ( #8 )
- Reply targeting and resume-reading chains verified unaffected on real devices

### 🇯🇵 日本語

**写真ビューアの没入トランジション + リストスクロール位置の記憶**

- 写真ビューア開閉時のステータスバー瞬間切替によるジャンプを修正：システム過渡モードで滑らかにフェード、共有シートから戻っても没入状態を正しく復元（ #34 ）
- スクロール位置の記憶を追加：サブスレッド/メインスレッドのリストは、ユーザーページへ移動して戻った後も閲覧位置を維持（ #8 ）
- 返信定位・続き読みチェーンは実機回帰で影響なしを確認

### 🇰🇷 한국어

**사진 뷰어 몰입 전환 + 리스트 스크롤 위치 기억**

- 사진 뷰어 열기/닫기 시 상태바 순간 전환으로 인한 점프 수정: 시스템 과도 모드로 부드러운 페이드, 공유 시트에서 돌아와도 몰입 상태 복원 ( #34 )
- 스크롤 위치 기억 추가: 하위 스레드/메인 스레드 리스트는 사용자 페이지 이동 후 돌아와도 열람 위치 유지 ( #8 )
- 답글定位·이어읽기 체인은 실기기 회귀에서 영향 없음 확인

---

## v4.0.0.45 (2026-09-08) 🎯 楼中楼回复绑定修正 + 吧名路由加固

### 🇨🇳 中文

**回复对象精准锁定 + 特殊吧名路由加固**

- 修复楼中楼回复降级：回帖页路由此前丢弃 subPostId，回复楼中楼内评论时对象会降级到楼层级——现已补齐参数消费，回复精确绑定目标评论（PR#33 后续加固）
- 三处回复导航的吧名参数补 `Uri.encode`：吧名含 `&`/`#`/空格等特殊字符时路由不再被破坏（与 v4.0.0.39 编码规范对齐）

### 🇬🇧 English

**Precise reply targeting + special-forum-name route hardening**

- Fixed sub-thread reply degradation: the reply route previously dropped subPostId, causing replies to a sub-thread comment to fall back to floor level — parameter consumption now completed for precise binding (PR#33 follow-up)
- Added `Uri.encode` to forum-name parameters in three reply navigation calls: routes survive forum names containing `&`/`#`/spaces (aligned with the v4.0.0.39 encoding spec)

### 🇯🇵 日本語

**返信ターゲットの精密化 + 特殊吧名ルートの強化**

- サブスレッド返信のターゲット降級を修正：ルートが subPostId を破棄し、返信がフロアレベルに落ちる問題を解消、パラメータ消費を補完（PR#33 続き）
- 3 箇所の返信ナビゲーションで吧名パラメータに `Uri.encode` を追加：`&`/`#`/空白を含む吧名でもルートが壊れない（v4.0.0.39 仕様に準拠）

### 🇰🇷 한국어

**답글 대상 정밀 고정 + 특수 바명 라우트 강화**

- 하위 스레드 답글 대상 강등 수정: 라우트가 subPostId 를 버려 답글이 층 레벨로 떨어지던 문제 해소, 파라미터 소비 보완 (PR#33 후속)
- 3 곳의 답글 내비게이션에 바명 파라미터 `Uri.encode` 추가: `&`/`#`/공백 포함 바명도 라우트 손상 없음 (v4.0.0.39 규격 준수)

---

## v4.0.0.44 (2026-09-07) 🚀 启动页自定义 + 楼中楼图片占位

### 🇨🇳 中文

**新增「启动页」自定义设置 + 楼中楼图片占位优化**

- 新增启动页设置：可在「首页 / 动态 / 消息 / 我」中任选启动时默认进入的页面（#30）
- 启动页与「隐藏动态入口」联动：动态被隐藏时自动回退首页，不越界不崩溃
- 楼中楼带图评论不再丢失：内容渲染补全新图片类型分支，详情页整条保留（#27）
- 帖子页楼中楼预览补「[图片]」占位提示，复制语义同步一致
- 楼中楼图片支持点击查看大图（复用主楼 PhotoView 机制）

### 🇬🇧 English

**Startup page preference + sub-thread image placeholder**

- New "Startup Page" setting: choose the default tab among Home / Explore / Notifications / Me ( #30 )
- Graceful fallback when Explore tab is hidden — no crash, no out-of-bounds
- Sub-thread image comments are no longer dropped: new image content type branch added, entries fully preserved ( #27 )
- Inline sub-thread previews now show an "[Image]" placeholder; copy semantics aligned
- Sub-thread images support tap-to-view via the existing PhotoView mechanism

### 🇯🇵 日本語

**起動ページ設定 + サブスレッド画像プレースホルダ**

- 「起動ページ」設定を追加：ホーム / ダイナミック / 通知 / マイ から選択可能（ #30 ）
- ダイナミックタブ非表示時はホームへ自動フォールバック、クラッシュなし
- サブスレッドの画像コメントが欠落しないよう修正：新画像タイプ分岐を追加（ #27 ）
- インライン予告行に「[画像]」プレースホルダを表示、コピー挙動も統一
- サブスレッド画像は既存 PhotoView で拡大表示対応

### 🇰🇷 한국어

**시작 페이지 설정 + 하위 스레드 이미지 자리표시**

- "시작 페이지" 설정 추가: 홈 / 동적 / 알림 / 나 중에서 선택 가능 ( #30 )
- 동적 탭 숨김 시 홈으로 자동 폴백, 크래시 없음
- 하위 스레드 이미지 댓글 유실 수정: 새 이미지 타입 분기 추가 ( #27 )
- 인라인 미리보기에 "[이미지]" 자리표시 표시, 복사 동작 통일
- 하위 스레드 이미지는 기존 PhotoView 로 확대 보기 지원

---

## v4.0.0.43 (2026-09-05) 💬 楼层回复定位修正 + 回帖页状态栏避让

### 🇨🇳 中文

**「无法成为第一个楼中楼」根治 + 回帖页顶栏避让**

- 修复楼层回复定位参数丢失：点击楼层「评论」或长按「回复」发出的内容，现在正确出现在该楼层的楼中楼中，不再误发为主贴新楼层（#26）
- 楼中楼内回复同步补齐定位参数，回复对象绑定更精准
- 修复回帖编辑页标题与系统状态栏重叠：顶部避让补齐，深浅色主题均正常（#29）
- 回复主贴路径保持原有语义，三条回复链路互不影响；全链路真机回归零崩溃

### 🇬🇧 English

**Floor-reply targeting fix + reply page status bar inset**

- Fixed missing floor-targeting parameter: tapping "Comment" on a floor or long-press "Reply" now posts into that floor's sub-thread instead of the main thread ( #26 )
- Sub-thread replies now carry the full targeting parameter for precise binding
- Fixed the reply editor's title overlapping the system status bar: top inset avoidance added, verified in both light & dark themes ( #29 )
- Main-thread reply semantics preserved; all three reply paths verified on real devices with zero crashes

### 🇯🇵 日本語

**階層返信のターゲット修正 + 返信ページのステータスバー回避**

- 階層の「コメント」や長押し「返信」が、メインスレッドではなく該当階層のサブスレッドに正しく投稿されるよう修正（ #26 ）
- サブスレッド内返信にもターゲットパラメータを補完し、返信対象を正確にバインド
- 返信エディタのタイトルとステータスバーの重なりを修正：上部インセット回避を追加、ライト/ダーク両テーマで確認（ #29 ）
- メインスレッド返信のセマンティクスは維持、3つの返信パスを実機回帰、クラッシュゼロ

### 🇰🇷 한국어

**층 replies 대상 수정 + 답글 페이지 상태바 회피**

- 층의 "댓글" 또는 길게 눌러 "답글"을 달 때 메인 스레드가 아닌 해당 층의 하위 스레드에 올바르게 게시되도록 수정 ( #26 )
- 하위 스레드 내 답글에도 대상 파라미터를 보완하여 정확한 바인딩
- 답글 편집기 제목과 상태바 겹침 수정: 상단 인셋 회피 추가, 라이트/다크 테마 모두 확인 ( #29 )
- 메인 스레드 답글 의미 유지, 3개 답글 경로 실기기 회귀, 크래시 제로

---

## v4.0.0.42 (2026-09-02) 👍 点赞列表完整渲染

### 🇨🇳 中文

**「收到的赞」列表渲染修复与体验完善**

- 修复「收到的赞」列表无法展示内容的问题：为点赞消息建立专用数据模型，完整解析点赞者信息与被赞帖子信息
- 列表条目展示：点赞者头像与昵称、相对时间、「赞了你的帖子：帖子标题」
- 交互完善：点击条目跳转对应帖子，点击点赞者头像进入其个人主页
- 新增列表空态兜底展示；下拉刷新与分页加载增加去重处理
- 空值防护加固：异常数据不再引发崩溃

### 🇬🇧 English

**Agree-me list rendering fix & polish**

- Fixed the "Received Likes" list appearing blank: introduced a dedicated data model to fully parse liker info and liked-thread info
- List items now show liker avatar & nickname, relative time, and "liked your thread: <title>"
- Interaction: tap an item to open the thread; tap the avatar to visit the liker's profile
- Added empty-state fallback; deduplication for pull-to-refresh and pagination
- Hardened null-safety against malformed data

### 🇯🇵 日本語

**「いただいたいいね」リストの描画修正**

- 「いただいたいいね」リストが空白になる問題を修正：いいね専用のデータモデルを導入し、いいねしたユーザーと対象スレッドの情報を完全に解析
- リスト項目：いいねしたユーザーのアバター・ニックネーム・相対時間・「スレッドにいいねしました：<タイトル>」
- 操作：項目をタップしてスレッドへ、アバターをタップしてプロフィールへ
- 空状態のフォールバック表示を追加；更新・ページネーション時の重複排除
- 異常データに対する null 安全性を強化

### 🇰🇷 한국어

**「받은 좋아요」목록 렌더링 수정**

- "받은 좋아요" 목록이 비어 있는 문제 수정: 전용 데이터 모델을 도입하여 좋아요 사용자 및 대상 게시글 정보를 완전히 파싱
- 목록 항목: 좋아요한 사용자 아바타·닉네임·상대 시간·"게시글에 좋아요를 눌렀습니다: <제목>"
- 조작: 항목 탭하여 게시글로 이동, 아바타 탭하여 프로필로 이동
- 빈 상태 폴백 표시 추가; 새로고침·페이지네이션 중복 제거
- 비정상 데이터에 대한 null 안전성 강화

---

## v4.0.0.41 (2026-09-01) 🖐️ 手势热区二次加固

### 🇨🇳 中文

**底栏手势热区确定性兜底 + 弹层避让补全**

#### 背景
v4.0.0.40 的避让修复在部分国产 ROM（ColorOS 16 等手势导航机型）上未完全生效：
系统在手势模式下可能将 `navigationBars` 与 `safeGestures` 两个 inset 同时上报为 0，
基于 inset 的避让在数学上等于没有生效。

#### 修复内容
- 新增统一的底栏安全区组件：inset 真实值优先，**两者均为 0 时以 28dp 手势热区高度确定性兜底**，
  并在该区域内主动消费点击，杜绝触摸穿透
- 「更多」底部弹层补齐底部避让——「只看楼主 / 收藏」等选项不再落入手势热区（#20）
- 帖子页与楼中楼页同步应用

### 🇺🇸 English

**Deterministic gesture-hotspot fallback + bottom-sheet inset hardening**

#### Background
On some ROMs (e.g. ColorOS 16 gesture navigation), both `navigationBars` and
`safeGestures` may be reported as 0, so the v4.0.0.40 inset-based avoidance was a no-op.

#### Fix
- New unified bottom-bar safety component: real insets first, **deterministic 28dp
  fallback when both are 0**, plus click consumption inside the region
- The "more" bottom sheet now also reserves gesture-hotspace height (#20)
- Applied to both the thread page and the sub-posts page

---

## v4.0.0.40 (2026-08-31) 🧭 举报跳转修正 + 全面屏手势避让

### 🇨🇳 中文

**举报跳转修正 + 手势导航底栏避让加固**

#### 举报成功跳转修正
- 修复举报成功后跳转网页的字符串插值缺失问题，举报流程跳转恢复正常，
  并按新规范对跳转地址进行 URI 编码处理

#### 全面屏手势底栏避让（#20）
- 修复全面屏手势导航下，帖子页底部点击会误触「只看楼主 / 收藏」等操作的问题
- 根因：底栏避让高度此前仅覆盖系统导航条（三键导航），未覆盖全面屏手势热区，
  两者现已同时避让（`navigationBars ∪ safeGestures`），手势与三键模式下均正常
- 同步加固楼中楼页面的同一问题

### 🇺🇸 English

**Report-jump fix + gesture-navigation bottom-bar inset hardening**

#### Report jump corrected
- Fixed a missing string interpolation in the post-report success callback that
  broke the follow-up web jump; the URL is now URI-encoded per the new convention

#### Gesture bottom-bar inset (#20)
- With gesture navigation, taps near the screen bottom could accidentally trigger
  "See OP only / Favorite" actions: the bottom bar only reserved height for
  navigation bars (3-button mode), not for the gesture hotspot
- Now reserves `navigationBars ∪ safeGestures`, correct under both navigation modes;
  the same fix is applied to the sub-posts (reply) page

---

## v4.0.0.39 (2026-08-31) 🛡️ 特殊字符路由加固

### 🇨🇳 中文

**含链接内容复制 / 外链跳转闪退根治**

#### 问题现象（#19）
复制包含链接的回帖内容、或点击帖子内的网页链接（如网盘链接）时，
应用在导航瞬间闪退；纯文本内容的复制则一切正常。

#### 根因
链接与特殊字符（`/` `?` `&` `:` 等）未经 URI 编码即被拼入导航路由，
破坏了 Navigation 的路由匹配规则，触发 `IllegalArgumentException`。

#### 修复内容
- 复制对话框路由（`copy_dialog`）与网页路由（`webview`）的参数统一采用
  `android.net.Uri.encode` 编码后传递，共加固 **6 处调用点**（楼层复制 ×2、
  楼中楼复制 ×2、外链跳转 ×1），并同步覆盖视频卡片跳转等同族路径
- 读取侧依赖 Navigation 框架的自动解码，往返无损——中文、空格、emoji、
  全角符号、完整 URL 均原样还原
- 排查期间另发现一处字符串插值笔误（分享链接场景），已记录待后续版本处理

#### 验证
- ✅ 复制纯文本 / 含特殊字符内容 → 对话框原文无损展示
- ✅ 点击网盘类外链 → 内置 WebView 完整渲染
- ✅ 视频卡片 → 内嵌播放器正常播放
- ✅ 帖子详情、楼中楼全量回归通过

### 🇺🇸 English

**Crash fix for copying content with links / opening external URLs**

#### Symptom (#19)
Copying a reply containing links, or tapping an in-post URL (e.g. a netdisk link),
crashed the app at navigation time; plain-text copying worked fine.

#### Root cause
Link and special characters (`/` `?` `&` `:` etc.) were concatenated into navigation
routes without URI encoding, breaking Navigation's route matching and throwing
`IllegalArgumentException`.

#### Fix
- All `copy_dialog` and `webview` route parameters are now passed through
  `android.net.Uri.encode` — **6 call sites** hardened (thread copy ×2,
  sub-post copy ×2, external link ×1), plus sibling paths such as video-card jumps
- The reading side relies on Navigation's built-in auto-decoding; round-trips are
  lossless for Chinese text, spaces, emoji, full-width symbols and full URLs

---

## v4.0.0.38 (2026-08-30) 🚀 启动稳定性修复 + 收藏偏好贯通

### 🇨🇳 中文

**启动崩溃根治 + 个体贡献合入 + 收藏页浏览偏好全链路生效**

#### 启动导航作用域修复（合入社区贡献 #15，感谢 @wufeng5702）
- `MainActivityV2` 顶层 `CompositionLocalProvider` 全局提供 `LocalNavigator`，
  根治部分机型冷启动即触发 `IllegalStateException: No navigator is available` 的闪退（#6 / #9 / #17）
- `NotificationsPage` 改为显式注入 navigator，调用链清晰无歧义
- `HotPage` 移除遮蔽函数参数的冗余局部变量，消除导航上下文歧义

#### Java 21 API 前向兼容
- `Extensions.kt` / `PbContentRender.kt` 中 `removeFirst()` / `removeLast()`
  统一替换为 `removeAt()`，避免在高版本 JDK 编译产物上于旧版 Android 运行时触发 `NoSuchMethodError`

#### 收藏页浏览偏好贯通（#3）
- 打通 `from=FROM_STORE` 传递链：`Routes.THREAD` 路由新增 `from` 参数 →
  `MainActivityV2` 解析注入 → 收藏页跳转携带标识
- `ThreadPage` 初始化时读取「从收藏进入默认只看楼主 / 默认倒序浏览」设置并作为有效初始值，
  两个此前「定义而未生效」的开关自此真正生效
- 刷新 / 重试 / 续读路径统一取用运行时状态值，手动切换的浏览偏好在会话内稳定保持

#### 构建产物
- ✅ Debug + Release APK（CI 全量构建通过）

### 🇺🇸 English

**Startup crash fix + community contribution merged + collection browse preferences fully wired**

#### Startup navigation scope fix (community PR #15 by @wufeng5702 merged with thanks)
- `LocalNavigator` is now provided globally at the top-level `CompositionLocalProvider` in `MainActivityV2`,
  eliminating the `IllegalStateException: No navigator is available` cold-start crash on affected devices (#6 / #9 / #17)
- `NotificationsPage` now receives the navigator explicitly; `HotPage` drops the parameter-shadowing local

#### Java 21 API forward compatibility
- `removeFirst()` / `removeLast()` replaced with `removeAt()` in `Extensions.kt` / `PbContentRender.kt`

#### Collection browse preferences wired end-to-end (#3)
- The `from=FROM_STORE` chain now flows through `Routes.THREAD` → `MainActivityV2` → `ThreadStorePage`
- `ThreadPage` reads the "default see-LZ / default descending" preferences as effective initial values;
  refresh / retry / load-more paths consistently use runtime state so manual toggles persist

---

## v4.0.0.4 (2026-06-12) 🔧 构建链升级 + 源码修复

### 🇨🇳 中文

**构建链现代化 + 源码腐化修复**

本版本对项目构建链进行了升级，并修复了代码库中存在的腐化问题。

#### 核心构建工具链
- **Android Gradle Plugin**: `8.2.2` → `8.5.2`
- **Gradle**: `8.2` → `8.7`

#### 依赖兼容性说明
由于 compose-destinations 1.10.0 的 KSP 处理器存在 NPE bug，以下依赖保持原始版本以保证编译兼容：
- Kotlin `1.9.22`、KSP `1.9.22-1.0.17`、Hilt `2.46.1`
- Compose BOM `2024.01.00`、Navigation Compose `2.7.6`
- 后续版本将在替换导航方案后继续升级

#### 源码修复
- 修复 8 个被 cat -n 行号前缀破坏的 Kotlin 接口文件
- 修复 Java→Kotlin 迁移残留的 Java 风格参数声明
- 创建缺失的 `ThemeSwitcher.kt` 接口文件
- `ErrorBean` 添加 `open` 修饰符，允许 Java 子类继承
- `OnItemClickListener` / `OnGrantedCallback` / `OnDeniedCallback` 改为 `fun interface`
- `ReplyPage` / `QuickPreviewUtil` 空安全修复

#### CI/CD 改进
- Release APK 构建不再依赖 keystore——无签名密钥时自动回退为 debug 签名
- Debug 和 Release APK 均始终上传为构建产物

#### 构建产物
- ✅ Debug APK (29.0MB)
- ✅ Release APK (8.4MB)

#### 版本信息
- `versionCode`: `400003` → `400004`
- `versionName`: `4.0.0-v4.0.0.3` → `4.0.0-v4.0.0.4`

### 🇺🇸 English

**Build Chain Upgrade + Source Code Remediation**

This version upgrades the build toolchain and fixes codebase decay issues.

#### Core Build Toolchain
- **Android Gradle Plugin**: `8.2.2` → `8.5.2`
- **Gradle**: `8.2` → `8.7`

#### Dependency Compatibility Notes
Due to a NPE bug in compose-destinations 1.10.0's KSP processor, the following dependencies remain at original versions:
- Kotlin `1.9.22`, KSP `1.9.22-1.0.17`, Hilt `2.46.1`
- Compose BOM `2024.01.00`, Navigation Compose `2.7.6`
- Further upgrades planned after replacing the navigation library

#### Source Fixes
- Fixed 8 Kotlin interface files corrupted by cat -n line number prefixes
- Fixed Java-style parameter declarations left from Java→Kotlin migration
- Created missing `ThemeSwitcher.kt` interface
- Added `open` modifier to `ErrorBean` for Java subclass inheritance
- Changed `OnItemClickListener` / `OnGrantedCallback` / `OnDeniedCallback` to `fun interface`
- Null safety fixes in `ReplyPage` / `QuickPreviewUtil`

#### CI/CD Improvements
- Release APK build no longer requires keystore — falls back to debug signing
- Both Debug and Release APKs always uploaded as artifacts

#### Build Artifacts
- ✅ Debug APK (29.0MB)
- ✅ Release APK (8.4MB)

#### Version Info
- `versionCode`: `400003` → `400004`
- `versionName`: `4.0.0-v4.0.0.3` → `4.0.0-v4.0.0.4`

### 🇯🇵 日本語

**ビルドチェーンアップグレード + ソースコード修復**

本バージョンでは、ビルドツールチェーンをアップグレードし、コードベースの腐化問題を修正しました。

#### コアビルドツールチェーン
- **Android Gradle Plugin**: `8.2.2` → `8.5.2`
- **Gradle**: `8.2` → `8.7`

#### 依存関係の互換性について
compose-destinations 1.10.0 の KSP プロセッサに NPE バグがあるため、以下の依存関係は元のバージョンを維持：
- Kotlin `1.9.22`、KSP `1.9.22-1.0.17`、Hilt `2.46.1`
- Compose BOM `2024.01.00`、Navigation Compose `2.7.6`

#### ソース修復
- cat -n 行番号プレフィックスで破損した 8 つの Kotlin ファイルを修復
- Java→Kotlin 移行の残骸（Java スタイルのパラメータ宣言）を修正
- 欠落していた `ThemeSwitcher.kt` インターフェースを作成
- `ErrorBean` に `open` 修飾子を追加
- 3 つのインターフェースを `fun interface` に変更
- null 安全性の修正

#### ビルド成果物
- ✅ Debug APK (29.0MB)
- ✅ Release APK (8.4MB)

### 🇰🇷 한국어

**빌드 체인 업그레이드 + 소스 코드 수정**

본 버전은 빌드 툴체인을 업그레이드하고 코드베이스 부패 문제를 수정했습니다.

#### 코어 빌드 툴체인
- **Android Gradle Plugin**: `8.2.2` → `8.5.2`
- **Gradle**: `8.2` → `8.7`

#### 의존성 호환성 참고
compose-destinations 1.10.0 KSP 프로세서의 NPE 버그로 인해 다음 의존성은 원래 버전 유지:
- Kotlin `1.9.22`, KSP `1.9.22-1.0.17`, Hilt `2.46.1`
- Compose BOM `2024.01.00`, Navigation Compose `2.7.6`

#### 소스 수정
- cat -n 줄번호 접두사로 손상된 8개 Kotlin 파일 수정
- Java→Kotlin 마이그레이션 잔재 수정
- 누락된 `ThemeSwitcher.kt` 인터페이스 생성
- `ErrorBean`에 `open` 수정자 추가
- 3개 인터페이스를 `fun interface`로 변경
- null 안전성 수정

#### 빌드 결과물
- ✅ Debug APK (29.0MB)
- ✅ Release APK (8.4MB)

### 🇨🇳 中文

**CI 修复与构建验证**

- 修复 GitHub Actions workflow 语法问题
  - secrets 引用方式改为环境变量
  - 签名步骤改为可选（无 secrets 时自动跳过）
  - 新增 Debug APK 构建和上传
  - 添加 `workflow_dispatch` 手动触发支持
- 回退不存在的依赖版本到原始可编译版本
- **CI 构建成功** — Debug APK (29.1MB) 自动生成

### 🇺🇸 English

**CI Fixes & Build Verification**

- Fixed GitHub Actions workflow syntax issues
  - Changed secrets reference to use environment variables
  - Made signing steps optional (auto-skip when secrets not configured)
  - Added Debug APK build and upload
  - Added `workflow_dispatch` manual trigger support
- Reverted non-existent dependency versions back to original working versions
- **CI build succeeded** — Debug APK (29.1MB) auto-generated

### 🇯🇵 日本語

**CI修正とビルド検証**

- GitHub Actionsワークフローの構文問題を修正
- 存在しない依存関係バージョンを元の動作バージョンに戻す
- **CIビルド成功** — Debug APK (29.1MB) 自動生成

### 🇰🇷 한국어

**CI 수정 및 빌드 검증**

- GitHub Actions 워크플로우 구문 문제 수정
- 존재하지 않는 종속성 버전을 원래 작동 버전으로 되돌림
- **CI 빌드 성공** — Debug APK (29.1MB) 자동 생성

---

## v4.0.0.2 (2026-06-09)

### 🇨🇳 中文

**安全修复**

- 移除 `usesCleartextTraffic="true"`，改用 `network_security_config.xml` 精确控制
- 默认禁止明文 HTTP 流量，仅允许局域网开发环境

### 🇺🇸 English

**Security Fixes**

- Replaced `usesCleartextTraffic="true"` with proper `network_security_config.xml`
- Default deny cleartext HTTP traffic, allow only LAN for development

### 🇯🇵 日本語

**セキュリティ修正**

- `usesCleartextTraffic="true"` を `network_security_config.xml` に置き換え

### 🇰🇷 한국어

**보안 수정**

- `usesCleartextTraffic="true"`를 `network_security_config.xml`로 교체

---

## v4.0.0.1 (2026-06-09)

### 🇨🇳 中文

**迭代升级版本 — 首次发布**

本版本为首次迭代升级，主要变更如下：

#### 文档完善
- 更新 README.md：保留原作者全部声明
- 新增四国语言（中/日/韩/英）迭代声明
- 新增构建说明和版本记录
- 新增免责声明
- 新增 CREDITS.md 原作者致敬文件
- 新增 AI_CHANGELOG.md 变更记录

#### 注意事项
- 依赖版本保持原始版本（v4.0.0-beta.1），确保编译通过
- 后续版本将逐步升级依赖

### 🇺🇸 English

**Iterative Upgrade — First Release**

This is the first iterative upgrade. Main changes:

#### Documentation
- Updated README.md with full preservation of original author credits
- Added 4-language (CN/JP/KR/EN) iteration notices
- Added build instructions and version history
- Added disclaimers
- Added CREDITS.md honoring original author
- Added AI_CHANGELOG.md change records

#### Notes
- Dependency versions kept at original (v4.0.0-beta.1) to ensure successful compilation
- Dependencies will be upgraded incrementally in future versions

### 🇯🇵 日本語

**イテレーション版 — 初回リリース**

#### ドキュメント
- README.md を更新、原作者のクレジットを完全に保持
- 4か国語のAIイテレーション宣言を追加
- CREDITS.md と AI_CHANGELOG.md を追加

### 🇰🇷 한국어

**반복 업그레이드 버전 — 첫 번째 릴리스**

#### 문서
- README.md 업데이트, 원저자 크레딧 완전 보존
- 4개국어 반복 업그레이드 선언 추가
- CREDITS.md와 AI_CHANGELOG.md 추가
