# Ventri Landing Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a pre-launch waitlist landing page for Ventri, deployed to Vercel as a static/hybrid Astro site.

**Architecture:** Single `landing/` directory inside the existing KMP repo. Astro hybrid output with a single SSR API route for Supabase writes. All other pages are statically prerendered. Language switching (EN/ES) is handled client-side via a translations file + `localStorage` — no routing changes needed for a single-page waitlist site.

**Contact collection:** The signup form collects email and/or WhatsApp number — both fields are optional individually, but at least one is required. An opt-in toggle (default off) lets users subscribe to follow-up news beyond the launch notification.

**Tech Stack:** Astro 4, Tailwind v4, `@supabase/supabase-js`, `@astrojs/vercel`, vanilla JS for carousel/accordion/i18n toggle.

---

## Dependency Flow

```
index.astro
├── Navbar.astro          ← LangToggle (EN | ES button)
├── Hero.astro            ← EmailForm (client:load island)
├── Problem.astro
├── HowItWorks.astro
├── Features.astro
├── Screenshots.astro     ← carousel JS (client:load island)
├── Faq.astro             ← accordion JS (client:load island)
├── FooterCta.astro       ← EmailForm (reused, client:load island)
└── Footer.astro

POST /api/subscribe.ts    ← server endpoint (hybrid output)
    └── @supabase/supabase-js → Supabase waitlist table

src/i18n/
└── translations.ts       ← { en: {...}, es: {...} } — all UI strings
```

---

## Project Structure

```
landing/
├── src/
│   ├── pages/
│   │   ├── index.astro
│   │   └── api/
│   │       └── subscribe.ts
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
│   │   └── translations.ts   ← all EN + ES strings
│   └── styles/
│       └── global.css
├── public/
│   └── screenshots/
├── .env
├── astro.config.mjs
└── package.json
```

---

## Section Map

| Order | Component     | BG token        | Key elements |
|-------|---------------|-----------------|--------------|
| 1     | Navbar        | `--bg-primary`  | Logo + LangToggle (EN\|ES) + "Get notified" anchor |
| 2     | Hero          | `--bg-primary`  | Badge, H1, subline, `<EmailForm id="hero-form" />`, trust line |
| 3     | Problem       | `--bg-dark`     | 3 pain cards on dark bg |
| 4     | HowItWorks    | `--bg-primary`  | 3 numbered steps |
| 5     | Features      | `--bg-alt`      | 6-card 3-col grid |
| 6     | Screenshots   | `--bg-alt`      | carousel, dot nav, JS island |
| 7     | Faq           | `--bg-primary`  | 4-item accordion |
| 8     | FooterCta     | `--accent`      | `<EmailForm id="footer-form" dark />` |
| 9     | Footer bar    | `--bg-dark`     | copyright line |

---

## Task 1: Scaffold Astro project

**Files:**
- Create: `landing/` (entire directory)
- Create: `landing/astro.config.mjs`
- Create: `landing/src/styles/global.css`

- [ ] **Step 1: Scaffold**

```bash
cd /home/oz/Projects/Personal/ventri
npm create astro@latest landing -- --template minimal --no-install
cd landing && npm install
npx astro add tailwind vercel --yes
npm install @supabase/supabase-js
```

- [ ] **Step 2: `astro.config.mjs`**

```js
import { defineConfig } from 'astro/config';
import tailwind from '@astrojs/tailwind';
import vercel from '@astrojs/vercel/serverless';

export default defineConfig({
  output: 'hybrid',
  adapter: vercel(),
  integrations: [tailwind()],
});
```

- [ ] **Step 3: `src/styles/global.css`**

```css
@import "tailwindcss";

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

- [ ] **Step 4: Verify dev server starts**

```bash
npm run dev
```

Expected: Astro dev server at `localhost:4321`, no errors.

- [ ] **Step 5: Commit**

```bash
git add landing/
git commit -m "feat(landing): scaffold Astro + Tailwind + Vercel adapter"
```

---

## Task 2: Supabase table + API route

**Files:**
- Create: `landing/src/pages/api/subscribe.ts`
- Create: `landing/.env` (gitignored — document in README, never commit)

- [ ] **Step 1: Create Supabase table** (run once in Supabase SQL editor)

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

- [ ] **Step 2: `.env`**

```
PUBLIC_SUPABASE_URL=https://<project>.supabase.co
SUPABASE_SERVICE_ROLE_KEY=<service-role-key>
```

- [ ] **Step 3: `src/pages/api/subscribe.ts`**

```ts
import type { APIRoute } from 'astro';
import { createClient } from '@supabase/supabase-js';

export const prerender = false;

const supabase = createClient(
  import.meta.env.PUBLIC_SUPABASE_URL,
  import.meta.env.SUPABASE_SERVICE_ROLE_KEY,
);

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const WHATSAPP_RE = /^\+?[1-9]\d{6,14}$/; // E.164-ish: optional +, 7-15 digits

