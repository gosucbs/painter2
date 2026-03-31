# AI 조색 어시스턴트 - 상세 설계 (Design Document)

> Plan 참조: `docs/01-plan/features/ai-color-matching.plan.md`

---

## 1. 프로젝트 구조

### 1.1 Android 프로젝트 구조 (Kotlin + Jetpack Compose)

```
app/
├── src/main/
│   ├── java/com/painterai/app/
│   │   ├── PainterAIApplication.kt          # Application class
│   │   ├── MainActivity.kt                   # Single Activity
│   │   │
│   │   ├── data/                              # 데이터 레이어
│   │   │   ├── remote/
│   │   │   │   ├── SupabaseClient.kt          # Supabase 초기화
│   │   │   │   ├── ClaudeApiService.kt        # Claude API 호출
│   │   │   │   └── dto/                       # API DTO
│   │   │   │       ├── ClaudeRequest.kt
│   │   │   │       ├── ClaudeResponse.kt
│   │   │   │       └── MessageDto.kt
│   │   │   ├── local/
│   │   │   │   ├── AppDatabase.kt             # Room DB
│   │   │   │   ├── dao/
│   │   │   │   │   ├── JobDao.kt
│   │   │   │   │   └── DraftDao.kt
│   │   │   │   └── entity/
│   │   │   │       ├── JobEntity.kt
│   │   │   │       └── DraftEntity.kt
│   │   │   └── repository/
│   │   │       ├── JobRepository.kt
│   │   │       ├── ConversationRepository.kt
│   │   │       ├── PhotoRepository.kt
│   │   │       └── AuthRepository.kt
│   │   │
│   │   ├── domain/                            # 도메인 레이어
│   │   │   ├── model/
│   │   │   │   ├── Job.kt                     # 작업 모델
│   │   │   │   ├── Recipe.kt                  # 배합 모델
│   │   │   │   ├── Toner.kt                   # 토너 모델
│   │   │   │   ├── Photo.kt                   # 사진 모델
│   │   │   │   ├── Conversation.kt            # 대화 모델
│   │   │   │   ├── Message.kt                 # 메시지 모델
│   │   │   │   └── AnalysisResult.kt          # AI 분석 결과
│   │   │   └── usecase/
│   │   │       ├── AnalyzeColorUseCase.kt     # AI 분석 요청
│   │   │       ├── SaveJobUseCase.kt          # 작업 저장
│   │   │       └── SearchJobUseCase.kt        # 작업 검색
│   │   │
│   │   ├── ui/                                # UI 레이어
│   │   │   ├── navigation/
│   │   │   │   └── NavGraph.kt                # Navigation 설정
│   │   │   ├── theme/
│   │   │   │   ├── Theme.kt
│   │   │   │   ├── Color.kt
│   │   │   │   └── Type.kt
│   │   │   ├── screen/
│   │   │   │   ├── home/
│   │   │   │   │   ├── HomeScreen.kt          # 메인 (작업 목록)
│   │   │   │   │   └── HomeViewModel.kt
│   │   │   │   ├── newjob/
│   │   │   │   │   ├── NewJobScreen.kt        # 새 작업 생성
│   │   │   │   │   └── NewJobViewModel.kt
│   │   │   │   ├── analysis/
│   │   │   │   │   ├── AnalysisScreen.kt      # 시편 촬영+배합+AI분석
│   │   │   │   │   └── AnalysisViewModel.kt
│   │   │   │   ├── chat/
│   │   │   │   │   ├── ChatScreen.kt          # AI 대화
│   │   │   │   │   └── ChatViewModel.kt
│   │   │   │   ├── archive/
│   │   │   │   │   ├── ArchiveScreen.kt       # 작업 아카이브
│   │   │   │   │   └── ArchiveViewModel.kt
│   │   │   │   └── auth/
│   │   │   │       ├── LoginScreen.kt         # 로그인
│   │   │   │       └── AuthViewModel.kt
│   │   │   └── component/
│   │   │       ├── RecipeInputCard.kt         # 배합 입력 컴포넌트
│   │   │       ├── PhotoCaptureCard.kt        # 사진 촬영 컴포넌트
│   │   │       ├── TonerRow.kt                # 토너 행 입력
│   │   │       ├── MessageBubble.kt           # 채팅 메시지 버블
│   │   │       └── JobCard.kt                 # 작업 카드
│   │   │
│   │   └── di/                                # DI (Hilt)
│   │       ├── AppModule.kt
│   │       ├── NetworkModule.kt
│   │       └── DatabaseModule.kt
│   │
│   └── res/
│       ├── values/
│       │   ├── strings.xml
│       │   └── colors.xml
│       └── drawable/
│
├── build.gradle.kts                           # 앱 레벨
└── gradle/
```

