-- =========================================================================
-- Row Level Security
-- General rule (spec section 64):
--   admin   -> full read of own school's data
--   teacher -> own exams / assigned classes / their students / their grading
--   student -> own profile, own class, assigned exams (never draft), own
--              submissions, own results. NEVER: other students, answer
--              keys, correct answers, rubrics, teacher grading internals.
-- =========================================================================

alter table public.schools enable row level security;
alter table public.profiles enable row level security;
alter table public.classes enable row level security;
alter table public.class_students enable row level security;
alter table public.teacher_classes enable row level security;
alter table public.subjects enable row level security;
alter table public.grading_scales enable row level security;
alter table public.exams enable row level security;
alter table public.questions enable row level security;
alter table public.question_answers enable row level security;
alter table public.question_regions enable row level security;
alter table public.answer_keys enable row level security;
alter table public.exam_assignments enable row level security;
alter table public.submissions enable row level security;
alter table public.submission_pages enable row level security;
alter table public.extracted_answers enable row level security;
alter table public.grade_reviews enable row level security;
alter table public.notifications enable row level security;
alter table public.audit_logs enable row level security;

-- ---------------------------------------------------------------------
-- schools
-- Insert/delete are deliberately not granted to any client role: school
-- provisioning happens through a service-role Edge Function (onboarding),
-- since a brand-new school has no admin profile yet to authorize it.
-- ---------------------------------------------------------------------
create policy schools_select on public.schools
  for select using (id = public.current_school_id());

create policy schools_update_admin on public.schools
  for update using (id = public.current_school_id() and public.is_admin())
  with check (id = public.current_school_id() and public.is_admin());

-- ---------------------------------------------------------------------
-- profiles
-- ---------------------------------------------------------------------
create policy profiles_select on public.profiles
  for select using (
    id = auth.uid()
    or (school_id = public.current_school_id() and public.current_role() in ('admin', 'teacher'))
  );

create policy profiles_update_self_or_admin on public.profiles
  for update using (
    id = auth.uid()
    or (school_id = public.current_school_id() and public.is_admin())
  )
  with check (
    id = auth.uid()
    or (school_id = public.current_school_id() and public.is_admin())
  );

create policy profiles_insert_admin on public.profiles
  for insert with check (school_id = public.current_school_id() and public.is_admin());

create policy profiles_delete_admin on public.profiles
  for delete using (school_id = public.current_school_id() and public.is_admin());

-- ---------------------------------------------------------------------
-- classes / class_students / teacher_classes / subjects
-- ---------------------------------------------------------------------
create policy classes_select on public.classes
  for select using (school_id = public.current_school_id());

create policy classes_write_admin on public.classes
  for all using (school_id = public.current_school_id() and public.is_admin())
  with check (school_id = public.current_school_id() and public.is_admin());

create policy class_students_select on public.class_students
  for select using (
    exists (
      select 1 from public.classes c
      where c.id = class_students.class_id
        and c.school_id = public.current_school_id()
    )
    and (
      public.is_admin()
      or student_id = auth.uid()
      or exists (
        select 1 from public.teacher_classes tc
        where tc.class_id = class_students.class_id and tc.teacher_id = auth.uid()
      )
    )
  );

create policy class_students_write_admin on public.class_students
  for all using (
    public.is_admin()
    and exists (select 1 from public.classes c where c.id = class_students.class_id and c.school_id = public.current_school_id())
  )
  with check (
    public.is_admin()
    and exists (select 1 from public.classes c where c.id = class_students.class_id and c.school_id = public.current_school_id())
  );

create policy teacher_classes_select on public.teacher_classes
  for select using (
    exists (select 1 from public.classes c where c.id = teacher_classes.class_id and c.school_id = public.current_school_id())
    and (public.is_admin() or teacher_id = auth.uid())
  );

create policy teacher_classes_write_admin on public.teacher_classes
  for all using (
    public.is_admin()
    and exists (select 1 from public.classes c where c.id = teacher_classes.class_id and c.school_id = public.current_school_id())
  )
  with check (
    public.is_admin()
    and exists (select 1 from public.classes c where c.id = teacher_classes.class_id and c.school_id = public.current_school_id())
  );

create policy subjects_select on public.subjects
  for select using (school_id = public.current_school_id());

create policy subjects_write on public.subjects
  for all using (school_id = public.current_school_id() and (public.is_admin() or public.is_teacher()))
  with check (school_id = public.current_school_id() and (public.is_admin() or public.is_teacher()));

create policy grading_scales_select on public.grading_scales
  for select using (school_id = public.current_school_id());

create policy grading_scales_write_admin on public.grading_scales
  for all using (school_id = public.current_school_id() and public.is_admin())
  with check (school_id = public.current_school_id() and public.is_admin());