export const POST: APIRoute = async ({ request }) => {
  const body = await request.json();
  const email: string | undefined = body.email?.trim() || undefined;
  const whatsapp: string | undefined = body.whatsapp?.trim() || undefined;
  const subscribeNews: boolean = body.subscribeNews === true;

  if (!email && !whatsapp) {
    return new Response(JSON.stringify({ error: 'Provide email or WhatsApp' }), { status: 400 });
  }
  if (email && !EMAIL_RE.test(email)) {
    return new Response(JSON.stringify({ error: 'Invalid email' }), { status: 400 });
  }
  if (whatsapp && !WHATSAPP_RE.test(whatsapp)) {
    return new Response(JSON.stringify({ error: 'Invalid WhatsApp number' }), { status: 400 });
  }

  const { error } = await supabase
    .from('waitlist')
    .insert({ email: email ?? null, whatsapp: whatsapp ?? null, subscribe_news: subscribeNews });

  // 23505 = unique_violation — treat as success (already registered)
  if (error && error.code !== '23505') {
    return new Response(JSON.stringify({ error: 'Server error' }), { status: 500 });
  }

  return new Response(JSON.stringify({ ok: true }), { status: 200 });
};
```

- [ ] **Step 4: Test endpoint**

```bash
# Email only
curl -X POST http://localhost:4321/api/subscribe \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","subscribeNews":false}'

# WhatsApp only
curl -X POST http://localhost:4321/api/subscribe \
  -H "Content-Type: application/json" \
  -d '{"whatsapp":"+15551234567","subscribeNews":true}'

# Both fields
curl -X POST http://localhost:4321/api/subscribe \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","whatsapp":"+15551234567","subscribeNews":false}'

# Missing both — expect 400
curl -X POST http://localhost:4321/api/subscribe \
  -H "Content-Type: application/json" \
  -d '{}'
```

Expected: `{"ok":true}` for valid payloads, rows in Supabase `waitlist` table with correct `subscribe_news` value.

- [ ] **Step 5: Commit**

```bash
git add landing/src/pages/api/subscribe.ts
git commit -m "feat(landing): add Supabase waitlist API route"
```

---

## Task 3: i18n translations file

**Files:**
- Create: `landing/src/i18n/translations.ts`

All user-visible strings live here. Components never contain hardcoded text — they reference keys from this file via the active locale. The client-side toggle reads `localStorage.getItem('lang') ?? 'en'` and swaps `data-i18n` element content on load and on toggle.

- [ ] **Step 1: Create `src/i18n/translations.ts`**

```ts
export type Lang = 'en' | 'es';

