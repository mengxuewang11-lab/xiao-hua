"use client";

import type { CSSProperties, FormEvent } from "react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { apiEndpoints, apiJson } from "@/lib/api";

type ThemePreset = {
  id: string;
  name: string;
  accent: string;
  accentStrong: string;
  accent2: string;
  page: string;
  sidebar: string;
  panel: string;
  surface: string;
  composer: string;
  muted: string;
  line: string;
  text: string;
  subtle: string;
  shadow: string;
};

type VariableStyle = CSSProperties & Record<`--${string}`, string>;

type ModelOption = {
  provider: string;
  providerName: string;
  model: string;
  displayName: string;
  freeTier: boolean;
  enabled: boolean;
  description: string;
};

type UiPreference = {
  themeId: string;
  customAccentColor: string;
  sidebarCollapsed: boolean;
  defaultProvider: string;
  defaultModel: string;
};

type ConversationSummary = {
  id: string;
  title: string;
  provider: string;
  modelName: string;
  createdAt: string;
  updatedAt: string;
};

type ChatMessage = {
  id: string;
  conversationId: string;
  role: "user" | "assistant" | "system";
  content: string;
  provider?: string | null;
  modelName?: string | null;
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
  createdAt: string;
};

type ConversationDetail = {
  conversation: ConversationSummary;
  messages: ChatMessage[];
};

type SendMessageResponse = {
  conversation: ConversationSummary;
  userMessage: ChatMessage;
  assistantMessage: ChatMessage;
};

