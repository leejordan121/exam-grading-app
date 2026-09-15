-- Extensions
create extension if not exists "pgcrypto";

-- Generic updated_at trigger
create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

-- NOTE: identity helper functions (current_school_id, current_role,
-- is_admin, is_teacher, is_student) that read public.profiles live in
-- 20260914230150_identity_helpers.sql, AFTER the profiles table is
-- created. `language sql` functions are validated against existing
-- relations at CREATE time, so they can't be defined here.
