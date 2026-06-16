export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL?.replace(/\/$/, "") ?? "/api";

export const apiEndpoints = {
  databaseStatus: "/test/database",
  createNovelRun: "/novel-agent/runs",
  chatModels: "/chat/models",
  chatPreferences: "/chat/preferences",
  chatConversations: "/chat/conversations",
  chatMessages: "/chat/messages",
} as const;

export function apiUrl(path: string) {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${API_BASE_URL}${normalizedPath}`;
}

export class ApiRequestError extends Error {
  constructor(
    message: string,
    public readonly status: number,
    public readonly code?: string,
  ) {
    super(message);
    this.name = "ApiRequestError";
  }
}

export async function apiJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(apiUrl(path), {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
  });

  if (!response.ok) {
    let message = `接口请求失败：${response.status}`;
    let code: string | undefined;
    try {
      const errorBody = (await response.json()) as { message?: string; code?: string };
      message = errorBody.message ?? message;
      code = errorBody.code;
    } catch {
      // 保留默认错误信息。
    }
    throw new ApiRequestError(message, response.status, code);
  }

  return (await response.json()) as T;
}
