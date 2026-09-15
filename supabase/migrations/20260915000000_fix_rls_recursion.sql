-- Bug fix: exams_select (student branch) queries exam_assignments, and
-- exam_assignments' policies query exams — Postgres applies RLS to every
-- table access including inside another policy's subquery, so this pair
-- forms an infinite recursion cycle ("infinite recursion detected in
-- policy for relation exams"). Every other policy that reads from exams
-- or exam_assignments inherits the same failure transitively.
--
-- Fix: replace the cross-references with SECURITY DEFINER helper
-- functions (same pattern as current_school_id()/is_admin() in
-- 20260914230150_identity_helpers.sql) so checking one table's ownership
-- never re-triggers the other table's RLS policy.

create or replace function public.owns_exam(p_exam_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.exams e
    where e.id = p_exam_id
      and e.school_id = public.current_school_id()
      and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
  )
$$;

create or replace function public.is_assigned_to_exam(p_exam_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.exam_assignments ea
    where ea.exam_id = p_exam_id and ea.student_id = auth.uid()
  )
$$;

drop policy if exists exams_select on public.exams;
create policy exams_select on public.exams
  for select using (
    school_id = public.current_school_id()
    and (
      public.is_admin()
      or (public.is_teacher() and teacher_id = auth.uid())
      or (public.is_student() and status <> 'draft' and public.is_assigned_to_exam(id))
    )
  );

drop policy if exists exam_assignments_select on public.exam_assignments;
create policy exam_assignments_select on public.exam_assignments
  for select using (
    student_id = auth.uid()
    or public.owns_exam(exam_id)
  );

drop policy if exists exam_assignments_write_owner on public.exam_assignments;
create policy exam_assignments_write_owner on public.exam_assignments
  for all using (public.owns_exam(exam_id))
  with check (public.owns_exam(exam_id));
