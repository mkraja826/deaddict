create table public.user_programs (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  program_id text not null check (program_id ~ '^[a-z][a-z0-9_]{2,49}$'),
  activated_at timestamptz not null,
  archived_at timestamptz,
  client_updated_at timestamptz not null,
  created_at timestamptz not null default now(),
  unique (user_id, id)
);

create table public.tracking_events (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  program_id text not null check (program_id ~ '^[a-z][a-z0-9_]{2,49}$'),
  kind text not null check (kind in ('ACTIVITY', 'URGE', 'CRAVING', 'SLIP', 'QUANTITY', 'TIME', 'COST')),
  quantity numeric check (quantity is null or quantity >= 0),
  unit text,
  cost_minor_units bigint check (cost_minor_units is null or cost_minor_units >= 0),
  urge_intensity smallint check (urge_intensity is null or urge_intensity between 1 and 5),
  occurred_at timestamptz not null,
  client_updated_at timestamptz not null,
  created_at timestamptz not null default now(),
  unique (user_id, id)
);

create table public.rescue_sessions (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  program_id text not null check (program_id ~ '^[a-z][a-z0-9_]{2,49}$'),
  started_at timestamptz not null,
  completed_at timestamptz,
  initial_urge smallint not null check (initial_urge between 1 and 5),
  final_urge smallint check (final_urge is null or final_urge between 1 and 5),
  trigger_key text,
  action_keys text[] not null default '{}',
  outcome text check (outcome is null or outcome in ('REDUCED', 'SAME', 'INCREASED', 'NOT_COMPLETED')),
  client_updated_at timestamptz not null,
  created_at timestamptz not null default now(),
  unique (user_id, id)
);

create index user_programs_user_id_idx on public.user_programs (user_id);
create index tracking_events_user_occurred_idx on public.tracking_events (user_id, occurred_at desc);
create index rescue_sessions_user_started_idx on public.rescue_sessions (user_id, started_at desc);

alter table public.user_programs enable row level security;
alter table public.tracking_events enable row level security;
alter table public.rescue_sessions enable row level security;

grant select, insert, update, delete on public.user_programs to authenticated;
grant select, insert, update, delete on public.tracking_events to authenticated;
grant select, insert, update, delete on public.rescue_sessions to authenticated;

create policy user_programs_select_own
on public.user_programs for select to authenticated
using ((select auth.uid()) = user_id);
create policy user_programs_insert_own
on public.user_programs for insert to authenticated
with check ((select auth.uid()) = user_id);
create policy user_programs_update_own
on public.user_programs for update to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);
create policy user_programs_delete_own
on public.user_programs for delete to authenticated
using ((select auth.uid()) = user_id);

create policy tracking_events_select_own
on public.tracking_events for select to authenticated
using ((select auth.uid()) = user_id);
create policy tracking_events_insert_own
on public.tracking_events for insert to authenticated
with check ((select auth.uid()) = user_id);
create policy tracking_events_update_own
on public.tracking_events for update to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);
create policy tracking_events_delete_own
on public.tracking_events for delete to authenticated
using ((select auth.uid()) = user_id);

create policy rescue_sessions_select_own
on public.rescue_sessions for select to authenticated
using ((select auth.uid()) = user_id);
create policy rescue_sessions_insert_own
on public.rescue_sessions for insert to authenticated
with check ((select auth.uid()) = user_id);
create policy rescue_sessions_update_own
on public.rescue_sessions for update to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);
create policy rescue_sessions_delete_own
on public.rescue_sessions for delete to authenticated
using ((select auth.uid()) = user_id);
