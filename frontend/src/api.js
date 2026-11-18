// Where the Spring Boot API lives. Empty in development: Vite forwards /api and /actuator to localhost:8080.
// In production (Vercel) VITE_API_URL is the Render address.
export const API_BASE = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '');

/** True when the API answers its health check with status UP. */
export async function isApiUp() {
  try {
    const response = await fetch(`${API_BASE}/actuator/health`);
    if (!response.ok) return false;
    const body = await response.json();
    return body.status === 'UP';
  } catch {
    return false;
  }
}