-- ---------------------------------------------------------------------
-- exams
-- Students may only see published-or-later exams they are assigned to.
-- Teachers see only their own exams. Admins see the whole school.
-- ---------------------------------------------------------------------
create policy exams_select on public.exams
  for select using (
    school_id = public.current_school_id()
    and (
      public.is_admin()
      or (public.is_teacher() and teacher_id = auth.uid())
      or (
        public.is_student()
        and status <> 'draft'
        and exists (
          select 1 from public.exam_assignments ea
          where ea.exam_id = exams.id and ea.student_id = auth.uid()
        )
      )
    )
  );

create policy exams_insert_teacher on public.exams
  for insert with check (
    school_id = public.current_school_id() and public.is_teacher() and teacher_id = auth.uid()
  );

create policy exams_update_owner on public.exams
  for update using (
    school_id = public.current_school_id()
    and (public.is_admin() or (public.is_teacher() and teacher_id = auth.uid()))
  )
  with check (
    school_id = public.current_school_id()
    and (public.is_admin() or (public.is_teacher() and teacher_id = auth.uid()))
  );

create policy exams_delete_owner on public.exams
  for delete using (
    school_id = public.current_school_id()
    and (public.is_admin() or (public.is_teacher() and teacher_id = auth.uid()))
  );

