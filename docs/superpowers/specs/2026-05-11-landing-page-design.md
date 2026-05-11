# Ventri Landing Page — Design Spec

**Goal:** Pre-launch waitlist landing page for Ventri. Collects email and/or WhatsApp, optional news opt-in. EN/ES language switching.

**Project location:** `/home/oz/Projects/Personal/ventri-landing/` — standalone directory, separate git repo, independent of the KMP monorepo.

---

## Architecture

Fully static Astro site. No server adapter. Form submits directly from the browser to Supabase using the anon key + RLS.

```
index.astro
├── Navbar.astro          ← LangToggle (EN | ES button)
├── Hero.astro            ← EmailForm
├── Problem.astro
├── HowItWorks.astro
├── Features.astro
├── Screenshots.astro     ← carousel (vanilla JS)
├── Faq.astro             ← accordion (native <details>)
├── FooterCta.astro       ← EmailForm (reused, dark variant)
└── Footer.astro

src/i18n/
├── translations.ts       ← { en: {...}, es: {...} } — all UI strings
└── i18n-client.ts        ← client runtime: reads localStorage, stamps data-i18n elements
```

No `/api/` routes. No SSR.

---

## Tech Stack

| Layer | Choice |
|---|---|
| Framework | Astro 4, `output: 'static'` |
| Styling | Tailwind v4 (`@astrojs/tailwind`) |
| Database client | `@supabase/supabase-js` (browser, anon key) |
| Hosting | Any static host (Netlify, Cloudflare Pages, GitHub Pages, etc.) |

---

## Project Structure

```
ventri-landing/
├── src/
│   ├── pages/
│   │   └── index.astro
│   ├── components/
│   │   ├── Navbar.astro
│   │   ├── EmailForm.astro
│   │   ├── Hero.astro
│   │   ├── Problem.astro
│   │   ├── HowItWorks.astro
│   │   ├── Features.astro
│   │   ├── Screenshots.astro
│   │   ├── Faq.astro
│   │   ├── FooterCta.astro
│   │   └── Footer.astro
│   ├── i18n/
│   │   ├── translations.ts
│   │   └── i18n-client.ts
│   └── styles/
│       └── global.css
├── public/
│   └── screenshots/
├── .env
├── .env.example
├── astro.config.mjs
└── package.json
```

---

## Environment Variables

Both are browser-safe (Astro `PUBLIC_` prefix exposes them to client JS).

```
PUBLIC_SUPABASE_URL=https://<project>.supabase.co
PUBLIC_SUPABASE_ANON_KEY=<anon-key>
```

`.env` is gitignored. `.env.example` is committed with placeholder values.

---

## Supabase

**Table: `waitlist`**

```sql
create table waitlist (
  id             uuid primary key default gen_random_uuid(),
  email          text,
  whatsapp       text,
  subscribe_news boolean not null default false,
  created_at     timestamptz not null default now(),
  constraint contact_required check (email is not null or whatsapp is not null)
);

alter table waitlist enable row level security;

create policy "anon insert only"
  on waitlist for insert
  to anon
  with check (true);
```

**RLS invariant:** anon key allows inserts only — no reads, no updates, no deletes.

**Duplicate handling:** Supabase returns error code `23505` (unique_violation) if a unique constraint fires. Treat as success — user already registered.

---

## Form Logic (client-side)

`EmailForm.astro` `<script>` handles all validation and submission:

1. Validate: at least one of email / WhatsApp must be non-empty.
2. Validate email format with `/^[^\s@]+@[^\s@]+\.[^\s@]+$/`.
3. Validate WhatsApp format with `/^\+?[1-9]\d{6,14}$/` (E.164-ish).
4. Call `supabase.from('waitlist').insert({ email, whatsapp, subscribe_news })`.
5. On success or 23505 → hide form, show success message.
6. On other error → show inline error in active language.

Error messages read `document.documentElement.lang` to stay in sync with i18n runtime.

---

## i18n

Client-side only. No routing changes.

- `translations.ts` exports `{ en: {...}, es: {...} }` with all UI strings.
- `i18n-client.ts` reads `localStorage.getItem('ventri-lang')`, defaults to `'en'`.
- On load and on toggle: stamps `document.documentElement.lang`, updates all `[data-i18n]` elements and input placeholders, updates toggle button label.
- Toggle button label shows the OTHER language (EN page → button says "ES", ES page → button says "EN").
- Language preference persists in `localStorage` across page reloads.

---

## Design Tokens

```css
:root {
  --bg-primary: #fffbf7;
  --bg-alt:     #fff7ed;
  --bg-dark:    #1c1917;
  --accent:     #ea580c;
  --accent-lt:  #ffedd5;
  --accent-bd:  #fed7aa;
  --txt-1:      #1c1917;
  --txt-2:      #57534e;
  --txt-muted:  #a8a29e;
}
```

---

## Section Map

| Order | Component | BG | Key elements |
|---|---|---|---|
| 1 | Navbar | `--bg-primary` | Logo + LangToggle + "Get notified" anchor |
| 2 | Hero | `--bg-primary` | Badge, H1, subline, EmailForm, trust line |
| 3 | Problem | `--bg-dark` | 3 pain cards |
| 4 | HowItWorks | `--bg-primary` | 3 numbered steps |
| 5 | Features | `--bg-alt` | 6-card 3-col grid |
| 6 | Screenshots | `--bg-alt` | Carousel + dot nav |
| 7 | FAQ | `--bg-primary` | 4-item accordion (`<details>`) |
| 8 | FooterCta | `--accent` | EmailForm dark variant |
| 9 | Footer | `--bg-dark` | Copyright line |

---

## Invariants

- V1: `anon` RLS policy → insert only, no reads
- V2: at least one of `email` / `whatsapp` non-null (DB constraint + client validation)
- V3: `subscribeNews` defaults `false` — opt-in, never opt-out-by-default
- V4: error code `23505` treated as success — idempotent registration
- V5: all UI strings live in `translations.ts` — no hardcoded text in components
- V6: `PUBLIC_SUPABASE_ANON_KEY` never committed — gitignored via `.env`