### 1.2 주요 라이브러리

| 라이브러리 | 버전 | 용도 |
|-----------|------|------|
| Jetpack Compose BOM | 2024.12 | UI 프레임워크 |
| Navigation Compose | 2.8.x | 화면 이동 |
| Hilt | 2.51 | 의존성 주입 |
| Room | 2.6.x | 로컬 DB |
| CameraX | 1.4.x | 카메라 촬영 |
| Coil | 3.x | 이미지 로딩 |
| Retrofit + OkHttp | 2.11 / 4.12 | HTTP 클라이언트 |
| Supabase Kotlin | 3.x | Supabase SDK |
| Kotlin Serialization | 1.7.x | JSON 직렬화 |
| Kotlin Coroutines | 1.9.x | 비동기 처리 |

---

## 2. 데이터베이스 설계 (Supabase)

### 2.1 ERD

```
┌─────────────┐     ┌──────────────┐     ┌──────────────┐
│   users      │     │    jobs       │     │   recipes     │
├─────────────┤     ├──────────────┤     ├──────────────┤
│ id (PK)     │◄────│ user_id (FK) │     │ id (PK)      │
│ email       │     │ id (PK)      │◄────│ job_id (FK)  │
│ name        │     │ vehicle_model│     │ type (std/    │
│ company     │     │ vehicle_year │     │   sample)    │
│ created_at  │     │ color_code   │     │ toners (jsonb)│
└─────────────┘     │ paint_brand  │     │ total_grams  │
                    │ work_area    │     │ source       │
                    │ result       │     └──────────────┘
                    │ notes        │
                    │ created_at   │     ┌──────────────┐
                    └──────┬───────┘     │   photos      │
                           │             ├──────────────┤
                           ├────────────►│ id (PK)      │
                           │             │ job_id (FK)  │
                           │             │ type         │
                           │             │ storage_path │
                           │             │ angle        │
                           │             │ created_at   │
                           │             └──────────────┘
                           │
                           │             ┌───────────────┐
                           └────────────►│ conversations  │
                                         ├───────────────┤
                                         │ id (PK)       │
                                         │ job_id (FK)   │
                                         │ messages (jsonb)│
                                         │ analysis_summary│
                                         │ created_at    │
                                         │ updated_at    │
                                         └───────────────┘
```

### 2.2 테이블 DDL