const themes: ThemePreset[] = [
  {
    id: "peach",
    name: "桃粉",
    accent: "#ff6ea8",
    accentStrong: "#d83f7f",
    accent2: "#ffc7dd",
    page: "#fff7fb",
    sidebar: "rgba(255, 242, 248, 0.92)",
    panel: "rgba(255, 255, 255, 0.78)",
    surface: "rgba(255, 255, 255, 0.86)",
    composer: "rgba(255, 255, 255, 0.92)",
    muted: "rgba(255, 110, 168, 0.12)",
    line: "rgba(128, 53, 87, 0.15)",
    text: "#27131d",
    subtle: "#7c5968",
    shadow: "rgba(255, 110, 168, 0.24)",
  },
  {
    id: "berry",
    name: "莓红",
    accent: "#f43f7f",
    accentStrong: "#be185d",
    accent2: "#fda4c4",
    page: "#fff5f8",
    sidebar: "rgba(255, 239, 246, 0.92)",
    panel: "rgba(255, 255, 255, 0.78)",
    surface: "rgba(255, 255, 255, 0.86)",
    composer: "rgba(255, 255, 255, 0.92)",
    muted: "rgba(244, 63, 127, 0.12)",
    line: "rgba(139, 38, 77, 0.15)",
    text: "#2b111c",
    subtle: "#825263",
    shadow: "rgba(244, 63, 127, 0.22)",
  },
  {
    id: "grape",
    name: "葡萄",
    accent: "#8b5cf6",
    accentStrong: "#6d28d9",
    accent2: "#c4b5fd",
    page: "#f8f5ff",
    sidebar: "rgba(246, 240, 255, 0.92)",
    panel: "rgba(255, 255, 255, 0.78)",
    surface: "rgba(255, 255, 255, 0.86)",
    composer: "rgba(255, 255, 255, 0.92)",
    muted: "rgba(139, 92, 246, 0.12)",
    line: "rgba(83, 55, 145, 0.16)",
    text: "#211533",
    subtle: "#695982",
    shadow: "rgba(139, 92, 246, 0.22)",
  },
  {
    id: "cyber",
    name: "赛博蓝",
    accent: "#38bdf8",
    accentStrong: "#0284c7",
    accent2: "#bae6fd",
    page: "#f0fbff",
    sidebar: "rgba(234, 249, 255, 0.92)",
    panel: "rgba(255, 255, 255, 0.78)",
    surface: "rgba(255, 255, 255, 0.86)",
    composer: "rgba(255, 255, 255, 0.92)",
    muted: "rgba(56, 189, 248, 0.14)",
    line: "rgba(2, 132, 199, 0.16)",
    text: "#102331",
    subtle: "#4e6e80",
    shadow: "rgba(56, 189, 248, 0.22)",
  },
  {
    id: "mint",
    name: "薄荷",
    accent: "#2dd4bf",
    accentStrong: "#0f9488",
    accent2: "#99f6e4",
    page: "#f0fffb",
    sidebar: "rgba(235, 254, 249, 0.92)",
    panel: "rgba(255, 255, 255, 0.78)",
    surface: "rgba(255, 255, 255, 0.86)",
    composer: "rgba(255, 255, 255, 0.92)",
    muted: "rgba(45, 212, 191, 0.13)",
    line: "rgba(15, 148, 136, 0.16)",
    text: "#102723",
    subtle: "#4f756e",
    shadow: "rgba(45, 212, 191, 0.2)",
  },
  {
    id: "lime",
    name: "青柠",
    accent: "#a3e635",
    accentStrong: "#65a30d",
    accent2: "#d9f99d",
    page: "#fbfff3",
    sidebar: "rgba(247, 255, 231, 0.92)",
    panel: "rgba(255, 255, 255, 0.78)",
    surface: "rgba(255, 255, 255, 0.86)",
    composer: "rgba(255, 255, 255, 0.92)",
    muted: "rgba(163, 230, 53, 0.16)",
    line: "rgba(101, 163, 13, 0.16)",
    text: "#1d260e",
    subtle: "#657247",
    shadow: "rgba(163, 230, 53, 0.2)",
  },
  {
    id: "orange",
    name: "橘子",
    accent: "#fb923c",
    accentStrong: "#ea580c",
    accent2: "#fed7aa",
    page: "#fff8f1",
    sidebar: "rgba(255, 244, 230, 0.92)",
    panel: "rgba(255, 255, 255, 0.78)",
    surface: "rgba(255, 255, 255, 0.86)",
    composer: "rgba(255, 255, 255, 0.92)",
    muted: "rgba(251, 146, 60, 0.14)",
    line: "rgba(194, 90, 26, 0.16)",
    text: "#2f1a0c",
    subtle: "#81624c",
    shadow: "rgba(251, 146, 60, 0.2)",
  },
  {
    id: "lemon",
    name: "柠檬",
    accent: "#facc15",
    accentStrong: "#ca8a04",
    accent2: "#fef08a",
    page: "#fffdee",
    sidebar: "rgba(255, 251, 219, 0.92)",
    panel: "rgba(255, 255, 255, 0.78)",
    surface: "rgba(255, 255, 255, 0.86)",
    composer: "rgba(255, 255, 255, 0.92)",
    muted: "rgba(250, 204, 21, 0.16)",
    line: "rgba(161, 98, 7, 0.15)",
    text: "#261d08",
    subtle: "#756942",
    shadow: "rgba(250, 204, 21, 0.18)",
  },
  {
    id: "ocean",
    name: "海盐",
    accent: "#60a5fa",
    accentStrong: "#2563eb",
    accent2: "#bfdbfe",
    page: "#f2f8ff",
    sidebar: "rgba(235, 244, 255, 0.92)",
    panel: "rgba(255, 255, 255, 0.78)",
    surface: "rgba(255, 255, 255, 0.86)",
    composer: "rgba(255, 255, 255, 0.92)",
    muted: "rgba(96, 165, 250, 0.13)",
    line: "rgba(37, 99, 235, 0.15)",
    text: "#102033",
    subtle: "#526d8d",
    shadow: "rgba(96, 165, 250, 0.2)",
  },
  {
    id: "mono",
    name: "黑粉",
    accent: "#f472b6",
    accentStrong: "#fbcfe8",
    accent2: "#a78bfa",
    page: "#080914",
    sidebar: "rgba(13, 16, 30, 0.92)",
    panel: "rgba(17, 21, 38, 0.82)",
    surface: "rgba(24, 28, 48, 0.86)",
    composer: "rgba(20, 24, 42, 0.94)",
    muted: "rgba(244, 114, 182, 0.14)",
    line: "rgba(244, 244, 255, 0.12)",
    text: "#f8fafc",
    subtle: "#b6bed2",
    shadow: "rgba(244, 114, 182, 0.18)",
  },
  {
    id: "night",
    name: "夜樱",
    accent: "#93c5fd",
    accentStrong: "#dbeafe",
    accent2: "#f9a8d4",
    page: "#0c1222",
    sidebar: "rgba(12, 18, 34, 0.92)",
    panel: "rgba(18, 27, 47, 0.82)",
    surface: "rgba(24, 34, 56, 0.86)",
    composer: "rgba(20, 30, 52, 0.94)",
    muted: "rgba(147, 197, 253, 0.14)",
    line: "rgba(226, 232, 240, 0.12)",
    text: "#f8fbff",
    subtle: "#b6c3d7",
    shadow: "rgba(147, 197, 253, 0.16)",
  },
];

