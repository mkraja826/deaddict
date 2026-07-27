alter table public.tracking_events
add column if not exists trigger_key text;