```sql
-- 사용자 (Supabase Auth 연동)
CREATE TABLE users (
  id UUID PRIMARY KEY REFERENCES auth.users(id),
  email TEXT NOT NULL,
  name TEXT,
  company TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 작업
CREATE TABLE jobs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id),
  vehicle_model TEXT NOT NULL,
  vehicle_year INT,
  color_code TEXT NOT NULL,
  paint_brand TEXT NOT NULL DEFAULT 'KCC_SUMIX',
  work_area TEXT,
  result TEXT DEFAULT 'in_progress' CHECK (result IN ('in_progress', 'success', 'fail')),
  notes TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_jobs_user ON jobs(user_id);
CREATE INDEX idx_jobs_color ON jobs(color_code);
CREATE INDEX idx_jobs_created ON jobs(created_at DESC);

-- 배합
CREATE TABLE recipes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
  type TEXT NOT NULL CHECK (type IN ('std', 'sample')),
  toners JSONB NOT NULL DEFAULT '[]',
  -- toners 형식: [{"code": "K9001", "grams": 57.40}, {"code": "K203", "grams": 13.94}]
  total_grams DECIMAL(10,2),
  source TEXT, -- 'website', 'spectrophotometer', 'manual'
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_recipes_job ON recipes(job_id);

-- 사진
CREATE TABLE photos (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
  type TEXT NOT NULL CHECK (type IN ('std', 'sample', 'vehicle', 'detail')),
  storage_path TEXT NOT NULL,
  angle TEXT CHECK (angle IN ('15', '45', '105', 'full', NULL)),
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_photos_job ON photos(job_id);

-- AI 대화
CREATE TABLE conversations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
  messages JSONB NOT NULL DEFAULT '[]',
  -- messages 형식: [{"role": "user"|"assistant", "content": "...", "timestamp": "..."}]
  analysis_summary TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_conversations_job ON conversations(job_id);

-- RLS 정책
ALTER TABLE jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE recipes ENABLE ROW LEVEL SECURITY;
ALTER TABLE photos ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can CRUD own jobs" ON jobs
  FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Users can CRUD own recipes" ON recipes
  FOR ALL USING (job_id IN (SELECT id FROM jobs WHERE user_id = auth.uid()));

CREATE POLICY "Users can CRUD own photos" ON photos
  FOR ALL USING (job_id IN (SELECT id FROM jobs WHERE user_id = auth.uid()));

CREATE POLICY "Users can CRUD own conversations" ON conversations
  FOR ALL USING (job_id IN (SELECT id FROM jobs WHERE user_id = auth.uid()));
```

### 2.3 Supabase Storage 구조

```
photos/
└── {user_id}/
    └── {job_id}/
        ├── std_full.jpg
        ├── std_45.jpg
        ├── sample_full.jpg
        ├── sample_45.jpg
        └── vehicle_full.jpg
```

Storage Policy: 본인 폴더만 읽기/쓰기 가능

---

## 3. API 설계

### 3.1 Claude API 연동

#### Supabase Edge Function (API 키 보호)

Claude API 키를 클라이언트에 노출하지 않기 위해 Supabase Edge Function을 프록시로 사용.

**Edge Function: `analyze-color`**

```typescript
// supabase/functions/analyze-color/index.ts
import { serve } from "https://deno.land/std/http/server.ts"

serve(async (req) => {
  const { messages, images } = await req.json()
  
  // Supabase Auth 토큰 검증
  const authHeader = req.headers.get('Authorization')
  // ... 토큰 검증 로직
  
  const response = await fetch('https://api.anthropic.com/v1/messages', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'x-api-key': Deno.env.get('CLAUDE_API_KEY'),
      'anthropic-version': '2023-06-01'
    },
    body: JSON.stringify({
      model: 'claude-sonnet-4-5-20250514',
      max_tokens: 4096,
      system: SYSTEM_PROMPT,
      messages: messages
    })
  })
  
  return new Response(await response.text(), {
    headers: { 'Content-Type': 'application/json' }
  })
})
```

#### 요청 형식

```json
{
  "messages": [
    {
      "role": "user",
      "content": [
        {
          "type": "image",
          "source": {
            "type": "base64",
            "media_type": "image/jpeg",
            "data": "<STD 시편 사진 base64>"
          }
        },
        {
          "type": "image",
          "source": {
            "type": "base64",
            "media_type": "image/jpeg",
            "data": "<조색 시편 사진 base64>"
          }
        },
        {
          "type": "text",
          "text": "## STD 배합 (KCC SUMIX)\nK9001: 57.40g\nK203: 13.94g\n...\n\n## 조색 배합\nK9001: 50.01g\nK200: 9.21g\n...\n\n위 STD와 조색 시편을 3각도(15°/45°/105°) 기준으로 비교 분석해주세요."
        }
      ]
    }
  ]
}
```

