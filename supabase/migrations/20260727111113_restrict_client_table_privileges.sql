revoke all privileges on table
  public.user_programs,
  public.tracking_events,
  public.rescue_sessions
from anon, authenticated;

grant select, insert, update, delete on table
  public.user_programs,
  public.tracking_events,
  public.rescue_sessions
to authenticated;
