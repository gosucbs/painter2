# Gap Analysis: ai-color-matching

> Design: `docs/02-design/features/ai-color-matching.design.md`
> Implementation: `app/src/main/java/com/painterai/app/`

---

## Overall Match Rate: **80%**

```
[Plan] ✅ → [Design] ✅ → [Do] ✅ → [Check] 🔄 80% → [Act] ⏳
```

---

## Score Breakdown

| Category | Score | Status |
|----------|:-----:|:------:|
| Project Structure | 74% | ⚠️ |
| Data Model | 94% | ✅ |
| API Integration | 91% | ✅ |
| Screen/UI | 74% | ⚠️ |
| Domain Model | 82% | ⚠️ |
| Component | 60% | ❌ |
| Build/Library | 85% | ✅ |
| Security | 70% | ⚠️ |
| **Overall** | **80%** | **⚠️** |

---

## Implemented (구현 완료)

### Screens (6/6)
- [x] LoginScreen + AuthViewModel
- [x] HomeScreen + HomeViewModel
- [x] NewJobScreen + NewJobViewModel
- [x] AnalysisScreen + AnalysisViewModel (핵심)
- [x] ChatScreen + ChatViewModel
- [x] ArchiveScreen + ArchiveViewModel

### Domain Models
- [x] Job (차량, 컬러코드, 결과, PaintBrand, JobResult)
- [x] Recipe + Toner (배합 입력)
- [x] Conversation + Message + MessageRole
- [x] Photo + PhotoType

### Data Layer
- [x] SupabaseClient 초기화
- [x] ClaudeApiService (Edge Function 호출)
- [x] AuthRepository (signIn/signUp/signOut)
- [x] JobRepository (CRUD + 검색)
- [x] ConversationRepository (생성/메시지추가/요약)
- [x] PhotoRepository (업로드/URL/목록)

### API/Backend
- [x] Supabase Edge Function `analyze-color`
- [x] Claude API 시스템 프롬프트 (3각도 분석, KCC SUMIX 토너)
- [x] Auth 토큰 검증

### UI Components
- [x] RecipeInputCard (토너 추가/삭제)
- [x] MessageBubble (채팅 버블)
- [x] JobCard (작업 카드)

### Infrastructure
- [x] Navigation (6개 라우트)
- [x] Hilt DI (AppModule)
- [x] Theme (Dark/Light)
- [x] build.gradle.kts 의존성

---

## Gaps (미구현 항목)

### Critical (구현 필수)

| # | Gap | Design Section | Impact |
|---|-----|---------------|--------|
| 1 | Recipe Supabase 저장 | §2.2 recipes 테이블 | 배합 데이터가 서버에 저장되지 않음 |
| 2 | Photo 업로드 연결 | §2.3 Storage | AnalysisScreen에서 선택한 사진이 Supabase에 업로드되지 않음 |
| 3 | ArchiveScreen 상세 | §4.5 | 사진 갤러리, 배합 표시, 대화기록 링크, 같은 컬러 이전 작업 연결 누락 |

### Major (품질 향상)

| # | Gap | Design Section | Impact |
|---|-----|---------------|--------|
| 4 | Room Database 레이어 | §1.1 data/local/ | AppDatabase, DAO, Entity 전체 미구현. 오프라인 지원 불가 |
| 5 | UseCase 레이어 | §1.1 domain/usecase/ | AnalyzeColorUseCase, SaveJobUseCase, SearchJobUseCase 미구현. 비즈니스 로직이 ViewModel에 직접 존재 |
| 6 | ChatScreen 사진 첨부 | §4.4 | 대화 중 사진 전송 불가 |
| 7 | CameraX 직접 촬영 | §1.2 CameraX | Gallery Picker로 대체됨. PhotoCaptureCard 미구현 |

### Minor (후순위)

| # | Gap | Design Section | Impact |
|---|-----|---------------|--------|
| 8 | 스트리밍 응답 | §3.1 | AI 응답이 일괄 수신 (스트리밍 아님) |
| 9 | Edge Function Rate Limit | §9 보안 | 과도한 API 호출 방어 없음 |
| 10 | Theme Color.kt / Type.kt 분리 | §1.1 ui/theme/ | Theme.kt에 인라인으로 합쳐짐 |

---

## Changed (설계 변경)

| 항목 | Design | Implementation | 판단 |
|------|--------|---------------|------|
| DTO 파일 구조 | 3개 분리 | 2개로 병합 | OK - 간소화 |
| Domain 모델 파일 | 7개 분리 | 3개로 병합 | OK - 관련 모델 그룹핑 |
| 사진 촬영 | CameraX 직접 | Gallery Picker | ⚠️ MVP에서 허용, Phase 2에서 CameraX 추가 |
| DI 모듈 | 3개 분리 | 1개로 병합 | OK - 앱 규모에 적합 |

---

## 우선순위별 액션 플랜

### P0 (Match Rate 90% 달성 필수)
1. **RecipeRepository 구현** + AnalysisViewModel에서 배합 저장 로직 추가
2. **Photo 업로드 연결** - AnalysisScreen에서 촬영 후 Supabase Storage 업로드
3. **ArchiveScreen 완성** - 사진/배합/대화기록/이전작업 표시

### P1 (Clean Architecture)
4. UseCase 레이어 추가 (AnalyzeColorUseCase 등)
5. Room Database 레이어 (로컬 캐시)

### P2 (UX 개선)
6. CameraX 직접 촬영
7. ChatScreen 사진 첨부
8. 스트리밍 응답

---

## 결론

**Match Rate: 80%** - 핵심 플로우(시편 촬영 → 배합 입력 → AI 분석 → 대화)는 동작하지만, 데이터 영속성(배합 저장, 사진 업로드)과 아카이브 상세 기능에 Gap이 존재합니다.

P0 항목 3개를 구현하면 90%+ 달성 가능합니다.