### 3.2 Android → Supabase API 호출

| 기능 | 메서드 | 엔드포인트 | 설명 |
|------|--------|-----------|------|
| 로그인 | POST | `/auth/v1/token?grant_type=password` | 이메일/비밀번호 |
| 회원가입 | POST | `/auth/v1/signup` | 이메일/비밀번호 |
| 작업 목록 | GET | `/rest/v1/jobs?order=created_at.desc` | 최근순 |
| 작업 생성 | POST | `/rest/v1/jobs` | 새 작업 |
| 작업 수정 | PATCH | `/rest/v1/jobs?id=eq.{id}` | 결과 업데이트 |
| 배합 저장 | POST | `/rest/v1/recipes` | STD/Sample 배합 |
| 사진 업로드 | POST | `/storage/v1/object/photos/{path}` | 시편 사진 |
| 대화 저장 | POST | `/rest/v1/conversations` | AI 대화 |
| 대화 업데이트 | PATCH | `/rest/v1/conversations?id=eq.{id}` | 메시지 추가 |
| AI 분석 | POST | `/functions/v1/analyze-color` | Edge Function |
| 작업 검색 | GET | `/rest/v1/jobs?color_code=eq.{code}` | 컬러코드 검색 |

---

## 4. 핵심 화면 상세 설계

### 4.1 S1: 메인 화면 (HomeScreen)

```
┌─────────────────────────────────┐
│  PainterAI              🔍 ⚙️  │
├─────────────────────────────────┤
│                                 │
│  최근 작업                       │
│  ┌─────────────────────────┐   │
│  │ 🚗 아이오닉6 T2G 2023   │   │
│  │    본넷/휀다 | 성공 ✅   │   │
│  │    2026-03-11            │   │
│  └─────────────────────────┘   │
│  ┌─────────────────────────┐   │
│  │ 🚗 모델Y PMNG 2025      │   │
│  │    전범퍼/본넷 | 진행중 🔄│   │
│  │    2026-03-20            │   │
│  └─────────────────────────┘   │
│  ┌─────────────────────────┐   │
│  │ 🚗 K9 D9B 2024          │   │
│  │    뒤도어 | 성공 ✅      │   │
│  │    2026-03-25            │   │
│  └─────────────────────────┘   │
│                                 │
│           ...                   │
│                                 │
├─────────────────────────────────┤
│        [ + 새 작업 시작 ]       │
└─────────────────────────────────┘
```

**동작:**
- 작업 목록 최근순 정렬, 무한 스크롤
- 카드 탭 → 해당 작업 AI 대화 화면으로 이동
- 검색 아이콘 → 컬러코드/차량명 검색
- FAB → 새 작업 생성 화면

### 4.2 S2: 새 작업 생성 (NewJobScreen)

```
┌─────────────────────────────────┐
│  ← 새 작업                       │
├─────────────────────────────────┤
│                                 │
│  차량 모델 *                     │
│  ┌─────────────────────────┐   │
│  │ 아이오닉6                │   │
│  └─────────────────────────┘   │
│                                 │
│  연식                           │
│  ┌─────────────────────────┐   │
│  │ 2023                    │   │
│  └─────────────────────────┘   │
│                                 │
│  컬러코드 *                     │
│  ┌─────────────────────────┐   │
│  │ T2G                     │   │
│  └─────────────────────────┘   │
│                                 │
│  도료사                         │
│  ┌─────────────────────────┐   │
│  │ KCC SUMIX          ▼   │   │
│  └─────────────────────────┘   │
│                                 │
│  작업 부위                      │
│  ┌─────────────────────────┐   │
│  │ 본넷, 휀다              │   │
│  └─────────────────────────┘   │
│                                 │
│  메모                           │
│  ┌─────────────────────────┐   │
│  │ 뒤도어 보수도장          │   │
│  └─────────────────────────┘   │
│                                 │
│        [ 작업 시작하기 ]         │
└─────────────────────────────────┘
```

