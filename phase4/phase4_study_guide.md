# ChronoFlux Phase 4 Study Guide: Google OAuth & Admin API Security

> **Context:** During Phase 1, Person A stubbed out the Google OAuth `/callback` endpoint in `OAuthController` and a basic `SecurityConfig`. However, the security was never enforced — `.anyRequest().permitAll()` left every endpoint wide open. In Phase 4, we completed the full end-to-end OAuth implementation: configured a real Google Cloud Console project, wired the React frontend to Google Identity Services, built the backend security filter chain, persisted users in PostgreSQL, and debugged three production-quality infrastructure bugs along the way.

---

## 1. Google Cloud Console Setup (Step-by-Step)

Before any code was written, we needed to register our application with Google so it can act as our **Identity Provider (IdP)**.

### Step 1: Create an OAuth Consent Screen
1. Navigate to **Google Cloud Console → APIs & Services → OAuth consent screen**.
2. Set **User Type** to `External` (allows any Google account; `Internal` is for Google Workspace orgs only).
3. Fill in the required fields: App name (`ChronoFlux`), User support email, Developer contact email.
4. Skip the **Scopes** screen (we only need `openid`, `email`, and `profile`, which are granted by default).
5. Under **Test users**, add the Gmail address you will use for testing (e.g., `employee.esdproject@gmail.com`).

> **Why Test Users?** While the app's publishing status is `Testing`, Google restricts login to only the accounts listed here. This prevents unauthorized people from accessing your app during development. In production, you would click "Publish app" to remove this restriction.

### Step 2: Create OAuth Client Credentials
1. Navigate to **APIs & Services → Credentials → Create Credentials → OAuth client ID**.
2. Choose **Application type: Web application**.
3. Set **Authorized JavaScript origins** to:
   - `http://localhost`
   - `http://localhost:80`
4. Set **Authorized redirect URIs** to:
   - `http://localhost`
5. Click **Create** and copy the **Client ID** (e.g., `318677345820-dvifkuv1o71ra4lng651mi316t2dvna0.apps.googleusercontent.com`).

### Key Terms Explained

| Term | What It Is | Where It's Used |
|---|---|---|
| **Client ID** | A public identifier (`*.apps.googleusercontent.com`) that tells Google *which app* is requesting login. Safe to expose in frontend code. | Passed to `@react-oauth/google` in React and to `GoogleIdTokenVerifier` in Spring Boot. |
| **Client Secret** | A private string for the server-side Authorization Code flow. Used to exchange an auth code for tokens. | **We don't use this.** Our Client-Side flow eliminates the need for a secret, reducing our attack surface. |
| **Authorized JavaScript Origins** | A whitelist of domains Google will allow the sign-in popup to load from. If the origin doesn't match, Google returns a `403 Forbidden`. | Must match exactly where the React app is served (`http://localhost`). |
| **Authorized Redirect URIs** | A whitelist of URLs Google can redirect the browser to after authentication. | Used in the traditional server-side flow. We set it for safety, but our client-side flow returns the token directly to the popup. |
| **ID Token** | A signed JWT containing identity claims (`email`, `name`, `picture`, `sub`). Proves *who* the user is. | Returned by Google to the React app, then POSTed to our backend `/callback`. |
| **`sub` (Subject)** | A unique, immutable string identifier for a Google Account. Never changes, even if the user changes their email. | We use `google_sub` as the primary lookup key in our `users` table, not `email`. |

---

## 2. The Security Gap (What Was Broken in Phase 1)

Before this fix, our `SecurityConfig.java` had this critical flaw:
```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/admin/auth/login", "/api/admin/auth/callback").permitAll()
    .anyRequest().permitAll() // ⚠️ DANGER: Everything is public
);
```
While a user *could* log in and receive an `AUTH_TOKEN` cookie, the backend never actually checked for this cookie on subsequent requests. Anyone could hit `POST /api/admin/events` using Postman and bypass authentication entirely.

Furthermore, we weren't saving the authenticated users into our PostgreSQL `users` table, which meant we had no audit trail for who created an event.

---

## 3. The End-to-End OAuth Flow (How It All Works)

Here is the complete sequence of events when a user clicks "Sign in with Google" in our app:

