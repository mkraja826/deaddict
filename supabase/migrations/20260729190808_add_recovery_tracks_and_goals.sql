create table public.recovery_tracks (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    program_id text not null,
    display_alias text,
    role text not null,
    status text not null,
    started_at timestamptz not null,
    paused_at timestamptz,
    maintenance_at timestamptz,
    archived_at timestamptz,
    client_updated_at timestamptz not null,
    revision bigint not null default 0,
    created_at timestamptz not null default now(),
    server_updated_at timestamptz not null default now(),
    constraint recovery_tracks_id_user_unique unique (id, user_id),
    constraint recovery_tracks_program_id_nonblank check (length(btrim(program_id)) > 0),
    constraint recovery_tracks_display_alias_valid check (
        display_alias is null or (length(btrim(display_alias)) between 1 and 80)
    ),
    constraint recovery_tracks_role_valid check (role in ('PRIMARY', 'SUPPORTING')),
    constraint recovery_tracks_status_valid check (
        status in ('ACTIVE', 'PAUSED', 'MAINTENANCE', 'ARCHIVED')
    ),
    constraint recovery_tracks_revision_valid check (revision >= 0),
    constraint recovery_tracks_updated_after_created check (client_updated_at >= created_at),
    constraint recovery_tracks_primary_eligible check (
        role <> 'PRIMARY' or status in ('ACTIVE', 'MAINTENANCE')
    ),
    constraint recovery_tracks_lifecycle_valid check (
        (status = 'ACTIVE' and paused_at is null and maintenance_at is null and archived_at is null)
        or (status = 'PAUSED' and paused_at is not null and maintenance_at is null and archived_at is null)
        or (status = 'MAINTENANCE' and paused_at is null and maintenance_at is not null and archived_at is null)
        or (status = 'ARCHIVED' and paused_at is null and maintenance_at is null and archived_at is not null)
    )
);

create unique index recovery_tracks_one_open_program_per_user
    on public.recovery_tracks (user_id, program_id)
    where status in ('ACTIVE', 'PAUSED', 'MAINTENANCE');

create unique index recovery_tracks_one_primary_per_user
    on public.recovery_tracks (user_id)
    where role = 'PRIMARY' and status in ('ACTIVE', 'MAINTENANCE');

create index recovery_tracks_user_status_idx
    on public.recovery_tracks (user_id, status, started_at);

create index recovery_tracks_client_updated_idx
    on public.recovery_tracks (client_updated_at);

create table public.recovery_goal_versions (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    recovery_track_id uuid not null,
    goal_type text not null,
    target_value numeric,
    unit_key text,
    period_type text,
    title text,
    effective_from timestamptz not null,
    effective_until timestamptz,
    client_updated_at timestamptz not null,
    revision bigint not null default 0,
    created_at timestamptz not null default now(),
    server_updated_at timestamptz not null default now(),
    constraint recovery_goal_track_owner_fk
        foreign key (recovery_track_id, user_id)
        references public.recovery_tracks (id, user_id)
        on delete cascade,
    constraint recovery_goal_type_valid check (
        goal_type in (
            'QUIT_COMPLETELY',
            'REDUCE_QUANTITY',
            'DAILY_LIMIT',
            'WEEKLY_LIMIT',
            'TIME_LIMIT',
            'SPENDING_LIMIT',
            'DELAY_FIRST_USE',
            'NO_USE_PERIOD',
            'AWARENESS_ONLY',
            'CUSTOM'
        )
    ),
    constraint recovery_goal_target_valid check (target_value is null or target_value >= 0),
    constraint recovery_goal_unit_valid check (
        target_value is null or (unit_key is not null and length(btrim(unit_key)) > 0)
    ),
    constraint recovery_goal_period_valid check (
        period_type is null or period_type in ('DAY', 'WEEK', 'MONTH', 'SESSION')
    ),
    constraint recovery_goal_title_valid check (
        title is null or (length(btrim(title)) between 1 and 120)
    ),
    constraint recovery_goal_effective_range_valid check (
        effective_until is null or effective_until > effective_from
    ),
    constraint recovery_goal_revision_valid check (revision >= 0),
    constraint recovery_goal_updated_after_created check (client_updated_at >= created_at)
);

