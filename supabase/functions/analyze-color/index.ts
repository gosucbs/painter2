import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const CLAUDE_API_KEY = Deno.env.get("CLAUDE_API_KEY")!
const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!
const SUPABASE_SERVICE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!

const SYSTEM_PROMPT = `당신은 자동차 보수도장 조색 전문 AI 어시스턴트 "PainterAI"입니다.

## 핵심 역할
STD(표준) 시편과 조색(Sample) 시편의 사진 및 배합 데이터를 분석하여,
어떤 토너를 증량/감량해야 하는지 방향을 제안합니다.

## 3각도 분석 기준 (BYK 색측기 체계)

### 45° (중간각 - 설계각/실제 인지 기준)
- 메탈릭 구조: 입자 크기(Fine/Medium/Large), 밀도, 배열 균일도
- Hue 방향: a*(빨강-초록), b*(노랑-파랑) 축 판단

### 105° (측면각 - 구조 확인 핵심)
- Flop: 각도 변화에 따른 명도 차이
- 암부 깊이: 어두운 영역의 농도
- 입자 영향: 메탈릭이 빛을 반사하지 않는 상태

### 15° (정면각 - 하이라이트)
- 명도 L*: 밝기 수준
- 스파클: 메탈릭 입자의 반짝임

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

## 중요 규칙
- 이 분석은 STD 기준 판독용이며 완벽 일치를 전제하지 않습니다
- 차체 상태, 도장 조건, 조명에 따라 달라질 수 있습니다
- 구체적인 g수 결정은 현장 기술자의 몫입니다 (방향만 제안)
- 불확실한 부분은 "확인 필요"로 명시합니다`

serve(async (req: Request) => {
  try {
    // Verify auth
    const authHeader = req.headers.get("Authorization")
    if (!authHeader) {
      return new Response(JSON.stringify({ error: "인증이 필요합니다" }), {
        status: 401,
        headers: { "Content-Type": "application/json" }
      })
    }

    const supabase = createClient(SUPABASE_URL, SUPABASE_SERVICE_KEY)
    const token = authHeader.replace("Bearer ", "")
    const { data: { user }, error: authError } = await supabase.auth.getUser(token)

    if (authError || !user) {
      return new Response(JSON.stringify({ error: "인증 실패" }), {
        status: 401,
        headers: { "Content-Type": "application/json" }
      })
    }

    const { messages } = await req.json()

    const response = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-api-key": CLAUDE_API_KEY,
        "anthropic-version": "2023-06-01"
      },
      body: JSON.stringify({
        model: "claude-sonnet-4-5-20250514",
        max_tokens: 4096,
        system: SYSTEM_PROMPT,
        messages: messages
      })
    })

    const data = await response.json()

    return new Response(JSON.stringify(data), {
      headers: { "Content-Type": "application/json" }
    })
  } catch (error) {
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { "Content-Type": "application/json" }
    })
  }
})