export const t = {
  en: {
    nav: {
      getNotified: 'Get notified',
      langToggle: 'ES',
    },
    hero: {
      badge: 'Coming to Android',
      headline: 'Never run out of anything again.',
      subline: 'Ventri tracks what you have at home, predicts when you\'ll run out, and builds your shopping list automatically.',
      trust: 'No spam. One notification when we launch.',
    },
    problem: {
      heading: 'Sound familiar?',
      card1Title: 'You ran out — again',
      card1Body: 'Milk, detergent, shampoo. Always at the worst time.',
      card2Title: 'Your shopping list is a lie',
      card2Body: 'You only remember what you\'re out of, never what\'s about to run out.',
      card3Title: 'You buy duplicates',
      card3Body: 'Three bottles of ketchup because you forgot you already had two.',
    },
    howItWorks: {
      heading: 'How it works',
      step1Title: 'Add your items',
      step1Body: 'Log what you keep at home — food, cleaning supplies, toiletries.',
      step2Title: 'Log your purchases',
      step2Body: 'When you restock, mark it bought. Ventri tracks how fast you use things.',
      step3Title: 'Never guess again',
      step3Body: 'Get alerts before you run out and let Ventri build your shopping list.',
    },
    features: {
      heading: 'Everything you need',
      f1Title: 'Depletion predictions',
      f1Body: 'Knows exactly how fast you consume each item.',
      f2Title: 'Smart shopping list',
      f2Body: 'Auto-populated based on what\'s running low.',
      f3Title: 'Priority system',
      f3Body: 'Flag essentials so critical items are always visible.',
      f4Title: 'Stock overview',
      f4Body: 'See everything at a glance — no surprises.',
      f5Title: 'Push notifications',
      f5Body: 'Alerts before you run out, not after.',
      f6Title: 'English & Spanish',
      f6Body: 'Full app experience in both languages.',
    },
    screenshots: {
      heading: 'See it in action',
    },
    faq: {
      heading: 'Questions',
      q1: 'Is it free?',
      a1: 'The core app is free. A premium tier for unlimited items is planned.',
      q2: 'When does it launch?',
      a2: 'We\'re targeting the Play Store later this year. Sign up to be first.',
      q3: 'iOS?',
      a3: 'Android first. iOS depends on demand — sign up and tell us.',
      q4: 'Does it need internet?',
      a4: 'No. Everything is stored locally on your device.',
    },
    footerCta: {
      heading: 'Be the first to know.',
      subline: 'Join the waitlist. No spam, ever.',
    },
    emailForm: {
      emailPlaceholder: 'your@email.com',
      whatsappPlaceholder: '+1 555 000 0000',
      orSeparator: 'or',
      contactHint: 'At least one is required.',
      newsToggle: 'Keep me updated with news',
      button: 'Notify me',
      success: '🎉 You\'re on the list!',
      errorNone: 'Enter your email or WhatsApp number.',
      errorInvalidEmail: 'Enter a valid email.',
      errorInvalidWhatsapp: 'Enter a valid WhatsApp number.',
      errorServer: 'Something went wrong. Try again.',
    },
    footer: {
      copy: '© 2026 Ventri. All rights reserved.',
    },
  },

  es: {
    nav: {
      getNotified: 'Avisarme',
      langToggle: 'EN',
    },
    hero: {
      badge: 'Próximamente en Android',
      headline: 'Nunca más te quedes sin nada.',
      subline: 'Ventri rastrea lo que tienes en casa, predice cuándo se agotará y construye tu lista de compras automáticamente.',
      trust: 'Sin spam. Una notificación cuando lancemos.',
    },
    problem: {
      heading: '¿Te suena familiar?',
      card1Title: 'Se te acabó — otra vez',
      card1Body: 'Leche, detergente, champú. Siempre en el peor momento.',
      card2Title: 'Tu lista de compras miente',
      card2Body: 'Solo recuerdas lo que ya se acabó, nunca lo que está a punto de agotarse.',
      card3Title: 'Compras duplicados',
      card3Body: 'Tres botellas de kétchup porque olvidaste que ya tenías dos.',
    },
    howItWorks: {
      heading: 'Cómo funciona',
      step1Title: 'Agrega tus artículos',
      step1Body: 'Registra lo que guardas en casa — comida, limpieza, higiene.',
      step2Title: 'Registra tus compras',
      step2Body: 'Cuando reabastes, márcalo como comprado. Ventri calcula qué tan rápido lo usas.',
      step3Title: 'Nunca más adivines',
      step3Body: 'Recibe alertas antes de quedarte sin nada y deja que Ventri construya tu lista.',
    },
    features: {
      heading: 'Todo lo que necesitas',
      f1Title: 'Predicción de agotamiento',
      f1Body: 'Sabe exactamente qué tan rápido consumes cada artículo.',
      f2Title: 'Lista de compras inteligente',
      f2Body: 'Se genera automáticamente según lo que escasea.',
      f3Title: 'Sistema de prioridades',
      f3Body: 'Marca lo esencial para que lo crítico siempre esté visible.',
      f4Title: 'Vista del inventario',
      f4Body: 'Ve todo de un vistazo — sin sorpresas.',
      f5Title: 'Notificaciones push',
      f5Body: 'Alertas antes de quedarte sin algo, no después.',
      f6Title: 'Inglés y español',
      f6Body: 'Experiencia completa en ambos idiomas.',
    },
    screenshots: {
      heading: 'Míralo en acción',
    },
    faq: {
      heading: 'Preguntas',
      q1: '¿Es gratis?',
      a1: 'La app principal es gratuita. Se planea un nivel premium para artículos ilimitados.',
      q2: '¿Cuándo sale?',
      a2: 'Apuntamos a la Play Store a finales de este año. Regístrate para ser el primero.',
      q3: '¿iOS?',
      a3: 'Primero Android. iOS depende de la demanda — regístrate y dinos.',
      q4: '¿Necesita internet?',
      a4: 'No. Todo se guarda localmente en tu dispositivo.',
    },
    footerCta: {
      heading: 'Sé el primero en saberlo.',
      subline: 'Únete a la lista de espera. Sin spam, nunca.',
    },
    emailForm: {
      emailPlaceholder: 'tu@correo.com',
      whatsappPlaceholder: '+1 555 000 0000',
      orSeparator: 'o',
      contactHint: 'Al menos uno es obligatorio.',
      newsToggle: 'Mantenerme al tanto con noticias',
      button: 'Avisarme',
      success: '🎉 ¡Estás en la lista!',
      errorNone: 'Ingresa tu correo o número de WhatsApp.',
      errorInvalidEmail: 'Ingresa un correo válido.',
      errorInvalidWhatsapp: 'Ingresa un número de WhatsApp válido.',
      errorServer: 'Algo salió mal. Intenta de nuevo.',
    },
    footer: {
      copy: '© 2026 Ventri. Todos los derechos reservados.',
    },
  },
} satisfies Record<Lang, unknown>;
```

- [ ] **Step 2: Verify TypeScript — no errors**

```bash
npx tsc --noEmit
```

Expected: 0 errors.

- [ ] **Step 3: Commit**

```bash
git add landing/src/i18n/translations.ts
git commit -m "feat(landing): add EN/ES translations"
```

---

## Task 4: i18n client runtime script

**Files:**
- Create: `landing/src/i18n/i18n-client.ts`

A tiny client script (inlined in `index.astro` as `<script>`) that: reads stored lang, stamps `<html lang="">`, and exposes `window.__setLang(lang)` for the toggle button.

- [ ] **Step 1: Create `src/i18n/i18n-client.ts`**

```ts
import { t, type Lang } from './translations';

