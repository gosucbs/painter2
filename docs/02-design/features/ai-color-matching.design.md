# AI 조색 어시스턴트 - 상세 설계 (Design Document)

> Plan 참조: `docs/01-plan/features/ai-color-matching.plan.md`

---

## 1. 프로젝트 구조

### 1.1 Android 프로젝트 구조 (Kotlin + Jetpack Compose)

> **구현 참고**: MVP 단계에서 UseCase 레이어와 Room DB는 제외. 비즈니스 로직은 ViewModel에서 직접 처리.

```
app/
├── src/main/
│   ├── java/com/painterai/app/
│   │   ├── PainterAIApplication.kt          # Application class
│   │   ├── MainActivity.kt                   # Single Activity
│   │   │
│   │   ├── data/                              # 데이터 레이어
│   │   │   ├── remote/
│   │   │   │   ├── SupabaseClient.kt          # Supabase 초기화 (object 싱글톤)
│   │   │   │   ├── ClaudeApiService.kt        # Claude API 호출 (OkHttp 직접 사용)
│   │   │   │   └── dto/                       # API DTO (2파일로 통합)
│   │   │   │       ├── ClaudeMessages.kt      # AnalyzeRequest, ChatMessage, AnalyzeResponse,
│   │   │   │       │                          #   ResponseContent, ContentBlocks
│   │   │   │       └── SupabaseDto.kt         # Supabase REST API DTO
│   │   │   └── repository/
│   │   │       ├── JobRepository.kt
│   │   │       ├── ConversationRepository.kt
│   │   │       ├── PhotoRepository.kt
│   │   │       └── AuthRepository.kt
│   │   │
│   │   ├── domain/                            # 도메인 레이어
│   │   │   └── model/                         # 모델 3파일로 통합
│   │   │       ├── Job.kt                     # Job + PaintBrand + JobResult 포함
│   │   │       ├── Conversation.kt            # Conversation + Message + MessageRole 포함
│   │   │       └── Photo.kt                   # Photo 모델
│   │   │       # (Phase 2: Recipe.kt, Toner.kt, AnalysisResult.kt 분리 예정)
│   │   │
│   │   ├── ui/                                # UI 레이어
│   │   │   ├── navigation/
│   │   │   │   └── NavGraph.kt                # Navigation 설정
│   │   │   ├── theme/
│   │   │   │   └── Theme.kt                   # 색상/타이포 통합 (Color, Type 인라인 정의)
│   │   │   ├── screen/
│   │   │   │   ├── home/
│   │   │   │   │   ├── HomeScreen.kt          # 메인 (작업 목록)
│   │   │   │   │   └── HomeViewModel.kt
│   │   │   │   ├── select/
│   │   │   │   │   └── SelectModeScreen.kt    # 도료사 선택 모드 (신규)
│   │   │   │   ├── newjob/
│   │   │   │   │   ├── NewJobScreen.kt        # 새 작업 생성
│   │   │   │   │   └── NewJobViewModel.kt
│   │   │   │   ├── analysis/
│   │   │   │   │   ├── AnalysisScreen.kt      # 사진 3장 촬영+AI분석
│   │   │   │   │   └── AnalysisViewModel.kt
│   │   │   │   ├── chat/
│   │   │   │   │   ├── ChatScreen.kt          # AI 대화
│   │   │   │   │   └── ChatViewModel.kt
│   │   │   │   ├── archive/
│   │   │   │   │   ├── ArchiveScreen.kt       # 작업 아카이브 (기본 정보 표시)
│   │   │   │   │   └── ArchiveViewModel.kt
│   │   │   │   └── auth/
│   │   │   │       ├── LoginScreen.kt         # 로그인
│   │   │   │       └── AuthViewModel.kt
│   │   │   └── component/
│   │   │       ├── RecipeInputCard.kt         # 배합 수동 입력 컴포넌트 (미사용 예약)
│   │   │       ├── PhotoCaptureCard.kt        # 사진 촬영 컴포넌트
│   │   │       ├── TonerRow.kt                # 토너 행 입력
│   │   │       ├── MessageBubble.kt           # 채팅 메시지 버블 (마크다운 렌더링)
│   │   │       └── JobCard.kt                 # 작업 카드
│   │   │
│   │   └── di/                                # DI (Hilt)
│   │       ├── AppModule.kt
│   │       └── NetworkModule.kt
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
| CameraX | 1.4.x | 카메라 촬영 |
| Coil | 3.x | 이미지 로딩 |
| OkHttp | 4.12 | HTTP 클라이언트 (Retrofit 미사용) |
| Supabase Kotlin | 3.x | Supabase SDK |
| Kotlin Serialization | 1.7.x | JSON 직렬화 |
| Kotlin Coroutines | 1.9.x | 비동기 처리 |

> **참고**: Room DB는 MVP에서 제외. Phase 2(오프라인 지원)에서 추가 예정.

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

사용 모델: `claude-sonnet-4-6`

```typescript
// supabase/functions/analyze-color/index.ts
import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

