// Supabase Edge Function: create-user
//
// Admin-only account provisioning (spec section 4.1 / 5: "school-generated
// student accounts", "optional teacher accounts"). Creates the auth user
// with a generated temporary password, inserts the matching profile row,
// and (for students) enrolls them into a class. Must run server-side: it
// needs the service-role key to call the Auth Admin API, which must never
// reach the Android app.
import { createClient } from "npm:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL")!;
const SERVICE_ROLE_KEY = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const ANON_KEY = (Deno.env.get("SUPABASE_ANON_KEY") || Deno.env.get("PUBLISHABLE_KEY"))!;

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "POST only" }, 405);

  let body: { email?: string; full_name?: string; role?: string; class_id?: string };
  try {
    body = await req.json();
  } catch {
    return json({ error: "Invalid JSON body" }, 400);
  }

  const { email, full_name, role, class_id } = body;
  if (!email || !full_name || !role) {
    return json({ error: "email, full_name and role are required" }, 400);
  }
  if (role !== "teacher" && role !== "student") {
    return json({ error: "role must be teacher or student" }, 400);
  }

  const authHeader = req.headers.get("Authorization");
  if (!authHeader) return json({ error: "Missing authorization" }, 401);

  const callerId = getUserIdFromJwt(authHeader);
  if (!callerId) return json({ error: "Invalid token" }, 401);

  const callerClient = createClient(SUPABASE_URL, ANON_KEY, {
    global: { headers: { Authorization: authHeader } },
  });
  const { data: caller, error: callerErr } = await callerClient
    .from("profiles")
    .select("id, school_id, role")
    .eq("id", callerId)
    .single();

  if (callerErr || !caller || caller.role !== "admin") {
    return json({ error: "Only an admin may create users" }, 403);
  }

  const supabase = createClient(SUPABASE_URL, SERVICE_ROLE_KEY);
  const tempPassword = randomPassword();

  const { data: created, error: createErr } = await supabase.auth.admin.createUser({
    email,
    password: tempPassword,
    email_confirm: true,
  });

  if (createErr || !created?.user) {
    return json({ error: createErr?.message ?? "Failed to create auth user" }, 400);
  }

  const { error: profileErr } = await supabase.from("profiles").insert({
    id: created.user.id,
    school_id: caller.school_id,
    role,
    full_name,
    email,
  });

  if (profileErr) {
    // Roll back the orphaned auth user so a retry doesn't collide on email.
    await supabase.auth.admin.deleteUser(created.user.id);
    return json({ error: profileErr.message }, 500);
  }

  let warning: string | undefined;
  if (role === "student" && class_id) {
    const { error: enrollErr } = await supabase
      .from("class_students")
      .insert({ class_id, student_id: created.user.id });
    if (enrollErr) warning = `User created but class enrollment failed: ${enrollErr.message}`;
  }

  return json({ user_id: created.user.id, temp_password: tempPassword, warning }, 200);
});

function randomPassword(): string {
  const bytes = new Uint8Array(12);
  crypto.getRandomValues(bytes);
  const b64 = btoa(String.fromCharCode(...bytes)).replace(/[+/=]/g, "");
  return `${b64.slice(0, 14)}A9!`;
}

function getUserIdFromJwt(authHeader: string): string | null {
  try {
    const token = authHeader.replace(/^Bearer\s+/i, "");
    const payloadSegment = token.split(".")[1];
    const normalized = payloadSegment.replace(/-/g, "+").replace(/_/g, "/");
    const decoded = JSON.parse(atob(normalized));
    return typeof decoded.sub === "string" ? decoded.sub : null;
  } catch {
    return null;
  }
}

function json(body: unknown, status: number): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}