const STORAGE_KEY = 'ventri-lang';

function applyLang(lang: Lang) {
  document.documentElement.lang = lang;
  localStorage.setItem(STORAGE_KEY, lang);

  document.querySelectorAll<HTMLElement>('[data-i18n]').forEach((el) => {
    const key = el.dataset.i18n!;
    const parts = key.split('.');
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    let value: any = t[lang];
    for (const part of parts) {
      value = value?.[part];
    }
    if (typeof value === 'string') {
      if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') {
        (el as HTMLInputElement).placeholder = value;
      } else {
        el.textContent = value;
      }
    }
  });

  // Update toggle button label (shows the OTHER language)
  document.querySelectorAll<HTMLElement>('[data-lang-toggle]').forEach((btn) => {
    btn.textContent = t[lang].nav.langToggle;
  });
}

export function initI18n() {
  const stored = localStorage.getItem(STORAGE_KEY) as Lang | null;
  const lang: Lang = stored === 'es' ? 'es' : 'en';
  applyLang(lang);

  document.querySelectorAll('[data-lang-toggle]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const current = (document.documentElement.lang as Lang) || 'en';
      applyLang(current === 'en' ? 'es' : 'en');
    });
  });
}
```

- [ ] **Step 2: Add inline script to `index.astro`**

```astro
---
// index.astro (head section)
---
<script>
  import { initI18n } from '../i18n/i18n-client';
  document.addEventListener('DOMContentLoaded', initI18n);
</script>
```

- [ ] **Step 3: Commit**

```bash
git add landing/src/i18n/i18n-client.ts
git commit -m "feat(landing): add client-side i18n runtime"
```

---

## Task 5: Navbar + LangToggle

**Files:**
- Create: `landing/src/components/Navbar.astro`

Sticky top bar. Logo left, right side: "Get notified" anchor + language toggle button (`data-lang-toggle`).

- [ ] **Step 1: Write `Navbar.astro`**

```astro
---
import { t } from '../i18n/translations';
const en = t.en.nav;
---
<header class="sticky top-0 z-50 bg-[var(--bg-primary)] border-b border-[var(--accent-bd)]">
  <div class="max-w-5xl mx-auto px-4 h-14 flex items-center justify-between">
    <span class="font-bold text-lg text-[var(--txt-1)] tracking-tight">Ventri</span>
    <nav class="flex items-center gap-3">
      <a
        href="#hero-form"
        class="text-sm font-medium text-[var(--accent)] hover:underline"
        data-i18n="nav.getNotified"
      >{en.getNotified}</a>
      <button
        data-lang-toggle
        class="text-xs font-semibold px-2 py-1 rounded border border-[var(--accent-bd)] text-[var(--txt-2)] hover:bg-[var(--accent-lt)] transition"
      >{en.langToggle}</button>
    </nav>
  </div>
</header>
```

- [ ] **Step 2: Verify in browser**

- Toggle button shows "ES" on load (EN default)
- Clicking changes all `data-i18n` elements to Spanish and button shows "EN"
- Refreshing preserves the selected language (localStorage)

- [ ] **Step 3: Commit**

```bash
git add landing/src/components/Navbar.astro
git commit -m "feat(landing): add Navbar with language toggle"
```

---

## Task 6: EmailForm island

**Files:**
- Create: `landing/src/components/EmailForm.astro`

Reusable component. Props: `id: string`, `dark?: boolean`. Renders `<form>` + success/error states. All string values use `data-i18n` attributes so the i18n runtime can swap them.

- [ ] **Step 1: Write `EmailForm.astro`**

Two input fields (email + WhatsApp), at least one required. News opt-in toggle below, default off. Error messages read current `document.documentElement.lang` to stay in sync with the i18n runtime.

```astro
---
interface Props {
  id: string;
  dark?: boolean;
}
const { id, dark = false } = Astro.props;
const mutedClass = dark ? 'text-orange-100' : 'text-[var(--txt-muted)]';
const inputClass = 'w-full px-4 py-2.5 rounded-lg border border-[var(--accent-bd)] bg-white text-[var(--txt-1)] text-sm focus:outline-none focus:ring-2 focus:ring-[var(--accent)]';
---
<div id={id} class="w-full max-w-md" data-form-wrapper>
  <div class="flex flex-col gap-2">
    <input
      type="email"
      name="email"
      data-i18n="emailForm.emailPlaceholder"
      placeholder="your@email.com"
      class={inputClass}
    />
    <div class="flex items-center gap-2">
      <div class="flex-1 h-px bg-[var(--accent-bd)]"></div>
      <span class={`text-xs ${mutedClass}`} data-i18n="emailForm.orSeparator">or</span>
      <div class="flex-1 h-px bg-[var(--accent-bd)]"></div>
    </div>
    <input
      type="tel"
      name="whatsapp"
      data-i18n="emailForm.whatsappPlaceholder"
      placeholder="+1 555 000 0000"
      class={inputClass}
    />
  </div>

  <!-- News opt-in toggle -->
  <label class="flex items-center gap-2 mt-3 cursor-pointer select-none">
    <input
      type="checkbox"
      name="subscribeNews"
      class="w-4 h-4 rounded accent-[var(--accent)] cursor-pointer"
    />
    <span class={`text-xs ${mutedClass}`} data-i18n="emailForm.newsToggle">Keep me updated with news</span>
  </label>

  <button
    type="button"
    data-submit-btn
    class="mt-3 w-full px-5 py-2.5 rounded-lg bg-[var(--accent)] text-white text-sm font-semibold hover:bg-orange-700 transition"
  >
    <span data-i18n="emailForm.button">Notify me</span>
  </button>

  <p class={`mt-2 text-sm hidden text-red-400`} data-error-msg></p>
  <p class={`mt-2 text-sm hidden ${mutedClass}`} data-success-msg data-i18n="emailForm.success">
    🎉 You're on the list!
  </p>