serve(async (req) => {
  // Supabase Auth JWT 토큰 검증
  const authHeader = req.headers.get('Authorization')
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return new Response(JSON.stringify({ error: "Unauthorized" }), {
      status: 401,
      headers: { "Content-Type": "application/json" }
    })
  }

  const supabaseClient = createClient(
    Deno.env.get('SUPABASE_URL')!,
    Deno.env.get('SUPABASE_ANON_KEY')!,
    { global: { headers: { Authorization: authHeader } } }
  )

  const { data: { user }, error: authError } = await supabaseClient.auth.getUser()
  if (authError || !user) {
    return new Response(JSON.stringify({ error: "Unauthorized" }), {
      status: 401,
      headers: { "Content-Type": "application/json" }
    })
  }

  const { messages } = await req.json()

  const response = await fetch('https://api.anthropic.com/v1/messages', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'x-api-key': Deno.env.get('CLAUDE_API_KEY'),
      'anthropic-version': '2023-06-01'
    },
    body: JSON.stringify({
      model: 'claude-sonnet-4-6',
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

#### 요청 형식 (사진 3장 + 작업 정보 텍스트)

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
            "data": "<차량사진 base64 - 목표 색상>"
          }
        },
        {
          "type": "image",
          "source": {
            "type": "base64",
            "media_type": "image/jpeg",
            "data": "<조색시편사진 base64 - 현재 조색 결과>"
          }
        },
        {
          "type": "image",
          "source": {
            "type": "base64",
            "media_type": "image/jpeg",
            "data": "<조색시편배합 base64 - 배합표 사진 OCR용>"
          }
        },
        {
          "type": "text",
          "text": "## 작업 정보\n차량: 아이오닉6 2023년식\n컬러코드: T2G\n도료사: KCC 수믹스\n\n## 첨부 사진 (순서대로)\n1. 차량사진 (목표 색상)\n2. 조색시편사진 (현재 조색 결과)\n3. 조색시편배합 (현재 배합표)\n\n조색 시편이 차량 색상에 더 가까워지려면 어떻게 해야 하는지 분석해주세요."
        }
      ]
    }
  ],
  "job_id": "uuid-here"
}
```

**HTTP 헤더:**
```
Authorization: Bearer <Supabase JWT token>
apikey: <Supabase anon key>
Content-Type: application/json
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

> **구현 변경사항**: 배합 수동 입력 방식 대신 사진 3장 OCR 방식으로 변경.
> 배합표 사진을 찍으면 AI가 직접 읽어서 분석 → 도장사가 타이핑 없이 사용 가능.

```
┌─────────────────────────────────┐
│  ← T2G 아이오닉6 2023           │
├─────────────────────────────────┤
│                                 │
│  ┌─────────┐┌─────────┐┌──────┐│
│  │차량사진  ││조색시편  ││조색  ││
│  │         ││사진      ││시편  ││
│  │ [사진]  ││ [사진]   ││배합  ││
│  │         ││          ││[사진]││
│  │ 📷 촬영 ││ 📷 촬영  ││📷촬영││
│  └─────────┘└─────────┘└──────┘│
│                                 │
│     [ 🤖 AI 분석 요청 ]         │
│   (3장 모두 있어야 활성화됨)     │
│                                 │
├─────────────────────────────────┤
│  AI 분석 결과                    │
│  ─────────────────              │
│  📐 45° (구조): 메탈릭          │
│  입자 밀도 부족. K702 미세증량   │
│  필요                           │
│                                 │
│  🌑 105° (암부): 암부 열림 과   │
│  다. K600 미세감량 권장          │
│                                 │
│  💡 15° (명도): 명도 방향 양호  │
│                                 │
│  📋 토너 조정 방향:              │
│  K702 +2~3g (메탈릭 밀도)       │
│  K600 -1g (암부 안정)           │
│                                 │
│  [ 💬 추가 질문 ] [ 성공 저장 ] │
│                  [ 실패 저장 ]  │
└─────────────────────────────────┘
```