const defaultPreference: UiPreference = {
  themeId: "peach",
  customAccentColor: "#ff6ea8",
  sidebarCollapsed: false,
  defaultProvider: "zhipu",
  defaultModel: "glm-4.7-flash",
};

const sparkStyles: VariableStyle[] = Array.from({ length: 36 }, (_, index) => ({
  "--spark-x": `${(index * 29) % 100}%`,
  "--spark-y": `${(index * 47) % 100}%`,
  "--spark-size": `${3 + (index % 4)}px`,
  "--spark-delay": `${-(index * 0.34).toFixed(2)}s`,
  "--spark-duration": `${5 + (index % 7)}s`,
}));

function SparkIcon() {
  return (
    <svg viewBox="0 0 24 24" className="size-4" aria-hidden="true">
      <path
        d="M12 3l1.7 5.2L19 10l-5.3 1.8L12 17l-1.7-5.2L5 10l5.3-1.8L12 3z"
        fill="currentColor"
      />
    </svg>
  );
}

function PenIcon() {
  return (
    <svg viewBox="0 0 24 24" className="size-4" aria-hidden="true">
      <path
        d="M4 20h4.2L18.7 9.5a2.2 2.2 0 0 0 0-3.1l-1.1-1.1a2.2 2.2 0 0 0-3.1 0L4 15.8V20zm2-3.4L15.9 6.7l1.4 1.4L7.4 18H6v-1.4z"
        fill="currentColor"
      />
    </svg>
  );
}

function SearchIcon() {
  return (
    <svg viewBox="0 0 24 24" className="size-4" aria-hidden="true">
      <path
        d="M10.8 4a6.8 6.8 0 0 1 5.4 10.9l3.4 3.4-1.3 1.3-3.4-3.4A6.8 6.8 0 1 1 10.8 4zm0 1.8a5 5 0 1 0 0 10 5 5 0 0 0 0-10z"
        fill="currentColor"
      />
    </svg>
  );
}

function SendIcon() {
  return (
    <svg viewBox="0 0 24 24" className="size-5" aria-hidden="true">
      <path
        d="M4 12l15-8-4.5 16-3-6-7.5-2zm4.3-.2l4.3 1.1 1.7 3.4 2.2-8-8.2 3.5z"
        fill="currentColor"
      />
    </svg>
  );
}

function SidebarIcon({ collapsed }: { collapsed: boolean }) {
  return (
    <svg viewBox="0 0 24 24" className="size-5" aria-hidden="true">
      <path
        d="M4 5.8A1.8 1.8 0 0 1 5.8 4h12.4A1.8 1.8 0 0 1 20 5.8v12.4a1.8 1.8 0 0 1-1.8 1.8H5.8A1.8 1.8 0 0 1 4 18.2V5.8zm1.8 0v12.4h4.5V5.8H5.8zm6.2 0v12.4h6.2V5.8H12z"
        fill="currentColor"
      />
      <path
        d={collapsed ? "M15.4 9.2 18.2 12l-2.8 2.8-1.2-1.2 1.6-1.6-1.6-1.6 1.2-1.2z" : "M16.6 9.2 13.8 12l2.8 2.8 1.2-1.2-1.6-1.6 1.6-1.6-1.2-1.2z"}
        fill="currentColor"
      />
    </svg>
  );
}

function buildShellStyle(theme: ThemePreset): VariableStyle {
  return {
    "--page": theme.page,
    "--sidebar": theme.sidebar,
    "--panel": theme.panel,
    "--surface": theme.surface,
    "--composer": theme.composer,
    "--muted": theme.muted,
    "--line": theme.line,
    "--text": theme.text,
    "--subtle": theme.subtle,
    "--accent": theme.accent,
    "--accent-strong": theme.accentStrong,
    "--accent-2": theme.accent2,
    "--accent-soft": `color-mix(in srgb, ${theme.accent} 28%, transparent)`,
    "--accent-shadow": theme.shadow,
  };
}