</div>

<script>
  const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  const WA_RE = /^\+?[1-9]\d{6,14}$/;

  document.querySelectorAll<HTMLElement>('[data-form-wrapper]').forEach((wrapper) => {
    const emailInput = wrapper.querySelector<HTMLInputElement>('input[name="email"]')!;
    const waInput = wrapper.querySelector<HTMLInputElement>('input[name="whatsapp"]')!;
    const newsInput = wrapper.querySelector<HTMLInputElement>('input[name="subscribeNews"]')!;
    const submitBtn = wrapper.querySelector<HTMLButtonElement>('[data-submit-btn]')!;
    const errorEl = wrapper.querySelector<HTMLElement>('[data-error-msg]')!;
    const successEl = wrapper.querySelector<HTMLElement>('[data-success-msg]')!;

    function getLang() { return document.documentElement.lang || 'en'; }

    submitBtn.addEventListener('click', async () => {
      const email = emailInput.value.trim();
      const whatsapp = waInput.value.trim();
      const subscribeNews = newsInput.checked;
      const lang = getLang();

      // Client validation
      if (!email && !whatsapp) {
        errorEl.textContent = lang === 'es'
          ? 'Ingresa tu correo o número de WhatsApp.'
          : 'Enter your email or WhatsApp number.';
        errorEl.classList.remove('hidden');
        return;
      }
      if (email && !EMAIL_RE.test(email)) {
        errorEl.textContent = lang === 'es'
          ? 'Ingresa un correo válido.'
          : 'Enter a valid email.';
        errorEl.classList.remove('hidden');
        return;
      }
      if (whatsapp && !WA_RE.test(whatsapp)) {
        errorEl.textContent = lang === 'es'
          ? 'Ingresa un número de WhatsApp válido.'
          : 'Enter a valid WhatsApp number.';
        errorEl.classList.remove('hidden');
        return;
      }
      errorEl.classList.add('hidden');

      const res = await fetch('/api/subscribe', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: email || undefined,
          whatsapp: whatsapp || undefined,
          subscribeNews,
        }),
      });

      if (res.ok) {
        wrapper.querySelector<HTMLElement>('.flex.flex-col')!.classList.add('hidden');
        submitBtn.classList.add('hidden');
        wrapper.querySelector<HTMLElement>('label')!.classList.add('hidden');
        successEl.classList.remove('hidden');
      } else {
        errorEl.textContent = lang === 'es'
          ? 'Algo salió mal. Intenta de nuevo.'
          : 'Something went wrong. Try again.';
        errorEl.classList.remove('hidden');
      }
    });
  });
</script>
```

- [ ] **Step 2: Test both form instances**

- Submit email only → success, row in Supabase with `whatsapp = null`
- Submit WhatsApp only → success, row in Supabase with `email = null`
- Submit both fields → success, both columns populated
- Submit nothing → client validation: "Enter your email or WhatsApp number"
- Submit invalid email → "Enter a valid email"
- Submit invalid WhatsApp → "Enter a valid WhatsApp number"
- Toggle news checkbox on → `subscribe_news = true` in Supabase row
- Language switch to ES → error messages appear in Spanish

- [ ] **Step 3: Commit**

```bash
git add landing/src/components/EmailForm.astro
git commit -m "feat(landing): add reusable EmailForm island with i18n"
```

---

## Task 7: Hero, Problem, HowItWorks sections

**Files:**
- Create: `landing/src/components/Hero.astro`
- Create: `landing/src/components/Problem.astro`
- Create: `landing/src/components/HowItWorks.astro`

All text elements carry `data-i18n="<key>"` with English default content. The client runtime replaces text on load.

- [ ] **Step 1: Write `Hero.astro`**

```astro
---
import EmailForm from './EmailForm.astro';
import { t } from '../i18n/translations';
const en = t.en.hero;
---
<section id="top" class="py-20 px-4 bg-[var(--bg-primary)]">
  <div class="max-w-2xl mx-auto text-center">
    <span class="inline-block mb-4 px-3 py-1 rounded-full text-xs font-semibold bg-[var(--accent-lt)] text-[var(--accent)]"
      data-i18n="hero.badge">{en.badge}</span>
    <h1 class="text-4xl sm:text-5xl font-black text-[var(--txt-1)] mb-4 leading-tight"
      data-i18n="hero.headline">{en.headline}</h1>
    <p class="text-lg text-[var(--txt-2)] mb-8"
      data-i18n="hero.subline">{en.subline}</p>
    <div class="flex justify-center">
      <EmailForm id="hero-form" />
    </div>
    <p class="mt-3 text-xs text-[var(--txt-muted)]"
      data-i18n="hero.trust">{en.trust}</p>
  </div>