**사진 3장 구성:**
1. 차량사진: 목표 색상 (실제 차체)
2. 조색시편사진: 현재 조색한 결과물
3. 조색시편배합: 현재 배합표 (토너코드 + g수가 보이는 사진)

**동작:**
- 사진 슬롯 3개 나란히 배치 (카메라 촬영 또는 갤러리 선택)
- 촬영한 사진은 자동으로 갤러리(DCIM/PainterAI)에 저장
- 사진 전송 전 1024px로 리사이즈 + JPEG 70% 압축 (API 전송 최적화)
- 3장 모두 선택 시 "AI 분석 요청" 버튼 활성화
- "AI 분석 요청" → Edge Function 호출 (사진 3장 base64 + 작업 정보 텍스트)
- AI가 배합표 사진 OCR로 토너코드/g수 직접 읽어 분석
- 결과 마크다운 렌더링 (##/###/bullet/bold 지원)
- "추가 질문하기" → 채팅 화면으로 전환 (기존 컨텍스트 유지)
- "성공 저장" / "실패 저장" → 결과 선택 후 Supabase 업데이트
- 사진은 Supabase Storage에 타입별(vehicle/sample/recipe)로 업로드

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

> **MVP 구현**: 기본 작업 정보 + AI 분석 요약만 표시.
> Phase 2에서 사진/배합 상세 뷰, 같은 컬러 이전 작업 연결 기능 추가 예정.

```
┌─────────────────────────────────┐
│  ← 작업 상세                    │
├─────────────────────────────────┤
│                                 │
│  ┌─────────────────────────┐   │
│  │  아이오닉6 T2G          │   │
│  │  연식: 2023             │   │
│  │  부위: 본넷, 휀다       │   │
│  │  도료사: KCC 수믹스     │   │
│  │  결과: 성공             │   │
│  └─────────────────────────┘   │
│                                 │
│  ┌─────────────────────────┐   │
│  │  AI 분석 요약           │   │
│  │  구조: Medium 중심, 양호│   │
│  │  암부: K600 조정으로    │   │
│  │  안정화                 │   │
│  └─────────────────────────┘   │
│                                 │
└─────────────────────────────────┘
```

**MVP 표시 항목:**
- 차량 모델, 컬러코드, 연식, 작업 부위
- 도료사, 작업 결과
- AI 분석 요약 (마지막 AI 메시지)

**Phase 2 예정:**
- 사진 갤러리 (차량/시편/배합표 사진)
- 배합 상세 (토너별 g수)
- 같은 컬러코드 이전 작업 연결
- 대화 기록 전체 보기

---

## 5. 핵심 도메인 모델 (Kotlin)

> **구현 참고**: MVP에서 모델 파일을 3개로 통합. Job.kt에 Job+PaintBrand+JobResult 포함.

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
    NOROO_WATERQ("노루 워터큐"),
}

enum class JobResult(val displayName: String) {
    IN_PROGRESS("진행중"),
    SUCCESS("성공"),
    FAIL("실패")
}
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

> **구현 변경**: 시스템 프롬프트는 Edge Function(`supabase/functions/analyze-color/index.ts`)에 위치.
> Android 코드에는 없고 서버사이드에서만 관리.
> KCC SUMIX(K코드) + 노루 워터큐(Q코드) 두 도료사 각 130종+ 토너 전체 목록 포함.

**지원 도료사:**
- KCC SUMIX (K코드): K100~K918, 총 약 65종
- 노루 워터큐 (Q코드): Q-0010~Q-9890, 총 약 72종

**분석 방식:**
- 사진 3장 입력: 차량사진 / 조색시편사진 / 조색시편배합 (배합표 OCR)
- 3각도 분석: 45° / 105° / 15° (BYK 색측기 체계 기반)
- 판독 순서: 105° → 45° → 15° → 배합표 읽기 → 조정 방향 제안

**출력 형식:**
```
### 📐 45° 분석
### 🌑 105° 분석
### 💡 15° 분석
### 📋 종합 판단
### 🔧 토너 조정 방향 (KCC SUMIX 또는 노루 워터큐 코드 기준)
### ⚠️ 주의사항
```

**중요 규칙:**
- 사진 기반 추정이며 색측기 수치를 대체하지 않음
- 구체적인 g수 결정은 현장 기술자의 최종 판단
- 배합표 사진이 불명확하면 판독 불가 명시
- 토너 조정은 반드시 해당 도료사 토너코드로 답변

---

## 7. 핵심 플로우 시퀀스