```
┌────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Browser  │     │ Google CDN   │     │  admin-api   │     │  PostgreSQL  │
│  (React)   │     │  (OAuth JS)  │     │ (Spring Boot)│     │              │
└─────┬──────┘     └──────┬───────┘     └──────┬───────┘     └──────┬───────┘
      │                   │                    │                    │
      │ 1. User clicks    │                    │                    │
      │    <GoogleLogin>   │                    │                    │
      │──────────────────>│                    │                    │
      │                   │                    │                    │
      │ 2. Google popup   │                    │                    │
      │    appears, user  │                    │                    │
      │    picks account  │                    │                    │
      │                   │                    │                    │
      │ 3. Google returns │                    │                    │
      │    ID Token (JWT) │                    │                    │
      │<──────────────────│                    │                    │
      │                   │                    │                    │
      │ 4. React POSTs {idToken: "eyJhbG..."}  │                    │
      │    to /api/admin/auth/callback         │                    │
      │───────────────────────────────────────>│                    │
      │                   │                    │                    │
      │                   │  5. TokenService   │                    │
      │                   │  verifies JWT sig  │                    │
      │                   │  with Google's     │                    │
      │                   │  public keys       │                    │
      │                   │                    │                    │
      │                   │                    │ 6. UserService     │
      │                   │                    │    upserts user    │
      │                   │                    │───────────────────>│
      │                   │                    │                    │
      │ 7. Set-Cookie: AUTH_TOKEN=eyJhbG...   │                    │
      │    (HttpOnly, Path=/)                  │                    │
      │<───────────────────────────────────────│                    │
      │                   │                    │                    │
      │ 8. Browser auto-attaches cookie        │                    │
      │    on ALL subsequent requests          │                    │
      │    GET /api/admin/schemas ─────────────>│                    │
      │                   │                    │                    │
      │                   │  9. JwtAuthFilter  │                    │
      │                   │  reads cookie,     │                    │
      │                   │  verifies token,   │                    │
      │                   │  sets SecurityCtx  │                    │
      │                   │                    │                    │
      │ 10. 200 OK [{...schemas...}]           │                    │
      │<───────────────────────────────────────│                    │
```

### Why Client-Side Flow vs. Server-Side Flow?

There are two main ways to implement Google Login:

1. **Server-Side Authorization Code Flow (Traditional)**
   - The user clicks "Login".
   - The backend redirects them to the **Authorization URL** (`https://accounts.google.com/o/oauth2/v2/auth`).
   - Google redirects back to the backend's **Redirect URL** (e.g., `/callback`) with an Authorization Code.
   - The backend securely exchanges that code for an ID Token and Access Token using the **Client Secret**.

2. **Client-Side ID Token Flow (Google Identity Services)** ← *What we use*
   - The frontend loads Google's JS library (via `@react-oauth/google`) and renders an iframe-based popup.
   - The user authenticates directly in the popup.
   - Google returns an **ID Token** (a signed JWT) directly to the React app.
   - The React app makes a `POST` to our backend `/callback` with the ID Token in the JSON body.
   - The backend uses `GoogleIdTokenVerifier` to cryptographically verify the token's signature.

**Why we chose the Client-Side flow:**
- Eliminates the need for a `Client Secret` on the backend → smaller attack surface.
- No server-side redirect dance → simpler SPA architecture.
- Google's official recommendation for modern single-page applications.

---

## 4. Implementation Deep Dive

### 4.1 The Environment Variable Pipeline (`.env` → Docker → Vite → React)

The Client ID needs to reach two places: the React frontend (at build time) and the Spring Boot backend (at runtime).

```
.env file                    docker-compose.yml              Container
─────────────────           ──────────────────────          ────────────────
GOOGLE_CLIENT_ID=318...  →  admin-api environment:          Spring reads via
                               GOOGLE_CLIENT_ID=${...}      @Value("${google.client.id}")
                            
                         →  frontend build args:            Vite bakes into JS bundle
                               VITE_GOOGLE_CLIENT_ID=${...} via import.meta.env.VITE_GOOGLE_CLIENT_ID
```

The Vite prefix `VITE_` is mandatory — Vite only exposes environment variables to client-side code if they start with `VITE_`. This prevents accidentally leaking server-side secrets into the browser bundle.

### 4.2 Frontend: `@react-oauth/google` Integration

**`main.tsx`** — Wrap the entire app in the `GoogleOAuthProvider`:
```tsx
import { GoogleOAuthProvider } from '@react-oauth/google';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <GoogleOAuthProvider clientId={import.meta.env.VITE_GOOGLE_CLIENT_ID}>
      <App />
    </GoogleOAuthProvider>
  </StrictMode>,
);
```

