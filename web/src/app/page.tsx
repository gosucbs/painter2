"use client";

import { useEffect, useState } from "react";
import { supabase, Job } from "@/lib/supabase";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import Link from "next/link";

export default function Dashboard() {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchJobs() {
      const { data } = await supabase
        .from("jobs")
        .select("*")
        .order("created_at", { ascending: false });
      setJobs(data || []);
      setLoading(false);
    }
    fetchJobs();
  }, []);

  const total = jobs.length;
  const success = jobs.filter((j) => j.result === "success").length;
  const fail = jobs.filter((j) => j.result === "fail").length;
  const inProgress = jobs.filter((j) => j.result === "in_progress").length;
  const successRate = total > 0 ? Math.round((success / total) * 100) : 0;

  if (loading) return <div className="text-center py-20">로딩 중...</div>;

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-bold">대시보드</h1>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-gray-500">전체 작업</CardTitle>
          </CardHeader>
          <CardContent><div className="text-3xl font-bold">{total}</div></CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-green-600">성공</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-green-600">{success}</div>
            <div className="text-sm text-gray-500">성공률 {successRate}%</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-red-600">실패</CardTitle>
          </CardHeader>
          <CardContent><div className="text-3xl font-bold text-red-600">{fail}</div></CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm text-yellow-600">진행중</CardTitle>
          </CardHeader>
          <CardContent><div className="text-3xl font-bold text-yellow-600">{inProgress}</div></CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader><CardTitle>최근 작업</CardTitle></CardHeader>
        <CardContent>
          {jobs.length === 0 ? (
            <p className="text-gray-500 text-center py-8">아직 작업이 없습니다</p>
          ) : (
            <div className="space-y-3">
              {jobs.slice(0, 10).map((job) => (
                <Link key={job.id} href={`/jobs/${job.id}`}
                  className="block p-4 rounded-lg border hover:bg-gray-50 transition">
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="font-medium">{job.vehicle_model} {job.color_code}</div>
                      <div className="text-sm text-gray-500">
                        {job.vehicle_year && `${job.vehicle_year}년식`}
                        {job.work_area && ` | ${job.work_area}`}
                      </div>
                      <div className="text-xs text-gray-400 mt-1">
                        {new Date(job.created_at).toLocaleDateString("ko-KR")}
                      </div>
                    </div>
                    <ResultBadge result={job.result} />
                  </div>
                </Link>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function ResultBadge({ result }: { result: string }) {
  switch (result) {
    case "success": return <Badge className="bg-green-100 text-green-700">성공</Badge>;
    case "fail": return <Badge className="bg-red-100 text-red-700">실패</Badge>;
    default: return <Badge className="bg-yellow-100 text-yellow-700">진행중</Badge>;
  }
}