> **MVP 구현 참고**: UseCase 레이어 없음. AnalysisViewModel이 직접 Repository/Service 호출.

### 7.1 시편 비교 분석 플로우 (사진 3장 OCR 방식)

```
사용자          AnalysisVM       PhotoRepo      ClaudeAPI      ConvRepo
  │                │                │              │              │
  │──촬영(차량)───►│                │              │              │
  │──촬영(시편)───►│                │              │              │
  │──촬영(배합표)─►│                │              │              │
  │                │                │              │              │
  │──AI분석요청──►│                │              │              │
  │                │──업로드(3장)──►│              │              │
  │                │──analyze──────────────────►│              │
  │                │   (사진3장 base64 + 작업정보 텍스트)        │
  │                │   AI가 배합표 사진 OCR로 토너/g수 직접 읽음 │
  │                │◄──응답──────────────────────│              │
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
2. build.gradle.kts 의존성 추가 (Room 제외)
3. Hilt 설정
4. Supabase 프로젝트 생성 및 테이블/RLS 설정
5. Supabase Edge Function 배포 (analyze-color, JWT 인증 포함)

### Step 2: 인증 (2일)
1. Supabase Auth 연동
2. LoginScreen / AuthViewModel
3. 로그인 상태 관리 (DataStore)

### Step 3: 작업 CRUD (2일)
1. Job 도메인 모델 + Repository
2. HomeScreen (작업 목록, 삭제 기능 포함)
3. NewJobScreen (작업 생성, 도료사 선택: KCC SUMIX / 노루 워터큐)
4. SelectModeScreen (도료사 선택 모드)
5. Supabase REST API 연동

### Step 4: 카메라 + 사진 (2일)
1. 카메라/갤러리 통합 (ActivityResultContracts)
2. 사진 촬영 → 갤러리 자동 저장 → Supabase Storage 업로드
3. 이미지 리사이즈/압축 (1024px, JPEG 70%)

### Step 5: Claude AI 연동 (3일)
1. ClaudeApiService (OkHttp로 Edge Function 호출, JWT 토큰 포함)
2. AnalysisScreen (사진 3장 슬롯: 차량/시편/배합표)
3. AnalysisViewModel (사진 → base64 → AI 분석 → 결과 저장)
4. 결과 마크다운 렌더링

### Step 6: AI 대화 (3일)
1. ChatScreen + ChatViewModel
2. MessageBubble 컴포넌트 (마크다운 렌더링)
3. 대화 기록 Supabase 저장/로드
4. 이전 컨텍스트 포함 연속 대화

### Step 7: 작업 아카이브 (1일)
1. ArchiveScreen (작업 기본 정보 + AI 분석 요약)
2. 성공/실패 표시

### Step 8: 마무리 (2일)
1. 한국어 에러 메시지 처리
2. UI 폴리싱
3. 테스트

### Phase 2 예정 (MVP 이후)
- Room DB 추가 (오프라인 지원)
- ArchiveScreen 사진/배합 상세 뷰
- 같은 컬러코드 이전 작업 연결
- 수동 배합 입력 (RecipeInputCard 활성화)

---

## 9. 보안 설계

| 위협 | 대응 | 상태 |
|------|------|------|
| Claude API 키 노출 | Supabase Edge Function으로 서버사이드 보관 | 구현됨 |
| 미인증 Edge Function 호출 | JWT 토큰 검증 (supabaseClient.auth.getUser()) | 구현됨 |
| 사진 데이터 유출 | Supabase Storage RLS (본인만 접근) | 구현됨 |
| 인증 토큰 탈취 | Supabase JWT + Refresh Token | 구현됨 |
| SQL Injection | Supabase REST API (파라미터 바인딩) | 구현됨 |
| 과도한 API 호출 | Edge Function에 rate limit 설정 | Phase 2 예정 |

---

## 10. 에러 처리 전략

| 상황 | UI 표시 | 처리 |
|------|---------|------|
| 네트워크 없음 | "인터넷 연결을 확인해주세요" | 로컬 캐시 표시, AI 기능 비활성 |
| Claude API 오류 | "AI 분석 중 오류가 발생했습니다. 다시 시도해주세요" | 재시도 버튼 |
| 사진 업로드 실패 | "사진 업로드에 실패했습니다" | 로컬 저장 후 재시도 |
| 인증 만료 | 자동 토큰 갱신 → 실패 시 로그인 화면 | Refresh Token |
| Supabase 서버 오류 | "서버 오류. 잠시 후 다시 시도해주세요" | 재시도 + 로컬 캐시 |