**`Layout.tsx`** — Replace the old hardcoded button with the official `<GoogleLogin>` component:
```tsx
import { GoogleLogin } from '@react-oauth/google';
import { api } from '../services/api';

const handleSuccess = async (credentialResponse: any) => {
  try {
    await api.post('/api/admin/auth/callback', {
      idToken: credentialResponse.credential
    });
    window.location.reload(); // Reload to pick up cookie
  } catch (err) {
    console.error('Login failed', err);
  }
};

// In the header JSX:
<GoogleLogin
  onSuccess={handleSuccess}
  onError={() => console.error('Login Failed')}
  useOneTap
  shape="rectangular"
  theme="filled_blue"
/>
```

### 4.3 Backend: The Database Entity Layer (`User` & `UserRepository`)

**Design Decision:** We map `google_sub` (the subject identifier from the Google ID token) as a unique column. This guarantees that even if a user changes their Google email address, we still recognize them as the same physical user.

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    private String name;
    private String avatarUrl;

    @Column(nullable = false)
    private String role = "ADMIN";

    @Column(unique = true)
    private String googleSub;
}
```

### 4.4 Backend: The `UserService` (Upserting Users)

When a user logs in, we check if they exist by `google_sub`. If not, we create them.

```java
@Service
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public User upsertUser(GoogleIdToken.Payload payload) {
        String googleSub = payload.getSubject();
        return userRepository.findByGoogleSub(googleSub)
            .orElseGet(() -> {
                User newUser = new User();
                newUser.setGoogleSub(googleSub);
                newUser.setEmail(payload.getEmail());
                newUser.setName((String) payload.get("name"));
                newUser.setAvatarUrl((String) payload.get("picture"));
                newUser.setRole("ADMIN"); // Demo mode — see grill Q below
                return userRepository.save(newUser);
            });
    }
}
```

### 4.5 Backend: The `JwtAuthenticationFilter` (The Gatekeeper)

This is the core of the security fix. It extends `OncePerRequestFilter` and intercepts *every* incoming HTTP request before it reaches the controllers.

**How it works:**
1. Extracts the `AUTH_TOKEN` from the HTTP cookies.
2. Uses `TokenService` to cryptographically verify the Google token signature against Google's public keys.
3. If valid, fetches the `User` from the database by `google_sub`.
4. Builds a `UsernamePasswordAuthenticationToken` and injects it into the `SecurityContextHolder`.

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String token = extractTokenFromCookies(request);

        if (token != null) {
            GoogleIdToken.Payload payload = tokenService.verifyToken(token);
            if (payload != null) {
                String googleSub = payload.getSubject();
                userRepository.findByGoogleSub(googleSub).ifPresent(user -> {
                    UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                            user, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                        );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("AUTH_TOKEN".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
```

### 4.6 Backend: Locking Down `SecurityConfig`

Finally, we apply the filter and lock the doors. Note that `requestMatchers` use the **stripped** paths (without `/api/admin`) because Tomcat's context-path strips that prefix before Spring Security evaluates the URL.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

### 4.7 Backend: The `OAuthController` (Issuing the Cookie)

```java
@RestController
@RequestMapping("/auth")
public class OAuthController {

    private final TokenService tokenService;
    private final UserService userService;

    @PostMapping("/callback")
    public ResponseEntity<?> callback(@RequestBody Map<String, String> payload,
                                       HttpServletResponse response) {
        String idTokenString = payload.get("idToken");
        GoogleIdToken.Payload tokenPayload = tokenService.verifyToken(idTokenString);

        if (tokenPayload != null) {
            userService.upsertUser(tokenPayload);

            Cookie cookie = new Cookie("AUTH_TOKEN", idTokenString);
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // false for HTTP localhost, true for HTTPS production
            cookie.setPath("/");
            cookie.setMaxAge(3600); // 1 hour

            response.addCookie(cookie);
            return ResponseEntity.ok(Map.of("message", "Login successful",
                                             "email", tokenPayload.getEmail()));
        } else {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid ID token"));
        }
    }
}
```

---

## 5. Interviewer Grill Checklist