</section>
```

- [ ] **Step 2: Write `Problem.astro`**

```astro
---
import { t } from '../i18n/translations';
const en = t.en.problem;
---
<section class="py-16 px-4 bg-[var(--bg-dark)]">
  <div class="max-w-4xl mx-auto">
    <h2 class="text-2xl font-bold text-white text-center mb-10"
      data-i18n="problem.heading">{en.heading}</h2>
    <div class="grid sm:grid-cols-3 gap-6">
      {[
        { titleKey: 'problem.card1Title', bodyKey: 'problem.card1Body', title: en.card1Title, body: en.card1Body },
        { titleKey: 'problem.card2Title', bodyKey: 'problem.card2Body', title: en.card2Title, body: en.card2Body },
        { titleKey: 'problem.card3Title', bodyKey: 'problem.card3Body', title: en.card3Title, body: en.card3Body },
      ].map(({ titleKey, bodyKey, title, body }) => (
        <div class="rounded-xl bg-white/5 border border-white/10 p-6">
          <h3 class="font-semibold text-white mb-2" data-i18n={titleKey}>{title}</h3>
          <p class="text-sm text-[var(--txt-muted)]" data-i18n={bodyKey}>{body}</p>
        </div>
      ))}
    </div>
  </div>
</section>
```

- [ ] **Step 3: Write `HowItWorks.astro`**

```astro
---
import { t } from '../i18n/translations';
const en = t.en.howItWorks;
---
<section class="py-16 px-4 bg-[var(--bg-primary)]">
  <div class="max-w-3xl mx-auto">
    <h2 class="text-2xl font-bold text-[var(--txt-1)] text-center mb-10"
      data-i18n="howItWorks.heading">{en.heading}</h2>
    <div class="flex flex-col gap-8">
      {[
        { num: '01', titleKey: 'howItWorks.step1Title', bodyKey: 'howItWorks.step1Body', title: en.step1Title, body: en.step1Body },
        { num: '02', titleKey: 'howItWorks.step2Title', bodyKey: 'howItWorks.step2Body', title: en.step2Title, body: en.step2Body },
        { num: '03', titleKey: 'howItWorks.step3Title', bodyKey: 'howItWorks.step3Body', title: en.step3Title, body: en.step3Body },
      ].map(({ num, titleKey, bodyKey, title, body }) => (
        <div class="flex gap-5 items-start">
          <span class="text-3xl font-black text-[var(--accent-bd)] leading-none">{num}</span>
          <div>
            <h3 class="font-semibold text-[var(--txt-1)] mb-1" data-i18n={titleKey}>{title}</h3>
            <p class="text-sm text-[var(--txt-2)]" data-i18n={bodyKey}>{body}</p>
          </div>
        </div>
      ))}
    </div>
  </div>
</section>
```

- [ ] **Step 4: Import all three in `index.astro` and verify layout**

```bash
npm run dev
```

Expected: Hero → Problem (dark) → HowItWorks visible, no overflow.

- [ ] **Step 5: Commit**

```bash
git add landing/src/components/Hero.astro landing/src/components/Problem.astro landing/src/components/HowItWorks.astro
git commit -m "feat(landing): add Hero, Problem, HowItWorks sections"
```

---

## Task 8: Features, Screenshots, FAQ sections

**Files:**
- Create: `landing/src/components/Features.astro`
- Create: `landing/src/components/Screenshots.astro`
- Create: `landing/src/components/Faq.astro`

Features grid highlights the EN/ES language support as feature f6 (`features.f6Title` / `features.f6Body`).

- [ ] **Step 1: Write `Features.astro`**

```astro
---
import { t } from '../i18n/translations';
const en = t.en.features;
const cards = [
  { icon: '📉', titleKey: 'features.f1Title', bodyKey: 'features.f1Body', title: en.f1Title, body: en.f1Body },
  { icon: '🛒', titleKey: 'features.f2Title', bodyKey: 'features.f2Body', title: en.f2Title, body: en.f2Body },
  { icon: '🚨', titleKey: 'features.f3Title', bodyKey: 'features.f3Body', title: en.f3Title, body: en.f3Body },
  { icon: '📦', titleKey: 'features.f4Title', bodyKey: 'features.f4Body', title: en.f4Title, body: en.f4Body },
  { icon: '🔔', titleKey: 'features.f5Title', bodyKey: 'features.f5Body', title: en.f5Title, body: en.f5Body },
  { icon: '🌐', titleKey: 'features.f6Title', bodyKey: 'features.f6Body', title: en.f6Title, body: en.f6Body },
];
---
<section class="py-16 px-4 bg-[var(--bg-alt)]">
  <div class="max-w-4xl mx-auto">
    <h2 class="text-2xl font-bold text-[var(--txt-1)] text-center mb-10"
      data-i18n="features.heading">{en.heading}</h2>
    <div class="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
      {cards.map(({ icon, titleKey, bodyKey, title, body }) => (
        <div class="bg-[var(--bg-primary)] rounded-xl border border-[var(--accent-bd)] p-5">
          <span class="text-2xl mb-3 block">{icon}</span>
          <h3 class="font-semibold text-[var(--txt-1)] mb-1" data-i18n={titleKey}>{title}</h3>
          <p class="text-sm text-[var(--txt-2)]" data-i18n={bodyKey}>{body}</p>
        </div>
      ))}
    </div>
  </div>
