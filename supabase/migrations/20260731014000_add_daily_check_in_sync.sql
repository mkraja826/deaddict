do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'recovery_goal_versions_id_user_unique'
          and conrelid = 'public.recovery_goal_versions'::regclass
    ) then
        alter table public.recovery_goal_versions
            add constraint recovery_goal_versions_id_user_unique unique (id, user_id);
    end if;
end
$$;

create table public.daily_check_ins (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    local_date_epoch_day bigint not null,
    mood smallint,
    stress smallint,
    energy smallint,
    sleep_quality smallint,
    created_at timestamptz not null,
    client_updated_at timestamptz not null,
    revision bigint not null default 0,
    server_updated_at timestamptz not null default now(),
    constraint daily_check_ins_id_user_unique unique (id, user_id),
    constraint daily_check_ins_user_date_unique unique (user_id, local_date_epoch_day),
    constraint daily_check_ins_date_valid check (local_date_epoch_day >= 0),
    constraint daily_check_ins_mood_valid check (mood is null or mood between 1 and 5),
    constraint daily_check_ins_stress_valid check (stress is null or stress between 1 and 5),
    constraint daily_check_ins_energy_valid check (energy is null or energy between 1 and 5),
    constraint daily_check_ins_sleep_valid check (sleep_quality is null or sleep_quality between 1 and 5),
    constraint daily_check_ins_revision_valid check (revision >= 0),
    constraint daily_check_ins_updated_after_created check (client_updated_at >= created_at)
);

create index daily_check_ins_user_date_idx
    on public.daily_check_ins (user_id, local_date_epoch_day desc);

create index daily_check_ins_client_updated_idx
    on public.daily_check_ins (client_updated_at);

create table public.track_check_in_entries (
    id uuid primary key,
    user_id uuid not null references auth.users(id) on delete cascade,
    local_date_epoch_day bigint not null,
    recovery_track_id uuid not null,
    goal_version_id uuid,
    outcome text not null,
    measured_value numeric,
    unit_key text,
    peak_urge smallint,
    created_at timestamptz not null,
    client_updated_at timestamptz not null,
    revision bigint not null default 0,
    server_updated_at timestamptz not null default now(),
    constraint track_check_in_entries_id_user_unique unique (id, user_id),
    constraint track_check_in_entries_daily_fk
        foreign key (user_id, local_date_epoch_day)
        references public.daily_check_ins (user_id, local_date_epoch_day)
        on update cascade
        on delete cascade,
    constraint track_check_in_entries_track_owner_fk
        foreign key (recovery_track_id, user_id)
        references public.recovery_tracks (id, user_id)
        on update cascade
        on delete cascade,
    constraint track_check_in_entries_goal_owner_fk
        foreign key (goal_version_id, user_id)
        references public.recovery_goal_versions (id, user_id)
        on update cascade
        on delete set null,
    constraint track_check_in_entries_owner_date_track_unique
        unique (user_id, local_date_epoch_day, recovery_track_id),
    constraint track_check_in_entries_date_valid check (local_date_epoch_day >= 0),
    constraint track_check_in_entries_outcome_valid check (
        outcome in (
            'GOAL_MET',
            'GOAL_PARTLY_MET',
            'GOAL_NOT_MET',
            'SLIP',
            'AWARENESS_LOGGED'
        )
    ),
    constraint track_check_in_entries_measurement_valid check (
        (measured_value is null and unit_key is null)
        or (
            measured_value is not null
            and measured_value >= 0
            and unit_key is not null
            and length(btrim(unit_key)) > 0
        )
    ),
    constraint track_check_in_entries_peak_urge_valid check (
        peak_urge is null or peak_urge between 1 and 5
    ),
    constraint track_check_in_entries_revision_valid check (revision >= 0),
    constraint track_check_in_entries_updated_after_created check (client_updated_at >= created_at)
);

create index track_check_in_entries_user_date_idx
    on public.track_check_in_entries (user_id, local_date_epoch_day desc);

create index track_check_in_entries_track_date_idx
    on public.track_check_in_entries (recovery_track_id, local_date_epoch_day desc);

create index track_check_in_entries_goal_idx
    on public.track_check_in_entries (goal_version_id);

create index track_check_in_entries_client_updated_idx
    on public.track_check_in_entries (client_updated_at);

create or replace function public.prefer_newer_daily_check_in()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
begin
    if new.revision < old.revision
       or (new.revision = old.revision and new.client_updated_at < old.client_updated_at)
       or (
           new.revision = old.revision
           and new.client_updated_at = old.client_updated_at
           and new.id::text < old.id::text
       ) then
        return old;
    end if;
    return new;
end;
$$;

create or replace function public.prefer_newer_track_check_in_entry()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
begin
    if new.revision < old.revision
       or (new.revision = old.revision and new.client_updated_at < old.client_updated_at)
       or (
           new.revision = old.revision
           and new.client_updated_at = old.client_updated_at
           and new.id::text < old.id::text
       ) then
        return old;
    end if;
    return new;
end;
$$;

create trigger daily_check_ins_a_prefer_newer
before update on public.daily_check_ins
for each row execute function public.prefer_newer_daily_check_in();

create trigger daily_check_ins_z_set_server_updated_at
before update on public.daily_check_ins
for each row execute function public.set_server_updated_at();

create trigger track_check_in_entries_a_prefer_newer
before update on public.track_check_in_entries
for each row execute function public.prefer_newer_track_check_in_entry();

create trigger track_check_in_entries_z_set_server_updated_at
before update on public.track_check_in_entries
for each row execute function public.set_server_updated_at();

alter table public.daily_check_ins enable row level security;
alter table public.track_check_in_entries enable row level security;

create policy daily_check_ins_select_own
on public.daily_check_ins for select
to authenticated
using ((select auth.uid()) = user_id);

create policy daily_check_ins_insert_own
on public.daily_check_ins for insert
to authenticated
with check ((select auth.uid()) = user_id);

create policy daily_check_ins_update_own
on public.daily_check_ins for update
to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

create policy daily_check_ins_delete_own
on public.daily_check_ins for delete
to authenticated
using ((select auth.uid()) = user_id);

create policy track_check_in_entries_select_own
on public.track_check_in_entries for select
to authenticated
using ((select auth.uid()) = user_id);

create policy track_check_in_entries_insert_own
on public.track_check_in_entries for insert
to authenticated
with check ((select auth.uid()) = user_id);

create policy track_check_in_entries_update_own
on public.track_check_in_entries for update
to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

create policy track_check_in_entries_delete_own
on public.track_check_in_entries for delete
to authenticated
using ((select auth.uid()) = user_id);

revoke all privileges on table
    public.daily_check_ins,
    public.track_check_in_entries
from anon, authenticated;

grant select, insert, update, delete on table
    public.daily_check_ins,
    public.track_check_in_entries
to authenticated;