**Q: Walk me through the entire OAuth flow from the user clicking "Sign in" to the first authenticated API call.**
> "When the user clicks 'Sign in with Google', the `@react-oauth/google` library opens a Google-hosted popup. The user selects their Google account and consents. Google's servers return a signed JWT (ID Token) directly to our React app via a JavaScript callback. React then POSTs this token to our Spring Boot `/auth/callback` endpoint. On the backend, `TokenService` uses `GoogleIdTokenVerifier` to cryptographically verify the JWT's signature against Google's public RSA keys — this confirms the token was genuinely issued by Google and hasn't been tampered with. We then call `UserService.upsertUser()` to persist the user in PostgreSQL (keyed by `google_sub`), create an HTTP-only cookie named `AUTH_TOKEN` containing the raw JWT, and return a 200. On all subsequent requests, the browser automatically attaches the cookie. Our `JwtAuthenticationFilter` extracts it, re-verifies the token, looks up the user, and injects them into Spring Security's `SecurityContextHolder`, which authorizes the request."

**Q: Why did you use an HTTP-Only cookie instead of storing the JWT in LocalStorage and sending it as a Bearer token?**
> "Storing tokens in LocalStorage makes them vulnerable to Cross-Site Scripting (XSS). If an attacker injects malicious JavaScript into the app, they can read LocalStorage and steal the token. By using an HTTP-Only cookie, the browser handles sending the token automatically, and JavaScript cannot read it — `document.cookie` will not return it. This completely mitigates XSS token theft."

**Q: If you are using cookies, aren't you vulnerable to Cross-Site Request Forgery (CSRF)? Why is CSRF disabled in your `SecurityConfig`?**
> "In a traditional MVC app where the browser automatically attaches cookies to cross-site requests, CSRF is a major risk. However, because our frontend and backend are on the same domain via the NGINX proxy at `localhost`, and because modern browsers default to `SameSite=Lax` cookie behavior, cross-origin sites cannot trigger requests that carry our cookie. In production, we would explicitly set `SameSite=Strict` on the `AUTH_TOKEN` cookie for maximum protection."

**Q: How does Spring Security know who the user is inside a Controller method?**
> "Because our `JwtAuthenticationFilter` populates the `SecurityContextHolder`, we can access the user directly in any controller using `@AuthenticationPrincipal User user`. Spring automatically injects the authenticated User object we placed in the context."

**Q: What happens if the Google Token expires?**
> "Google ID tokens expire in 1 hour. When an expired token is sent, `GoogleIdTokenVerifier` returns `null`, our filter does not populate the `SecurityContext`, and Spring Security returns `403 Forbidden`. The frontend would catch this and redirect to the login page."

**Q: Why is Swagger UI not protected?**
> "In development, leaving Swagger unprotected is a deliberate DX choice so frontend engineers can read the API contracts without needing to mock an OAuth login. In production, we would either put Swagger behind authentication or disable it entirely via Spring profiles."

**Q: Your `UserService` defaults to `setRole("ADMIN")`. Anyone with a Google account can become an admin?**
> "Yes, this is a known 'demo mode' shortcut. In a real-world enterprise application, we would implement the Principle of Least Privilege: default new users to a `PENDING` role, and require a super-admin to manually elevate privileges. Alternatively, we could whitelist specific email domains (e.g., `@chronoflux-studios.com`) or restrict to an invite-only model."

**Q: Why don't you have a username/password system?**
> "By using Google as our sole Identity Provider, we eliminate the liability of storing passwords entirely. No Bcrypt hashing, no salt management, no 'Forgot Password' flows. If our database is breached, attackers get zero passwords. Offloading authentication to a trusted provider is a modern best practice that reduces our attack surface."

**Q: Why did you choose `google_sub` over `email` as the unique identifier?**
> "Google allows users to change their email address. The `sub` claim is an immutable, globally unique identifier for a Google Account that never changes. If we used `email` as our primary key and a user changed their Gmail, we'd lose their history and create a duplicate account. Using `sub` guarantees a stable identity mapping."

**Q: Why does the frontend POST the token to the backend instead of the backend doing the entire OAuth redirect flow?**
> "We chose the Client-Side ID Token flow (Google Identity Services) over the traditional Server-Side Authorization Code flow. This eliminates the need for a `Client Secret` on the backend, removes the complex redirect dance (which can cause issues with SPAs and browser history), and is Google's recommended approach for modern single-page applications. The trade-off is that the ID Token briefly exists in JavaScript memory, but since it's short-lived (1 hour) and we immediately transfer it into an HTTP-Only cookie, the exposure window is minimal."

---

