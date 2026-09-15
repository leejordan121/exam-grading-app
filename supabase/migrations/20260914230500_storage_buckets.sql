-- =========================================================================
-- Storage buckets
-- All private. Path convention: {school_id}/{exam_id | submission_id}/...
-- so storage.foldername(name) gives us [1]=school_id, [2]=exam_id/submission_id
-- for use in RLS policies below (spec section 46 / 91).
-- =========================================================================
insert into storage.buckets (id, name, public)
values
  ('exam-papers', 'exam-papers', false),
  ('answer-keys', 'answer-keys', false),
  ('student-submissions', 'student-submissions', false),
  ('processed-images', 'processed-images', false),
  ('grading-reports', 'grading-reports', false),
  ('exports', 'exports', false)
on conflict (id) do nothing;

-- ---------------------------------------------------------------------
-- exam-papers: {school_id}/{exam_id}/paper.pdf
-- Readable by admin/teacher(owner) always; by student only once the exam
-- is published-or-later and assigned to them.
-- ---------------------------------------------------------------------
create policy exam_papers_select on storage.objects
  for select using (
    bucket_id = 'exam-papers'
    and (storage.foldername(name))[1]::uuid = public.current_school_id()
    and exists (
      select 1 from public.exams e
      where e.id = (storage.foldername(name))[2]::uuid
        and e.school_id = public.current_school_id()
        and (
          public.is_admin()
          or (public.is_teacher() and e.teacher_id = auth.uid())
          or (
            public.is_student()
            and e.status <> 'draft'
            and exists (
              select 1 from public.exam_assignments ea
              where ea.exam_id = e.id and ea.student_id = auth.uid()
            )
          )
        )
    )
  );

create policy exam_papers_write_owner on storage.objects
  for all using (
    bucket_id = 'exam-papers'
    and (storage.foldername(name))[1]::uuid = public.current_school_id()
    and exists (
      select 1 from public.exams e
      where e.id = (storage.foldername(name))[2]::uuid
        and e.school_id = public.current_school_id()
        and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
    )
  )
  with check (
    bucket_id = 'exam-papers'
    and (storage.foldername(name))[1]::uuid = public.current_school_id()
    and exists (
      select 1 from public.exams e
      where e.id = (storage.foldername(name))[2]::uuid
        and e.school_id = public.current_school_id()
        and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
    )
  );

-- ---------------------------------------------------------------------
-- answer-keys: {school_id}/{exam_id}/... — teacher(owner)/admin ONLY.
-- No student policy exists on this bucket at all.
-- ---------------------------------------------------------------------
create policy answer_keys_owner_only on storage.objects
  for all using (
    bucket_id = 'answer-keys'
    and (storage.foldername(name))[1]::uuid = public.current_school_id()
    and exists (
      select 1 from public.exams e
      where e.id = (storage.foldername(name))[2]::uuid
        and e.school_id = public.current_school_id()
        and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
    )
  )
  with check (
    bucket_id = 'answer-keys'
    and (storage.foldername(name))[1]::uuid = public.current_school_id()
    and exists (
      select 1 from public.exams e
      where e.id = (storage.foldername(name))[2]::uuid
        and e.school_id = public.current_school_id()
        and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
    )
  );

-- ---------------------------------------------------------------------
-- student-submissions: {school_id}/{submission_id}/page-N.jpg
-- Student owns their own submission while it's still in progress;
-- teacher/admin can read (for grading/review) but not write.
-- ---------------------------------------------------------------------
create policy student_submissions_select on storage.objects
  for select using (
    bucket_id = 'student-submissions'
    and (storage.foldername(name))[1]::uuid = public.current_school_id()
    and exists (
      select 1 from public.submissions s
      join public.exams e on e.id = s.exam_id
      where s.id = (storage.foldername(name))[2]::uuid
        and (
          s.student_id = auth.uid()
          or (e.school_id = public.current_school_id() and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid())))
        )
    )
  );

create policy student_submissions_write_own on storage.objects
  for insert with check (
    bucket_id = 'student-submissions'
    and (storage.foldername(name))[1]::uuid = public.current_school_id()
    and exists (
      select 1 from public.submissions s
      where s.id = (storage.foldername(name))[2]::uuid
        and s.student_id = auth.uid()
        and s.status in ('draft', 'uploading')
    )
  );

create policy student_submissions_delete_own on storage.objects
  for delete using (
    bucket_id = 'student-submissions'
    and (storage.foldername(name))[1]::uuid = public.current_school_id()
    and exists (
      select 1 from public.submissions s
      where s.id = (storage.foldername(name))[2]::uuid
        and s.student_id = auth.uid()
        and s.status in ('draft', 'uploading')
    )
  );

-- ---------------------------------------------------------------------
-- processed-images: system-generated by the grading pipeline (service
-- role, bypasses RLS). Clients get read-only access.
-- ---------------------------------------------------------------------
create policy processed_images_select on storage.objects
  for select using (
    bucket_id = 'processed-images'
    and (storage.foldername(name))[1]::uuid = public.current_school_id()
    and exists (
      select 1 from public.submissions s
      join public.exams e on e.id = s.exam_id
      where s.id = (storage.foldername(name))[2]::uuid
        and (
          s.student_id = auth.uid()
          or (e.school_id = public.current_school_id() and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid())))
        )
    )
  );

-- ---------------------------------------------------------------------
-- grading-reports / exports: teacher/admin only, own school.
-- ---------------------------------------------------------------------
create policy grading_reports_staff_only on storage.objects
  for all using (
    bucket_id = 'grading-reports'
    and (storage.foldername(name))[1]::uuid = public.current_school_id()
    and (public.is_admin() or public.is_teacher())
  )
  with check (
    bucket_id = 'grading-reports'
    and (storage.foldername(name))[1]::uuid = public.current_school_id()
    and (public.is_admin() or public.is_teacher())
  );

create policy exports_staff_only on storage.objects
  for all using (
    bucket_id = 'exports'
    and (storage.foldername(name))[1]::uuid = public.current_school_id()
    and (public.is_admin() or public.is_teacher())
  )
  with check (
    bucket_id = 'exports'
    and (storage.foldername(name))[1]::uuid = public.current_school_id()
    and (public.is_admin() or public.is_teacher())
  );