**동작:**
- 차량 모델, 컬러코드는 필수
- 도료사 기본값: KCC SUMIX
- "작업 시작하기" → Supabase에 job 생성 → 분석 화면으로 이동

### 4.3 S3: 시편 분석 화면 (AnalysisScreen) - 핵심

```
┌─────────────────────────────────┐
│  ← T2G 아이오닉6 2023           │
├─────────────────────────────────┤
│                                 │
│  ┌──────────┐ ┌──────────┐    │
│  │  STD     │ │ Sample   │    │
│  │  시편    │ │ 시편     │    │
│  │ [사진]   │ │ [사진]   │    │
│  │          │ │          │    │
│  │ 📷 촬영  │ │ 📷 촬영  │    │
│  └──────────┘ └──────────┘    │
│                                 │
│  ── STD 배합 ──                 │
│  ┌─────────┬────────┐         │
│  │ 토너코드 │  g 수   │         │
│  ├─────────┼────────┤         │
│  │ K9001   │ 57.40  │         │
│  │ K203    │ 13.94  │         │
│  │ K060    │  6.27  │         │
│  │ + 토너 추가        │         │
│  └────────────────────┘        │
│  합계: 99.71g                   │
│                                 │
│  ── 조색 배합 ──                 │
│  ┌─────────┬────────┐         │
│  │ 토너코드 │  g 수   │         │
│  ├─────────┼────────┤         │
│  │ K9001   │ 90.01  │         │
│  │ K200    │  9.21  │         │
│  │ + 토너 추가        │         │
│  └────────────────────┘        │
│  합계: 127.85g                  │
│                                 │
│     [ 🤖 AI 분석 요청 ]         │
│                                 │
├─────────────────────────────────┤
│  AI 분석 결과                    │
│  ─────────────────              │
│  📐 45° (구조): STD 대비 메탈   │
│  릭 입자 밀도 부족. Medium 구    │
│  조 유지하되 K702 미세증량 필요  │
│                                 │
│  🌑 105° (암부): 암부 열림 과   │
│  다. K600 미세감량으로 암부 안   │
│  정화 권장                       │
│                                 │
│  💡 15° (명도): 명도 방향 양호  │
│  스파클 과다 없음                │
│                                 │
│  📋 조정 방향:                   │
│  K702 +2~3g (메탈릭 밀도)       │
│  K600 -1g (암부 안정)           │
│  K913 미세증량 (105° 안정화)    │
│                                 │
│    [ 💬 추가 질문하기 ]          │
│    [ 💾 작업 저장 ]              │
└─────────────────────────────────┘
```

**동작:**
- STD/Sample 사진 나란히 배치
- 촬영 버튼 → CameraX로 촬영 → Supabase Storage 업로드
- 배합 입력: 토너코드(텍스트) + g수(숫자) 행 추가/삭제
- "AI 분석 요청" → Edge Function 호출 → 스트리밍 응답 표시
- "추가 질문하기" → 채팅 화면으로 전환 (기존 컨텍스트 유지)
- "작업 저장" → 결과(성공/실패) 선택 후 저장

### 4.4 S4: AI 대화 화면 (ChatScreen)