export default function NovelWorkspace() {
  const [preference, setPreference] = useState<UiPreference>(defaultPreference);
  const [models, setModels] = useState<ModelOption[]>([]);
  const [conversations, setConversations] = useState<ConversationSummary[]>([]);
  const [activeConversation, setActiveConversation] = useState<ConversationSummary | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [prompt, setPrompt] = useState("");
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const activeTheme = useMemo(() => {
    if (preference.themeId === "custom") {
      return {
        ...themes[0],
        id: "custom",
        name: "自定义",
        accent: preference.customAccentColor,
        accentStrong: preference.customAccentColor,
        accent2: preference.customAccentColor,
        muted: `color-mix(in srgb, ${preference.customAccentColor} 14%, white)`,
        shadow: `color-mix(in srgb, ${preference.customAccentColor} 24%, transparent)`,
      };
    }

    return themes.find((item) => item.id === preference.themeId) ?? themes[0];
  }, [preference.customAccentColor, preference.themeId]);

  const shellStyle = useMemo(() => buildShellStyle(activeTheme), [activeTheme]);
  const selectedModelKey = `${preference.defaultProvider}:${preference.defaultModel}`;
  const selectedModel = models.find(
    (item) => item.provider === preference.defaultProvider && item.model === preference.defaultModel,
  );

  const savePreference = useCallback(
    async (next: Partial<UiPreference>) => {
      const payload = { ...preference, ...next };
      setPreference(payload);
      setError(null);
      try {
        const saved = await apiJson<UiPreference>(apiEndpoints.chatPreferences, {
          method: "PUT",
          body: JSON.stringify(payload),
        });
        setPreference(saved);
      } catch (exception) {
        setError(exception instanceof Error ? exception.message : "偏好保存失败");
      }
    },
    [preference],
  );

  const loadConversation = useCallback(async (conversationId: string) => {
    setError(null);
    const detail = await apiJson<ConversationDetail>(`${apiEndpoints.chatConversations}/${conversationId}`);
    setActiveConversation(detail.conversation);
    setMessages(detail.messages);
  }, []);

  const refreshConversations = useCallback(async () => {
    const latest = await apiJson<ConversationSummary[]>(apiEndpoints.chatConversations);
    setConversations(latest);
    return latest;
  }, []);

  useEffect(() => {
    let alive = true;

    async function boot() {
      try {
        const [preferenceResult, modelsResult, conversationsResult] = await Promise.all([
          apiJson<UiPreference>(apiEndpoints.chatPreferences),
          apiJson<ModelOption[]>(apiEndpoints.chatModels),
          apiJson<ConversationSummary[]>(apiEndpoints.chatConversations),
        ]);

        if (!alive) {
          return;
        }

        setPreference(preferenceResult);
        setModels(modelsResult);
        setConversations(conversationsResult);

        if (conversationsResult[0]) {
          const detail = await apiJson<ConversationDetail>(
            `${apiEndpoints.chatConversations}/${conversationsResult[0].id}`,
          );
          if (alive) {
            setActiveConversation(detail.conversation);
            setMessages(detail.messages);
          }
        }
      } catch (exception) {
        if (alive) {
          setError(exception instanceof Error ? exception.message : "工作台加载失败");
        }
      } finally {
        if (alive) {
          setLoading(false);
        }
      }
    }

    void boot();

    return () => {
      alive = false;
    };
  }, []);

  async function startNewChat() {
    setActiveConversation(null);
    setMessages([]);
    setPrompt("");
    setError(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const content = prompt.trim();
    if (!content || sending) {
      return;
    }

    setSending(true);
    setError(null);
    try {
      const response = await apiJson<SendMessageResponse>(apiEndpoints.chatMessages, {
        method: "POST",
        body: JSON.stringify({
          conversationId: activeConversation?.id,
          content,
          provider: preference.defaultProvider,
          model: preference.defaultModel,
        }),
      });
      setPrompt("");
      setActiveConversation(response.conversation);
      setMessages((current) => {
        const existing = new Set(current.map((item) => item.id));
        return [
          ...current,
          ...[response.userMessage, response.assistantMessage].filter((item) => !existing.has(item.id)),
        ];
      });
      const latest = await refreshConversations();
      const updatedActive = latest.find((item) => item.id === response.conversation.id);
      if (updatedActive) {
        setActiveConversation(updatedActive);
      }
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "消息发送失败");
    } finally {
      setSending(false);
    }
  }

  function handleModelChange(value: string) {
    const [provider, model] = value.split(":");
    void savePreference({ defaultProvider: provider, defaultModel: model });
  }

  function handleThemeChange(themeId: string) {
    const theme = themes.find((item) => item.id === themeId);
    void savePreference({
      themeId,
      customAccentColor: theme?.accent ?? preference.customAccentColor,
    });
  }

  const sidebarCollapsed = preference.sidebarCollapsed;
  const hasMessages = messages.length > 0;

  return (
    <main className="workspace-shell h-dvh overflow-hidden bg-[var(--page)] text-[var(--text)]" style={shellStyle}>
      <div className="motion-bg" aria-hidden="true">
        <span className="glow-orb glow-orb-one" />
        <span className="glow-orb glow-orb-two" />
        <span className="glow-orb glow-orb-three" />
        <span className="motion-beam motion-beam-one" />
        <span className="motion-beam motion-beam-two" />
        {sparkStyles.map((style, index) => (
          <span key={index} className="spark-dot" style={style} />
        ))}
      </div>

      <div
        className={`relative z-10 grid h-dvh grid-cols-1 overflow-hidden transition-[grid-template-columns] duration-300 ${
          sidebarCollapsed ? "lg:grid-cols-[76px_minmax(0,1fr)]" : "lg:grid-cols-[300px_minmax(0,1fr)]"
        }`}
      >
        <aside className="hidden h-dvh min-h-0 flex-col overflow-hidden border-r border-[var(--line)] bg-[var(--sidebar)] p-3 shadow-[18px_0_60px_rgba(30,20,50,0.08)] backdrop-blur-2xl lg:flex">
          <div className={`flex shrink-0 items-center ${sidebarCollapsed ? "justify-center" : "justify-between"} px-1 py-2`}>
            <div className={`flex items-center gap-3 ${sidebarCollapsed ? "justify-center" : ""}`}>
              <div className="grid size-10 place-items-center rounded-2xl bg-[var(--accent)] text-lg font-black text-white shadow-lg shadow-[var(--accent-shadow)]">
                花
              </div>
              {!sidebarCollapsed ? (
                <div>
                  <p className="text-[10px] font-bold uppercase tracking-[0.32em] text-[var(--accent-strong)]">
                    Xiao Hua
                  </p>
                  <h1 className="mt-0.5 text-base font-semibold">无限小说工坊</h1>
                </div>
              ) : null}
            </div>
            {!sidebarCollapsed ? (
              <button
                className="rounded-2xl border border-[var(--line)] bg-[var(--surface)] p-2 text-[var(--subtle)] transition hover:-translate-y-0.5 hover:text-[var(--text)]"
                onClick={() => savePreference({ sidebarCollapsed: true })}
                type="button"
                aria-label="收起侧边栏"
              >
                <SidebarIcon collapsed={false} />
              </button>
            ) : null}
          </div>

          {sidebarCollapsed ? (
            <div className="mt-3 flex flex-col items-center gap-3">
              <button
                className="grid size-11 place-items-center rounded-2xl border border-[var(--line)] bg-[var(--surface)] text-[var(--subtle)] transition hover:text-[var(--text)]"
                onClick={() => savePreference({ sidebarCollapsed: false })}
                type="button"
                aria-label="展开侧边栏"
              >
                <SidebarIcon collapsed />
              </button>
              <button
                className="grid size-11 place-items-center rounded-2xl bg-[var(--accent)] text-white shadow-lg shadow-[var(--accent-shadow)]"
                onClick={startNewChat}
                type="button"
                aria-label="新建聊天"
              >
                <PenIcon />
              </button>
              <button className="grid size-11 place-items-center rounded-2xl border border-[var(--line)] bg-[var(--surface)] text-[var(--subtle)]">
                <SearchIcon />
              </button>
            </div>
          ) : (
            <>
              <button
                onClick={startNewChat}
                type="button"
                className="mt-4 flex shrink-0 items-center gap-3 rounded-3xl bg-[var(--accent)] px-4 py-3 text-sm font-semibold text-white shadow-lg shadow-[var(--accent-shadow)] transition hover:-translate-y-0.5"
              >
                <span className="grid size-9 place-items-center rounded-2xl bg-white/18">
                  <SparkIcon />
                </span>
                新建聊天
              </button>

              <div className="mt-5 flex shrink-0 items-center gap-2 rounded-3xl border border-[var(--line)] bg-[var(--surface)] px-3 py-2.5">
                <SearchIcon />
                <span className="text-sm text-[var(--subtle)]">搜索作品、设定和聊天</span>
              </div>

              <div className="thin-scrollbar mt-5 min-h-0 flex-1 overflow-y-auto pr-1">
                <p className="px-2 text-xs font-semibold uppercase tracking-[0.22em] text-[var(--subtle)]">最近</p>
                <div className="mt-3 space-y-2">
                  {conversations.length === 0 ? (
                    <p className="rounded-3xl border border-dashed border-[var(--line)] px-4 py-5 text-sm leading-6 text-[var(--subtle)]">
                      还没有聊天记录。发出第一条灵感后，这里会自动保存。
                    </p>
                  ) : (
                    conversations.map((conversation) => (
                      <button
                        key={conversation.id}
                        onClick={() => loadConversation(conversation.id)}
                        type="button"
                        className={`w-full rounded-3xl border px-3 py-3 text-left transition ${
                          conversation.id === activeConversation?.id
                            ? "border-[var(--accent-soft)] bg-[var(--muted)]"
                            : "border-transparent hover:border-[var(--line)] hover:bg-[var(--surface)]"
                        }`}
                      >
                        <p className="truncate text-sm font-semibold">{conversation.title}</p>
                        <p className="mt-1 truncate text-xs text-[var(--subtle)]">
                          {conversation.provider} / {conversation.modelName}
                        </p>
                      </button>
                    ))
                  )}
                </div>
              </div>

              <div className="mt-4 shrink-0 rounded-3xl border border-[var(--line)] bg-[var(--surface)] p-3">
                <div className="flex items-center gap-3">
                  <div className="grid size-9 place-items-center rounded-2xl bg-[var(--text)] text-sm font-bold text-[var(--page)]">
                    Q
                  </div>
                  <div>
                    <p className="text-sm font-semibold">创作者模式</p>
                    <p className="text-xs text-[var(--subtle)]">偏好已落库保存</p>
                  </div>
                </div>
              </div>
            </>
          )}
        </aside>

        <section className="flex h-dvh min-w-0 flex-col overflow-hidden">
          <header className="flex shrink-0 items-center justify-between gap-3 border-b border-[var(--line)] bg-[var(--panel)] px-3 py-3 backdrop-blur-2xl sm:px-5">
            <div className="flex min-w-0 items-center gap-3">
              <button
                className="grid size-10 place-items-center rounded-2xl border border-[var(--line)] bg-[var(--surface)] text-[var(--subtle)] transition hover:text-[var(--text)]"
                onClick={() => savePreference({ sidebarCollapsed: !sidebarCollapsed })}
                type="button"
                aria-label={sidebarCollapsed ? "展开侧边栏" : "收起侧边栏"}
              >
                <SidebarIcon collapsed={sidebarCollapsed} />
              </button>
              <div className="grid size-10 place-items-center rounded-2xl bg-[var(--accent)] text-base font-black text-white shadow-lg shadow-[var(--accent-shadow)] lg:hidden">
                花
              </div>
              <div className="min-w-0">
                <p className="text-[10px] font-bold uppercase tracking-[0.28em] text-[var(--accent-strong)]">
                  Novel Agent
                </p>
                <h2 className="truncate text-base font-semibold sm:text-lg">
                  {activeConversation?.title ?? "新的小说灵感"}
                </h2>
              </div>
            </div>

            <div className="flex min-w-0 items-center gap-2">
              <select
                value={selectedModelKey}
                onChange={(event) => handleModelChange(event.target.value)}
                className="hidden max-w-[220px] rounded-full border border-[var(--line)] bg-[var(--surface)] px-3 py-2 text-xs font-semibold text-[var(--text)] outline-none transition focus:border-[var(--accent-soft)] md:block"
                title={selectedModel?.description}
              >
                {models.map((model) => (
                  <option key={`${model.provider}:${model.model}`} value={`${model.provider}:${model.model}`}>
                    {model.providerName} · {model.displayName}{model.freeTier ? " · 免费" : ""}{model.enabled ? "" : " · 未配置"}
                  </option>
                ))}
              </select>
              <div className="thin-scrollbar hidden max-w-[44vw] items-center gap-1 overflow-x-auto rounded-full border border-[var(--line)] bg-[var(--surface)] p-1 shadow-sm xl:flex">
                {themes.map((item) => (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => handleThemeChange(item.id)}
                    className={`flex shrink-0 items-center gap-2 rounded-full px-2.5 py-1.5 text-xs font-medium transition ${
                      item.id === preference.themeId
                        ? "bg-[var(--muted)] text-[var(--accent-strong)]"
                        : "text-[var(--subtle)] hover:text-[var(--text)]"
                    }`}
                    aria-label={`切换到${item.name}主题`}
                  >
                    <span className="size-3 rounded-full shadow-inner" style={{ backgroundColor: item.accent }} />
                    <span>{item.name}</span>
                  </button>
                ))}
                <label className="flex shrink-0 cursor-pointer items-center gap-2 rounded-full px-2.5 py-1.5 text-xs font-medium text-[var(--subtle)] transition hover:text-[var(--text)]">
                  自定义
                  <input
                    type="color"
                    value={preference.customAccentColor}
                    onChange={(event) => {
                      void savePreference({
                        themeId: "custom",
                        customAccentColor: event.target.value,
                      });
                    }}
                    className="size-5 cursor-pointer rounded-full border-0 bg-transparent p-0"
                    aria-label="自定义主题色"
                  />
                </label>
              </div>
            </div>
          </header>

          <div className="thin-scrollbar min-h-0 flex-1 overflow-y-auto px-4 py-6 sm:px-6 lg:px-10">
            <div className="mx-auto flex min-h-full w-full max-w-4xl flex-col">
              {loading ? (
                <section className="grid min-h-full place-items-center text-center text-sm text-[var(--subtle)]">
                  正在打开小花工作台...
                </section>
              ) : hasMessages ? (
                <div className="space-y-6 pb-4">
                  {messages.map((message) => (
                    <article
                      key={message.id}
                      className={`flex gap-3 ${message.role === "user" ? "justify-end" : "justify-start"}`}
                    >
                      {message.role === "assistant" ? (
                        <div className="grid size-9 shrink-0 place-items-center rounded-2xl bg-[var(--accent)] text-sm font-black text-white shadow-lg shadow-[var(--accent-shadow)]">
                          花
                        </div>
                      ) : null}

                      <div className={`max-w-[760px] ${message.role === "user" ? "order-first flex justify-end" : "w-full"}`}>
                        <div
                          className={`whitespace-pre-wrap rounded-[1.65rem] border px-5 py-4 text-sm leading-7 shadow-sm backdrop-blur ${
                            message.role === "user"
                              ? "max-w-[680px] border-transparent bg-[var(--accent)] text-white shadow-[var(--accent-shadow)]"
                              : "border-[var(--line)] bg-[var(--surface)] text-[var(--text)]"
                          }`}
                        >
                          <p
                            className={`mb-1 text-xs font-semibold ${
                              message.role === "user" ? "text-white/75" : "text-[var(--accent-strong)]"
                            }`}
                          >
                            {message.role === "user" ? "你" : "小花总导演"}
                            {message.modelName ? ` · ${message.modelName}` : ""}
                          </p>
                          {message.content}
                        </div>
                      </div>

                      {message.role === "user" ? (
                        <div className="grid size-9 shrink-0 place-items-center rounded-2xl bg-[var(--text)] text-sm font-bold text-[var(--page)]">
                          你
                        </div>
                      ) : null}
                    </article>
                  ))}
                </div>
              ) : (
                <section className="grid min-h-full place-items-center py-12 text-center">
                  <div className="w-full max-w-2xl">
                    <div className="mx-auto grid size-12 place-items-center rounded-2xl bg-[var(--accent)] text-lg font-black text-white shadow-xl shadow-[var(--accent-shadow)]">
                      花
                    </div>
                    <h3 className="mt-5 text-[32px] font-semibold leading-tight tracking-[-0.04em]">
                      今天想让哪本小说开花？
                    </h3>
                    <p className="mx-auto mt-3 max-w-xl text-sm leading-7 text-[var(--subtle)]">
                      输入一句灵感，小花会先帮你判断题材、核心爽点和创意方向。主题、侧边栏和聊天记录都会保存到本地数据库。
                    </p>
                    <div className="mt-6 grid gap-3 sm:grid-cols-3">
                      {["都市悬疑", "青春成长", "奇幻冒险"].map((item) => (
                        <button
                          key={item}
                          type="button"
                          onClick={() => setPrompt(`帮我设计一个${item}小说开局，要适合长期连载。`)}
                          className="rounded-3xl border border-[var(--line)] bg-[var(--surface)] px-4 py-4 text-sm font-semibold transition hover:-translate-y-0.5 hover:border-[var(--accent-soft)]"
                        >
                          {item}
                        </button>
                      ))}
                    </div>
                  </div>
                </section>
              )}
            </div>
          </div>

          <footer className="shrink-0 border-t border-[var(--line)] bg-[var(--panel)] px-4 py-3 backdrop-blur-2xl sm:px-6 lg:px-10">
            <div className="mx-auto max-w-4xl">
              {error ? (
                <div className="mb-2 rounded-2xl border border-red-300/60 bg-red-50 px-4 py-2 text-xs text-red-700">
                  {error}
                </div>
              ) : null}
              <form
                onSubmit={handleSubmit}
                className="rounded-[2rem] border border-[var(--line)] bg-[var(--composer)] p-3 shadow-2xl shadow-[var(--accent-shadow)]"
              >
                <textarea
                  value={prompt}
                  onChange={(event) => setPrompt(event.target.value)}
                  rows={2}
                  placeholder="输入一个小说想法，比如：一个普通维修工能看见旧物记忆..."
                  className="min-h-14 w-full resize-none bg-transparent px-3 py-2 text-sm leading-6 outline-none placeholder:text-[var(--subtle)]"
                />
                <div className="flex flex-col gap-3 border-t border-[var(--line)] px-2 pt-3 sm:flex-row sm:items-center sm:justify-between">
                  <div className="flex flex-wrap items-center gap-2 text-xs text-[var(--subtle)]">
                    <select
                      value={selectedModelKey}
                      onChange={(event) => handleModelChange(event.target.value)}
                      className="rounded-full border border-[var(--line)] bg-[var(--surface)] px-3 py-1.5 text-xs font-semibold text-[var(--text)] outline-none md:hidden"
                    >
                      {models.map((model) => (
                        <option key={`${model.provider}:${model.model}`} value={`${model.provider}:${model.model}`}>
                          {model.providerName} · {model.displayName}{model.freeTier ? " · 免费" : ""}{model.enabled ? "" : " · 未配置"}
                        </option>
                      ))}
                    </select>
                    <span className="rounded-full bg-[var(--muted)] px-3 py-1.5">
                      {selectedModel?.displayName ?? preference.defaultModel}
                    </span>
                    <span className="rounded-full bg-[var(--muted)] px-3 py-1.5">记录已落库</span>
                    <span className="rounded-full bg-[var(--muted)] px-3 py-1.5">可切换模型</span>
                  </div>
                  <button
                    className="inline-flex items-center justify-center gap-2 rounded-full bg-[var(--accent)] px-4 py-2 text-sm font-semibold text-white shadow-lg shadow-[var(--accent-shadow)] transition hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-60"
                    type="submit"
                    disabled={sending || !prompt.trim()}
                  >
                    {sending ? "小花思考中..." : "发送给总导演"}
                    <SendIcon />
                  </button>
                </div>
              </form>
              <p className="mt-2 text-center text-[11px] text-[var(--subtle)]">
                当前主题：{activeTheme.name}。当前模型：{selectedModel?.providerName ?? preference.defaultProvider} / {selectedModel?.displayName ?? preference.defaultModel}。
              </p>
            </div>
          </footer>
        </section>
      </div>
    </main>
  );
}
