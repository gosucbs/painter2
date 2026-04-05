import { createClient } from "@supabase/supabase-js";

const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL!;
const supabaseAnonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!;

export const supabase = createClient(supabaseUrl, supabaseAnonKey);

export type Job = {
  id: string;
  user_id: string | null;
  vehicle_model: string;
  vehicle_year: number | null;
  color_code: string;
  paint_brand: string;
  work_area: string | null;
  result: "in_progress" | "success" | "fail";
  notes: string | null;
  created_at: string;
  updated_at: string;
};

export type Conversation = {
  id: string;
  job_id: string;
  messages: Message[];
  analysis_summary: string | null;
  created_at: string;
  updated_at: string;
};

export type Message = {
  role: "user" | "assistant";
  content: string;
  timestamp: string;
};
