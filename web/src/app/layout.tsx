import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "小花无限小说工坊",
  description: "自主 AI 小说创作智能体用户网站",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN" className="h-full antialiased">
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
