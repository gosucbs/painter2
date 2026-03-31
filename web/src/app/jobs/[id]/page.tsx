"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { supabase, Job, Conversation } from "@/lib/supabase";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import Link from "next/link";

type Photo = {
  id: string;
  type: string;
  storage_path: string;
};

export default function JobDetailPage() {
  const params = useParams();
  const id = params.id as string;

  const [job, setJob] = useState<Job | null>(null);
  const [conversation, setConversation] = useState<Conversation | null>(null);
  const [photos, setPhotos] = useState<Photo[]>([]);
  const [photoUrls, setPhotoUrls] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editForm, setEditForm] = useState({
    vehicle_model: "",
    color_code: "",
    vehicle_year: "",
    work_area: "",
    paint_brand: "",
    result: "",
    notes: "",
  });

  useEffect(() => {
    async function fetchData() {
      const { data: jobData } = await supabase
        .from("jobs").select("*").eq("id", id).single();
      setJob(jobData);
      if (jobData) {
        setEditForm({
          vehicle_model: jobData.vehicle_model || "",
          color_code: jobData.color_code || "",
          vehicle_year: jobData.vehicle_year?.toString() || "",
          work_area: jobData.work_area || "",
          paint_brand: jobData.paint_brand || "",
          result: jobData.result || "in_progress",
          notes: jobData.notes || "",
        });
      }

      const { data: convData } = await supabase
        .from("conversations").select("*").eq("job_id", id)
        .order("created_at", { ascending: false }).limit(1);
      if (convData && convData.length > 0) {
        const conv = convData[0];
        const messages = typeof conv.messages === "string"
          ? JSON.parse(conv.messages) : conv.messages;
        setConversation({ ...conv, messages });
      }

      const { data: photoData } = await supabase
        .from("photos").select("*").eq("job_id", id);
      if (photoData && photoData.length > 0) {
        setPhotos(photoData);
        const urls: Record<string, string> = {};
        for (const photo of photoData) {
          const { data } = await supabase.storage
            .from("photos").createSignedUrl(photo.storage_path, 3600);
          if (data?.signedUrl) urls[photo.id] = data.signedUrl;
        }
        setPhotoUrls(urls);
      }

      setLoading(false);
    }
    fetchData();
  }, [id]);

  async function saveEdit() {
    setSaving(true);
    const { error } = await supabase.from("jobs").update({
      vehicle_model: editForm.vehicle_model,
      color_code: editForm.color_code,
      vehicle_year: editForm.vehicle_year ? parseInt(editForm.vehicle_year) : null,
      work_area: editForm.work_area || null,
      paint_brand: editForm.paint_brand,
      result: editForm.result,
      notes: editForm.notes || null,
      updated_at: new Date().toISOString(),
    }).eq("id", id);

    if (!error) {
      const { data } = await supabase.from("jobs").select("*").eq("id", id).single();
      setJob(data);
      setEditing(false);
    }
    setSaving(false);
  }

  if (loading) return <div className="text-center py-20">로딩 중...</div>;
  if (!job) return <div className="text-center py-20">작업을 찾을 수 없습니다</div>;

  const photoTypeLabel = (type: string) => {
    switch (type) {
      case "vehicle": return "차량사진";
      case "sample": return "조색시편사진";
      case "recipe": return "조색시편배합";
      default: return type;
    }
  };

  return (
    <div className="space-y-6">
      <Link href="/jobs" className="text-blue-600 hover:underline text-sm">
        &larr; 작업 목록
      </Link>

      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-bold">{job.vehicle_model} {job.color_code}</h1>
          <ResultBadge result={job.result} />
        </div>
        {!editing && (
          <div className="flex gap-2">
            <Button variant="outline" onClick={() => setEditing(true)}>수정</Button>
            <Button variant="destructive" onClick={async () => {
              if (!confirm(`"${job.vehicle_model} ${job.color_code}" 작업을 삭제하시겠습니까?`)) return;
              await supabase.from("jobs").delete().eq("id", id);
              window.location.href = "/jobs";
            }}>삭제</Button>
          </div>
        )}
      </div>

      {/* 작업 정보 */}
      <Card>
        <CardHeader><CardTitle>작업 정보</CardTitle></CardHeader>
        <CardContent>
          {editing ? (
            <div className="space-y-4">
              <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                <EditField label="차량 모델" value={editForm.vehicle_model}
                  onChange={(v) => setEditForm({ ...editForm, vehicle_model: v })} />
                <EditField label="컬러코드" value={editForm.color_code}
                  onChange={(v) => setEditForm({ ...editForm, color_code: v.toUpperCase() })} />
                <EditField label="연식" value={editForm.vehicle_year}
                  onChange={(v) => setEditForm({ ...editForm, vehicle_year: v })} />
                <EditField label="작업 부위" value={editForm.work_area}
                  onChange={(v) => setEditForm({ ...editForm, work_area: v })} />
                <EditField label="도료사" value={editForm.paint_brand}
                  onChange={(v) => setEditForm({ ...editForm, paint_brand: v })} />
                <div>
                  <label className="text-sm text-gray-500">결과</label>
                  <select
                    value={editForm.result}
                    onChange={(e) => setEditForm({ ...editForm, result: e.target.value })}
                    className="w-full mt-1 px-3 py-2 border rounded-md text-sm"
                  >
                    <option value="in_progress">진행중</option>
                    <option value="success">성공</option>
                    <option value="fail">실패</option>
                  </select>
                </div>
              </div>
              <div>
                <label className="text-sm text-gray-500">메모</label>
                <textarea
                  value={editForm.notes}
                  onChange={(e) => setEditForm({ ...editForm, notes: e.target.value })}
                  className="w-full mt-1 px-3 py-2 border rounded-md text-sm"
                  rows={2}
                />
              </div>
              <div className="flex gap-2">
                <Button onClick={saveEdit} disabled={saving}>
                  {saving ? "저장 중..." : "저장"}
                </Button>
                <Button variant="outline" onClick={() => setEditing(false)}>취소</Button>
              </div>
            </div>
          ) : (
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <InfoRow label="차량 모델" value={job.vehicle_model} />
              <InfoRow label="컬러코드" value={job.color_code} />
              <InfoRow label="연식" value={job.vehicle_year ? `${job.vehicle_year}년` : "-"} />
              <InfoRow label="도료사" value={job.paint_brand} />
              <InfoRow label="작업 부위" value={job.work_area || "-"} />
              <InfoRow label="결과" value={job.result === "success" ? "성공" : job.result === "fail" ? "실패" : "진행중"} />
              <InfoRow label="작업일" value={new Date(job.created_at).toLocaleDateString("ko-KR")} />
              <InfoRow label="메모" value={job.notes || "-"} />
            </div>
          )}
        </CardContent>
      </Card>

      {/* 사진 */}
      <Card>
        <CardHeader><CardTitle>촬영 사진</CardTitle></CardHeader>
        <CardContent>
          {photos.length === 0 ? (
            <p className="text-gray-500 text-center py-4">사진이 없습니다</p>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {photos.map((photo) => (
                <div key={photo.id} className="space-y-2">
                  <div className="text-sm font-medium text-gray-600">{photoTypeLabel(photo.type)}</div>
                  {photoUrls[photo.id] ? (
                    <img src={photoUrls[photo.id]} alt={photoTypeLabel(photo.type)}
                      className="w-full rounded-lg border object-cover aspect-[4/3]" />
                  ) : (
                    <div className="w-full aspect-[4/3] bg-gray-100 rounded-lg flex items-center justify-center text-gray-400">로딩 중...</div>
                  )}
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* AI 분석 결과 */}
      <Card>
        <CardHeader><CardTitle>AI 분석 결과</CardTitle></CardHeader>
        <CardContent>
          {conversation?.messages?.filter((m) => m.role === "assistant").length ? (
            <div className="whitespace-pre-wrap text-sm leading-relaxed">
              {conversation.messages.filter((m) => m.role === "assistant").map((m) => m.content).join("\n\n---\n\n")}
            </div>
          ) : (
            <p className="text-gray-500">분석 결과가 없습니다</p>
          )}
        </CardContent>
      </Card>

      {/* 대화 기록 */}
      <Card>
        <CardHeader><CardTitle>대화 기록</CardTitle></CardHeader>
        <CardContent>
          {conversation?.messages?.length ? (
            <div className="space-y-3">
              {conversation.messages.map((msg, i) => (
                <div key={i} className={`p-4 rounded-lg ${msg.role === "user" ? "bg-blue-50 ml-8" : "bg-gray-50 mr-8"}`}>
                  <div className="text-xs font-medium text-gray-500 mb-1">{msg.role === "user" ? "사용자" : "AI"}</div>
                  <div className="whitespace-pre-wrap text-sm">{msg.content}</div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-gray-500">대화 기록이 없습니다</p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

function EditField({ label, value, onChange }: { label: string; value: string; onChange: (v: string) => void }) {
  return (
    <div>
      <label className="text-sm text-gray-500">{label}</label>
      <Input value={value} onChange={(e) => onChange(e.target.value)} className="mt-1" />
    </div>
  );
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="text-sm text-gray-500">{label}</div>
      <div className="font-medium">{value}</div>
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
