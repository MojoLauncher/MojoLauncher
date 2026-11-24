-- KarabakhLauncher account roles and owner-controlled admin approvals.
-- Apply in Supabase SQL Editor AFTER enabling Supabase Auth.
create extension if not exists pgcrypto;

create table if not exists public.launcher_profiles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  username text unique not null,
  role text not null default 'user' check (role in ('user','admin','owner')),
  created_at timestamptz not null default now()
);

create table if not exists public.admin_requests (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  username text not null,
  status text not null default 'pending' check (status in ('pending','approved','rejected')),
  created_at timestamptz not null default now(),
  reviewed_at timestamptz,
  reviewed_by uuid references auth.users(id)
);

create index if not exists admin_requests_status_created_idx
  on public.admin_requests(status, created_at desc);

alter table public.launcher_profiles enable row level security;
alter table public.admin_requests enable row level security;

-- A signed-in user may read their own profile only.
drop policy if exists "profile self read" on public.launcher_profiles;
create policy "profile self read" on public.launcher_profiles
  for select to authenticated using (auth.uid() = user_id);

-- Users may submit/read only their own request; role changes are never client-writable.
drop policy if exists "request self read" on public.admin_requests;
create policy "request self read" on public.admin_requests
  for select to authenticated using (auth.uid() = user_id);

drop policy if exists "request self create" on public.admin_requests;
create policy "request self create" on public.admin_requests
  for insert to authenticated with check (auth.uid() = user_id and status = 'pending');

-- No client-side update/delete policies. Owner review must run in a trusted Edge Function
-- using the service role key (stored only as a Supabase secret, never in the APK).