## 6. Bugs Encountered & Fixes Applied

During the implementation, we encountered three distinct production-quality bugs. Each one is an excellent interview talking point.

### Bug #1: Spring Security Context-Path 403

**Symptom:** `POST /api/admin/auth/callback` returned `403 Forbidden` even though `SecurityConfig` had `.requestMatchers("/api/admin/auth/**").permitAll()`.

**Root Cause:** Our `application.yml` sets `server.servlet.context-path: /api/admin`. Tomcat strips this prefix before handing the URL to Spring Security. So the actual path Spring Security evaluates is `/auth/callback`, not `/api/admin/auth/callback`. The matcher failed to match, fell through to `.anyRequest().authenticated()`, and returned 403.

**Fix:**
```java
// ❌ BEFORE (broken)
.requestMatchers("/api/admin/auth/**").permitAll()

// ✅ AFTER (correct — paths are relative to context-path)
.requestMatchers("/auth/**").permitAll()
.requestMatchers("/actuator/**").permitAll()
```

**Interview Angle:** "This is a classic Spring Security 6 gotcha, especially when running behind a reverse proxy. Tomcat handles context-path stripping at the servlet container level, before Spring Security even sees the request."

---

### Bug #2: NGINX 502 Bad Gateway (Docker DNS Caching)

**Symptom:** After running `docker-compose up -d --build admin-api`, navigating to the app returned `502 Bad Gateway` from NGINX.

**Root Cause:** Docker rebuilds assign containers new internal IP addresses. Open-source NGINX resolves DNS names (e.g., `proxy_pass http://admin-api:8080`) exactly **once** at startup and caches the IP indefinitely. The NGINX container still pointed at the old, dead IP.

**Fix:**
```bash
sudo docker restart chronoflux-frontend-1
```

**Interview Angle:** "In production, this is solved by using a dynamic reverse proxy like Traefik or Envoy that integrates with Docker/Kubernetes events, or by using the NGINX `resolver` directive to force periodic DNS re-resolution."

---

### Bug #3: `Secure` Cookie Flag (Infinite Login Loop)

**Symptom:** Google login succeeded (backend returned `200 OK`), but the frontend immediately prompted to sign in again. Schemas and Events pages returned empty. An infinite loop of login → success → re-prompt.

**Root Cause:** The `AUTH_TOKEN` cookie was set with `cookie.setSecure(true)`. The `Secure` flag tells the browser: *"Only send this cookie over HTTPS."* Since we run on plain `http://localhost`, the browser received the cookie but silently **refused to attach it** to subsequent requests. The `JwtAuthenticationFilter` never saw it → `SecurityContext` stayed empty → 403 on every protected endpoint.

**Fix:**
```java
// ❌ BEFORE (breaks on HTTP)
cookie.setSecure(true);

// ✅ AFTER (works on HTTP localhost; set true in production with HTTPS)
cookie.setSecure(false);
```

**Interview Angle:** "The `Secure` flag doesn't mean 'more secure in all contexts' — it means 'only works over HTTPS.' It's a critical defense-in-depth measure for production, but it actively breaks functionality when there is no TLS. This is why environment-aware configuration (e.g., Spring Profiles) is essential."

---

## 7. Testing & Verification

### Step 1: Verify Unauthenticated Denial
```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost/api/admin/schemas
# Expected: 403 (no cookie → blocked)
```

### Step 2: Verify Auth Endpoints Are Accessible
```bash
curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost/api/admin/auth/callback \
  -H "Content-Type: application/json" -d '{"idToken":"fake"}'
# Expected: 401 (endpoint reachable, but token is invalid)
```

### Step 3: Verify Public Endpoints Still Work
```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost/api/admin/actuator/health
# Expected: 200
```

### Step 4: Verify Login & Database Persistence
1. Open `http://localhost` in the browser.
2. Click the Google Sign-In button and authenticate with the test user.
3. Check the database:
```bash
sudo docker exec chronoflux-postgres-1 psql -U chronoflux -d chronoflux \
  -c "SELECT email, role, google_sub FROM users;"
```
4. **Expected:** A row with your email, role `ADMIN`, and a `google_sub` value.

### Step 5: Verify Authenticated Access
1. While logged in, open DevTools (F12) → **Application** → **Cookies** for `http://localhost`.
2. Confirm `AUTH_TOKEN` exists and is marked `HttpOnly`.
3. Navigate to the Events page.
4. **Expected:** Events list loads successfully. Network tab shows `GET /api/admin/events` returning `200 OK`.