```
┌─────────────────────────────────┐
│  ← T2G 아이오닉6 | 💬 AI 대화   │
├─────────────────────────────────┤
│                                 │
│  ┌─────────────────────────┐   │
│  │ 🤖 AI                    │   │
│  │ 45° 분석 결과: 메탈릭    │   │
│  │ 구조는 Medium 중심으로   │   │
│  │ STD와 유사하나, 입자     │   │
│  │ 분포가 다소 불균일...    │   │
│  └─────────────────────────┘   │
│                                 │
│       ┌─────────────────────┐  │
│       │ 👤 사용자            │  │
│       │ 105도 암부를 더      │  │
│       │ 자세히 분석해줘      │  │
│       └─────────────────────┘  │
│                                 │
│  ┌─────────────────────────┐   │
│  │ 🤖 AI                    │   │
│  │ 105° 암부 상세 분석:     │   │
│  │ - 암부 개방도: STD 대비  │   │
│  │   약간 열림 (밝음)       │   │
│  │ - 원인: K600 블랙 비율   │   │
│  │   부족으로 판단          │   │
│  │ ...                      │   │
│  └─────────────────────────┘   │
│                                 │
├─────────────────────────────────┤
│  📷 ┌──────────────────┐ 전송  │
│     │ 메시지 입력...    │       │
│     └──────────────────┘       │
└─────────────────────────────────┘
```

**동작:**
- 채팅 형태 UI, 스크롤 가능
- 사진 첨부 가능 (카메라/갤러리)
- 대화 기록은 Supabase conversations 테이블에 실시간 저장
- 이전 대화 컨텍스트가 Claude API에 포함되어 연속 대화 가능

### 4.5 S5: 작업 아카이브 (ArchiveScreen)

```
┌─────────────────────────────────┐
│  ← T2G 아이오닉6 2023 상세     │
├─────────────────────────────────┤
│                                 │
│  결과: ✅ 성공                  │
│  일자: 2026-03-11               │
│  부위: 본넷, 휀다               │
│  도료사: KCC SUMIX              │
│                                 │
│  ── 사진 ──                     │
│  [STD] [Sample] [차량] [시편2]  │
│                                 │
│  ── STD 배합 ──                 │
│  K9001: 57.40g                  │
│  K203:  13.94g                  │
│  ...                            │
│  합계: 99.71g                   │
│                                 │
│  ── 최종 조색 배합 ──            │
│  K9001: 90.01g                  │
│  K200:   9.21g                  │
│  ...                            │
│  합계: 127.85g                  │
│                                 │
│  ── AI 분석 요약 ──              │
│  구조: Medium 중심, 양호        │
│  암부: K600 조정으로 안정화     │
│  최종: 1차 수정 후 마무리       │
│                                 │
│  [ 💬 대화 기록 보기 ]           │
│  [ 📋 같은 컬러 이전 작업 ]     │
└─────────────────────────────────┘
```

---

## 5. 핵심 도메인 모델 (Kotlin)

### 5.1 Job

```kotlin
data class Job(
    val id: String,
    val userId: String,
    val vehicleModel: String,
    val vehicleYear: Int?,
    val colorCode: String,
    val paintBrand: PaintBrand = PaintBrand.KCC_SUMIX,
    val workArea: String?,
    val result: JobResult = JobResult.IN_PROGRESS,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class PaintBrand(val displayName: String) {
    KCC_SUMIX("KCC 수믹스"),
    // 향후 추가
    // NOROO("노루페인트"),
    // SAMHWA("삼화페인트"),
}

enum class JobResult { IN_PROGRESS, SUCCESS, FAIL }
```

### 5.2 Recipe

```kotlin
data class Recipe(
    val id: String,
    val jobId: String,
    val type: RecipeType,
    val toners: List<Toner>,
    val totalGrams: Double,
    val source: String?
)

enum class RecipeType { STD, SAMPLE }

data class Toner(
    val code: String,    // "K9001", "K203" 등
    val grams: Double    // 그램 수
)
```

### 5.3 Message & Conversation

```kotlin
data class Conversation(
    val id: String,
    val jobId: String,
    val messages: List<Message>,
    val analysisSummary: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class Message(
    val role: MessageRole,
    val content: String,
    val images: List<String>?,  // base64 또는 URL
    val timestamp: Instant
)

enum class MessageRole { USER, ASSISTANT }
```

---

## 6. Claude API 시스템 프롬프트 상세

