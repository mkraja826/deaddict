create index recovery_goal_versions_track_owner_idx
    on public.recovery_goal_versions (recovery_track_id, user_id);