---

## 8. Debugging Methodology: How We Systematically Fixed OAuth

This section documents the exact debugging approach used to isolate and resolve each issue. In an interview, being able to articulate *how* you debug is just as valuable as knowing the fix.

### The Golden Rule: Isolate the Layer

Our stack has four layers a request passes through. When something fails, the first question is always: **which layer is the failure in?**

```
Browser → NGINX (port 80) → Spring Boot (port 8080) → PostgreSQL
```

The primary tool for isolation is `curl`, used to bypass layers and test each one independently.

---

### Debugging Session #1: The Context-Path 403

**Symptom:** After deploying the new `SecurityConfig` with `.anyRequest().authenticated()`, hitting `http://localhost/api/admin/auth/login` returned 403.

**Step 1 — Is it NGINX or Spring Boot?** Bypass NGINX entirely by hitting Spring Boot directly:
```bash
curl -i http://localhost:8080/api/admin/actuator/health
# Result: 200 OK → Spring Boot is running
```

**Step 2 — Is the auth endpoint reachable?** Test the specific auth path directly:
```bash
curl -i -X POST http://localhost:8080/api/admin/auth/callback \
  -H "Content-Type: application/json" -d '{"idToken":"test"}'
# Result: 403 Forbidden → The problem is in Spring Security, not NGINX
```

**Step 3 — Read the SecurityConfig.** We had:
```java
.requestMatchers("/api/admin/auth/**").permitAll()
```

**Step 4 — Check `application.yml`.** Found `server.servlet.context-path: /api/admin`. This was the clue. Tomcat strips the context-path *before* Spring Security evaluates the URL. So Spring Security sees `/auth/callback`, not `/api/admin/auth/callback`. The matcher failed.

**Step 5 — Fix and verify:**
```java
.requestMatchers("/auth/**").permitAll()  // Use stripped path
```
```bash
curl -i -X POST http://localhost:8080/api/admin/auth/callback \
  -H "Content-Type: application/json" -d '{"idToken":"test"}'
# Result: 401 (endpoint reachable, token is invalid — this is correct!)
```

---

### Debugging Session #2: The NGINX 502 Bad Gateway

**Symptom:** After rebuilding `admin-api` with the fix, `http://localhost/api/admin/auth/login` returned `502 Bad Gateway`.

**Step 1 — Is Spring Boot alive?** Hit it directly, bypassing NGINX:
```bash
curl -i http://localhost:8080/api/admin/actuator/health
# Result: 200 OK {"status":"UP"} → Spring Boot is healthy!
```

**Step 2 — Is it NGINX?** Hit the same endpoint through NGINX:
```bash
curl -i http://localhost/api/admin/actuator/health
# Result: 502 Bad Gateway → NGINX can't reach Spring Boot
```

**Step 3 — Diagnosis.** The direct call on port 8080 works, but through NGINX on port 80 it fails. This means the issue is between NGINX and the `admin-api` container. Since we rebuilt `admin-api` (which assigns it a new Docker network IP) but didn't restart NGINX, NGINX is still using the cached IP of the old container.

**Step 4 — Fix and verify:**
```bash
sudo docker restart chronoflux-frontend-1
curl -i http://localhost/api/admin/actuator/health
# Result: 200 OK → NGINX re-resolved the DNS
```

---

### Debugging Session #3: Frontend/Backend Mismatch (Missing `/login` Endpoint)

**Symptom:** Clicking the "Sign in with Google" button in the React UI returned 403.

**Step 1 — What does the button do?** Inspect the React code in `Layout.tsx`:
```tsx
window.location.href = '/api/admin/auth/login';  // Full browser redirect
```

**Step 2 — Does that endpoint exist?** Check the backend `OAuthController.java`:
```java
@RequestMapping("/auth")
public class OAuthController {
    @PostMapping("/callback")   // Only POST /callback exists
    // ... no GET /login endpoint!
}
```

**Step 3 — Diagnosis.** The frontend is redirecting the browser to `GET /api/admin/auth/login`, but the backend has no such endpoint. Because `/auth/**` is permitted, Spring Security allows the request through, but Spring MVC has no handler — however, `GET /auth/login` doesn't match the `POST /callback` handler, and the dispatcher returns a 403 (not 404) due to the way Spring Security's default denial works with method-level mismatches.