```kotlin
const val SYSTEM_PROMPT = """
당신은 자동차 보수도장 조색 전문 AI 어시스턴트 "PainterAI"입니다.

## 핵심 역할
STD(표준) 시편과 조색(Sample) 시편의 사진 및 배합 데이터를 분석하여,
어떤 토너를 증량/감량해야 하는지 방향을 제안합니다.

## 3각도 분석 기준 (BYK 색측기 체계)

### 45° (중간각 - 설계각/실제 인지 기준)
- 메탈릭 구조: 입자 크기(Fine/Medium/Large), 밀도, 배열 균일도
- Hue 방향: a*(빨강-초록), b*(노랑-파랑) 축 판단
- 이 각도가 사람이 차량을 볼 때 가장 많이 보는 각도

### 105° (측면각 - 구조 확인 핵심)
- Flop: 각도 변화에 따른 명도 차이
- 암부 깊이: 어두운 영역의 농도
- 입자 영향: 메탈릭이 빛을 반사하지 않는 상태
- ※ 105°를 먼저 확인하여 "구조 문제인지 색상 문제인지" 분리

### 15° (정면각 - 하이라이트)
- 명도 L*: 밝기 수준
- 스파클: 메탈릭 입자의 반짝임
- 페이스 색감: 정면에서의 전체 인상

## 판독 순서 (필수)
1. 먼저 105°로 구조 vs 색상 문제 분리
2. 45°로 메탈릭 구조/Hue 확인
3. 15°로 명도/스파클 확인
4. 배합 비교 → 토너별 역할 분석 → 조정 방향 제안

## KCC SUMIX 주요 토너 역할
- K9001: 백색 (명도 조정의 기본)
- K200/K203: 레드 계열
- K060: 옐로우 계열
- K702: 실버/메탈릭 구조 (미세 입자)
- K907: 블루 계열
- K600: 블랙 (암부 깊이 조정)
- K805: 미세 조정용
- K906: 그린 계열
- K918: 바이올렛/퍼플 계열
- K913: 중입자 실버 (105° 안정화)
- K814: 미들 실버
- K802: 소형 실버

## 출력 형식
분석 결과는 아래 구조로 제공:

### 📐 45° 분석
- STD vs Sample 차이점
- 메탈릭 구조 비교

### 🌑 105° 분석
- 암부/Flop 차이
- 구조적 원인 진단

### 💡 15° 분석
- 명도/스파클 비교

### 📋 종합 판단
- 원인: 구조 문제 / 색상 문제 / 복합
- 핵심 차이 요약

### 🔧 토너 조정 방향
- 증량 필요 토너: [코드] [방향] [이유]
- 감량 필요 토너: [코드] [방향] [이유]
- 주의사항

## 중요 규칙
- 이 분석은 STD 기준 판독용이며 완벽 일치를 전제하지 않습니다
- 차체 상태, 도장 조건, 조명에 따라 달라질 수 있습니다
- 구체적인 g수 결정은 현장 기술자의 몫입니다 (방향만 제안)
- "색이 아니라 구조가 문제"인 경우를 먼저 분리해야 합니다
- 불확실한 부분은 "확인 필요"로 명시합니다
"""
```

---

## 7. 핵심 유즈케이스 시퀀스

### 7.1 시편 비교 분석 플로우

```
사용자          AnalysisVM       PhotoRepo      ClaudeAPI      ConvRepo
  │                │                │              │              │
  │──촬영(STD)────►│                │              │              │
  │                │──업로드───────►│              │              │
  │                │◄──URL─────────│              │              │
  │                │                │              │              │
  │──촬영(Sample)─►│                │              │              │
  │                │──업로드───────►│              │              │
  │                │◄──URL─────────│              │              │
  │                │                │              │              │
  │──배합입력─────►│                │              │              │
  │                │                │              │              │
  │──AI분석요청──►│                │              │              │
  │                │──analyze──────────────────►│              │
  │                │   (사진2장 + 배합 텍스트)    │              │
  │                │◄──스트리밍응답───────────────│              │
  │◄──결과표시─────│                │              │              │
  │                │──대화저장───────────────────────────────►│
  │                │                │              │              │
  │──추가질문─────►│                │              │              │
  │                │──이전컨텍스트+질문──────────►│              │
  │                │◄──응답──────────────────────│              │
  │◄──결과표시─────│                │              │              │
  │                │──대화업데이트──────────────────────────►│
```

