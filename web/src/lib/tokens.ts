export type Tokens = { accessToken: string; refreshToken: string; expiresIn: number };

const KEY = "erp-lab.tokens";

export function getTokens(): Tokens | null {
  try {
    const raw = localStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as Tokens) : null;
  } catch {
    return null;
  }
}

export function setTokens(tokens: Tokens) {
  localStorage.setItem(KEY, JSON.stringify(tokens));
}

export function clearTokens() {
  localStorage.removeItem(KEY);
}