**Step 4 — Fix.** Instead of creating a `/login` redirect endpoint (which would be the server-side flow), we switched to the **Client-Side ID Token flow** using `@react-oauth/google`:
- Installed the library: `npm install @react-oauth/google`
- Replaced the redirect button with the `<GoogleLogin>` component
- The component opens a Google popup, gets the JWT, and the `onSuccess` handler POSTs it to `/callback`

**Step 5 — Verify the backend still accepts POSTs to `/callback`:**
```bash
curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost/api/admin/auth/callback \
  -H "Content-Type: application/json" -d '{"idToken":"fake"}'
# Result: 401 → Endpoint reachable, invalid token correctly rejected
```

---

### Debugging Session #4: Google's 403 (Consent Screen Configuration)

**Symptom:** After integrating `@react-oauth/google`, clicking the Google Sign-In button showed a 403 error from Google's servers (not our backend).

**Step 1 — Is it our backend or Google?** Test our backend independently:
```bash
# Direct to Spring Boot
curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/admin/auth/callback \
  -H "Content-Type: application/json" -d '{"idToken":"test"}'
# Result: 401 → Our backend is fine

# Through NGINX
curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost/api/admin/auth/callback \
  -H "Content-Type: application/json" -d '{"idToken":"test"}'
# Result: 401 → NGINX proxy is fine
```

**Step 2 — Diagnosis.** Both our layers return 401 (correct for an invalid token), not 403. The 403 must be coming from Google's Identity Services JavaScript library before it even reaches our backend.

**Step 3 — Check Google Cloud Console:**
- Verified **Authorized JavaScript origins** includes `http://localhost` and `http://localhost:80`
- Verified **Authorized redirect URIs** includes `http://localhost`
- Verified the test user email was added under **OAuth consent screen → Test users**
- Waited 2-3 minutes for Google's credential propagation

**Step 4 — Verify:** Opened an incognito window, navigated to `http://localhost`, clicked Google Sign-In, selected the correct test user account → Popup succeeded.

---

### Debugging Session #5: The Secure Cookie Infinite Loop

**Symptom:** Google login popup succeeded, backend returned `200 OK "Login successful"`, but the frontend immediately re-prompted for login. Schemas page showed empty data.

**Step 1 — Is the cookie being set?** Open DevTools → Network tab → find the `POST /api/admin/auth/callback` response → check the `Set-Cookie` header:
```
Set-Cookie: AUTH_TOKEN=eyJhbG...; Max-Age=3600; Path=/; Secure; HttpOnly
```
The cookie *is* being set. But notice the `Secure` flag.

**Step 2 — Is the cookie being sent back?** Check the next request (e.g., `GET /api/admin/schemas`) in the Network tab → look at the request headers → **No `Cookie` header present.** The browser is not sending the cookie.

**Step 3 — Diagnosis.** The `Secure` flag means the browser will only attach this cookie to HTTPS requests. We're on `http://localhost` (no TLS). The browser received the cookie but silently drops it on every subsequent HTTP request.

**Step 4 — Fix:** In `OAuthController.java`, change `cookie.setSecure(true)` to `cookie.setSecure(false)`.

**Step 5 — Rebuild and verify:**
```bash
sudo docker-compose up -d --build admin-api
sudo docker restart chronoflux-frontend-1
```
Then in DevTools: `GET /api/admin/schemas` now shows `Cookie: AUTH_TOKEN=eyJhbG...` in the request headers and returns `200 OK` with data.

---

### Summary: The Debugging Toolkit

| Tool / Technique | What It Tells You |
|---|---|
| `curl -i http://localhost:8080/...` | Bypasses NGINX — tests Spring Boot directly |
| `curl -i http://localhost/...` | Tests the full path through NGINX |
| Compare direct vs proxied responses | Isolates whether the bug is in NGINX or Spring |
| `curl -s -o /dev/null -w "%{http_code}"` | Quick status code check without noisy output |
| Browser DevTools → Network tab | Shows exact request/response headers, cookies sent |
| Browser DevTools → Application → Cookies | Shows cookie attributes (HttpOnly, Secure, Path) |
| `sudo docker logs <container>` | Shows Spring Boot stack traces and NGINX error logs |
| `sudo docker restart <container>` | Fixes NGINX DNS cache after a backend rebuild |
| `sudo docker exec ... psql -c "SELECT..."` | Verifies data persistence in PostgreSQL |