</section>
```

- [ ] **Step 2: Write `Screenshots.astro`**

Vanilla JS carousel. Uses `public/screenshots/` images; placeholder divs until real assets are available.

```astro
---
import { t } from '../i18n/translations';
const en = t.en.screenshots;
const slides = [
  { src: '/screenshots/overview.png', alt: 'Overview screen' },
  { src: '/screenshots/items.png',    alt: 'Items screen' },
  { src: '/screenshots/shopping.png', alt: 'Shopping screen' },
];
---
<section class="py-16 px-4 bg-[var(--bg-alt)]">
  <div class="max-w-4xl mx-auto text-center">
    <h2 class="text-2xl font-bold text-[var(--txt-1)] mb-10"
      data-i18n="screenshots.heading">{en.heading}</h2>
    <div class="relative overflow-hidden">
      <div id="carousel-track" class="flex transition-transform duration-300">
        {slides.map(({ src, alt }, i) => (
          <div class="flex-shrink-0 w-full flex justify-center">
            <div class="w-[220px] h-[440px] bg-[var(--bg-dark)] rounded-3xl overflow-hidden flex items-center justify-center text-white/30 text-sm">
              {/* Replace with <img src={src} alt={alt} class="w-full h-full object-cover" /> when screenshots are ready */}
              {alt}
            </div>
          </div>
        ))}
      </div>
    </div>
    <div class="flex justify-center gap-2 mt-6" id="dots">
      {slides.map((_, i) => (
        <button
          class={`w-2 h-2 rounded-full transition ${i === 0 ? 'bg-[var(--accent)]' : 'bg-[var(--accent-bd)]'}`}
          data-index={i}
        />
      ))}
    </div>
    <div class="flex justify-center gap-4 mt-4">
      <button id="prev" class="px-4 py-2 text-sm rounded-lg border border-[var(--accent-bd)] text-[var(--txt-2)] hover:bg-[var(--accent-lt)]">←</button>
      <button id="next" class="px-4 py-2 text-sm rounded-lg border border-[var(--accent-bd)] text-[var(--txt-2)] hover:bg-[var(--accent-lt)]">→</button>
    </div>
  </div>
</section>

<script>
  const track = document.getElementById('carousel-track')!;
  const dots = document.querySelectorAll('#dots button');
  let current = 0;
  const total = dots.length;

  function go(n: number) {
    current = (n + total) % total;
    track.style.transform = `translateX(-${current * 100}%)`;
    dots.forEach((d, i) => {
      d.classList.toggle('bg-[var(--accent)]', i === current);
      d.classList.toggle('bg-[var(--accent-bd)]', i !== current);
    });
  }

  document.getElementById('prev')!.addEventListener('click', () => go(current - 1));
  document.getElementById('next')!.addEventListener('click', () => go(current + 1));
  dots.forEach((d, i) => d.addEventListener('click', () => go(i)));
</script>
```

- [ ] **Step 3: Write `Faq.astro`**

```astro
---
import { t } from '../i18n/translations';
const en = t.en.faq;
const items = [
  { qKey: 'faq.q1', aKey: 'faq.a1', q: en.q1, a: en.a1 },
  { qKey: 'faq.q2', aKey: 'faq.a2', q: en.q2, a: en.a2 },
  { qKey: 'faq.q3', aKey: 'faq.a3', q: en.q3, a: en.a3 },
  { qKey: 'faq.q4', aKey: 'faq.a4', q: en.q4, a: en.a4 },
];
---
<section class="py-16 px-4 bg-[var(--bg-primary)]">
  <div class="max-w-2xl mx-auto">
    <h2 class="text-2xl font-bold text-[var(--txt-1)] text-center mb-8"
      data-i18n="faq.heading">{en.heading}</h2>
    <div class="flex flex-col gap-3">
      {items.map(({ qKey, aKey, q, a }) => (
        <details class="group border border-[var(--accent-bd)] rounded-lg">
          <summary class="flex justify-between items-center px-5 py-4 cursor-pointer font-medium text-[var(--txt-1)] list-none">
            <span data-i18n={qKey}>{q}</span>
            <span class="ml-4 text-[var(--accent)] group-open:rotate-45 transition-transform">+</span>
          </summary>
          <p class="px-5 pb-4 text-sm text-[var(--txt-2)]" data-i18n={aKey}>{a}</p>
        </details>
      ))}
    </div>
  </div>
