-- PainterAI Supabase 테이블 생성 SQL
-- SQL Editor에 복사해서 실행하세요

-- 1. 사용자
CREATE TABLE users (
  id UUID PRIMARY KEY REFERENCES auth.users(id),
  email TEXT NOT NULL,
  name TEXT,
  company TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. 작업
CREATE TABLE jobs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id),
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

-- 3. 배합
CREATE TABLE recipes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
  type TEXT NOT NULL CHECK (type IN ('std', 'sample')),
  toners JSONB NOT NULL DEFAULT '[]',
  total_grams DECIMAL(10,2),
  source TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_recipes_job ON recipes(job_id);

-- 4. 사진
CREATE TABLE photos (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
  type TEXT NOT NULL CHECK (type IN ('std', 'sample', 'vehicle', 'detail')),
  storage_path TEXT NOT NULL,
  angle TEXT CHECK (angle IN ('15', '45', '105', 'full', NULL)),
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_photos_job ON photos(job_id);

-- 5. AI 대화
CREATE TABLE conversations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
  messages JSONB NOT NULL DEFAULT '[]',
  analysis_summary TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_conversations_job ON conversations(job_id);

-- 6. RLS (Row Level Security) 활성화
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE recipes ENABLE ROW LEVEL SECURITY;
ALTER TABLE photos ENABLE ROW LEVEL SECURITY;
ALTER TABLE conversations ENABLE ROW LEVEL SECURITY;

-- 7. RLS 정책
CREATE POLICY "Users can read own profile" ON users
  FOR SELECT USING (auth.uid() = id);

CREATE POLICY "Users can update own profile" ON users
  FOR UPDATE USING (auth.uid() = id);

CREATE POLICY "Users can insert own profile" ON users
  FOR INSERT WITH CHECK (auth.uid() = id);

CREATE POLICY "Allow all access to jobs" ON jobs
  FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all access to recipes" ON recipes
  FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all access to photos" ON photos
  FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all access to conversations" ON conversations
  FOR ALL USING (true) WITH CHECK (true);

-- 8. 회원가입 시 자동으로 users 테이블에 추가하는 트리거
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO public.users (id, email)
  VALUES (NEW.id, NEW.email);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
