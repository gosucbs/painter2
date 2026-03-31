# PainterAI 웹 대시보드 Plan

## 개요
Supabase에 저장된 조색 작업 데이터를 웹에서 조회할 수 있는 대시보드

## 기술 스택
- Next.js 15 (App Router)
- Supabase JS SDK
- Tailwind CSS
- shadcn/ui
- TypeScript

## 주요 화면

### 1. 대시보드 메인 (`/`)
- 전체 작업 수, 성공률, 최근 작업 요약
- 통계 카드 (총 작업, 성공, 실패, 진행중)

### 2. 작업 목록 (`/jobs`)
- 테이블 형태로 전체 작업 목록
- 컬러코드/차량 모델 검색
- 결과별 필터 (성공/실패/진행중)
- 정렬 (최신순/차량명순)

### 3. 작업 상세 (`/jobs/[id]`)
- 작업 정보 (차량, 컬러코드, 연식, 부위, 결과)
- AI 분석 결과 (대화 내용)
- 배합 데이터
- 사진 (Supabase Storage)

### 4. 컬러코드 검색 (`/colors`)
- 컬러코드별 작업 이력
- 같은 컬러코드 이전 작업 비교

## 데이터 소스
기존 Supabase 테이블 그대로 사용:
- `jobs` - 작업 목록
- `conversations` - AI 대화/분석 결과
- `recipes` - 배합 데이터
- `photos` - 사진 경로

## 프로젝트 위치
`C:\Users\Administrator\workspace\painter2\web\`