</section>
```

- [ ] **Step 4: Verify all sections visible, carousel works**

- [ ] **Step 5: Commit**

```bash
git add landing/src/components/Features.astro landing/src/components/Screenshots.astro landing/src/components/Faq.astro
git commit -m "feat(landing): add Features, Screenshots, FAQ sections"
```

---

## Task 9: FooterCta + Footer + wire index.astro

**Files:**
- Create: `landing/src/components/FooterCta.astro`
- Create: `landing/src/components/Footer.astro`
- Modify: `landing/src/pages/index.astro`

- [ ] **Step 1: Write `FooterCta.astro`**

```astro
---
import EmailForm from './EmailForm.astro';
import { t } from '../i18n/translations';
const en = t.en.footerCta;
---
<section class="py-16 px-4 bg-[var(--accent)] text-center">
  <h2 class="text-2xl font-bold text-white mb-2"
    data-i18n="footerCta.heading">{en.heading}</h2>
  <p class="text-orange-100 mb-6 text-sm"
    data-i18n="footerCta.subline">{en.subline}</p>
  <div class="flex justify-center">
    <EmailForm id="footer-form" dark />
  </div>
</section>
```

- [ ] **Step 2: Write `Footer.astro`**

```astro
---
import { t } from '../i18n/translations';
const en = t.en.footer;
---
<footer class="py-4 px-4 bg-[var(--bg-dark)] text-center">
  <p class="text-xs text-[var(--txt-muted)]"
    data-i18n="footer.copy">{en.copy}</p>
</footer>
```

- [ ] **Step 3: Wire `index.astro`**

```astro
---
import Navbar from '../components/Navbar.astro';
import Hero from '../components/Hero.astro';
import Problem from '../components/Problem.astro';
import HowItWorks from '../components/HowItWorks.astro';
import Features from '../components/Features.astro';
import Screenshots from '../components/Screenshots.astro';
import Faq from '../components/Faq.astro';
import FooterCta from '../components/FooterCta.astro';
import Footer from '../components/Footer.astro';
import '../styles/global.css';
---
<!doctype html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>Ventri — Never run out of anything again</title>
    <meta name="description" content="Ventri tracks your household inventory and tells you what to buy before you run out." />
  </head>
  <body>
    <Navbar />
    <main>
      <Hero />
      <Problem />
      <HowItWorks />
      <Features />
      <Screenshots />
      <Faq />
      <FooterCta />
    </main>
    <Footer />
    <script>
      import { initI18n } from '../i18n/i18n-client';
      document.addEventListener('DOMContentLoaded', initI18n);
    </script>
  </body>
</html>
```

- [ ] **Step 4: Run full build**

```bash
npm run build
```

Expected: BUILD SUCCESS, no TypeScript errors.

- [ ] **Step 5: Commit**

```bash
git add landing/src/components/FooterCta.astro landing/src/components/Footer.astro landing/src/pages/index.astro
git commit -m "feat(landing): wire all sections in index.astro"
```

---

## Verification Checklist

- [ ] `cd landing && npm run dev` — all 9 sections visible, no console errors
- [ ] Navbar language toggle switches all text to Spanish, button shows "EN"
- [ ] Refreshing after selecting ES preserves Spanish (localStorage)
- [ ] Switching back to EN restores English
- [ ] EmailForm in both Hero and FooterCta submits correctly in both languages
- [ ] Submit email only → success, `whatsapp = null` in Supabase
- [ ] Submit WhatsApp only → success, `email = null` in Supabase
- [ ] Submit both fields → success, both columns populated
- [ ] Submit neither field → "Enter your email or WhatsApp number" (in active language)
- [ ] Submit invalid email → "Enter a valid email" (in active language)
- [ ] Submit invalid WhatsApp → "Enter a valid WhatsApp number" (in active language)
- [ ] Submit same email twice → shows success (23505 treated as success)
- [ ] News toggle off (default) → `subscribe_news = false` in Supabase
- [ ] News toggle on → `subscribe_news = true` in Supabase
- [ ] Error messages appear in the currently active language
- [ ] Screenshots carousel: prev/next/dots cycle correctly
- [ ] FAQ items expand/collapse natively
- [ ] Resize to 375px — single column, no horizontal overflow
- [ ] `npm run build` completes without errors
- [ ] Deploy to Vercel with `PUBLIC_SUPABASE_URL` + `SUPABASE_SERVICE_ROLE_KEY` in project env → smoke-test live form and language toggle