create unique index recovery_goal_versions_one_current_per_track
    on public.recovery_goal_versions (recovery_track_id)
    where effective_until is null;

create index recovery_goal_versions_user_track_idx
    on public.recovery_goal_versions (user_id, recovery_track_id, effective_from);

create index recovery_goal_versions_client_updated_idx
    on public.recovery_goal_versions (client_updated_at);

create or replace function public.set_server_updated_at()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
begin
    new.server_updated_at = now();
    return new;
end;
$$;

create trigger recovery_tracks_set_server_updated_at
before update on public.recovery_tracks
for each row execute function public.set_server_updated_at();

create trigger recovery_goal_versions_set_server_updated_at
before update on public.recovery_goal_versions
for each row execute function public.set_server_updated_at();

alter table public.recovery_tracks enable row level security;
alter table public.recovery_goal_versions enable row level security;

create policy recovery_tracks_select_own
on public.recovery_tracks for select
to authenticated
using ((select auth.uid()) = user_id);

create policy recovery_tracks_insert_own
on public.recovery_tracks for insert
to authenticated
with check ((select auth.uid()) = user_id);

create policy recovery_tracks_update_own
on public.recovery_tracks for update
to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

create policy recovery_tracks_delete_own
on public.recovery_tracks for delete
to authenticated
using ((select auth.uid()) = user_id);

create policy recovery_goal_versions_select_own
on public.recovery_goal_versions for select
to authenticated
using ((select auth.uid()) = user_id);

create policy recovery_goal_versions_insert_own
on public.recovery_goal_versions for insert
to authenticated
with check ((select auth.uid()) = user_id);

create policy recovery_goal_versions_update_own
on public.recovery_goal_versions for update
to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

create policy recovery_goal_versions_delete_own
on public.recovery_goal_versions for delete
to authenticated
using ((select auth.uid()) = user_id);

revoke all privileges on table
    public.recovery_tracks,
    public.recovery_goal_versions
from anon, authenticated;

grant select, insert, update, delete on table
    public.recovery_tracks,
    public.recovery_goal_versions
to authenticated;

insert into public.recovery_tracks (
    id,
    user_id,
    program_id,
    display_alias,
    role,
    status,
    started_at,
    paused_at,
    maintenance_at,
    archived_at,
    client_updated_at,
    revision,
    created_at
)
select
    legacy.id,
    legacy.user_id,
    legacy.program_id,
    null,
    case
        when legacy.archived_at is null
             and legacy.id = (
                 select candidate.id
                 from public.user_programs candidate
                 where candidate.user_id = legacy.user_id
                   and candidate.archived_at is null
                 order by candidate.activated_at, candidate.id
                 limit 1
             )
        then 'PRIMARY'
        else 'SUPPORTING'
    end,
    case when legacy.archived_at is null then 'ACTIVE' else 'ARCHIVED' end,
    legacy.activated_at,
    null,
    null,
    legacy.archived_at,
    legacy.client_updated_at,
    0,
    legacy.created_at
from public.user_programs legacy
on conflict (id) do nothing;

insert into public.recovery_goal_versions (
    id,
    user_id,
    recovery_track_id,
    goal_type,
    target_value,
    unit_key,
    period_type,
    title,
    effective_from,
    effective_until,
    client_updated_at,
    revision,
    created_at
)
select
    gen_random_uuid(),
    track.user_id,
    track.id,
    'AWARENESS_ONLY',
    null,
    null,
    null,
    null,
    track.started_at,
    case when track.status = 'ARCHIVED' then track.archived_at else null end,
    track.client_updated_at,
    0,
    track.created_at
from public.recovery_tracks track
where not exists (
    select 1
    from public.recovery_goal_versions existing
    where existing.recovery_track_id = track.id
);