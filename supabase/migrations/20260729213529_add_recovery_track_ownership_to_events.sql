alter table public.recovery_tracks
    add constraint recovery_tracks_id_user_program_unique
    unique (id, user_id, program_id);

alter table public.tracking_events
    add column if not exists recovery_track_id uuid;

alter table public.rescue_sessions
    add column if not exists recovery_track_id uuid;

update public.tracking_events as event
set recovery_track_id = (
    select track.id
    from public.recovery_tracks as track
    where track.user_id = event.user_id
      and track.program_id = event.program_id
    order by
        case
            when event.occurred_at >= track.started_at
             and (track.archived_at is null or event.occurred_at <= track.archived_at)
            then 0 else 1
        end,
        abs(extract(epoch from (event.occurred_at - track.started_at))),
        track.started_at desc,
        track.id
    limit 1
)
where event.recovery_track_id is null;

update public.rescue_sessions as session
set recovery_track_id = (
    select track.id
    from public.recovery_tracks as track
    where track.user_id = session.user_id
      and track.program_id = session.program_id
    order by
        case
            when session.started_at >= track.started_at
             and (track.archived_at is null or session.started_at <= track.archived_at)
            then 0 else 1
        end,
        abs(extract(epoch from (session.started_at - track.started_at))),
        track.started_at desc,
        track.id
    limit 1
)
where session.recovery_track_id is null;

alter table public.tracking_events
    add constraint tracking_events_recovery_track_owner_program_fkey
    foreign key (recovery_track_id, user_id, program_id)
    references public.recovery_tracks (id, user_id, program_id)
    on delete cascade
    not valid;

alter table public.tracking_events
    validate constraint tracking_events_recovery_track_owner_program_fkey;

alter table public.rescue_sessions
    add constraint rescue_sessions_recovery_track_owner_program_fkey
    foreign key (recovery_track_id, user_id, program_id)
    references public.recovery_tracks (id, user_id, program_id)
    on delete cascade
    not valid;

alter table public.rescue_sessions
    validate constraint rescue_sessions_recovery_track_owner_program_fkey;

create index if not exists tracking_events_recovery_track_time_idx
    on public.tracking_events (user_id, recovery_track_id, occurred_at desc)
    where recovery_track_id is not null;

create index if not exists rescue_sessions_recovery_track_time_idx
    on public.rescue_sessions (user_id, recovery_track_id, started_at desc)
    where recovery_track_id is not null;

comment on column public.tracking_events.recovery_track_id is
    'Permanent Recovery Track journey owning this event. Nullable only for legacy client compatibility.';

comment on column public.rescue_sessions.recovery_track_id is
    'Permanent Recovery Track journey owning this Rescue session. Nullable only for legacy client compatibility.';
