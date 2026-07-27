begin;
create extension if not exists pgtap with schema extensions;
select plan(12);

insert into auth.users (
  id, instance_id, aud, role, email, encrypted_password,
  email_confirmed_at, created_at, updated_at
) values
  ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'a@example.test', '', now(), now(), now()),
  ('20000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000000', 'authenticated', 'authenticated', 'b@example.test', '', now(), now(), now());

set local role authenticated;
select set_config('request.jwt.claim.sub', '10000000-0000-0000-0000-000000000001', true);
select set_config('request.jwt.claim.role', 'authenticated', true);

select lives_ok(
  $$insert into public.user_programs (id, user_id, program_id, activated_at, client_updated_at)
    values ('11000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'gaming', now(), now())$$,
  'user A can insert own program'
);
select throws_ok(
  $$insert into public.user_programs (id, user_id, program_id, activated_at, client_updated_at)
    values ('12000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 'gaming', now(), now())$$,
  '42501',
  'new row violates row-level security policy for table "user_programs"',
  'user A cannot insert for user B'
);

reset role;
insert into public.tracking_events (id, user_id, program_id, kind, occurred_at, client_updated_at)
values ('21000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 'caffeine', 'URGE', now(), now());
insert into public.rescue_sessions (id, user_id, program_id, started_at, initial_urge, client_updated_at)
values ('31000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 'alcohol', now(), 4, now());

set local role authenticated;
select set_config('request.jwt.claim.sub', '10000000-0000-0000-0000-000000000001', true);
select set_config('request.jwt.claim.role', 'authenticated', true);

select results_eq(
  $$select count(*) from public.user_programs$$,
  array[1::bigint],
  'user A sees only own programs'
);
select results_eq(
  $$select count(*) from public.tracking_events$$,
  array[0::bigint],
  'user A cannot read user B tracking'
);
select results_eq(
  $$select count(*) from public.rescue_sessions$$,
  array[0::bigint],
  'user A cannot read user B Rescue sessions'
);
select results_eq(
  $$update public.tracking_events set kind = 'SLIP' returning id$$,
  array[]::uuid[],
  'user A cannot update user B tracking'
);
select results_eq(
  $$delete from public.tracking_events returning id$$,
  array[]::uuid[],
  'user A cannot delete user B tracking'
);
select results_eq(
  $$update public.rescue_sessions set final_urge = 2 returning id$$,
  array[]::uuid[],
  'user A cannot update user B Rescue session'
);
select results_eq(
  $$delete from public.rescue_sessions returning id$$,
  array[]::uuid[],
  'user A cannot delete user B Rescue session'
);
select throws_ok(
  $$update public.user_programs
    set user_id = '20000000-0000-0000-0000-000000000002'
    where id = '11000000-0000-0000-0000-000000000001'$$,
  '42501',
  'new row violates row-level security policy for table "user_programs"',
  'user A cannot transfer ownership'
);
select results_eq(
  $$select count(*) from public.tracking_events where user_id = '20000000-0000-0000-0000-000000000002'$$,
  array[0::bigint],
  'explicit user_id filters do not bypass RLS'
);
select results_eq(
  $$select count(*) from public.rescue_sessions where user_id = '20000000-0000-0000-0000-000000000002'$$,
  array[0::bigint],
  'explicit Rescue user_id filters do not bypass RLS'
);

select * from finish();
rollback;
