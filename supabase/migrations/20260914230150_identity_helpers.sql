-- Identity helpers used throughout RLS policies.
-- SECURITY DEFINER lets these read profiles without triggering recursive RLS
-- evaluation on the profiles table itself. Must be created after
-- public.profiles exists (language sql functions are validated against
-- existing relations at CREATE time).
create or replace function public.current_school_id()
returns uuid
language sql
stable
security definer
set search_path = public
as $$
  select school_id from public.profiles where id = auth.uid()
$$;

create or replace function public.current_role()
returns text
language sql
stable
security definer
set search_path = public
as $$
  select role from public.profiles where id = auth.uid()
$$;

create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select public.current_role() = 'admin'
$$;

create or replace function public.is_teacher()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select public.current_role() = 'teacher'
$$;

create or replace function public.is_student()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select public.current_role() = 'student'
$$;

-- Prevent a client from escalating their own role/school via a self-update.
-- Service-role calls (used by admin-provisioning Edge Functions) bypass RLS
-- and this trigger entirely, so legitimate role/school assignment still works.
create or replace function public.prevent_profile_privilege_escalation()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.role() = 'authenticated' and not public.is_admin() then
    if new.role is distinct from old.role or new.school_id is distinct from old.school_id then
      raise exception 'Only an admin may change role or school_id';
    end if;
  end if;
  return new;
end;
$$;

create trigger trg_profiles_no_self_escalation before update on public.profiles
  for each row execute function public.prevent_profile_privilege_escalation();