-- ---------------------------------------------------------------------
-- questions (public shape only — no answers)
-- ---------------------------------------------------------------------
create policy questions_select on public.questions
  for select using (
    exists (
      select 1 from public.exams e
      where e.id = questions.exam_id
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

create policy questions_write_owner on public.questions
  for all using (
    exists (
      select 1 from public.exams e
      where e.id = questions.exam_id
        and e.school_id = public.current_school_id()
        and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
    )
  )
  with check (
    exists (
      select 1 from public.exams e
      where e.id = questions.exam_id
        and e.school_id = public.current_school_id()
        and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
    )
  );

-- ---------------------------------------------------------------------
-- question_answers / question_regions / answer_keys
-- Teacher (owner) + admin ONLY. Deliberately no student-readable policy
-- exists on any of these tables — RLS defaults to deny.
-- ---------------------------------------------------------------------
create policy question_answers_owner_only on public.question_answers
  for all using (
    exists (
      select 1 from public.questions q
      join public.exams e on e.id = q.exam_id
      where q.id = question_answers.question_id
        and e.school_id = public.current_school_id()
        and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
    )
  )
  with check (
    exists (
      select 1 from public.questions q
      join public.exams e on e.id = q.exam_id
      where q.id = question_answers.question_id
        and e.school_id = public.current_school_id()
        and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
    )
  );

create policy question_regions_owner_only on public.question_regions
  for all using (
    exists (
      select 1 from public.questions q
      join public.exams e on e.id = q.exam_id
      where q.id = question_regions.question_id
        and e.school_id = public.current_school_id()
        and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
    )
  )
  with check (
    exists (
      select 1 from public.questions q
      join public.exams e on e.id = q.exam_id
      where q.id = question_regions.question_id
        and e.school_id = public.current_school_id()
        and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
    )
  );

create policy answer_keys_owner_only on public.answer_keys
  for all using (
    exists (
      select 1 from public.exams e
      where e.id = answer_keys.exam_id
        and e.school_id = public.current_school_id()
        and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
    )
  )
  with check (
    exists (
      select 1 from public.exams e
      where e.id = answer_keys.exam_id
        and e.school_id = public.current_school_id()
        and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
    )
  );

-- ---------------------------------------------------------------------
-- exam_assignments
-- ---------------------------------------------------------------------
create policy exam_assignments_select on public.exam_assignments
  for select using (
    student_id = auth.uid()
    or exists (
      select 1 from public.exams e
      where e.id = exam_assignments.exam_id
        and e.school_id = public.current_school_id()
        and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
    )
  );

create policy exam_assignments_write_owner on public.exam_assignments
  for all using (
    exists (
      select 1 from public.exams e
      where e.id = exam_assignments.exam_id
        and e.school_id = public.current_school_id()
        and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
    )
  )
  with check (
    exists (
      select 1 from public.exams e
      where e.id = exam_assignments.exam_id
        and e.school_id = public.current_school_id()
        and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
    )
  );

-- Student may update only the download-tracking fields on their own
-- assignment row (e.g. marking it downloaded). Enforced at app layer;
-- RLS just scopes which rows are reachable.
create policy exam_assignments_student_touch on public.exam_assignments
  for update using (student_id = auth.uid())
  with check (student_id = auth.uid());

-- ---------------------------------------------------------------------
-- submissions
-- ---------------------------------------------------------------------
create policy submissions_select on public.submissions
  for select using (
    student_id = auth.uid()
    or exists (
      select 1 from public.exams e
      where e.id = submissions.exam_id
        and e.school_id = public.current_school_id()
        and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
    )
  );

create policy submissions_insert_student on public.submissions
  for insert with check (
    student_id = auth.uid()
    and exists (
      select 1 from public.exam_assignments ea
      join public.exams e on e.id = ea.exam_id
      where ea.exam_id = submissions.exam_id
        and ea.student_id = auth.uid()
        and e.status = 'published'
    )
  );

-- Student may only touch their own submission while it is still in
-- progress; once it moves past 'uploaded' the grading pipeline (service
-- role) owns it.
create policy submissions_update_student on public.submissions
  for update using (student_id = auth.uid() and status in ('draft', 'uploading'))
  with check (student_id = auth.uid());

create policy submissions_update_teacher on public.submissions
  for update using (
    exists (
      select 1 from public.exams e
      where e.id = submissions.exam_id
        and e.school_id = public.current_school_id()
        and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
    )
  )
  with check (
    exists (
      select 1 from public.exams e
      where e.id = submissions.exam_id
        and e.school_id = public.current_school_id()
        and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
    )
  );

-- ---------------------------------------------------------------------
-- submission_pages
-- ---------------------------------------------------------------------
create policy submission_pages_select on public.submission_pages
  for select using (
    exists (
      select 1 from public.submissions s
      where s.id = submission_pages.submission_id
        and (
          s.student_id = auth.uid()
          or exists (
            select 1 from public.exams e
            where e.id = s.exam_id
              and e.school_id = public.current_school_id()
              and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
          )
        )
    )
  );

create policy submission_pages_write_student on public.submission_pages
  for all using (
    exists (
      select 1 from public.submissions s
      where s.id = submission_pages.submission_id
        and s.student_id = auth.uid()
        and s.status in ('draft', 'uploading')
    )
  )
  with check (
    exists (
      select 1 from public.submissions s
      where s.id = submission_pages.submission_id
        and s.student_id = auth.uid()
        and s.status in ('draft', 'uploading')
    )
  );

-- ---------------------------------------------------------------------
-- extracted_answers
-- Teacher/admin: always visible for their own exams.
-- Student: visible only once graded AND the exam settings allow a
-- question-level breakdown to be shown, AND the result has been released.
-- ---------------------------------------------------------------------
create policy extracted_answers_select_staff on public.extracted_answers
  for select using (
    exists (
      select 1 from public.submissions s
      join public.exams e on e.id = s.exam_id
      where s.id = extracted_answers.submission_id
        and e.school_id = public.current_school_id()
        and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
    )
  );

create policy extracted_answers_select_student on public.extracted_answers
  for select using (
    exists (
      select 1 from public.submissions s
      join public.exams e on e.id = s.exam_id
      where s.id = extracted_answers.submission_id
        and s.student_id = auth.uid()
        and s.status = 'graded'
        and s.result_visible = true
        and e.show_question_breakdown = true
    )
  );

-- Writes (AI grading pipeline) happen via service role in Edge Functions.
-- Teacher/admin may still update rows directly for manual overrides.
create policy extracted_answers_update_staff on public.extracted_answers
  for update using (
    exists (
      select 1 from public.submissions s
      join public.exams e on e.id = s.exam_id
      where s.id = extracted_answers.submission_id
        and e.school_id = public.current_school_id()
        and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
    )
  )
  with check (
    exists (
      select 1 from public.submissions s
      join public.exams e on e.id = s.exam_id
      where s.id = extracted_answers.submission_id
        and e.school_id = public.current_school_id()
        and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
    )
  );

-- ---------------------------------------------------------------------
-- grade_reviews (audit trail of manual overrides)
-- ---------------------------------------------------------------------
create policy grade_reviews_select on public.grade_reviews
  for select using (
    exists (
      select 1 from public.submissions s
      join public.exams e on e.id = s.exam_id
      where s.id = grade_reviews.submission_id
        and (
          (s.student_id = auth.uid() and s.result_visible = true)
          or (e.school_id = public.current_school_id() and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid())))
        )
    )
  );

create policy grade_reviews_insert_staff on public.grade_reviews
  for insert with check (
    reviewed_by = auth.uid()
    and exists (
      select 1 from public.submissions s
      join public.exams e on e.id = s.exam_id
      where s.id = grade_reviews.submission_id
        and e.school_id = public.current_school_id()
        and (public.is_admin() or (public.is_teacher() and e.teacher_id = auth.uid()))
    )
  );

-- ---------------------------------------------------------------------
-- notifications — each user only ever sees their own.
-- Inserts are done by Edge Functions/triggers via service role.
-- ---------------------------------------------------------------------
create policy notifications_select_own on public.notifications
  for select using (user_id = auth.uid());

create policy notifications_update_own on public.notifications
  for update using (user_id = auth.uid())
  with check (user_id = auth.uid());

-- ---------------------------------------------------------------------
-- audit_logs — admin read-only. Writes are service-role only.
-- ---------------------------------------------------------------------
create policy audit_logs_select_admin on public.audit_logs
  for select using (school_id = public.current_school_id() and public.is_admin());
