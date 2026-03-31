"use client";

import { useEffect, useState } from "react";
import { supabase, Job } from "@/lib/supabase";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import Link from "next/link";

export default function ColorsPage() {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function fetchJobs() {
      const { data } = await supabase
        .from("jobs")
        .select("*")
        .order("color_code", { ascending: true });
      setJobs(data || []);
      setLoading(false);
    }
    fetchJobs();
  }, []);

  // Group by color code
  const grouped = jobs.reduce<Record<string, Job[]>>((acc, job) => {
    const code = job.color_code.toUpperCase();
    if (!acc[code]) acc[code] = [];
    acc[code].push(job);
    return acc;
  }, {});

  const filteredCodes = Object.keys(grouped).filter(
    (code) =>
      search === "" || code.toLowerCase().includes(search.toLowerCase())
  );

  if (loading) return <div className="text-center py-20">로딩 중...</div>;

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-center">컬러코드 검색</h1>

      <Input
        placeholder="컬러코드 검색 (예: T2G, D9B)..."
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        className="max-w-xs"
      />

      {filteredCodes.length === 0 ? (
        <p className="text-gray-500 text-center py-8">검색 결과가 없습니다</p>
      ) : (
        <div className="grid gap-4">
          {filteredCodes.map((code) => {
            const codeJobs = grouped[code];
            const successCount = codeJobs.filter(
              (j) => j.result === "success"
            ).length;
            return (
              <Card key={code}>
                <CardHeader>
                  <CardTitle className="flex items-center justify-between">
                    <span className="font-mono text-xl">{code}</span>
                    <span className="text-sm font-normal text-gray-500">
                      {codeJobs.length}건 (성공 {successCount}건)
                    </span>
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-2">
                    {codeJobs.map((job) => (
                      <div
                        key={job.id}
                        className="flex items-center justify-between p-3 rounded border hover:bg-gray-50 transition"
                      >
                        <Link href={`/jobs/${job.id}`} className="flex-1">
                          <span className="font-medium">
                            {job.vehicle_model}
                          </span>
                          <span className="text-sm text-gray-500 ml-2">
                            {job.vehicle_year && `${job.vehicle_year}년식`}
                            {job.work_area && ` | ${job.work_area}`}
                          </span>
                        </Link>
                        <div className="flex items-center gap-3">
                          <ResultBadge result={job.result} />
                          <span className="text-xs text-gray-400">
                            {new Date(job.created_at).toLocaleDateString("ko-KR")}
                          </span>
                          <Link
                            href={`/jobs/${job.id}`}
                            className="text-sm text-blue-600 hover:underline"
                          >
                            수정
                          </Link>
                          <button
                            onClick={async () => {
                              if (!confirm(`"${job.vehicle_model} ${job.color_code}" 작업을 삭제하시겠습니까?`)) return;
                              await supabase.from("jobs").delete().eq("id", job.id);
                              setJobs((prev) => prev.filter((j) => j.id !== job.id));
                            }}
                            className="text-sm text-red-600 hover:underline"
                          >
                            삭제
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            );
          })}
        </div>
      )}
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
