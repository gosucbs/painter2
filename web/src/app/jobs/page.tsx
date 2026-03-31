"use client";

import { useEffect, useState } from "react";
import { supabase, Job } from "@/lib/supabase";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import Link from "next/link";

export default function JobsPage() {
  const [jobs, setJobs] = useState<Job[]>([]);
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState<string>("all");
  const [loading, setLoading] = useState(true);
  const [sortKey, setSortKey] = useState<string>("created_at");
  const [sortAsc, setSortAsc] = useState(false);

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

  function toggleSort(key: string) {
    if (sortKey === key) {
      setSortAsc(!sortAsc);
    } else {
      setSortKey(key);
      setSortAsc(true);
    }
  }

  const filtered = jobs
    .filter((job) => {
      const matchSearch =
        search === "" ||
        job.color_code.toLowerCase().includes(search.toLowerCase()) ||
        job.vehicle_model.toLowerCase().includes(search.toLowerCase());
      const matchFilter = filter === "all" || job.result === filter;
      return matchSearch && matchFilter;
    })
    .sort((a, b) => {
      const valA = (a as Record<string, unknown>)[sortKey];
      const valB = (b as Record<string, unknown>)[sortKey];
      const strA = String(valA ?? "");
      const strB = String(valB ?? "");
      const cmp = strA.localeCompare(strB);
      return sortAsc ? cmp : -cmp;
    });

  if (loading) return <div className="text-center py-20">로딩 중...</div>;

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-center">작업 목록</h1>

      <div className="flex gap-3 items-center">
        <Input
          placeholder="컬러코드 또는 차량명 검색..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="max-w-xs"
        />
        <div className="flex gap-2">
          {["all", "success", "fail", "in_progress"].map((f) => (
            <button
              key={f}
              onClick={() => setFilter(f)}
              className={`px-3 py-1 rounded-full text-sm border ${
                filter === f
                  ? "bg-blue-600 text-white border-blue-600"
                  : "hover:bg-gray-100"
              }`}
            >
              {f === "all"
                ? "전체"
                : f === "success"
                ? "성공"
                : f === "fail"
                ? "실패"
                : "진행중"}
            </button>
          ))}
        </div>
      </div>

      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <SortHead label="차량" field="vehicle_model" sortKey={sortKey} sortAsc={sortAsc} onSort={toggleSort} />
                <SortHead label="컬러코드" field="color_code" sortKey={sortKey} sortAsc={sortAsc} onSort={toggleSort} />
                <SortHead label="연식" field="vehicle_year" sortKey={sortKey} sortAsc={sortAsc} onSort={toggleSort} />
                <SortHead label="부위" field="work_area" sortKey={sortKey} sortAsc={sortAsc} onSort={toggleSort} />
                <SortHead label="도료사" field="paint_brand" sortKey={sortKey} sortAsc={sortAsc} onSort={toggleSort} />
                <SortHead label="결과" field="result" sortKey={sortKey} sortAsc={sortAsc} onSort={toggleSort} />
                <SortHead label="날짜" field="created_at" sortKey={sortKey} sortAsc={sortAsc} onSort={toggleSort} />
                <TableHead className="text-right">관리</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} className="text-center py-8 text-gray-500">
                    작업이 없습니다
                  </TableCell>
                </TableRow>
              ) : (
                filtered.map((job) => (
                  <TableRow key={job.id} className="cursor-pointer hover:bg-gray-50">
                    <TableCell>
                      <Link href={`/jobs/${job.id}`} className="font-medium hover:text-blue-600">
                        {job.vehicle_model}
                      </Link>
                    </TableCell>
                    <TableCell className="font-mono">{job.color_code}</TableCell>
                    <TableCell>{job.vehicle_year || "-"}</TableCell>
                    <TableCell>{job.work_area || "-"}</TableCell>
                    <TableCell>{job.paint_brand}</TableCell>
                    <TableCell>
                      <ResultBadge result={job.result} />
                    </TableCell>
                    <TableCell className="text-sm text-gray-500">
                      {new Date(job.created_at).toLocaleDateString("ko-KR")}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex gap-2 justify-end">
                        <Link
                          href={`/jobs/${job.id}`}
                          className="text-sm text-blue-600 hover:underline"
                        >
                          수정
                        </Link>
                        <button
                          onClick={async (e) => {
                            e.stopPropagation();
                            if (!confirm(`"${job.vehicle_model} ${job.color_code}" 작업을 삭제하시겠습니까?`)) return;
                            await supabase.from("jobs").delete().eq("id", job.id);
                            setJobs((prev) => prev.filter((j) => j.id !== job.id));
                          }}
                          className="text-sm text-red-600 hover:underline"
                        >
                          삭제
                        </button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}

function SortHead({
  label, field, sortKey, sortAsc, onSort,
}: {
  label: string; field: string; sortKey: string; sortAsc: boolean; onSort: (f: string) => void;
}) {
  const active = sortKey === field;
  return (
    <TableHead>
      <button
        onClick={() => onSort(field)}
        className="flex items-center gap-1 hover:text-blue-600 font-medium"
      >
        {label}
        <span className="text-xs">
          {active ? (sortAsc ? "▲" : "▼") : "⇅"}
        </span>
      </button>
    </TableHead>
  );
}

function ResultBadge({ result }: { result: string }) {
  switch (result) {
    case "success": return <Badge className="bg-green-100 text-green-700">성공</Badge>;
    case "fail": return <Badge className="bg-red-100 text-red-700">실패</Badge>;
    default: return <Badge className="bg-yellow-100 text-yellow-700">진행중</Badge>;
  }
}
