import type { Metadata } from "next";
import { Geist } from "next/font/google";
import "./globals.css";
import Link from "next/link";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "PainterAI Dashboard",
  description: "자동차 도장 AI 조색 대시보드",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko" className={`${geistSans.variable} h-full antialiased`}>
      <body className="min-h-full flex flex-col bg-gray-50 dark:bg-gray-950">
        <nav className="border-b bg-white dark:bg-gray-900 px-6 py-3">
          <div className="max-w-7xl mx-auto flex items-center gap-6">
            <Link href="/" className="text-xl font-bold text-blue-600">
              PainterAI
            </Link>
            <Link href="/jobs" className="text-sm hover:text-blue-600">
              작업 목록
            </Link>
            <Link href="/colors" className="text-sm hover:text-blue-600">
              컬러코드 검색
            </Link>
          </div>
        </nav>
        <main className="flex-1 max-w-7xl mx-auto w-full p-6">{children}</main>
      </body>
    </html>
  );
}