### 7.2 작업 저장 플로우

```
사용자          AnalysisVM       JobRepo        RecipeRepo
  │                │                │              │
  │──작업저장─────►│                │              │
  │                │──결과업데이트─►│              │
  │                │──STD배합저장──────────────►│
  │                │──Sample배합저장─────────────►│
  │                │                │              │
  │◄──저장완료─────│                │              │
```

---

## 8. 구현 순서 (Phase 1 MVP)

### Step 1: 프로젝트 초기 설정 (1일)
1. Android 프로젝트 생성 (Kotlin + Compose)
2. build.gradle.kts 의존성 추가
3. Hilt 설정
4. Supabase 프로젝트 생성 및 테이블/RLS 설정
5. Supabase Edge Function 배포 (analyze-color)

### Step 2: 인증 (2일)
1. Supabase Auth 연동
2. LoginScreen / AuthViewModel
3. 로그인 상태 관리 (DataStore)

### Step 3: 작업 CRUD (2일)
1. Job 도메인 모델 + Repository
2. HomeScreen (작업 목록)
3. NewJobScreen (작업 생성)
4. Supabase REST API 연동

### Step 4: 카메라 + 사진 (2일)
1. CameraX 통합
2. 사진 촬영 → Supabase Storage 업로드
3. PhotoCaptureCard 컴포넌트

### Step 5: 배합 입력 (2일)
1. RecipeInputCard 컴포넌트
2. TonerRow 컴포넌트 (동적 추가/삭제)
3. Recipe 모델 + Supabase 저장

### Step 6: Claude AI 연동 (3일)
1. ClaudeApiService (Edge Function 호출)
2. AnalyzeColorUseCase
3. AnalysisScreen (사진 + 배합 + AI 결과 통합)
4. 스트리밍 응답 처리

### Step 7: AI 대화 (3일)
1. ChatScreen + ChatViewModel
2. MessageBubble 컴포넌트
3. 대화 기록 Supabase 저장/로드
4. 이전 컨텍스트 포함 연속 대화

### Step 8: 작업 아카이브 (2일)
1. ArchiveScreen (작업 상세 뷰)
2. 성공/실패 태그
3. 같은 컬러코드 이전 작업 연결

### Step 9: 마무리 (2일)
1. 에러 처리/로딩 상태
2. 오프라인 상태 처리
3. UI 폴리싱
4. 테스트

---

## 9. 보안 설계

| 위협 | 대응 |
|------|------|
| Claude API 키 노출 | Supabase Edge Function으로 서버사이드 보관 |
| 사진 데이터 유출 | Supabase Storage RLS (본인만 접근) |
| 인증 토큰 탈취 | Supabase JWT + Refresh Token |
| SQL Injection | Supabase REST API (파라미터 바인딩) |
| 과도한 API 호출 | Edge Function에 rate limit 설정 |

---

## 10. 에러 처리 전략

| 상황 | UI 표시 | 처리 |
|------|---------|------|
| 네트워크 없음 | "인터넷 연결을 확인해주세요" | 로컬 캐시 표시, AI 기능 비활성 |
| Claude API 오류 | "AI 분석 중 오류가 발생했습니다. 다시 시도해주세요" | 재시도 버튼 |
| 사진 업로드 실패 | "사진 업로드에 실패했습니다" | 로컬 저장 후 재시도 |
| 인증 만료 | 자동 토큰 갱신 → 실패 시 로그인 화면 | Refresh Token |
| Supabase 서버 오류 | "서버 오류. 잠시 후 다시 시도해주세요" | 재시도 + 로컬 캐시 |
